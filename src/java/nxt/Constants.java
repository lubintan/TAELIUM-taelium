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
import java.math.BigDecimal;
//seen.
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public final class Constants {

    public static final boolean isTestnet = false;
    public static final boolean isOffline = false;
    public static final boolean isLightClient = Taelium.getBooleanProperty("tael.isLightClient");
    public static final String customLoginWarning = Taelium.getStringProperty("nxt.customLoginWarning", null, false, "UTF-8");

    public static final String COIN_SYMBOL = "Taels";
    public static final String ACCOUNT_PREFIX = "TAEL";
    public static final String PROJECT_NAME = "Taelium";
    public static final int MAX_NUMBER_OF_TRANSACTIONS = Taelium.getIntProperty("tael.maxNumberOfTransactions", 255);
    public static final int MIN_TRANSACTION_SIZE = 176;
    public static final int MAX_PAYLOAD_LENGTH = MAX_NUMBER_OF_TRANSACTIONS * MIN_TRANSACTION_SIZE;
    
    public static final String dateFormat = "dd-MMM-yyyy";
    
    public static final Date blockchainStartDate = NtpTime.toDate("14-MAY-2018");
    
    public static final Date interestRewardsTxFeesKickInDate = NtpTime.addDays(blockchainStartDate, 1);
    
    public static final int BLOCK_TIME = 600; // should be 60. reducing it for testing purposes. 
    public static final int DAILY_BLOCKS = 40; //1440 blocks. Ie. one day's worth of blocks.
    public static final BigInteger ONE_TAEL = BigInteger.valueOf(10000).multiply(BigInteger.valueOf(10000)); // 8 zeroes.
    public static final BigInteger INITIAL_BALANCE_HAEDS = BigInteger.valueOf(1000).multiply(BigInteger.valueOf(1000000)).multiply(ONE_TAEL);
    public static final BigInteger INITIAL_BALANCE_TAELS = haedsToTaels(INITIAL_BALANCE_HAEDS);
    public static BigInteger MAX_BALANCE_HAEDS = Taelium.getBlockchain().getHeight() > 0 ? Taelium.getBlockchain().getLastBlock().getSupplyCurrent():
    														INITIAL_BALANCE_HAEDS; // make this non-final
    
    public static BigInteger MAX_BALANCE_TAELS = Taelium.getBlockchain().getHeight() > 0 ? haedsToTaels(Taelium.getBlockchain().getLastBlock().getSupplyCurrent()):
														INITIAL_BALANCE_TAELS; // make this non-final
    
    
    public static final BigInteger INITIAL_VAULT_HAEDS = INITIAL_BALANCE_HAEDS;
//    		BigInteger.valueOf(25000000).multiply(ONE_TAEL);
    public static final int MA_WINDOW = 10; //days. 
    public static final BigInteger VAULT_SUPPLY_BUFFER = BigInteger.valueOf(0).multiply(ONE_TAEL);
    public static final double K = 0.01/(365.0*(double)DAILY_BLOCKS);
//  K = (0.125/(365*NUM_OF_DAILY_BLOCKS))
    public static final BigInteger INITIAL_REWARD = BigDecimal.valueOf(2.5).multiply(new BigDecimal(ONE_TAEL)).toBigInteger(); // 2.5 TAELS
    public static final double INITIAL_R_YEAR = 0.05;//0.05 
    public static final double INTEREST_DIVISOR = 365.0;
    public static final double R_MAX = 0.2;
    public static final double R_MIN = -0.1;
//    public static final double R_DEFAULT = 0.05;
    public static final BigDecimal H = new BigDecimal(taelsToHaeds(BigInteger.valueOf(20).multiply(BigInteger.valueOf(1000000)))); 
    public static final int PRECISION = 8; //dec places
    public static final BigInteger MIN_FEE_TAELS = BigInteger.ZERO;
    public static final BigInteger MIN_FEE_HAEDS = taelsToHaeds(MIN_FEE_TAELS);
    public static final BigInteger STD_FEE = BigInteger.valueOf(100000);
    
    public static final int EFF_BAL_HEIGHT = DAILY_BLOCKS;
    public static final BigInteger INITIAL_BASE_TARGET = BigInteger.valueOf(2).pow(63).divide(BigInteger.valueOf(BLOCK_TIME).multiply(INITIAL_BALANCE_TAELS)); //153722867;
    public static final BigInteger MAX_BASE_TARGET = INITIAL_BASE_TARGET.multiply(BigInteger.valueOf(50));//7686143350
    public static final BigInteger MIN_BASE_TARGET = INITIAL_BASE_TARGET.multiply(BigInteger.valueOf(9)).divide(BigInteger.valueOf(10));//138350580
    public static final int MIN_BLOCKTIME_LIMIT = BLOCK_TIME - 7;
    public static final int MAX_BLOCKTIME_LIMIT = BLOCK_TIME + 7;
    public static final int BASE_TARGET_GAMMA = 64;
    public static final int MAX_ROLLBACK = Math.max(Taelium.getIntProperty("tael.maxRollback"), 720);
    public static final int GUARANTEED_BALANCE_CONFIRMATIONS = DAILY_BLOCKS;
//    		isTestnet ? Nxt.getIntProperty("nxt.testnetGuaranteedBalanceConfirmations", ) : 5;
    public static final int LEASING_DELAY = isTestnet ? Taelium.getIntProperty("nxt.testnetLeasingDelay", 1440) : 1440;
    public static final BigInteger MIN_FORGING_BALANCE_HAEDS = BigInteger.ONE; //One Haed
    


    public static final int MAX_TIMEDRIFT = 15; // allow up to 15 s clock difference
    public static final int FORGING_DELAY = Taelium.getIntProperty("tael.forgingDelay");
    public static final int FORGING_SPEEDUP = Taelium.getIntProperty("tael.forgingSpeedup");
    public static final int BATCH_COMMIT_SIZE = Taelium.getIntProperty("tael.batchCommitSize", Integer.MAX_VALUE);

    public static final byte MAX_PHASING_VOTE_TRANSACTIONS = 10;
    public static final byte MAX_PHASING_WHITELIST_SIZE = 10;
    public static final byte MAX_PHASING_LINKED_TRANSACTIONS = 10;
    public static final int MAX_PHASING_DURATION = 14 * 1440;
    public static final int MAX_PHASING_REVEALED_SECRET_LENGTH = 100;

    public static final int MAX_ALIAS_URI_LENGTH = 1000;
    public static final int MAX_ALIAS_LENGTH = 100;

    public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 160;
    public static final int MAX_ENCRYPTED_MESSAGE_LENGTH = 160 + 16;

    public static final int MAX_PRUNABLE_MESSAGE_LENGTH = 42 * 1024;
    public static final int MAX_PRUNABLE_ENCRYPTED_MESSAGE_LENGTH = 42 * 1024;

    public static final int MIN_PRUNABLE_LIFETIME = isTestnet ? 1440 * 60 : 14 * 1440 * 60;
    public static final int MAX_PRUNABLE_LIFETIME;
    public static final boolean ENABLE_PRUNING;
    static {
        int maxPrunableLifetime = Taelium.getIntProperty("tael.maxPrunableLifetime");
        ENABLE_PRUNING = maxPrunableLifetime >= 0;
        MAX_PRUNABLE_LIFETIME = ENABLE_PRUNING ? Math.max(maxPrunableLifetime, MIN_PRUNABLE_LIFETIME) : Integer.MAX_VALUE;
    }
    public static final boolean INCLUDE_EXPIRED_PRUNABLE = Taelium.getBooleanProperty("tael.includeExpiredPrunable");

    public static final int MAX_ACCOUNT_NAME_LENGTH = 100;
    public static final int MAX_ACCOUNT_DESCRIPTION_LENGTH = 1000;

    public static final int MAX_ACCOUNT_PROPERTY_NAME_LENGTH = 32;
    public static final int MAX_ACCOUNT_PROPERTY_VALUE_LENGTH = 160;

    public static final long MAX_ASSET_QUANTITY_QNT = 1000000000L * 100000000L;
    public static final int MIN_ASSET_NAME_LENGTH = 3;
    public static final int MAX_ASSET_NAME_LENGTH = 10;
    public static final int MAX_ASSET_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_SINGLETON_ASSET_DESCRIPTION_LENGTH = 160;
    public static final int MAX_ASSET_TRANSFER_COMMENT_LENGTH = 1000;
    public static final int MAX_DIVIDEND_PAYMENT_ROLLBACK = 1441;

    public static final int MAX_POLL_NAME_LENGTH = 100;
    public static final int MAX_POLL_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_POLL_OPTION_LENGTH = 100;
    public static final int MAX_POLL_OPTION_COUNT = 100;
    public static final int MAX_POLL_DURATION = 14 * 1440;

    public static final byte MIN_VOTE_VALUE = -92;
    public static final byte MAX_VOTE_VALUE = 92;
    public static final byte NO_VOTE_VALUE = Byte.MIN_VALUE;

    public static final int MAX_DGS_LISTING_QUANTITY = 1000000000;
    public static final int MAX_DGS_LISTING_NAME_LENGTH = 100;
    public static final int MAX_DGS_LISTING_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_DGS_LISTING_TAGS_LENGTH = 100;
    public static final int MAX_DGS_GOODS_LENGTH = 1000;

    public static final int MIN_CURRENCY_NAME_LENGTH = 3;
    public static final int MAX_CURRENCY_NAME_LENGTH = 10;
    public static final int MIN_CURRENCY_CODE_LENGTH = 3;
    public static final int MAX_CURRENCY_CODE_LENGTH = 5;
    public static final int MAX_CURRENCY_DESCRIPTION_LENGTH = 1000;
    public static final long MAX_CURRENCY_TOTAL_SUPPLY = 1000000000L * 100000000L;
    public static final int MAX_MINTING_RATIO = 10000; // per mint units not more than 0.01% of total supply
    public static final byte MIN_NUMBER_OF_SHUFFLING_PARTICIPANTS = 3;
    public static final byte MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS = 30; // max possible at current block payload limit is 51
    public static final short MAX_SHUFFLING_REGISTRATION_PERIOD = (short)1440 * 7;
    public static final short SHUFFLING_PROCESSING_DEADLINE = (short)(isTestnet ? 10 : 100);

    public static final int MAX_TAGGED_DATA_NAME_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_TAGGED_DATA_TAGS_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_TYPE_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_CHANNEL_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_FILENAME_LENGTH = 100;
    public static final int MAX_TAGGED_DATA_DATA_LENGTH = 42 * 1024;

    public static final int MAX_REFERENCED_TRANSACTION_TIMESPAN = 60 * 1440 * 60;
    public static final int CHECKSUM_BLOCK_1 = Integer.MAX_VALUE;

    public static final int LAST_CHECKSUM_BLOCK = 0;
    // LAST_KNOWN_BLOCK must also be set in html/www/js/nrs.constants.js
    public static final int LAST_KNOWN_BLOCK = isTestnet ? 0 : 0;

    public static final int[] MIN_VERSION = new int[] {1, 0};
    public static final int[] MIN_PROXY_VERSION = new int[] {1, 0};

    static final BigInteger UNCONFIRMED_POOL_DEPOSIT_NQT = BigInteger.valueOf(100).multiply(ONE_TAEL);
    public static final BigInteger SHUFFLING_DEPOSIT_NQT = BigInteger.valueOf(1000).multiply(ONE_TAEL);

    public static final boolean correctInvalidFees = Taelium.getBooleanProperty("tael.correctInvalidFees");

    public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    public static final String ALLOWED_CURRENCY_CODE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private Constants() {} // never
    
    public static BigDecimal divideDec(BigInteger numerator, BigInteger denominator) {
    		BigDecimal num = new BigDecimal(numerator);
    		BigDecimal denom = new BigDecimal(denominator);
    		
    		return num.divide(denom);
    }
    
    public static BigInteger haedsToTaels(BigInteger haeds) {
		return divideDec(haeds, ONE_TAEL).toBigInteger();
}
    public static BigInteger taelsToHaeds(BigInteger taels) {
    		return taels.multiply(ONE_TAEL);
    }
    
    public static void updateMaxBal(BigInteger currentSupply) {
//    		MAX_BALANCE_HAEDS = currentSupply;
//    		MAX_BALANCE_TAELS = haedsToTaels(currentSupply);
    }
}
