/**
 * 
 */
package nxt;

import nxt.util.Logger;
import nxt.util.Time;
import nxt.Account;
import nxt.AccountLedger.LedgerEvent;
import nxt.crypto.Crypto;
import nxt.db.DbUtils;

//import nxt.Constants;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
/**
 * @author Lubin
 *
 */
public class CalculateInterestAndG {
	
	private CalculateInterestAndG() {};
	
	static double rDay;
//	static BigInteger currentBlockHoldings;
	static BigInteger supplyCurrent;
	static BigInteger vault;
	static BigInteger g;
//  apply("CREATE TABLE IF NOT EXISTS daily_data (block_id BIGINT NOT NULL, "
//  + "deltaT DECIMAL NOT NULL, maDeltaAvgHoldings DECIMAL NOT NULL, "
//  + "x DECIMAL NOT NULL, f_deltaT DECIMAL NOT NULL, "
//  + "rYear DECIMAL NOT NULL)");
	static BigInteger averageHoldings;
	static BigInteger deltaT;
	static BigInteger maDeltaAvgHoldings;
	static double x;
	static double f_deltaT;
	static double rYear;
	static long dayCounter = 1; //starts from 1. 0 means null.
	
	
	static void updateDayCounter() {
		
		long dayFromDb = 0;
		
		try (Connection con = Db.db.getConnection();
	             PreparedStatement pstmt = con.prepareStatement("select MAX(DAY) from DAILY_DATA")) {
	            try(ResultSet rs = pstmt.executeQuery()){
	            	while (rs.next()) {
	                    dayFromDb = rs.getLong("MAX(DAY)");
	            	}
	        }
            dayCounter = dayFromDb==0 ? 1: dayFromDb + 1;
		} catch (SQLException e) {
			Logger.logDebugMessage("KENA EXCEPTION");
			Logger.logDebugMessage(e.toString());
//            throw new RuntimeException(e.toString(), e);
        }
		
		
	}
	
 	private static BigInteger calculateMaDeltaOfHoldings(BigInteger currentBlockHoldings) {
		Logger.logDebugMessage("HEREEEEE!!!!");
 		
 		int currentHeight = Nxt.getBlockchain().getLastBlock().getHeight();
		List<BigInteger> deltaTList = new ArrayList<>();;
		// 1st element. Ensure currentHeight >= Constants.DAILY_BLOCKS before calling this function.
//		Logger.logDebugMessage("AVERAGE HOLDINGS: ", averageHoldings);
		
//		if (dayCounter == 0) {updateDayCounter();}
//		else {dayCounter = dayCounter + 1;}

		updateDayCounter();
		
		if (dayCounter == 1) {
			calculateTodaysHoldings(currentBlockHoldings,currentHeight);
			deltaT = averageHoldings;
			deltaTList.add(deltaT);
		}else {
			deltaT = calculateTodaysDeltaOfHoldings(currentBlockHoldings, currentHeight, currentHeight + 1 - Constants.DAILY_BLOCKS);
		
//		deltaT = averageHoldings.subtract(loadAvgHoldings(dayCounter - 1));
			deltaTList.add(deltaT);
						
			Logger.logDebugMessage("----- MA calculation ----");
			Logger.logDebugMessage("current Day: " + dayCounter);
			
			for (int counter = 1; deltaTList.size() < Constants.MA_WINDOW; counter++) {
				
				long daySearching = dayCounter - counter;
				long daySearching2 = dayCounter - counter - 1;
				
				if (daySearching2 < 0) {
					break;
				}
				
	//			Logger.logDebugMessage("daySearching: ",daySearching);
	//			Logger.logDebugMessage("daySearching2: ", daySearching2);
				
				Logger.logDebugMessage("Adding to MA day: " + daySearching);
				
				BigInteger thisDeltaT = loadAvgHoldings(daySearching).subtract(loadAvgHoldings(daySearching2)); 
				deltaTList.add(thisDeltaT);
			}
		}
		
		BigInteger sumDeltaT = BigInteger.ZERO;
		
		Logger.logDebugMessage("size deltaTList: " + deltaTList.size());
		for (BigInteger eachDeltaT : deltaTList) {
			sumDeltaT = sumDeltaT.add(eachDeltaT);
		}
		
		return sumDeltaT.divide(BigInteger.valueOf(deltaTList.size()));	
	}
	
	private static BigInteger calculateDeltaOfHoldings(int todaysLastBlock, int yesterdaysLastBlock) {
		BigInteger todaysAverageHoldings = getTotalPastHoldingsFromDb(todaysLastBlock + 1 - Constants.DAILY_BLOCKS, 
				todaysLastBlock)
				.divide(BigInteger.valueOf(Constants.DAILY_BLOCKS));
		BigInteger yesterdaysAverageHoldings = getTotalPastHoldingsFromDb(yesterdaysLastBlock + 1 - Constants.DAILY_BLOCKS,
				yesterdaysLastBlock)
				.divide(BigInteger.valueOf(Constants.DAILY_BLOCKS));
		
		return todaysAverageHoldings.subtract(yesterdaysAverageHoldings);
	}
	
	private static BigInteger calculateTodaysHoldings(BigInteger currentBlockHoldings, int todaysLastBlock) {
		Logger.logDebugMessage("Computing Daily Holdings..");
		
		
		
		BigInteger todaysAverageHoldings = (getTotalPastHoldingsFromDb(todaysLastBlock + 2 - Constants.DAILY_BLOCKS, 
				todaysLastBlock).add(currentBlockHoldings))
				.divide(BigInteger.valueOf(Constants.DAILY_BLOCKS));
		
		averageHoldings = todaysAverageHoldings;
		
		Logger.logDebugMessage("currentBLockHoldings: "+ currentBlockHoldings);
		Logger.logDebugMessage("avg holdings: "+averageHoldings);
		return todaysAverageHoldings;
	}
	
	private static BigInteger calculateTodaysDeltaOfHoldings(BigInteger currentBlockHoldings, int todaysLastBlock, int yesterdaysLastBlock) {
//		Logger.logDebugMessage("HERE!!!");
		
		BigInteger yesterdaysAverageHoldings = getTotalPastHoldingsFromDb(yesterdaysLastBlock + 1 - Constants.DAILY_BLOCKS,
				yesterdaysLastBlock)
				.divide(BigInteger.valueOf(Constants.DAILY_BLOCKS));
		BigInteger todaysAverageHoldings = calculateTodaysHoldings(currentBlockHoldings, todaysLastBlock);
				
//				(getTotalPastHoldingsFromDb(todaysLastBlock + 2 - Constants.DAILY_BLOCKS, 
//				todaysLastBlock).add(currentBlockHoldings))
//				.divide(BigInteger.valueOf(Constants.DAILY_BLOCKS));
		
//		averageHoldings = todaysAverageHoldings;
		
		
		return todaysAverageHoldings.subtract(yesterdaysAverageHoldings);
	}
	
	private static BigInteger getTotalPastHoldingsFromDb(int startHeightInclusive, int endHeightInclusive) {
		//descending so startHeight < endHeight.
		BigInteger totalForgingHoldings = BigInteger.ZERO;
		for (int counter = startHeightInclusive ; counter <= endHeightInclusive; counter++ ) {
			BigInteger thisBlocksForgingHoldings = Nxt.getBlockchain().getBlockAtHeight(counter).getTotalForgingHoldings();
			
			Logger.logDebugMessage(thisBlocksForgingHoldings.toString());
			Logger.logDebugMessage("at height " + counter);
			
			totalForgingHoldings = totalForgingHoldings.add(thisBlocksForgingHoldings);	
		}
		
		return totalForgingHoldings;
	}
	
	public static void getSupplyCurrent() {
		if (Nxt.getBlockchain().getHeight() < (1)) {
			supplyCurrent = Constants.INITIAL_BALANCE_HAEDS;
		}else {
			supplyCurrent = Nxt.getBlockchain().getLastBlock().getSupplyCurrent();
		}

	}
	
	public static void updateSupplyCurrent(BigInteger change) {
				supplyCurrent = supplyCurrent.add(change);	
	}
	
	public static void getVault() {
		if (Nxt.getBlockchain().getHeight() < (1)) {
			vault = Constants.INITIAL_VAULT_HAEDS;
		}else {
			vault = Nxt.getBlockchain().getLastBlock().getVault();
		}
	}
	
	public static void updateVault(BigInteger change) {
//		supplyCap = supplyCap + g
//	    if supplyCap <= (supplyCurrent + BUFFER_BETWEEN_SUPCAP_AND_SUPCUR):
//	        supplyCap = supplyCurrent + BUFFER_BETWEEN_SUPCAP_AND_SUPCUR
		
		vault = vault.add(change);
		
		if (vault.compareTo(supplyCurrent.add(Constants.VAULT_SUPPLY_BUFFER)) < 0 ) {
			vault = supplyCurrent.add(Constants.VAULT_SUPPLY_BUFFER);
		}
	}
	
	public static void getLatestRYear() {
		if (Nxt.getBlockchain().getHeight() < (1)) {
			rYear = Constants.INITIAL_R_YEAR;
		}else {
			rYear = Nxt.getBlockchain().getLastBlock().getLatestRYear();
		}
	}
	
	private static BigInteger giveInterest(double rYear, long id) {
		//To be called only when 1440th block is to be generated!
		
		BigInteger totalPayout = BigInteger.ZERO;
		rDay = rYear / Constants.INTEREST_DIVISOR;
			
//		select ID,BALANCE from account where latest=true	
		
		
		if(rDay != 0) {
			try (Connection con = Db.db.getConnection();
		             PreparedStatement pstmt = con.prepareStatement("select ID from account where latest=true	")) {
		            try(ResultSet rs = pstmt.executeQuery()){
		            	while (rs.next()) {
		                    long accountId = rs.getLong("ID");
		                    Account thisAcct = Account.getAccount(accountId);
		                    
		                    BigDecimal decBalHaeds = new BigDecimal(thisAcct.getBalanceNQT());
		                    BigDecimal decPayment = decBalHaeds.multiply(BigDecimal.valueOf(rDay));
		                    BigInteger payment = decPayment.toBigInteger(); 
		                    
		                    
		                    
		                    if (rDay < 0) {
		                    		if (payment.compareTo(BigInteger.ZERO) == 0) {
		                    			payment = BigInteger.ONE.negate();
		                    		}
		                    } //if account's balance is too low such that interest is less than smallest denomination (1 haed),
		                    //if interest > 0 do nothing, if interest < 0 pay -1 haed.
		                    
		                    if (thisAcct.getBalanceNQT().compareTo(BigInteger.ZERO) >= 0) {
		                    		thisAcct.addToBalanceAndUnconfirmedBalanceNQT(LedgerEvent.INTEREST_PAYMENT, id, payment);
		                    		BigInteger beforeAcct = thisAcct.getBalanceNQT();
		                    		if (thisAcct.getUnconfirmedBalanceNQT().compareTo(BigInteger.ZERO) < 0) {
		                    			Logger.logDebugMessage("======= Negative Balance Error!! =======");
		                    			Logger.logDebugMessage(Crypto.rsEncode(accountId));
		                    			Logger.logDebugMessage("Before balance: " + String.valueOf(beforeAcct));
		                    			Logger.logDebugMessage("payment:" + payment);
		                    			Logger.logDebugMessage("After balance: " + String.valueOf(thisAcct.getUnconfirmedBalanceNQT()));
		                    			System.exit(1);
		                    		}
		                    		
		                    		totalPayout = totalPayout.add(payment);
		                   
		                    }
		                    
		                    }
		            	
		            			Logger.logDebugMessage("totalPayout: " + totalPayout);
		            			Logger.logDebugMessage("supplyCurrent:" + supplyCurrent);
		                }
		            }
		            
		        catch (SQLException e) {
		            throw new RuntimeException(e.toString(), e);
		        }
		}//end if rDay!=0
		
		return totalPayout;
		
	}
		
	private static void calculateG(BigInteger thisBlockVolume) {
		BigInteger todaysVolume;
		BigInteger yesterdaysVolume;
		BigDecimal decimalSupplyCurrent = new BigDecimal(supplyCurrent);
		int currentHeight = Nxt.getBlockchain().getLastBlock().getHeight();
		int yesterdaysEndHeight = currentHeight + 1 - Constants.DAILY_BLOCKS;
		int yesterdaysStartHeight = yesterdaysEndHeight + 1 - Constants.DAILY_BLOCKS;
		
		if (yesterdaysStartHeight < 1) {
			yesterdaysVolume = BigInteger.ZERO;
		}else {
			yesterdaysVolume = getTotalPastTxVolumeFromDb(yesterdaysStartHeight ,yesterdaysEndHeight);
		}
		
		todaysVolume = calculateTodaysTxVolume(thisBlockVolume);
			
//		g = (rDay*supplyCurrent) + dailyVolume - yesterdayVolume
		g = BigDecimal.valueOf(rDay).multiply(decimalSupplyCurrent).toBigInteger().add(todaysVolume).subtract(yesterdaysVolume);	
		


		
		}

	
	
	public static BigInteger getTotalPastTxVolumeFromDb(int startHeightInclusive, int endHeightInclusive) {
		//start < end.
		Logger.logDebugMessage("startHeight: " + startHeightInclusive + "endHeight: " + endHeightInclusive );
		BigInteger totalTxVolume = BigInteger.ZERO;
		for (int counter = startHeightInclusive ; counter <= endHeightInclusive; counter++ ) {
			BigInteger thisBlocksTxVolume = Nxt.getBlockchain().getBlockAtHeight(counter).getTotalAmountNQT();
			totalTxVolume = totalTxVolume.add(thisBlocksTxVolume);	
		}
		
		return totalTxVolume;
	}
	
	private static BigInteger calculateTodaysTxVolume(BigInteger thisBlockVolume) {
		int currentHeight = Nxt.getBlockchain().getLastBlock().getHeight();
		
		Logger.logDebugMessage("current Height: " + currentHeight);
		//about to gerate 1440th block. So now at 1439th block.
		return getTotalPastTxVolumeFromDb(currentHeight + 2 - Constants.DAILY_BLOCKS, currentHeight )
				.add(thisBlockVolume);
	}
	
	public static void dailyUpdate(long id, long height, BigInteger thisBlockTxVolume, BigInteger currentBlockHoldings) {
		//calculate r.

		BigDecimal decimalSupplyCurrent = new BigDecimal(supplyCurrent);
		maDeltaAvgHoldings = calculateMaDeltaOfHoldings(currentBlockHoldings);//averageHoldings, deltaT, maDeltaAvgHoldings set here.

		BigDecimal decimalMaOfDeltaT = new BigDecimal(maDeltaAvgHoldings);
		x = decimalMaOfDeltaT.divide(decimalSupplyCurrent, Constants.PRECISION, RoundingMode.HALF_UP).doubleValue();
		
		f_deltaT = 0.15 * x / (1 + Math.abs(x));
		
		//for first day, r = default value.
		if (Nxt.getBlockchain().getLastBlock().getHeight() < (2 * Constants.DAILY_BLOCKS)) {
			rYear = Constants.R_DEFAULT;
		}else {
			
			
			rYear = Nxt.getBlockchain().getLastBlock().getLatestRYear() - f_deltaT;
			
			rYear = (rYear > Constants.R_MAX) ? Constants.R_MAX: 
				(rYear < Constants.R_MIN) ? Constants.R_MIN :
					rYear;
			//maDeltaAvgHoldings = sum(listDeltaAvgHoldings) / float(len(listDeltaAvgHoldings))
			//x = maDeltaAvgHoldings/supplyCurrent
			//f_deltaT = 0.15 * (x)/(1+abs(x))
	        //rYear = rYearYest - f_deltaT
		}
		
		//perform interest payments.
		BigInteger totalPayout = giveInterest(rYear, id);
		
		//update supplyCurrent.
		updateSupplyCurrent(totalPayout);
		
		//compute g.
		calculateG(thisBlockTxVolume);
		updateVault(g);
		
		//save daily info to db.
		
		Logger.logDebugMessage("avgHoldings: "+ averageHoldings);
		Logger.logDebugMessage("deltaT: "+deltaT);
		Logger.logDebugMessage("x: "+x);
		
		saveDailyData(id, height);
	}
	
	
	
	static BigInteger loadAvgHoldings(long day) {
		
		BigInteger dailyHoldings = BigInteger.ZERO;
		
		try (Connection con = Db.db.getConnection();
	             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM daily_data where DAY = ?")) {
			pstmt.setLong(1, day);
	            try (ResultSet rs = pstmt.executeQuery()) {
	                if (rs.next()) {
//	                    apply("CREATE TABLE IF NOT EXISTS daily_data (db_id IDENTITY, block_id BIGINT NOT NULL, height BIGINT NOT NULL," 
//	                			+"day BIGINT NOT NULL, "
//	                        + "deltaT DECIMAL NOT NULL, maDeltaAvgHoldings DECIMAL NOT NULL, "
//	                        + "x DECIMAL NOT NULL, f_deltaT DECIMAL NOT NULL, "
//	                        + "rYear DECIMAL NOT NULL, supply_current DECIMAL NOT NULL, vault DECIMAL NOT NULL, g DECIMAL NOT NULL)");
				        try {
				             dailyHoldings = rs.getBigDecimal("average_holdings").toBigInteger();
				        	} catch (SQLException e) {
				            throw new RuntimeException(e.toString(), e);
				        		}
                		}
	            }
	        } catch (SQLException e) {
	            throw new RuntimeException(e.toString(), e);
        		}
		
		return dailyHoldings;
    }
	
	static void saveDailyData(long id, long height) {
        try {
        		Connection con = Db.db.getConnection();
//                apply("CREATE TABLE IF NOT EXISTS daily_data (db_id IDENTITY, block_id BIGINT NOT NULL, height BIGINT NOT NULL," 
//            			+"day BIGINT NOT NULL, averageHoldings DECIMAL NOT NULL, "
//                    + "deltaT DECIMAL NOT NULL, maDeltaAvgHoldings DECIMAL NOT NULL, "
//                    + "x DECIMAL NOT NULL, f_deltaT DECIMAL NOT NULL, "
//                    + "rYear DECIMAL NOT NULL, supply_current DECIMAL NOT NULL, vault DECIMAL NOT NULL, g DECIMAL NOT NULL)");
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO daily_data (block_id, height, day, average_holdings," 
            			+ "deltaT, ma_delta_avg_holdings, x, "
                    + "f_deltaT, rYear, supply_current, vault, g) "
                    + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")) {
                int i = 0;
                pstmt.setLong(++i, id);
                pstmt.setLong(++i,  height);
                pstmt.setLong(++i, dayCounter);
//                dayCounter += 1;
                pstmt.setBigDecimal(++i, new BigDecimal(averageHoldings));
                pstmt.setBigDecimal(++i, new BigDecimal(deltaT));
                pstmt.setBigDecimal(++i, new BigDecimal(maDeltaAvgHoldings));
                pstmt.setDouble(++i, x);
                pstmt.setDouble(++i, f_deltaT);
                pstmt.setDouble(++i, rYear);
                pstmt.setBigDecimal(++i, new BigDecimal(supplyCurrent));
                pstmt.setBigDecimal(++i, new BigDecimal(vault));
                pstmt.setBigDecimal(++i, new BigDecimal(g));
                pstmt.executeUpdate();
            }
  
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
	
	
}
