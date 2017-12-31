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
import java.util.List;
/**
 * @author Lubin
 *
 */
public class CalculateReward {
	private static BigInteger blockReward = Constants.INITIAL_REWARD;
	private CalculateReward() {};
	
	public static void calculateReward(BigInteger todaysVolume) {
		int currentHeight = Nxt.getBlockchain().getHeight();
		
		if (currentHeight < Constants.DAILY_BLOCKS) { blockReward = Constants.INITIAL_REWARD;}
		else {
	
			BigInteger yesterdaysVolume = CalculateInterestAndG
				.getTotalPastTxVolumeFromDb(currentHeight + 2 - Constants.DAILY_BLOCKS  , currentHeight);
			
			yesterdaysVolume = yesterdaysVolume.add(todaysVolume);
				
			BigDecimal x = new BigDecimal(yesterdaysVolume);
			x = x.divide(BigDecimal.valueOf(Constants.H), Constants.PRECISION, RoundingMode.HALF_UP);
			x = x.divide(BigDecimal.ONE.add(x.abs()), Constants.PRECISION, RoundingMode.HALF_UP);
			
			BigDecimal decimalSupplyCurrent = new BigDecimal( CalculateInterestAndG.supplyCurrent);
			decimalSupplyCurrent = decimalSupplyCurrent.multiply(BigDecimal.valueOf(Constants.K));
			
	//		return x.multiply(decimalSupplyCurrent).multiply(BigDecimal.valueOf(Constants.ONE_NXT)).longValue();
			
			//long to BigInt question mark 5 - need to convert to haeds? Supply current is already in heads.
			
			blockReward =  x.multiply(decimalSupplyCurrent).toBigInteger();
		}
	}
	
	public static BigInteger getBlockReward() {
		return blockReward;
	}
	
//    x = yesterdaysVolume/H
//    reward = K * supplyCurrent * x/(1 + abs(x))
	
}
