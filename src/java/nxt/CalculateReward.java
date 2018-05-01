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
import java.util.Date;
import java.util.List;
/**
 * @author Lubin
 *
 */
public class CalculateReward {
	private static BigInteger blockReward = Constants.INITIAL_REWARD;
	private static int blockRewardCalculated = 1;
	private CalculateReward() {};
	
	public static BigInteger calculateReward(Date date) {		
		CalculateInterestAndG.updateDayCounter();
		if (Nxt.getBlockchain().getHeight() < 0) {blockReward = BigInteger.ZERO;}
		else if (CalculateInterestAndG.dayCounter < 2) { blockReward = Constants.INITIAL_REWARD;}
		else {
	
			BigInteger yesterdaysVolume = CalculateInterestAndG
				.getTotalPastTxVolumeFromDb(date, blockRewardCalculated);
							
			BigDecimal x = new BigDecimal(yesterdaysVolume);			
			x = x.divide(Constants.H, Constants.PRECISION, RoundingMode.HALF_UP);
			x = x.divide(BigDecimal.ONE.add(x.abs()), Constants.PRECISION, RoundingMode.HALF_UP);
			
			BigDecimal decimalSupplyCurrent = new BigDecimal( CalculateInterestAndG.supplyCurrent);
			decimalSupplyCurrent = decimalSupplyCurrent.multiply(BigDecimal.valueOf(Constants.K));
			
	//		return x.multiply(decimalSupplyCurrent).multiply(BigDecimal.valueOf(Constants.ONE_NXT)).longValue();
			
			//long to BigInt question mark 5 - need to convert to haeds? Supply current is already in heads.
			
			blockReward =  x.multiply(decimalSupplyCurrent).toBigInteger();
			
		}
		return blockReward;

	}
	
	public static BigInteger getBlockReward() {
		CalculateInterestAndG.updateDayCounter();
		if (CalculateInterestAndG.dayCounter < 3) { blockReward = Constants.INITIAL_REWARD;}
		else {blockReward = Nxt.getBlockchain().getLastBlock().getBlockReward();}
		
		return blockReward;
	}
	
//    x = yesterdaysVolume/H
//    reward = K * supplyCurrent * x/(1 + abs(x))
	
}
