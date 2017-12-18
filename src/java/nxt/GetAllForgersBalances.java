/**
 * 
 */
package nxt;

import nxt.Generator;
import nxt.peer.Peers;
import nxt.peer.Peer;
import nxt.util.JSON;
import nxt.util.Logger;
import nxt.crypto.Crypto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.math.BigInteger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * @author Lubin
 *
 */
public class GetAllForgersBalances {
	
	private static Map<Long,BigInteger> allForgersBalances = new HashMap<>();
	private static BigInteger sumAllForgersBalances = BigInteger.ZERO;
	public static Set<Long> allForgerIds  = new HashSet<Long>();
	private int forgersReleaseDelay = 10; //blocks
	
	private GetAllForgersBalances() {}

//	public static Map<Long,BigInteger>  getLocalForgersBalancesMap() {
//		return Generator.getLocalForgerBalanceMap();
//	}
	
	public static Set<Long> getLocalForgerIds() {
		return Generator.getLocalForgerIds();
	}

	public static Set<Long> getPeersForgerIds(){
		Set<Long> peersForgerIds = new HashSet<Long>();
		
		Set<Peer> activePeers = new HashSet<Peer>(Peers.getActivePeers());
		
		for (Peer peer: activePeers) {
			JSONObject request = new JSONObject();
	        request.put("requestType", "getPeersForgerIds");
	        JSONObject response = peer.send(JSON.prepareRequest(request));
	        
	        if ((response == null) || (response.isEmpty()) ) { continue;}
	        
	        else {
		        JSONArray thisPeersForgerIds = (JSONArray)response.get("forgerIds");
		        if (thisPeersForgerIds == null) {return Collections.emptySet();}
	//	        if (thisPeersForgerIds.isEmpty()) {continue;}
		        
		        
	//	        thisPeersForgerIds.forEach(eachForgerId -> peersForgerIds.add(Convert.parseUnsignedLong(eachForgerId)));
	//			System.out.println("|||||||||| thisPeersForgerIds |||||||||||");
	//			System.out.println(thisPeersForgerIds);
				
				for ( Object eachId : thisPeersForgerIds) {
					peersForgerIds.add(Long.parseLong(eachId.toString()));
				}
		        
	//	        System.out.println("∑∑∑∑∑∑∑∑∑");
	//	        System.out.println(thisPeersForgerIds);
	//	        System.out.println("∑∑∑∑∑∑∑∑∑");
	        }
		}
		
		return peersForgerIds;
	}
	
	public static void getAllForgerIds(){
		allForgerIds.removeAll(allForgerIds); //remove everything (reset) 
		allForgerIds.addAll(getPeersForgerIds());
		allForgerIds.addAll(getLocalForgerIds());
		
//		if (allForgerIds == null) {
//			allForgerIds = Collections.emptySet();
//			return;
//		} 
		
//		Logger.logDebugMessage("===     ALL FORGERS     ===");
//		Logger.logDebugMessage(String.valueOf(Nxt.getBlockchain().getHeight()));
//		Logger.logDebugMessage(String.valueOf(allForgerIds));
//		for (long eachForgerId : allForgerIds) {
//			Logger.logDebugMessage(Crypto.rsEncode(eachForgerId));
//		}
		
		return;
	}
	
	public static void updateForgersBalances() {
		getAllForgerIds();
		if (allForgerIds.isEmpty()) {
			return;
		}
		
//		System.out.println("∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆");
//		System.out.println(Nxt.getBlockchain().getHeight());
//		System.out.println();
		for (long eachForgerId : allForgerIds) {
			Account account = Account.getAccount(eachForgerId);
//			System.out.println(Crypto.rsEncode(eachForgerId));
//			System.out.println(BigInteger.valueOf(account.getBalanceNQT()));
			
    			allForgersBalances.put(eachForgerId, BigInteger.valueOf(account.getBalanceNQT()) );
		}
		
//		System.out.println(allForgersBalances.values());
//		System.out.println("∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆∆");

	}
	
	public static BigInteger getSumAllForgersBalances() {
		updateForgersBalances();
		
		if (allForgersBalances == null || allForgersBalances.isEmpty()) {
			return BigInteger.ZERO;
		}
		else {
			BigInteger sumBalances = BigInteger.ZERO;
			
			for (BigInteger balance: allForgersBalances.values()) {
				sumBalances = sumBalances.add(balance);
			}
			
    		
			
			return sumBalances;		
		}
	}	
}
