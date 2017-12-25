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
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

public final class Genesis {

    private static final byte[] CREATOR_PUBLIC_KEY;
    public static final long CREATOR_ID;
    public static final long EPOCH_BEGINNING;
    static {
        try (InputStream is = ClassLoader.getSystemResourceAsStream("data/genesisParameters.json")) {
            JSONObject genesisParameters = (JSONObject)JSONValue.parseWithException(new InputStreamReader(is));
            CREATOR_PUBLIC_KEY = Convert.parseHexString((String)genesisParameters.get("genesisPublicKey"));
            CREATOR_ID = Account.getId(CREATOR_PUBLIC_KEY);
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
            EPOCH_BEGINNING = dateFormat.parse((String) genesisParameters.get("epochBeginning")).getTime();
        } catch (IOException|ParseException|java.text.ParseException e) {
            throw new RuntimeException("Failed to load genesis parameters", e);
        }
    }

    private static JSONObject genesisAccountsJSON = null;

    private static byte[] loadGenesisAccountsJSON() {
        MessageDigest digest = Crypto.sha256();
        try (InputStreamReader is = new InputStreamReader(new DigestInputStream(
                ClassLoader.getSystemResourceAsStream("data/genesisAccounts" + (Constants.isTestnet ? "-testnet.json" : ".json")), digest))) {
            genesisAccountsJSON = (JSONObject) JSONValue.parseWithException(is);
            
//            System.out.println(genesisAccountsJSON);
//            System.out.println("/n************************/n");
        } catch (IOException|ParseException e) {
            throw new RuntimeException("Failed to process genesis recipients accounts", e);
        }
        digest.update((byte)(Constants.isTestnet ? 1 : 0));
        digest.update(Convert.toBytes(EPOCH_BEGINNING));
        return digest.digest();
    }

    static BlockImpl newGenesisBlock() {
        return new BlockImpl(CREATOR_PUBLIC_KEY, loadGenesisAccountsJSON());
    }

    static void apply() {
        if (genesisAccountsJSON == null) {
            loadGenesisAccountsJSON();
        }
        int count = 0;
        JSONArray publicKeys = (JSONArray) genesisAccountsJSON.get("publicKeys");
        Logger.logDebugMessage("Loading public keys");
        for (Object jsonPublicKey : publicKeys) {
            byte[] publicKey = Convert.parseHexString((String)jsonPublicKey);
            Account account = Account.addOrGetAccount(Account.getId(publicKey));
            account.apply(publicKey);
            if (count++ % 100 == 0) {
                Db.db.commitTransaction();
            }
        }
        Logger.logDebugMessage("Loaded " + publicKeys.size() + " public keys");
        count = 0;
        JSONObject balances = (JSONObject) genesisAccountsJSON.get("balances");
        Logger.logDebugMessage("Loading genesis amounts");
        BigInteger total = BigInteger.ZERO;
		//long to BigInt question mark 1
        // ANSWERED.
   
        for (Map.Entry<String, String> entry : ((Map<String, String>)balances).entrySet()) {
        			
//        			Logger.logDebugMessage(entry.getValue());
        			BigInteger thisBalance = new BigInteger(entry.getValue());
//        			Logger.logDebugMessage("thisBalance: " + thisBalance.toString());
        	
            Account account = Account.addOrGetAccount(Convert.parseAccountId(entry.getKey()));
            account.addToBalanceAndUnconfirmedBalanceNQT(null, 0, thisBalance);
            total = total.add(thisBalance);
            if (count++ % 100 == 0) {
                Db.db.commitTransaction();
            }//initial account balances distributed here.
        }
        if (total.compareTo(Constants.MAX_BALANCE_HAEDS) > 0) {
            throw new RuntimeException("Total balance " + total + " exceeds maximum allowed " + Constants.MAX_BALANCE_HAEDS);
        }
        Logger.logDebugMessage("Total balance %s %s", Constants.haedsToTaels(total).toString(), Constants.COIN_SYMBOL);
        Account creatorAccount = Account.addOrGetAccount(Genesis.CREATOR_ID);
        creatorAccount.apply(Genesis.CREATOR_PUBLIC_KEY);
//        creatorAccount.addToBalanceAndUnconfirmedBalanceNQT(null, 0, -total);
        genesisAccountsJSON = null;
    }

    private Genesis() {} // never

}
