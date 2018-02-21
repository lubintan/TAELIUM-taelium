/**
 * 
 */
package nxt;

import nxt.util.Logger;
import nxt.Account;
import nxt.AccountLedger.LedgerEvent;
import nxt.crypto.Crypto;
import nxt.db.DerivedDbTable;


//import nxt.Constants;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
/**
 * @author Lubin
 *
 */
public class CalculateInterestAndG {
	
	private CalculateInterestAndG() {};
	
	static double rDay = Constants.INITIAL_R_YEAR/Constants.INTEREST_DIVISOR;
//	static BigInteger currentBlockHoldings;
	static BigInteger supplyCurrent = Constants.INITIAL_BALANCE_HAEDS;
	static BigInteger vault = Constants.INITIAL_VAULT_HAEDS;
	static BigInteger g = BigInteger.ZERO;
//  apply("CREATE TABLE IF NOT EXISTS daily_data (block_id BIGINT NOT NULL, "
//  + "deltaT DECIMAL NOT NULL, maDeltaAvgHoldings DECIMAL NOT NULL, "
//  + "x DECIMAL NOT NULL, f_deltaT DECIMAL NOT NULL, "
//  + "rYear DECIMAL NOT NULL)");
	static BigInteger averageHoldings = BigInteger.ZERO;
	static BigInteger deltaT = BigInteger.ZERO;
	static BigInteger maDeltaAvgHoldings = BigInteger.ZERO;
//	static BigInteger totalTxed = BigInteger.ZERO;
	static double x = 0;
	static double f_deltaT = 0;
	static double rYear = Constants.INITIAL_R_YEAR;
	static long dayCounter = 1; //starts from 1, 0 means null.
	
	private static int blockGCalculated = 2;
	private static int blockDdSaved = 2;
	private static int blockAvgHoldingsSaved = 2;
	private static int blockAvgHoldingsCalculated = 1;
	
	
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
		
		Logger.logDebugMessage("");
		Logger.logDebugMessage("============== UPDATE TO DAY: " + dayCounter + "=================");
		
		
	}
	
 	private static BigInteger calculateMaDeltaOfHoldings(Date date) {
//		Logger.logDebugMessage("HEREEEEE!!!!");
 		
 		Logger.logDebugMessage("Calculating MA of Delta of holdings......");
 		
// 		int currentHeight = Nxt.getBlockchain().getHeight();
		List<BigInteger> deltaTList = new ArrayList<>();;

		updateDayCounter();
		
		if (dayCounter == 1) {
			averageHoldings = getTotalPastHoldingsFromDb(date, blockAvgHoldingsCalculated);
			deltaT = averageHoldings;
			deltaTList.add(deltaT);
		}else {
			deltaT = calculateTodaysDeltaOfHoldings(date);
		
			Logger.logDebugMessage("Delta T: " + deltaT.toString() + " at date: " + NtpTime.toString(date));
			
//		deltaT = averageHoldings.subtract(loadAvgHoldings(dayCounter - 1));
			deltaTList.add(deltaT);
						
//			Logger.logDebugMessage("----- MA calculation ----");
//			Logger.logDebugMessage("current Day: " + dayCounter);
			
			for (int counter = 1; deltaTList.size() < Constants.MA_WINDOW; counter++) {
				
				long day1Searching = dayCounter - counter;
				long day2Searching = dayCounter - counter - 1;
				
				if (day2Searching < 0) {
					break;
				}
				
	//			Logger.logDebugMessage("daySearching: ",daySearching);
	//			Logger.logDebugMessage("daySearching2: ", daySearching2);
				
//				Logger.logDebugMessage("Adding to MA day: " + daySearching);
				BigInteger day1Holdings = loadAvgHoldings(day1Searching);
				BigInteger day2Holdings = loadAvgHoldings(day2Searching);
				BigInteger thisDeltaT = day1Holdings.subtract(day2Holdings); 
				Logger.logDebugMessage("day1 - day2 holdings:");
				Logger.logDebugMessage(" " + day1Holdings.toString() + " - " + day2Holdings.toString() + " = ");
				Logger.logDebugMessage(thisDeltaT.toString());
				
				deltaTList.add(thisDeltaT);
			}
		}
		
		BigInteger sumDeltaT = BigInteger.ZERO;
		
		Logger.logDebugMessage("size deltaTList: " + deltaTList.size());
		for (BigInteger eachDeltaT : deltaTList) {
			sumDeltaT = sumDeltaT.add(eachDeltaT);
		}
		
		Logger.logDebugMessage("MA of Delta Holdings: " +  sumDeltaT.divide(BigInteger.valueOf(deltaTList.size())).toString());
		
		return sumDeltaT.divide(BigInteger.valueOf(deltaTList.size()));	
	}
	
// 	private static BigInteger calculateTodaysHoldings(Date date) {
// 		averageHoldings = getTotalPastHoldingsFromDb(date);
// 		return averageHoldings;
// 	}
	
	private static BigInteger calculateTodaysDeltaOfHoldings(Date date) {
		//note that the avg holdings for each day in the daily data table, since it's computed at the start of the day, is actually the avg holdings from
		//the previous day.
		//Eg. date = 23 Feb 2018
		//getTotalPastHoldingsFromDb: gives avg holdings computed from 22 Feb 2018.
		//loadAvgHoldingsByDate(yesterday = 22 Feb 2018): gives avg holdings computed from 21 Feb 2018. 
		averageHoldings = getTotalPastHoldingsFromDb(date, blockAvgHoldingsCalculated);
		
		Date yesterday = subtractOneDayFromDate(date);
		
		BigInteger yesterdaysAverageHoldings = loadAvgHoldingsByDate(yesterday);
		
		Logger.logDebugMessage("DELTA TODAY = " + averageHoldings.toString() + " - " + yesterdaysAverageHoldings.toString());
		Logger.logDebugMessage(" = " + averageHoldings.subtract(yesterdaysAverageHoldings).toString());
		
		return averageHoldings.subtract(yesterdaysAverageHoldings);
	}
	
	private static BigInteger getTotalPastHoldingsFromDb(Date date, int offset) {
		//avg holdings from the day before input date.
		BigInteger totalForgingHoldings = BigInteger.ZERO;
		
		Date yesterday = subtractOneDayFromDate(date);
		
		int nextHeight = Nxt.getBlockchain().getHeight() - (offset - 1);
		if (nextHeight < 0) { return totalForgingHoldings;}
		
		Block nextBlock = Nxt.getBlockchain().getBlockAtHeight(nextHeight);
		int numBlocks = 0;
		
		while (nextBlock.getDate().equals(yesterday) && (nextHeight > 0)) {
			BigInteger thisBlocksForgingHoldings = nextBlock.getTotalForgingHoldings();
			totalForgingHoldings = totalForgingHoldings.add(thisBlocksForgingHoldings);
			numBlocks = numBlocks + 1;
			
			nextHeight = nextHeight - 1;
			nextBlock = Nxt.getBlockchain().getBlockAtHeight(nextHeight);
			
		}
		
		return totalForgingHoldings.equals(BigInteger.ZERO) ? BigInteger.ZERO : 
			totalForgingHoldings.divide(BigInteger.valueOf(numBlocks));
	}
	
    private static Date subtractOneDayFromDate(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		calendar.setTime(date);
		calendar.add(Calendar.DATE, -1);
		date = calendar.getTime();
		return date;
	}
	
	public static BigInteger getSupplyCurrent() {
		if (Nxt.getBlockchain().getHeight() < (1)) {
			supplyCurrent = Constants.INITIAL_BALANCE_HAEDS;
		}else {
			supplyCurrent = updateSupplyCurrent();
		}
		
		
		Logger.logDebugMessage("Get Supply Current:" + supplyCurrent.toString());
		
		return supplyCurrent;

	}
	
	private static BigInteger updateSupplyCurrent() {
		BigInteger totalSupplyCurrent = BigInteger.ZERO;
		
//		BlockchainProcessorImpl.getInstance().printAccountTable("update super currently");
		
//		Logger.logDebugMessage("");
//		Logger.logDebugMessage("~~~~~~~~~~~~~ UPDATE SUPPLY CURRENT ~~~~~~~~~~~~");
		
		try (Connection con = Db.db.getConnection();
	             PreparedStatement pstmt = con.prepareStatement("select * from account where latest=true	")) {
	            try(ResultSet rs = pstmt.executeQuery()){
	            	while (rs.next()) {
//	                    long accountId = rs.getLong("ID");
//	                    Account thisAcct = Account.getAccount(accountId);
	                    BigInteger balance = new BigInteger(rs.getBytes("balance"));	                    
	                    long accountId = rs.getLong("ID");
	                    Account thisAcct = Account.getAccount(accountId);
	                    
	                    if (balance.compareTo(BigInteger.ZERO) > 0) {
	                    	totalSupplyCurrent = totalSupplyCurrent.add(balance);
	                    	
//	                    	Logger.logDebugMessage(Crypto.rsEncode(accountId) + " " + balance.equals(thisAcct.getBalanceNQT()));
//	                    	Logger.logDebugMessage("");
	                    	
	                    }
	                    
                    }
	            	
//	            			Logger.logDebugMessage("supplyCurrent From Account Table:" + totalSupplyCurrent);
	                }
	            }
	            
	        catch (SQLException e) {
	            throw new RuntimeException(e.toString(), e);
	        }		
		
//		Logger.logDebugMessage("~~~~~~~~~~~~~ END UPDATE SUPPLY CURRENT ~~~~~~~~~~~~");
//		Logger.logDebugMessage("");
				

		
		supplyCurrent = totalSupplyCurrent;
		return supplyCurrent;
	}
	
//	public static BigInteger getVault(BigInteger change) {
//		if (Nxt.getBlockchain().getHeight() < (1)) {
//			vault = Constants.INITIAL_VAULT_HAEDS;
//		}else {
//			updateVault(change);
//		}
//		
//		return vault;
//	}
	
	private static void updateVault(BigInteger change) {
//		supplyCap = supplyCap + g
//	    if supplyCap <= (supplyCurrent + BUFFER_BETWEEN_SUPCAP_AND_SUPCUR):
//	        supplyCap = supplyCurrent + BUFFER_BETWEEN_SUPCAP_AND_SUPCUR
		
		Logger.logDebugMessage("---------updateVault-----------");
		
		vault = loadLatestVault();
		
		Logger.logDebugMessage("load latest vault: " + vault.toString());
		Logger.logDebugMessage("change: " + change.toString());
		
		vault = vault.add(change);
		
		if (vault.compareTo(supplyCurrent.add(Constants.VAULT_SUPPLY_BUFFER)) < 0 ) {
			vault = supplyCurrent.add(Constants.VAULT_SUPPLY_BUFFER);
		}
		
		Logger.logDebugMessage("final vault: " + vault.toString());
	}
	
	public static double getLatestRYear() {
		if (Nxt.getBlockchain().getHeight() < (1)) {
			rYear = Constants.INITIAL_R_YEAR;
		}else {
			rYear = Nxt.getBlockchain().getLastBlock().getLatestRYear();
		}
		
		Logger.logDebugMessage("getLatestRYear: " + rYear);
		
		return rYear;
	}
	
	public static BigInteger giveInterest(Date date, Boolean isGenerator, List<TransactionImpl> blockTxes) {
		//To be called only when 1440th block is to be generated!
		rYear = getLatestRYear();
//		BlockchainProcessorImpl.getInstance().printAccountTable("interesting giving");
		
		Logger.logDebugMessage("");
		Logger.logDebugMessage("**************** *************** *****************");
		Logger.logDebugMessage("**************** GIVING INTEREST *****************");
		Logger.logDebugMessage("**************** DATE: " + NtpTime.toString(date) + "**************");
		int height = Nxt.getBlockchain().getHeight();
		Logger.logDebugMessage("HEIGHT" + height);
		BigInteger totalPayout = BigInteger.ZERO;
		rDay = rYear / Constants.INTEREST_DIVISOR;
			
		
		BlockchainProcessorImpl.getInstance().printAccountTable("  before----------");
		
//		select ID,BALANCE from account where latest=true	
		
		
		if(rDay != 0) {
			try (Connection con = Db.db.getConnection();
		             PreparedStatement pstmt = con.prepareStatement("select * from account where latest=true	")) {
		            try(ResultSet rs = pstmt.executeQuery()){
		            
		            	Logger.logDebugMessage("rYear: " + rYear + "    rDay: " + rDay);
		            	Logger.logDebugMessage("");
		            	
//		            	//remove transactions that will bring balance below zero after giving out interest.
//		                Iterator<UnconfirmedTransaction> unconfirmedTxIterator = 
//		                		TransactionProcessorImpl.getInstance().getAllUnconfirmedTransactions().iterator();
//		                
		            	while (rs.next()) {
		                    long accountId = rs.getLong("ID");
		                    Account thisAcct = Account.getAccount(accountId);
		                    
		                    BigDecimal decBalHaeds = new BigDecimal(thisAcct.getBalanceNQT());
		                    BigDecimal decPayment = decBalHaeds.multiply(BigDecimal.valueOf(rDay));
		                    BigInteger payment = decPayment.toBigInteger(); //will floor to 0 if less than 1.
		                    
		                    
		                    if (rDay < 0) {
		                    		if (payment.compareTo(BigInteger.ZERO) == 0) {
		                    			payment = BigInteger.ONE.negate();
		                    		}
		                    } //if account's balance is too low such that interest is less than smallest denomination (1 haed),
		                    //if interest > 0 do nothing, if interest < 0 pay -1 haed.
		                    
		                    if (thisAcct.getBalanceNQT().compareTo(BigInteger.ZERO) > 0) {
//			                    	Logger.logDebugMessage(Crypto.rsEncode(accountId));
//			                    	Logger.logDebugMessage("DBBALANCE == GETBAL: " + dbBalance.equals(thisAcct.getBalanceNQT()));
////			                    	Logger.logDebugMessage("dbBalance: " +dbBalance);
//	                    			Logger.logDebugMessage("Acct: " + Crypto.rsEncode(thisAcct.getId()) +" Unconfirmed Bal: " + thisAcct.getUnconfirmedBalanceNQT().toString());
//	                    			Logger.logDebugMessage("payment:" + payment.toString());
//	                    			Logger.logDebugMessage("After balance: " + thisAcct.getBalanceNQT().add(payment).toString());	             
		                    		
//		                    		if (thisAcct.getUnconfirmedBalanceNQT().add(payment).compareTo(BigInteger.ZERO) < 0) {
//		                    			if (isGenerator) {
//		                    				Logger.logDebugMessage("In isGenerator, Acct ID: " + Crypto.rsEncode(thisAcct.getId()));
//		                    				
//		                    				//reject all unconfirmed txes from this sender.
//		                    				 while(unconfirmedTxIterator.hasNext()) {
//		         		                		UnconfirmedTransaction unconfirmedTx = unconfirmedTxIterator.next();
//		         		                		TransactionImpl transaction = unconfirmedTx.getTransaction();
//		         		                		
//		         		                		if (transaction.getSenderId() == thisAcct.getId()) {
//		         		                			TransactionProcessorImpl.getInstance().removeUnconfirmedTransaction(transaction);
//		         		                			Logger.logDebugMessage("````GENERATOR INTEREST REJECT TX```");
//			         		                		Logger.logDebugMessage("ERROR! Sending amount and tx fee greater than UNCONFIRMED balance! Rejecting Tx: " + transaction.getId());
//			        		                    		Logger.logDebugMessage("Offending Account: " + Crypto.rsEncode(accountId) );
//			        		                    		Logger.logDebugMessage("Unconfirmed Balance: " + thisAcct.getUnconfirmedBalanceNQT().toString());
//			        		                    		Logger.logDebugMessage("Sending amount + tx fee: " + transaction.getAmountNQT().add(Constants.STD_FEE));
//			        		                    		Logger.logDebugMessage("```````");
//		         		                			
//		         		                		}
//		                    				 }
//		                    			}
//		                    			
//		                    			else { // if not generator, ie. receiving block from peers, 
//		                    				//reject unconfirmed tx only if it's local, not from the block.
//		                    				while(unconfirmedTxIterator.hasNext()) {
//		         		                		UnconfirmedTransaction unconfirmedTx = unconfirmedTxIterator.next();
//		         		                		TransactionImpl transaction = unconfirmedTx.getTransaction();
//		         		                		
//		         		                		if (transaction.getSenderId() == thisAcct.getId()) {
//		         		                			if (!blockTxes.contains(transaction)) {
//		         		                				TransactionProcessorImpl.getInstance().removeUnconfirmedTransaction(transaction);
//		         		                				Logger.logDebugMessage("```PEER DOWNLOAD REJECT TX````");
//				         		                		Logger.logDebugMessage("ERROR! Sending amount and tx fee greater than UNCONFIRMED balance! Rejecting Tx: " + transaction.getId());
//				        		                    		Logger.logDebugMessage("Offending Account: " + Crypto.rsEncode(accountId) );
//				        		                    		Logger.logDebugMessage("Unconfirmed Balance: " + thisAcct.getUnconfirmedBalanceNQT().toString());
//				        		                    		Logger.logDebugMessage("Sending amount + tx fee: " + transaction.getAmountNQT().add(Constants.STD_FEE));
//				        		                    		Logger.logDebugMessage("```````");
//		         		                			}
//		         		                		}
//		                    				 }
//		                    			}
//		                    		}
		                    	
		                    	
		                    	
		                    		thisAcct.addToBalanceAndUnconfirmedBalanceNQT(LedgerEvent.INTEREST_PAYMENT, date.getTime(), payment);
//		                    		Logger.logDebugMessage("");
//		                    		BigInteger beforeAcct = thisAcct.getBalanceNQT();
//		                    		if (thisAcct.getUnconfirmedBalanceNQT().compareTo(BigInteger.ZERO) < 0) {
//		                    			Logger.logDebugMessage("======= Negative Balance Error!! =======");
//		                    			Logger.logDebugMessage(Crypto.rsEncode(accountId));
//		                    			Logger.logDebugMessage("Before balance: " + String.valueOf(beforeAcct));
//		                    			Logger.logDebugMessage("payment:" + payment);
//		                    			Logger.logDebugMessage("After balance: " + String.valueOf(thisAcct.getUnconfirmedBalanceNQT()));
//		                    			System.exit(1);
//		                    		}
		                    		
		                    		totalPayout = totalPayout.add(payment);
		                   
		                    		}
		                    
		                    }
		            	
		            			Logger.logDebugMessage("totalPayout: " + totalPayout);
//		            			Logger.logDebugMessage("supplyCurrent:" + supplyCurrent);
		            			
		            			BlockchainProcessorImpl.getInstance().printAccountTable("  after----------");
		                }
		            }
		            
		        catch (SQLException e) {
		            throw new RuntimeException(e.toString(), e);
		        }
		}//end if rDay!=0
		
		
		
		Logger.logDebugMessage("*************END GIVING INTEREST *****************");
		
//		Logger.logDebugMessage("**************** *************** *****************");
		Logger.logDebugMessage("");
		
		updateSupplyCurrent();
		
		return totalPayout;
		
	}
	
	private static void calculateG(Date date) {
		BigDecimal decimalSupplyCurrent = new BigDecimal(supplyCurrent);
		
		BigInteger todaysVolume =  getTotalPastTxVolumeFromDb(date, blockGCalculated);
		
//		totalTxed = todaysVolume;
		
		Date yesterday = subtractOneDayFromDate(date);
		
		BigInteger yesterdaysVolume = loadTotalTxedByDate(yesterday);
		
		BigInteger rDayXSupplyCurrent = BigDecimal.valueOf(rDay).multiply(decimalSupplyCurrent).toBigInteger();
		BigInteger todayMinusYesterdayVolume = todaysVolume.subtract(yesterdaysVolume);
		
		Logger.logDebugMessage("Calculating G....");
		Logger.logDebugMessage("todaysVolume: " + todaysVolume.toString());
		Logger.logDebugMessage("yesterdaysVolume: " + yesterdaysVolume.toString());
		Logger.logDebugMessage("rDayXSupplyCurrent: " + rDayXSupplyCurrent.toString());
		Logger.logDebugMessage("todayMinusYesterdayVolume: " + todayMinusYesterdayVolume.toString());
		Logger.logDebugMessage("G: " + rDayXSupplyCurrent.add(todayMinusYesterdayVolume).toString());
		
		g = rDayXSupplyCurrent.add(todayMinusYesterdayVolume);	
		}

	
	
	public static BigInteger getTotalPastTxVolumeFromDb(Date date, int offset) {
		//start < end.
		
		Logger.logDebugMessage("Get TOTAL PAST TX VOLUME FROM DB");
		Logger.logDebugMessage("DATE: " + NtpTime.toString(date));
		
		
		BigInteger totalTxVolume = BigInteger.ZERO;
		
		Date yesterday = subtractOneDayFromDate(date);
		
		Logger.logDebugMessage("YESTERDATE: " + NtpTime.toString(yesterday));
		Logger.logDebugMessage("Nxt.Blockchain Height: " + Nxt.getBlockchain().getHeight());
		int nextHeight = Nxt.getBlockchain().getHeight() - (offset - 1);
		
		if (nextHeight < 0) {return totalTxVolume;}
		
		Block nextBlock = Nxt.getBlockchain().getBlockAtHeight(nextHeight);
		
		Logger.logDebugMessage("next height:" + nextHeight);
		
		while (nextBlock.getDate().equals(yesterday) && (nextHeight > 0)) {
			Logger.logDebugMessage("NEXT HEIGHT:" + nextHeight);
			
			BigInteger thisBlocksTxVolume = nextBlock.getTotalAmountNQT();
			totalTxVolume = totalTxVolume.add(thisBlocksTxVolume);
			
			nextHeight = nextHeight - 1;
			nextBlock = Nxt.getBlockchain().getBlockAtHeight(nextHeight);
			
		}
		Logger.logDebugMessage("Total Tx Volume:" + totalTxVolume.toString());
		return totalTxVolume;
	}
	
	public static void calculateRYear(Date date) {
		//calculate r.

				BigDecimal decimalSupplyCurrent = new BigDecimal(getSupplyCurrent());
				maDeltaAvgHoldings = calculateMaDeltaOfHoldings(date);//averageHoldings, deltaT, maDeltaAvgHoldings set here.

				BigDecimal decimalMaOfDeltaT = new BigDecimal(maDeltaAvgHoldings);
				x = decimalMaOfDeltaT.divide(decimalSupplyCurrent, Constants.PRECISION, RoundingMode.HALF_UP).doubleValue();
				
				f_deltaT = 0.15 * x / (1 + Math.abs(x));
				
				//for first day, r = default value.
				if (dayCounter < 3) {
					rYear = Constants.INITIAL_R_YEAR;
				}else {
					rYear = loadLatestRYear() - f_deltaT;
					
					rYear = (rYear > Constants.R_MAX) ? Constants.R_MAX: 
						(rYear < Constants.R_MIN) ? Constants.R_MIN :
							rYear;
					//maDeltaAvgHoldings = sum(listDeltaAvgHoldings) / float(len(listDeltaAvgHoldings))
					//x = maDeltaAvgHoldings/supplyCurrent
					//f_deltaT = 0.15 * (x)/(1+abs(x))
			        //rYear = rYearYest - f_deltaT
				}
	}
	
	public static void calculateGUpdateVault(Date date) {
				calculateG(date);
				updateVault(g);
	}
	
	public static void init() {
		final DerivedDbTable dailyDataTable = new DerivedDbTable("daily_data") {};
	}
	
	public static Date loadLatestDate() {
		
		long dayFromDb = 0;
		
		try (Connection con = Db.db.getConnection();
	             PreparedStatement pstmt = con.prepareStatement("select MAX(DAY) from DAILY_DATA")) {
	            try(ResultSet rs = pstmt.executeQuery()){
	            	while (rs.next()) {
	                    dayFromDb = rs.getLong("MAX(DAY)");
	            	}
	        }
		} catch (SQLException e) {
			Logger.logDebugMessage(e.toString());
//            throw new RuntimeException(e.toString(), e);
        }
		
		String dateString = null;
		try (Connection con = Db.db.getConnection();
	             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM daily_data where DAY = ?")) {
			pstmt.setLong(1, dayFromDb);
	            try (ResultSet rs = pstmt.executeQuery()) {
	                if (rs.next()) {
//	                    apply("CREATE TABLE IF NOT EXISTS daily_data (db_id IDENTITY, block_id BIGINT NOT NULL, height BIGINT NOT NULL," 
//	                			+"day BIGINT NOT NULL, "
//	                        + "deltaT DECIMAL NOT NULL, maDeltaAvgHoldings DECIMAL NOT NULL, "
//	                        + "x DECIMAL NOT NULL, f_deltaT DECIMAL NOT NULL, "
//	                        + "rYear DECIMAL NOT NULL, supply_current DECIMAL NOT NULL, vault DECIMAL NOT NULL, g DECIMAL NOT NULL)");
				        try {
				             dateString = rs.getString("date");
				        	} catch (SQLException e) {
				            throw new RuntimeException(e.toString(), e);
				        		}
                		}
	            }
	        } catch (SQLException e) {
	            throw new RuntimeException(e.toString(), e);
        		}
		
		return dateString == null ? null : NtpTime.toDate(dateString);
	}
	
	public static BigInteger loadLatestVault() {
		
//		BlockchainProcessorImpl.getInstance().printDDTable(" Load Latest Vault ");
		
		long dayFromDb = 0;
		
		try (Connection con = Db.db.getConnection();
	             PreparedStatement pstmt = con.prepareStatement("select MAX(DAY) from DAILY_DATA")) {
	            try(ResultSet rs = pstmt.executeQuery()){
	            	while (rs.next()) {
	                    dayFromDb = rs.getLong("MAX(DAY)");
	            	}
	        }
		} catch (SQLException e) {
			Logger.logDebugMessage(e.toString());
//            throw new RuntimeException(e.toString(), e);
        }
		
		Logger.logDebugMessage("DAY From DB: " + dayFromDb);
		
		BigInteger myVault = Constants.INITIAL_VAULT_HAEDS;
		try (Connection con = Db.db.getConnection();
	             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM daily_data where DAY = ?")) {
			pstmt.setLong(1, dayFromDb);
	            try (ResultSet rs = pstmt.executeQuery()) {
	                if (rs.next()) {
//	                    apply("CREATE TABLE IF NOT EXISTS daily_data (db_id IDENTITY, block_id BIGINT NOT NULL, height BIGINT NOT NULL," 
//	                			+"day BIGINT NOT NULL, "
//	                        + "deltaT DECIMAL NOT NULL, maDeltaAvgHoldings DECIMAL NOT NULL, "
//	                        + "x DECIMAL NOT NULL, f_deltaT DECIMAL NOT NULL, "
//	                        + "rYear DECIMAL NOT NULL, supply_current DECIMAL NOT NULL, vault DECIMAL NOT NULL, g DECIMAL NOT NULL)");
				        try {
				             myVault = rs.getBigDecimal("vault").toBigInteger();
				        	} catch (SQLException e) {
				            throw new RuntimeException(e.toString(), e);
				        		}
                		}
	            }
	        } catch (SQLException e) {
	            throw new RuntimeException(e.toString(), e);
        		}
		
		return myVault;
	}
	
	public static double loadLatestRYear() {
		
		long dayFromDb = 0;
		
		try (Connection con = Db.db.getConnection();
	             PreparedStatement pstmt = con.prepareStatement("select MAX(DAY) from DAILY_DATA")) {
	            try(ResultSet rs = pstmt.executeQuery()){
	            	while (rs.next()) {
	                    dayFromDb = rs.getLong("MAX(DAY)");
	            	}
	        }
		} catch (SQLException e) {
			Logger.logDebugMessage(e.toString());
//            throw new RuntimeException(e.toString(), e);
        }
		
		double myRYear = rYear;
		try (Connection con = Db.db.getConnection();
	             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM daily_data where DAY = ?")) {
			pstmt.setLong(1, dayFromDb);
	            try (ResultSet rs = pstmt.executeQuery()) {
	                if (rs.next()) {
//	                    apply("CREATE TABLE IF NOT EXISTS daily_data (db_id IDENTITY, block_id BIGINT NOT NULL, height BIGINT NOT NULL," 
//	                			+"day BIGINT NOT NULL, "
//	                        + "deltaT DECIMAL NOT NULL, maDeltaAvgHoldings DECIMAL NOT NULL, "
//	                        + "x DECIMAL NOT NULL, f_deltaT DECIMAL NOT NULL, "
//	                        + "rYear DECIMAL NOT NULL, supply_current DECIMAL NOT NULL, vault DECIMAL NOT NULL, g DECIMAL NOT NULL)");
				        try {
				             myRYear = rs.getDouble("rYear");
				        	} catch (SQLException e) {
				            throw new RuntimeException(e.toString(), e);
				        		}
                		}
	            }
	        } catch (SQLException e) {
	            throw new RuntimeException(e.toString(), e);
        		}
		
		Logger.logDebugMessage("Load Latest R Year: " + myRYear);
		
		return myRYear;
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
	
static BigInteger loadAvgHoldingsByDate(Date date) {
		
		BigInteger dailyHoldings = BigInteger.ZERO;
		
		try (Connection con = Db.db.getConnection();
	             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM daily_data where DATE = ?")) {
			pstmt.setString(1, NtpTime.toString(date));
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

static BigInteger loadTotalTxedByDate(Date date) {
	
	BigInteger dailyTxed = BigInteger.ZERO;
	
	try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM daily_data where DATE = ?")) {
		pstmt.setString(1, NtpTime.toString(date));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
//                    apply("CREATE TABLE IF NOT EXISTS daily_data (db_id IDENTITY, block_id BIGINT NOT NULL, height BIGINT NOT NULL," 
//                			+"day BIGINT NOT NULL, "
//                        + "deltaT DECIMAL NOT NULL, maDeltaAvgHoldings DECIMAL NOT NULL, "
//                        + "x DECIMAL NOT NULL, f_deltaT DECIMAL NOT NULL, "
//                        + "rYear DECIMAL NOT NULL, supply_current DECIMAL NOT NULL, vault DECIMAL NOT NULL, g DECIMAL NOT NULL)");
			        try {
			             dailyTxed = rs.getBigDecimal("total_txed").toBigInteger();
			        	} catch (SQLException e) {
			            throw new RuntimeException(e.toString(), e);
			        		}
            		}
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
    		}
	
	return dailyTxed;
}
	
	static void saveDailyData(long id, long height, Date date) {
		
		Logger.logDebugMessage("^^^    SAVING DD    ^^^");
		averageHoldings = getTotalPastHoldingsFromDb(date, blockAvgHoldingsSaved + 1);
		updateDayCounter();
		getSupplyCurrent();
		
		BigInteger totalTxed = getTotalPastTxVolumeFromDb(date, blockDdSaved + 1);
		
		try {
        		Connection con = Db.db.getConnection();
//                apply("CREATE TABLE IF NOT EXISTS daily_data (db_id IDENTITY, block_id BIGINT NOT NULL, height BIGINT NOT NULL," 
//            			+"day BIGINT NOT NULL, averageHoldings DECIMAL NOT NULL, "
//                    + "deltaT DECIMAL NOT NULL, maDeltaAvgHoldings DECIMAL NOT NULL, "
//                    + "x DECIMAL NOT NULL, f_deltaT DECIMAL NOT NULL, "
//                    + "rYear DECIMAL NOT NULL, supply_current DECIMAL NOT NULL, vault DECIMAL NOT NULL, g DECIMAL NOT NULL)");
            try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO daily_data (block_id, height, day, date, total_txed, average_holdings," 
            			+ "deltaT," 
            			+ "ma_delta_avg_holdings," 
            			+ "x,"
                    + "f_deltaT,"
                    + "rYear,"
                    + "supply_current,"
                    + "vault,"
                    + "g) "
                    + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                int i = 0;
                pstmt.setLong(++i, id);
                pstmt.setLong(++i,  height);
                pstmt.setLong(++i, dayCounter);
                pstmt.setString(++i, NtpTime.toString(date));
                pstmt.setBigDecimal(++i, new BigDecimal(totalTxed));
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
                
                Logger.logDebugMessage("id: " + id);
                Logger.logDebugMessage("height: " + height);
                Logger.logDebugMessage("dayCounter: " + dayCounter);
                Logger.logDebugMessage("date: " + NtpTime.toString(date));
                Logger.logDebugMessage("total Txed: " + new BigDecimal(totalTxed).toString());
                Logger.logDebugMessage("avg holdings: " + new BigDecimal(averageHoldings).toString());
                Logger.logDebugMessage("deltaT: " + new BigDecimal(deltaT).toString());
                Logger.logDebugMessage("maDeltaAvgHoldings: " + new BigDecimal(maDeltaAvgHoldings).toString());
                Logger.logDebugMessage("x: " + x);
                Logger.logDebugMessage("f_deltaT: " + f_deltaT);
                Logger.logDebugMessage("rYear: " + rYear);
                Logger.logDebugMessage("supplyCurrent: " + new BigDecimal(supplyCurrent).toString());
                Logger.logDebugMessage("vault: " + new BigDecimal(vault).toString());
                Logger.logDebugMessage("g: " + new BigDecimal(g).toString());
            }
  
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
	
	public static boolean checkIfDateInDailyData(Date date) {
		String existingDate = null;
		
		try (Connection con = Db.db.getConnection();
	            PreparedStatement pstmt = con.prepareStatement("SELECT TOP 1 date FROM daily_data WHERE date = ?")) {
				pstmt.setString(1, NtpTime.toString(date));
	            try (ResultSet rs = pstmt.executeQuery()) {
	                if (rs.next()) {
	                		existingDate = rs.getString("date");
	            		}
	            }
	        } catch (SQLException e) {
	            throw new RuntimeException(e.toString(), e);
	    		}
	
		return existingDate != null; // True if exists
	}
	
	
}
