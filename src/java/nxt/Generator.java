/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt;
//seen.
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Listener;
import nxt.util.Listeners;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import nxt.Account;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public final class Generator implements Comparable<Generator> {

    public enum Event {
        GENERATION_DEADLINE, START_FORGING, STOP_FORGING
    }

    private static final int MAX_FORGERS = Taelium.getIntProperty("tael.maxNumberOfForgers");
    private static final byte[] fakeForgingPublicKey = Taelium.getBooleanProperty("nxt.enableFakeForging") ?
            Account.getPublicKey(Convert.parseAccountId(Taelium.getStringProperty("nxt.fakeForgingAccount"))) : null;

    private static final Listeners<Generator,Event> listeners = new Listeners<>();

    private static final ConcurrentMap<String, Generator> generators = new ConcurrentHashMap<>();
    private static final Collection<Generator> allGenerators = Collections.unmodifiableCollection(generators.values());
    private static volatile List<Generator> sortedForgers = null;
    private static long lastBlockId;
    private static int delayTime = Constants.FORGING_DELAY;

    private static final Runnable generateBlocksThread = new Runnable() {

        private volatile boolean logged;

        @Override
        public void run() {

            try {
                try {
                    BlockchainImpl.getInstance().updateLock();
                    try {
                        Block lastBlock = Taelium.getBlockchain().getLastBlock();
                        if (lastBlock == null || lastBlock.getHeight() < Constants.LAST_KNOWN_BLOCK) {
                            return;
                        }
                        final int generationLimit = Taelium.getEpochTime() - delayTime;
                        if (lastBlock.getId() != lastBlockId || sortedForgers == null) {
                            lastBlockId = lastBlock.getId();
                            if (lastBlock.getTimestamp() > Taelium.getEpochTime() - 600) {
                                Block previousBlock = Taelium.getBlockchain().getBlock(lastBlock.getPreviousBlockId());
                                for (Generator generator : generators.values()) {
                                    generator.setLastBlock(previousBlock);
                                    int timestamp = generator.getTimestamp(generationLimit);
                                    if (timestamp != generationLimit && generator.getHitTime() > 0 && timestamp < lastBlock.getTimestamp()) {
                                    	//if generator's timestamp earlier than current highest block
                                        Logger.logDebugMessage("Pop off: " + generator.toString() + " will pop off last block " + lastBlock.getStringId());
                                        Logger.logDebugMessage("Pop off due to generator timestamp earlier than current highest block.");
                                        List<BlockImpl> poppedOffBlock = BlockchainProcessorImpl.getInstance().popOffTo(previousBlock);
                                        for (BlockImpl block : poppedOffBlock) {
                                            TransactionProcessorImpl.getInstance().processLater(block.getTransactions());
                                        }
                                        lastBlock = previousBlock;
                                        lastBlockId = previousBlock.getId();
                                        break;
                                    }
                                }
                            }
                            List<Generator> forgers = new ArrayList<>();
                            for (Generator generator : generators.values()) {
                                generator.setLastBlock(lastBlock);
                                if (generator.effectiveBalance.signum() > 0) {//can forge if effective balance greater than 0.
                                    forgers.add(generator);
                                }
                            }
                            Collections.sort(forgers);
                            sortedForgers = Collections.unmodifiableList(forgers);
                            logged = false;
                        }
                        if (!logged) {
                            for (Generator generator : sortedForgers) {
                                if (generator.getHitTime() - generationLimit > 60) {
                                    break;
                                }
//                                Logger.logDebugMessage(generator.toString());
                                logged = true;
                            }
                        }
                        for (Generator generator : sortedForgers) {
                            if (generator.getHitTime() > generationLimit || generator.forge(lastBlock, generationLimit)) {
//                            		Logger.logDebugMessage("Generator ID: " + Crypto.rsEncode(generator.accountId));
                                return;
                            }
                        }
                    } finally {
                        BlockchainImpl.getInstance().updateUnlock();
                    }
                } catch (Exception e) {
                    Logger.logMessage("Error in block generation thread", e);
                }
            } catch (Throwable t) {
                Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
                t.printStackTrace();
                System.exit(1);
            }

        }

    };
    // end of Runnable generateBlocksThread 
    static {
        if (!Constants.isLightClient) {
            ThreadPool.scheduleThread("GenerateBlocks", generateBlocksThread, 500, TimeUnit.MILLISECONDS);
        }
    }

    static void init() {}

    public static boolean addListener(Listener<Generator> listener, Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Generator> listener, Event eventType) {
        return listeners.removeListener(listener, eventType);
    }

    public static Generator startForging(String secretPhrase) {
        if (generators.size() >= MAX_FORGERS) {
            throw new RuntimeException("Cannot forge with more than " + MAX_FORGERS + " accounts on the same node");
        }
        Generator generator = new Generator(secretPhrase);
        Generator old = generators.putIfAbsent(secretPhrase, generator);
        if (old != null) {
            Logger.logDebugMessage(old + " is already forging");
            return old; //if generator with same secretPhrase is already forging, return.
        }
        listeners.notify(generator, Event.START_FORGING);
        Logger.logDebugMessage(generator + " started");
        return generator;
    }

    public static Generator stopForging(String secretPhrase) {
        Generator generator = generators.remove(secretPhrase);
        if (generator != null) {
            Taelium.getBlockchain().updateLock();
            try {
                sortedForgers = null;
            } finally {
                Taelium.getBlockchain().updateUnlock();
            }
            Logger.logDebugMessage(generator + " stopped");
            listeners.notify(generator, Event.STOP_FORGING);
        }
        return generator;
    }

    public static int stopForging() {
        int count = generators.size();
        Iterator<Generator> iter = generators.values().iterator();
        while (iter.hasNext()) {
            Generator generator = iter.next();
            iter.remove();
            Logger.logDebugMessage(generator + " stopped");
            listeners.notify(generator, Event.STOP_FORGING);
        }
        Taelium.getBlockchain().updateLock();
        try {
            sortedForgers = null;
        } finally {
            Taelium.getBlockchain().updateUnlock();
        }
        return count;
    }

    public static Generator getGenerator(String secretPhrase) {
        return generators.get(secretPhrase);
    }

    public static int getGeneratorCount() {
        return generators.size();
    }

    public static Collection<Generator> getAllGenerators() {
        return allGenerators;
    }

    public static List<Generator> getSortedForgers() {
        List<Generator> forgers = sortedForgers;
        
        return forgers == null ? Collections.emptyList() : forgers;
    }
    
    public static Set<Long> getLocalForgerIds() {
    		
    		Set<Long> localForgerIds = new HashSet<Long>();
    		
    		Logger.logDebugMessage("sortedForgers: " + sortedForgers);
//    		Logger.logDebugMessage("1st element: " + sortedForgers.get(0).getAccountId());
    		
    		if ((sortedForgers != null) && (!sortedForgers.isEmpty())) {
	    		for (Generator eachForger : sortedForgers ) {
	    			localForgerIds.add(eachForger.accountId);
	    		}
	    		return localForgerIds;
    		}
    		else {
    			return Collections.emptySet();
    		}
//    		return localForgerIds==null ? Collections.emptySet() : localForgerIds;
//    		return localForgerIds;
    }
    
    public static Map<Long, BigInteger> getLocalForgerBalanceMap(){
    		Map<Long, BigInteger> forgerBalanceMap = new HashMap<>(); 
    		// Hash map no duplicates. Duplicate entries, latest overwrites earlier.
    		if ((sortedForgers != null) && (!sortedForgers.isEmpty())) {
	        for (Generator eachForger : sortedForgers) {
	        		Account account = Account.getAccount(eachForger.accountId);
	        		forgerBalanceMap.put(eachForger.accountId, account.getBalanceNQT() );
	        }
	        return forgerBalanceMap;
        }
        else {
        		return Collections.emptyMap();
        }
//        return forgerBalanceMap == null ? Collections.emptyMap() : forgerBalanceMap;
    }

    public static long getNextHitTime(long lastBlockId, int curTime) {
        BlockchainImpl.getInstance().readLock();
        try {
            if (lastBlockId == Generator.lastBlockId && sortedForgers != null) {
                for (Generator generator : sortedForgers) {
                    if (generator.getHitTime() >= curTime - Constants.FORGING_DELAY) {
                        return generator.getHitTime();
                    }
                }
            }
            return 0;
        } finally {
            BlockchainImpl.getInstance().readUnlock();
        }
    }

    static void setDelay(int delay) {
        Generator.delayTime = delay;
    }

    static boolean verifyHit(BigInteger hit, BigInteger effectiveBalance, Block previousBlock, int timestamp) {
        int elapsedTime = timestamp - previousBlock.getTimestamp();
        if (elapsedTime <= 0) {
            return false;
        }
        BigInteger effectiveBaseTarget = previousBlock.getBaseTarget().multiply(effectiveBalance);
        BigInteger prevTarget = effectiveBaseTarget.multiply(BigInteger.valueOf(elapsedTime - 1));
        BigInteger target = prevTarget.add(effectiveBaseTarget);
//        
//        Logger.logDebugMessage("--------------------------------");
//        Logger.logDebugMessage("prev block base target: " + previousBlock.getBaseTarget().toString());
//        Logger.logDebugMessage("effBaseTarget: " + effectiveBaseTarget.toString());
//        Logger.logDebugMessage("prevTarget: " + prevTarget.toString());
//        Logger.logDebugMessage("target: " + target.toString());
//        Logger.logDebugMessage("hit: " + hit.toString());
//        Logger.logDebugMessage("elapsedTime: " + elapsedTime);
//        
//        
//        Logger.logDebugMessage("--------------------------------");
        
        return hit.compareTo(target) < 0
                && (hit.compareTo(prevTarget) >= 0  
                || (Constants.isTestnet ? elapsedTime > 300 : elapsedTime > 3600)
                || Constants.isOffline);
    }

    static boolean allowsFakeForging(byte[] publicKey) {
        return Constants.isTestnet && publicKey != null && Arrays.equals(publicKey, fakeForgingPublicKey);
    } //can only fake forge on tesnet

    static BigInteger getHit(byte[] publicKey, Block block) {
        if (allowsFakeForging(publicKey)) {
            return BigInteger.ZERO;
        }
        MessageDigest digest = Crypto.sha256();
        digest.update(block.getGenerationSignature());
        byte[] generationSignatureHash = digest.digest(publicKey);
        return new BigInteger(1, new byte[] {generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});
    }

    static long getHitTime(BigInteger effectiveBalance, BigInteger hit, Block block) {
        return block.getTimestamp()
                + hit.divide(block.getBaseTarget().multiply(effectiveBalance)).longValue();
    }


    private final long accountId;
    private final String secretPhrase;
    private final byte[] publicKey;
    private volatile long hitTime;
    private volatile BigInteger hit;
    private volatile BigInteger effectiveBalance;
    private volatile long deadline;

    private Generator(String secretPhrase) {
        this.secretPhrase = secretPhrase;
        this.publicKey = Crypto.getPublicKey(secretPhrase);
        this.accountId = Account.getId(publicKey);
        Taelium.getBlockchain().updateLock();
        try {
            if (Taelium.getBlockchain().getHeight() >= Constants.LAST_KNOWN_BLOCK) {
                setLastBlock(Taelium.getBlockchain().getLastBlock());
            }
            sortedForgers = null;
        } finally {
            Taelium.getBlockchain().updateUnlock();
        }
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getDeadline() {
        return deadline;
    }

    public long getHitTime() {
        return hitTime;
    }

    @Override
    public int compareTo(Generator g) {
        int i = this.hit.multiply(g.effectiveBalance).compareTo(g.hit.multiply(this.effectiveBalance));
        if (i != 0) {
            return i;
        }
        return Long.compare(accountId, g.accountId);
    }

    @Override
    public String toString() {
        return "Forger " + Crypto.rsEncode(accountId);
    }

    private void setLastBlock(Block lastBlock) {
        int height = lastBlock.getHeight();
        Account account = Account.getAccount(accountId, height);
        if (account == null) {
            effectiveBalance = BigInteger.ZERO;
        } else {
            effectiveBalance = account.getEffectiveBalanceNXT(height).compareTo(BigInteger.ZERO) > 0 ? 
            		account.getEffectiveBalanceNXT(height):
            				BigInteger.ZERO;
            		
//            		BigInteger.valueOf(Math.max(account.getEffectiveBalanceNXT(height), 0));
        }
        if (effectiveBalance.signum() == 0) {
            hitTime = 0;
            hit = BigInteger.ZERO;
            return;
        }
        hit = getHit(publicKey, lastBlock);
        hitTime = getHitTime(effectiveBalance, hit, lastBlock);
        deadline = Math.max(hitTime - lastBlock.getTimestamp(), 0);
        listeners.notify(this, Event.GENERATION_DEADLINE);
    }

    boolean forge(Block lastBlock, int generationLimit) throws BlockchainProcessor.BlockNotAcceptedException {
        int timestamp = getTimestamp(generationLimit);
        
        byte[] pbKey = Crypto.getPublicKey(secretPhrase);
        long acctId = Account.getId(pbKey);
        
//        Logger.logDebugMessage(Crypto.rsEncode(acctId) + " is forging...");
        
        
        if (!verifyHit(hit, effectiveBalance, lastBlock, timestamp)) {
//            Logger.logDebugMessage(this.toString() + " failed to forge at " + timestamp + " height " + lastBlock.getHeight() + " last timestamp " + lastBlock.getTimestamp());
            return false;
        }
        // means verifyHit returned True!
        int start = Taelium.getEpochTime();
        while (true) {
            try { //successful forging
            		//insert reward to forger here.
            		BlockchainProcessorImpl.getInstance().generateBlock(secretPhrase, timestamp);
                setDelay(Constants.FORGING_DELAY);//20s delay. (wait 15s to allow more tx to be in the block.)
                return true;
            } catch (BlockchainProcessor.TransactionNotAcceptedException e) {
                // the bad transaction has been expunged, try again
                if (Taelium.getEpochTime() - start > 10) { // give up after trying for 10 s
                    throw e;
                }
            }
        }
    }

    private int getTimestamp(int generationLimit) {
        return (generationLimit - hitTime > 3600) ? generationLimit : (int)hitTime + 1;
    }

    /** Active block generators */
    private static final Set<Long> activeGeneratorIds = new HashSet<>();

    /** Active block identifier */
    private static long activeBlockId;

    /** Sorted list of generators for the next block */
    private static final List<ActiveGenerator> activeGenerators = new ArrayList<>();

    /** Generator list has been initialized */
    private static boolean generatorsInitialized = false;

    /**
     * Return a list of generators for the next block.  The caller must hold the blockchain
     * read lock to ensure the integrity of the returned list.
     *
     * @return                      List of generator account identifiers
     */
    public static List<ActiveGenerator> getNextGenerators() {
        List<ActiveGenerator> generatorList;
        Blockchain blockchain = Taelium.getBlockchain();
        synchronized(activeGenerators) {
            if (!generatorsInitialized) {
                activeGeneratorIds.addAll(BlockDb.getBlockGenerators(Math.max(1, blockchain.getHeight() - 10000)));
                //get generators from last 1000 blocks.
                activeGeneratorIds.forEach(activeGeneratorId -> activeGenerators.add(new ActiveGenerator(activeGeneratorId)));
                // for each of these generators, append to activeGenerators(starts out empty) the id.
                //so activeGenerators is now the last 1000 generator ids.
                Logger.logDebugMessage(activeGeneratorIds.size() + " block generators found");
                Taelium.getBlockchainProcessor().addListener(block -> {
                    long generatorId = block.getGeneratorId();
                    synchronized(activeGenerators) {
                        if (!activeGeneratorIds.contains(generatorId)) {
                            activeGeneratorIds.add(generatorId);
                            activeGenerators.add(new ActiveGenerator(generatorId));
                        }
                    }
                }, BlockchainProcessor.Event.BLOCK_PUSHED);
                generatorsInitialized = true;
            }
            long blockId = blockchain.getLastBlock().getId();
            if (blockId != activeBlockId) {
                activeBlockId = blockId;
                Block lastBlock = blockchain.getLastBlock();
                for (ActiveGenerator generator : activeGenerators) {
                    generator.setLastBlock(lastBlock);
                }
                Collections.sort(activeGenerators);
            }
            generatorList = new ArrayList<>(activeGenerators);
        }
        return generatorList;
    }

    /**
     * Active generator
     */
    public static class ActiveGenerator implements Comparable<ActiveGenerator> {
        private final long accountId;
        private long hitTime;
        private BigInteger effectiveBalanceNXT;
        private byte[] publicKey;

        public ActiveGenerator(long accountId) {
            this.accountId = accountId;
            this.hitTime = Long.MAX_VALUE;
        }

        public long getAccountId() {
            return accountId;
        }

        public BigInteger getEffectiveBalance() {
            return effectiveBalanceNXT;
        }

        public long getHitTime() {
            return hitTime;
        }

        private void setLastBlock(Block lastBlock) {
            if (publicKey == null) {
                publicKey = Account.getPublicKey(accountId);
                if (publicKey == null) {
                    hitTime = Long.MAX_VALUE;
                    return;
                }
            }
            int height = lastBlock.getHeight();
            Account account = Account.getAccount(accountId, height);
            if (account == null) {
                hitTime = Long.MAX_VALUE;
                return;
            }
            effectiveBalanceNXT = account.getEffectiveBalanceNXT(height).compareTo(BigInteger.ZERO) > 0 ? 
            		account.getEffectiveBalanceNXT(height):
            				BigInteger.ZERO;
//            = Math.max(account.getEffectiveBalanceNXT(height), 0);
            if (effectiveBalanceNXT.compareTo(BigInteger.ZERO) == 0) {
                hitTime = Long.MAX_VALUE;
                return;
            }
            
//            Logger.logDebugMessage("effBalNXT: " + effectiveBalanceNXT.toString());
//            BigInteger effectiveBalance = BigInteger.valueOf(effectiveBalanceNXT);
            BigInteger hit = Generator.getHit(publicKey, lastBlock);
            hitTime = Generator.getHitTime(effectiveBalanceNXT, hit, lastBlock);
        }

        @Override
        public int hashCode() {
            return Long.hashCode(accountId);
        }

        @Override
        public boolean equals(Object obj) {
            return (obj != null && (obj instanceof ActiveGenerator) && accountId == ((ActiveGenerator)obj).accountId);
        }

        @Override
        public int compareTo(ActiveGenerator obj) {
            return (hitTime < obj.hitTime ? -1 : (hitTime > obj.hitTime ? 1 : 0));
        }
    }
}
