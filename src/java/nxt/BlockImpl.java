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
import nxt.AccountLedger.LedgerEvent;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.Logger;
import nxt.GetAllForgersBalances;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

final class BlockImpl implements Block {

    private final int version;
    private final int timestamp;
    private final long previousBlockId;
    private volatile byte[] generatorPublicKey;
    private final byte[] previousBlockHash;
    private final BigInteger totalAmountNQT;
    private final BigInteger totalFeeNQT;
    private final int payloadLength;
    private final byte[] generationSignature;
    private final byte[] payloadHash;
    private volatile List<TransactionImpl> blockTransactions;

    private byte[] blockSignature;
    private BigInteger cumulativeDifficulty = BigInteger.ZERO;
    private BigInteger baseTarget = Constants.INITIAL_BASE_TARGET;
    private volatile long nextBlockId;
    private int height = -1;
    private volatile long id;
    private volatile String stringId = null;
    private volatile long generatorId;
    private volatile byte[] bytes = null;
    private BigInteger totalForgingHoldings = BigInteger.ZERO;
    private double latestRYear = 0;
    private BigInteger supplyCurrent = BigInteger.ZERO;
    private BigInteger blockReward = BigInteger.ZERO;
    private Date date = NtpTime.getCurrentDate();
    private Boolean firstBlockOfDay = false;
    
    

    //this is used for genesis block only!
    BlockImpl(byte[] generatorPublicKey, byte[] generationSignature) { 
        this(-1, 0, 0, BigInteger.ZERO, BigInteger.ZERO, 0, new byte[32], generatorPublicKey, generationSignature, new byte[64],
                new byte[32], Collections.emptyList(), new Date(Genesis.EPOCH_BEGINNING), BigInteger.ZERO, 
                0.0, Constants.INITIAL_BALANCE_HAEDS, BigInteger.ZERO, false);
        this.height = 0;
    }
    
    // used in generateblock. used before blockSignature is generated.
    BlockImpl(int version, int timestamp, long previousBlockId, BigInteger totalAmountNQT, BigInteger totalFeeNQT, int payloadLength, byte[] payloadHash,
              byte[] generatorPublicKey, byte[] generationSignature, byte[] previousBlockHash, List<TransactionImpl> transactions, 
              String secretPhrase, Date date, BigInteger totalForgingHoldings, double rYear, BigInteger supplyCurrent, 
              BigInteger blockReward, Boolean firstBlockOfDay) {
        this(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash,
                generatorPublicKey, generationSignature, null, previousBlockHash, transactions, date, totalForgingHoldings,
                rYear, supplyCurrent, blockReward, firstBlockOfDay);
        
        
        blockSignature = Crypto.sign(bytes(), secretPhrase);
        bytes = null;
    }
    
    //used in the other BlockImpls and in parseBlock/ All in this file. used after blockSignature is generated.
    BlockImpl(int version, int timestamp, long previousBlockId, BigInteger totalAmountNQT, BigInteger totalFeeNQT, int payloadLength, byte[] payloadHash,
              byte[] generatorPublicKey, byte[] generationSignature, byte[] blockSignature, 
              byte[] previousBlockHash, List<TransactionImpl> transactions, Date date, BigInteger totalForgingHoldings,
              double latestRYear, BigInteger supplyCurrent, BigInteger blockReward, Boolean firstBlockOfDay) {
        this.version = version;
        this.timestamp = timestamp;
        this.previousBlockId = previousBlockId;
        this.totalAmountNQT = totalAmountNQT;
        this.totalFeeNQT = totalFeeNQT;
        this.payloadLength = payloadLength;
        this.payloadHash = payloadHash;
        this.generatorPublicKey = generatorPublicKey;
        this.generationSignature = generationSignature;
        this.blockSignature = blockSignature;
        this.previousBlockHash = previousBlockHash;
        if (transactions != null) {
            this.blockTransactions = Collections.unmodifiableList(transactions);
        }
        this.date = date;
        this.totalForgingHoldings = totalForgingHoldings;
        this.latestRYear = latestRYear;
        this.supplyCurrent = supplyCurrent;
        this.blockReward = blockReward;
        this.firstBlockOfDay = firstBlockOfDay;
        
//        Logger.logDebugMessage("***** supplyCurrent: " + this.supplyCurrent);
        
    }
    // used in loadBlock.
    BlockImpl(int version, int timestamp, long previousBlockId, BigInteger totalAmountNQT, BigInteger totalFeeNQT, int payloadLength,
              byte[] payloadHash, long generatorId, byte[] generationSignature, byte[] blockSignature,
              byte[] previousBlockHash, BigInteger cumulativeDifficulty, BigInteger baseTarget, long nextBlockId, int height, long id,
              List<TransactionImpl> blockTransactions, BigInteger totalForgingHoldings, 
              double latestRYear, BigInteger supplyCurrent, BigInteger blockReward, Date date, Boolean firstBlockOfDay) {
    	
        this(version, timestamp, previousBlockId, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash,
                null, generationSignature, blockSignature, previousBlockHash, null, date, totalForgingHoldings,
                latestRYear, supplyCurrent, blockReward, firstBlockOfDay);
        this.cumulativeDifficulty = cumulativeDifficulty;
        this.baseTarget = baseTarget;
        this.nextBlockId = nextBlockId;
        this.height = height;
        this.id = id;
        this.generatorId = generatorId;
        this.blockTransactions = blockTransactions;
//        this.latestRYear = latestRYear;
//        this.supplyCurrent = supplyCurrent;
//        this.vault = vault;
//        this.blockReward = blockReward;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public long getPreviousBlockId() {
        return previousBlockId;
    }

    @Override
    public byte[] getGeneratorPublicKey() {
        if (generatorPublicKey == null) {
            generatorPublicKey = Account.getPublicKey(generatorId);
        }
        return generatorPublicKey;
    }

    @Override
    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }

    @Override
    public BigInteger getTotalAmountNQT() {
        return totalAmountNQT;
    }

    @Override
    public BigInteger getTotalFeeNQT() {
        return totalFeeNQT;
    }

    @Override
    public int getPayloadLength() {
        return payloadLength;
    }

    @Override
    public byte[] getPayloadHash() {
        return payloadHash;
    }

    @Override
    public byte[] getGenerationSignature() {
        return generationSignature;
    }

    @Override
    public byte[] getBlockSignature() {
        return blockSignature;
    }

    @Override
    public List<TransactionImpl> getTransactions() {
        if (this.blockTransactions == null) {
            List<TransactionImpl> transactions = Collections.unmodifiableList(TransactionDb.findBlockTransactions(getId()));
            for (TransactionImpl transaction : transactions) {
                transaction.setBlock(this);
            }
            this.blockTransactions = transactions;
        }
        return this.blockTransactions;
    }

    @Override
    public BigInteger getBaseTarget() {
        return baseTarget;
    }

    @Override
    public BigInteger getCumulativeDifficulty() {
        return cumulativeDifficulty;
    }
    
    @Override
    public BigInteger getTotalForgingHoldings() {
        return totalForgingHoldings;
    }
    
    @Override
    public double getLatestRYear() {
        return latestRYear;
    }
    
    @Override
    public BigInteger getSupplyCurrent() {
        return supplyCurrent;
    }
    

    
    @Override
    public BigInteger getBlockReward() {
        return blockReward;
    }
    
    @Override
	public boolean getFirstBlockOfDay() {
		// TODO Auto-generated method stub
		return firstBlockOfDay;
	}
    
    @Override
	public Date getDate() {
    		return date;
    }
    
    @Override
    public long getNextBlockId() {
        return nextBlockId;
    }

    void setNextBlockId(long nextBlockId) {
        this.nextBlockId = nextBlockId;
    }

    @Override
    public int getHeight() {
        if (height == -1) {
            throw new IllegalStateException("Block height not yet set");
        }
        return height;
    }

    @Override
    public long getId() {
        if (id == 0) {
            if (blockSignature == null) {
                throw new IllegalStateException("Block is not signed yet");
            }
            byte[] hash = Crypto.sha256().digest(bytes());
            BigInteger bigInteger = new BigInteger(1, new byte[] {hash[7], hash[6], hash[5], hash[4], hash[3], hash[2], hash[1], hash[0]});
            id = bigInteger.longValue();
            stringId = bigInteger.toString();
        }
        return id;
    }

    @Override
    public String getStringId() {
        if (stringId == null) {
            getId();
            if (stringId == null) {
                stringId = Long.toUnsignedString(id);
            }
        }
        return stringId;
    }

    @Override
    public long getGeneratorId() {
        if (generatorId == 0) {
            generatorId = Account.getId(getGeneratorPublicKey());
        }
        return generatorId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BlockImpl && this.getId() == ((BlockImpl)o).getId();
    }

    @Override
    public int hashCode() { //getId() returns 64-bit long.
        return (int)(getId() ^ (getId() >>> 32));
    }

    @Override
    public JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        json.put("version", version);
        json.put("timestamp", timestamp);
        json.put("previousBlock", Long.toUnsignedString(previousBlockId));
        json.put("totalAmountNQT", totalAmountNQT.toString());
        json.put("totalFeeNQT", totalFeeNQT.toString());
        json.put("payloadLength", payloadLength);
        json.put("payloadHash", Convert.toHexString(payloadHash));
        json.put("generatorPublicKey", Convert.toHexString(getGeneratorPublicKey()));
        json.put("generationSignature", Convert.toHexString(generationSignature));
        json.put("previousBlockHash", Convert.toHexString(previousBlockHash));
        json.put("blockSignature", Convert.toHexString(blockSignature));
        JSONArray transactionsData = new JSONArray();
        getTransactions().forEach(transaction -> transactionsData.add(transaction.getJSONObject()));
        json.put("transactions", transactionsData);
        json.put("date", NtpTime.toString(date));
        json.put("totalForgingHoldings", totalForgingHoldings.toString());
        json.put("latestRYear", BigDecimal.valueOf(latestRYear).toString());
        json.put("supplyCurrent", supplyCurrent.toString());
        json.put("blockReward", blockReward.toString());
        json.put("firstBlockOfDay", firstBlockOfDay.toString());
       
//        Logger.logDebugMessage("/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/");
//        Logger.logDebugMessage("/\\/\\/\\/\\     PUTTING BLOCK     \\/\\/\\/\\/\\");
//        Logger.logDebugMessage("Effective Balances at Height: " + height);
//        Logger.logDebugMessage("Block ID: " + id);
//        
//        Logger.logDebugMessage("Timestamp: " + timestamp);
//        Logger.logDebugMessage("DATE: " + NtpTime.toString(date));
//        Logger.logDebugMessage("Date2: " + date.toString());
//        Logger.logDebugMessage("Date3: " + date.getTime());
//        Logger.logDebugMessage("Supply Current: " + supplyCurrent);
//        Logger.logDebugMessage("Latest R Year: " + latestRYear);
//        Logger.logDebugMessage("firstBlockOfDay: " + firstBlockOfDay);
        

//        Logger.logDebugMessage("/\\/\\/\\/\\/\\   END PUTTING BLOCK   /\\/\\/\\/\\/");
//        Logger.logDebugMessage("/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/");
        
        
        
        return json;
    }

    static BlockImpl parseBlock(JSONObject blockData) throws NxtException.NotValidException {
        try {
            int version = ((Long) blockData.get("version")).intValue();
            int timestamp = ((Long) blockData.get("timestamp")).intValue();
            long previousBlock = Convert.parseUnsignedLong((String) blockData.get("previousBlock"));
            BigInteger totalAmountNQT = new BigInteger((String)blockData.get("totalAmountNQT"));
            BigInteger totalFeeNQT = new BigInteger((String)blockData.get("totalFeeNQT"));
            int payloadLength = ((Long) blockData.get("payloadLength")).intValue();
            byte[] payloadHash = Convert.parseHexString((String) blockData.get("payloadHash"));
            byte[] generatorPublicKey = Convert.parseHexString((String) blockData.get("generatorPublicKey"));
            byte[] generationSignature = Convert.parseHexString((String) blockData.get("generationSignature"));
            byte[] blockSignature = Convert.parseHexString((String) blockData.get("blockSignature"));
            byte[] previousBlockHash = version == 1 ? null : Convert.parseHexString((String) blockData.get("previousBlockHash"));
            List<TransactionImpl> blockTransactions = new ArrayList<>();
            for (Object transactionData : (JSONArray) blockData.get("transactions")) {
                blockTransactions.add(TransactionImpl.parseTransaction((JSONObject) transactionData));
            }
            Date date = NtpTime.toDate((String)blockData.get("date"));
            BigInteger totalForgingHoldings = new BigInteger((String)blockData.get("totalForgingHoldings"));
            BigInteger blockReward = new BigInteger((String)blockData.get("blockReward"));
            BigInteger supplyCurrent = new BigInteger((String)blockData.get("supplyCurrent"));
            BigDecimal latestRYearDec = new BigDecimal((String)blockData.get("latestRYear"));
            double latestRYear = latestRYearDec.doubleValue();
            Boolean firstBlockOfDay = Boolean.valueOf(((String)blockData.get("firstBlockOfDay")));
            
//            Logger.logDebugMessage("/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/");
//            Logger.logDebugMessage("/\\/\\/\\/\\     PARSING BLOCK     \\/\\/\\/\\/\\");
//            Logger.logDebugMessage("Timestamp: " + timestamp);
//            Logger.logDebugMessage("DATE: " + NtpTime.toString(date));
//            Logger.logDebugMessage("Date2: " + date.toString());
//            Logger.logDebugMessage("Date3: " + date.getTime());
//            Logger.logDebugMessage("Date original string: " + (String)blockData.get("date"));
//            Logger.logDebugMessage("Supply Current: " + supplyCurrent);
//            Logger.logDebugMessage("Latest R Year: " + latestRYear);
//            Logger.logDebugMessage("firstBlockOfDay: " + firstBlockOfDay);
//            Logger.logDebugMessage("/\\/\\/\\/\\/\\   END PARSE BLOCK   /\\/\\/\\/\\/");
//            Logger.logDebugMessage("/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/\\/");
            BlockImpl block = new BlockImpl(version, timestamp, previousBlock, totalAmountNQT, totalFeeNQT, payloadLength, payloadHash, generatorPublicKey,
                    generationSignature, blockSignature, previousBlockHash, blockTransactions, date, totalForgingHoldings,
                    latestRYear, supplyCurrent, blockReward, firstBlockOfDay);
            if (!block.checkSignature()) {
                throw new NxtException.NotValidException("Invalid block signature");
            }
            return block;
        } catch (NxtException.NotValidException|RuntimeException e) {
            Logger.logDebugMessage("Failed to parse block: " + blockData.toJSONString());
            throw e;
        }
    }

    @Override
    public byte[] getBytes() {
        return Arrays.copyOf(bytes(), bytes.length);
    }

    byte[] bigIntToByte(BigInteger input) {
		// extends/truncates the byte array to size 10 bytes.
    byte negOne = (byte)0xFF;
    byte zero = (byte)0x00;

    byte[] inputBytes = input.toByteArray();
    int size = inputBytes.length;
	int sign = input.signum();
    int padLength = 10 - size;
    byte [] result = new byte[10];
    
    if (padLength > 0){
        for (int i = 0; i < size; i++){
                result[padLength + i] = inputBytes[i];
            }
        if (sign < 0){
            for (int j=0; j < padLength; j++){
                result[j] = negOne;
            }
        }
        else if (sign > 0){
            for (int j=0; j < padLength; j++){
                result[j] = zero;
            }
                            
            
        }
        else{
            byte [] zeroBytes = {zero};
            result = Arrays.copyOf(zeroBytes, 10);
        }
    }
    else{
        result = Arrays.copyOf(inputBytes, 10);
    }
  
    
//    BigInteger bigA = new BigInteger(result);
  //Swap Order
    byte [] copy = new byte[10];
    for (int i = 0; i<10; i++) {
    		copy[i] = result[9-i];
    }
    result = copy;
    return result;
 }
    
    
    
    
    byte[] bytes() {
    		
        if (bytes == null) {
            ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 8 + 4 + 8 + 8 + 4 + 32 + 32 + 32 + 32 + (blockSignature != null ? 64 : 0) + 2 + 2 + 8 + 10 + 10 + 10 + 8 + 4);
            // +2 +2 at the end for 10-byte BigInt. (instead of 8-byte long).
            // +10 +10 +8 +10
            // +8 (for date)
            // +4 (for first block of new day)
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(version); //4
            buffer.putInt(timestamp);//4
            buffer.putLong(previousBlockId);//8
            buffer.putInt(getTransactions().size());//4
            buffer.put(bigIntToByte(totalAmountNQT));//10
            buffer.put(bigIntToByte(totalFeeNQT));//10
            
          buffer.put(bigIntToByte(totalForgingHoldings)); //10
          buffer.put(bigIntToByte(supplyCurrent)); //10
          buffer.put(bigIntToByte(blockReward)); //10
          buffer.putDouble(latestRYear); //8
          
          buffer.putLong(date.getTime()); //8
          
          if (firstBlockOfDay) { //4
        	  	buffer.putInt(1);
          }
          else {
        	  	buffer.putInt(0);
          }
          
            
            buffer.putInt(payloadLength);//4
            buffer.put(payloadHash);//32
            buffer.put(getGeneratorPublicKey());//32
            buffer.put(generationSignature);//32
            buffer.put(previousBlockHash);//32
            

            
            if (blockSignature != null) {
                buffer.put(blockSignature);
            }
            
            
            
            
            bytes = buffer.array();
        }
        return bytes;
    }

    boolean verifyBlockSignature() {
        return checkSignature() && Account.setOrVerify(getGeneratorId(), getGeneratorPublicKey());
    }

    private volatile boolean hasValidSignature = false;

    private boolean checkSignature() {
        if (! hasValidSignature) {
            byte[] data = Arrays.copyOf(bytes(), bytes.length - 64);
            
            hasValidSignature = blockSignature != null && Crypto.verify(blockSignature, data, getGeneratorPublicKey());
        }
        return hasValidSignature;
    }

    boolean verifyGenerationSignature() throws BlockchainProcessor.BlockOutOfOrderException {
    		
        try {

            BlockImpl previousBlock = BlockchainImpl.getInstance().getBlock(getPreviousBlockId());
            if (previousBlock == null) {
                throw new BlockchainProcessor.BlockOutOfOrderException("Can't verify signature because previous block is missing", this);
            }

            Account account = Account.getAccount(getGeneratorId());
            BigInteger effectiveBalance = account == null ? BigInteger.ZERO : account.getEffectiveBalanceNXT();
            
         
            
            if (effectiveBalance.compareTo(BigInteger.ZERO) <= 0) {
                return false;
            }

            MessageDigest digest = Crypto.sha256();
            digest.update(previousBlock.generationSignature);
            byte[] generationSignatureHash = digest.digest(getGeneratorPublicKey());
            
          
            
            if (!Arrays.equals(generationSignature, generationSignatureHash)) {
                return false;
            }

            BigInteger hit = new BigInteger(1, new byte[]{generationSignatureHash[7], generationSignatureHash[6], generationSignatureHash[5], generationSignatureHash[4], generationSignatureHash[3], generationSignatureHash[2], generationSignatureHash[1], generationSignatureHash[0]});
            
            
            
            return Generator.verifyHit(hit, effectiveBalance, previousBlock, timestamp);

        } catch (RuntimeException e) {

            Logger.logMessage("Error verifying block generation signature", e);
            return false;

        }

    }

    void apply() {
        Account generatorAccount = Account.addOrGetAccount(getGeneratorId());
        generatorAccount.apply(getGeneratorPublicKey());
        BigInteger totalBackFees = BigInteger.ZERO;
        if (this.height > 3) {
            BigInteger[] backFees = new BigInteger[] {BigInteger.ZERO,BigInteger.ZERO,BigInteger.ZERO};
            for (TransactionImpl transaction : getTransactions()) {
                BigInteger[] fees = transaction.getBackFees();
                for (int i = 0; i < fees.length; i++) {
                    backFees[i] = backFees[i].add(fees[i]);
                }
            }
            for (int i = 0; i < backFees.length; i++) {
                if (backFees[i].equals(BigInteger.ZERO)) {
                    break;
                }
                totalBackFees = totalBackFees.add(backFees[i]);
                Account previousGeneratorAccount = Account.getAccount(BlockDb.findBlockAtHeight(this.height - i - 1).getGeneratorId());
                Logger.logDebugMessage("Back fees %s %s to forger at height %d", Constants.haedsToTaels(backFees[i]).toString(), Constants.COIN_SYMBOL, this.height - i - 1);
                previousGeneratorAccount.addToBalanceAndUnconfirmedBalanceNQT(LedgerEvent.BLOCK_GENERATED, getId(), backFees[i]);
                previousGeneratorAccount.addToForgedBalanceNQT(backFees[i]);
            }
        }
        if (totalBackFees.compareTo(BigInteger.ZERO) != 0) {
            Logger.logDebugMessage("Fee reduced by %s %s at height %d", Constants.haedsToTaels(totalBackFees).toString(), Constants.COIN_SYMBOL, this.height);
        }
        
      
        
//        Logger.logDebugMessage("Balance before: " + String.valueOf(generatorAccount.getBalanceNQT()));
        
        generatorAccount.addToBalanceAndUnconfirmedBalanceNQT(LedgerEvent.BLOCK_GENERATED, getId(), totalFeeNQT.subtract(totalBackFees).add(blockReward));
        generatorAccount.addToForgedBalanceNQT(totalFeeNQT.subtract(totalBackFees).add(blockReward));
        
//        Logger.logDebugMessage("Balance after: " + String.valueOf(generatorAccount.getBalanceNQT()));
        //Just use forged balance should be ok! Because don't want to have sender or anything. 
        //As long as generator's balance is increased! (need to addToBalanceAndUnconfirmedBalanceNQT)
        //And generator can spend that balance. And it's somewhat invisible that the balance is separated to user.
        
        
    }

    void setPrevious(BlockImpl block) {
        if (block != null) {
            if (block.getId() != getPreviousBlockId()) {
                // shouldn't happen as previous id is already verified, but just in case
                throw new IllegalStateException("Previous block id doesn't match");
            }
            this.height = block.getHeight() + 1;
            this.calculateBaseTarget(block);
        } else {
            this.height = 0;
        }
        short index = 0;
        for (TransactionImpl transaction : getTransactions()) {
            transaction.setBlock(this);
            transaction.setIndex(index++);
        }
    }

    void loadTransactions() {
        for (TransactionImpl transaction : getTransactions()) {
            transaction.bytes();
            transaction.getAppendages();
        }
    }

    private void calculateBaseTarget(BlockImpl previousBlock) {
        BigInteger prevBaseTarget = previousBlock.baseTarget;
        int blockchainHeight = previousBlock.height;
        if (blockchainHeight > 2 && blockchainHeight % 2 == 0) {
            BlockImpl block = BlockDb.findBlockAtHeight(blockchainHeight - 2);
            int blocktimeAverage = (this.timestamp - block.timestamp) / 3;
//            Logger.logDebugMessage("-----------------------------------------");
//            Logger.logDebugMessage("this.timestamp: " + this.timestamp + " block-3.timestamp: " + block.timestamp);
//            Logger.logDebugMessage("<><><><><>blocktimeAverage: " + blocktimeAverage);
//            Logger.logDebugMessage("-----------------------------------------");
            
            
//            Logger.logDebugMessage("************** Block Time Average: " + blocktimeAverage);
            
            if (blocktimeAverage > Constants.BLOCK_TIME) {
                baseTarget = prevBaseTarget.multiply(BigInteger.valueOf(Math.min(blocktimeAverage, Constants.MAX_BLOCKTIME_LIMIT))).divide(BigInteger.valueOf(Constants.BLOCK_TIME));
                
//                Logger.logDebugMessage("base target increased from " + prevBaseTarget + " to " + baseTarget);
            } else {
//                baseTarget = prevBaseTarget - prevBaseTarget * Constants.BASE_TARGET_GAMMA
//                        * (Constants.BLOCK_TIME - Math.max(blocktimeAverage, Constants.MIN_BLOCKTIME_LIMIT)) / (100 * Constants.BLOCK_TIME);
            		baseTarget = prevBaseTarget.subtract(
            				prevBaseTarget.multiply(BigInteger.valueOf(Constants.BASE_TARGET_GAMMA).multiply(
            						BigInteger.valueOf(Constants.BLOCK_TIME- Math.max(blocktimeAverage, Constants.MIN_BLOCKTIME_LIMIT)))).divide(
            								BigInteger.valueOf(100 * Constants.BLOCK_TIME)
            								)
            						
            				);
//            		Logger.logDebugMessage("base target decreased from " + prevBaseTarget + " to " + baseTarget);
//            		Logger.logDebugMessage("prevBTgamma60part: " + prevBaseTarget.multiply(BigInteger.valueOf(Constants.BASE_TARGET_GAMMA).multiply(
//    						BigInteger.valueOf(Constants.BLOCK_TIME- Math.max(blocktimeAverage, Constants.MIN_BLOCKTIME_LIMIT)))).divide(
//    								BigInteger.valueOf(100 * Constants.BLOCK_TIME))
//    								);
    						
    				
         
            }
            if (baseTarget.compareTo(BigInteger.ZERO) < 0 || baseTarget.compareTo(Constants.MAX_BASE_TARGET) > 0) {
                baseTarget = Constants.MAX_BASE_TARGET;
                Logger.logDebugMessage("***HIT BASE TARGET CEILING***");
                
            }
            if (baseTarget.compareTo(Constants.MIN_BASE_TARGET) < 0) {
                baseTarget = Constants.MIN_BASE_TARGET;
                Logger.logDebugMessage("***HIT BASE TARGET FLOOR***");
            }
        } else {
            baseTarget = prevBaseTarget;
        }
        cumulativeDifficulty = previousBlock.cumulativeDifficulty.add(Convert.two64.divide(baseTarget)); 
        //two64 = 2**64.
    }
    
    void calculateTotalForgingHoldings() {
    		totalForgingHoldings = GetAllForgersBalances.getSumAllForgersBalances();    		
    }
    
    void setInterestRateYearly(double r) {
		latestRYear = r;
}    
    
    
    void setSupplyCurrent(BigInteger supplyCurrentValue) {
    		supplyCurrent = supplyCurrentValue;
    }
    

    void setBlockReward(BigInteger reward) {
		blockReward = reward;
}
    void setDate(Date retrievedDate) {
    		date = retrievedDate;
    }
    
    void setFirstBlockOfDay() {
    		firstBlockOfDay = true;
    }




}
