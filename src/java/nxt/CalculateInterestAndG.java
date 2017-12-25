/**
 * 
 */
package nxt;

import nxt.util.Logger;
import nxt.util.Time;
import nxt.Account;
import nxt.AccountLedger.LedgerEvent;
import nxt.crypto.Crypto;

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
	
	static double rYear = 0;
	static double rDay = 0;
//	static BigInteger currentBlockHoldings;
	static BigInteger supplyCurrent;
	static BigInteger vault;
	static BigInteger g;
	
	
 	private static BigInteger calculateMaDeltaOfHoldings(BigInteger currentBlockHoldings) {
		int currentHeight = Nxt.getBlockchain().getLastBlock().getHeight();
		List<BigInteger> deltaTList = new ArrayList<>();;
		// 1st element. Ensure currentHeight >= Constants.DAILY_BLOCKS before calling this function.
		deltaTList.add(calculateTodaysDeltaOfHoldings(currentBlockHoldings, currentHeight, currentHeight + 1 - Constants.DAILY_BLOCKS));
		
		for (int counter = 1; deltaTList.size() <= Constants.MA_WINDOW; counter++) {
			int nextHeight = currentHeight + 1 - (counter * Constants.DAILY_BLOCKS);
			int nextNextHeight = nextHeight - (Constants.DAILY_BLOCKS);
			
			if ((nextNextHeight - Constants.DAILY_BLOCKS) < 0) {
				break;
			}
			
			deltaTList.add(calculateDeltaOfHoldings(nextHeight, nextNextHeight));
		}
		
		BigInteger sumDeltaT = BigInteger.ZERO;
		for (BigInteger deltaT : deltaTList) {
			sumDeltaT = sumDeltaT.add(deltaT);
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
	
	private static BigInteger calculateTodaysDeltaOfHoldings(BigInteger currentBlockHoldings, int todaysLastBlock, int yesterdaysLastBlock) {
		BigInteger yesterdaysAverageHoldings = getTotalPastHoldingsFromDb(yesterdaysLastBlock + 1 - Constants.DAILY_BLOCKS,
				yesterdaysLastBlock)
				.divide(BigInteger.valueOf(Constants.DAILY_BLOCKS));
		BigInteger todaysAverageHoldings = (getTotalPastHoldingsFromDb(todaysLastBlock + 2 - Constants.DAILY_BLOCKS, 
				todaysLastBlock).add(currentBlockHoldings))
				.divide(BigInteger.valueOf(Constants.DAILY_BLOCKS));
		
		return todaysAverageHoldings.subtract(yesterdaysAverageHoldings);
	}
	
	private static BigInteger getTotalPastHoldingsFromDb(int startHeightInclusive, int endHeightInclusive) {
		//descending so startHeight < endHeight.
		BigInteger totalForgingHoldings = BigInteger.ZERO;
		for (int counter = startHeightInclusive ; counter <= endHeightInclusive; counter++ ) {
			BigInteger thisBlocksForgingHoldings = Nxt.getBlockchain().getBlockAtHeight(counter).getTotalForgingHoldings();
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
		BigInteger totalTxVolume = BigInteger.ZERO;
		for (int counter = startHeightInclusive ; counter <= endHeightInclusive; counter++ ) {
			BigInteger thisBlocksTxVolume = Nxt.getBlockchain().getBlockAtHeight(counter).getTotalAmountNQT();
			totalTxVolume = totalTxVolume.add(thisBlocksTxVolume);	
		}
		
		return totalTxVolume;
	}
	
	private static BigInteger calculateTodaysTxVolume(BigInteger thisBlockVolume) {
		int currentHeight = Nxt.getBlockchain().getLastBlock().getHeight();
		//about to gerate 1440th block. So now at 1439th block.
		return getTotalPastTxVolumeFromDb(currentHeight + 2 - Constants.DAILY_BLOCKS, currentHeight )
				.add(thisBlockVolume);
	}
	
	public static void dailyUpdate(long id, BigInteger thisBlockTxVolume, BigInteger currentBlockHoldings) {
		//calculate r.

		//for first day, r = default value.
		if (Nxt.getBlockchain().getLastBlock().getHeight() < (Constants.DAILY_BLOCKS * 2)) {
			rYear = Constants.R_DEFAULT;
		}else {
			BigDecimal decimalSupplyCurrent = new BigDecimal(supplyCurrent);
			BigDecimal decimalMaOfDeltaT = new BigDecimal(calculateMaDeltaOfHoldings(currentBlockHoldings));
			double x = decimalMaOfDeltaT.divide(decimalSupplyCurrent, Constants.PRECISION, RoundingMode.HALF_UP).doubleValue();
			
			double f_deltaT = 0.15 * x / (1 + Math.abs(x));
			
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
		
		//update r and supplyCurrent.
		updateSupplyCurrent(totalPayout);
		
		//compute g.
		calculateG(thisBlockTxVolume);
		updateVault(g);
		
	}
	
	
}
