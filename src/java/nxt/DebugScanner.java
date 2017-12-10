package nxt;

import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Logger;
import nxt.crypto.Crypto;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import nxt.GetAllForgersBalances;
import nxt.util.ThreadPool;


public class DebugScanner{
	private DebugScanner(){}
	
    private static final Runnable scanningThread = new Runnable() {

        @Override
        public void run() {
	        	Collection<? extends Peer> allPeers = Peers.getAllPeers();
	        	List<Peer> activePeers = Peers.getActivePeers();
	        	Set<Long> forgers = GetAllForgersBalances.allForgerIds;
	        	
	        	Logger.logDebugMessage("========== DebugScanner at Height: " + String.valueOf(Nxt.getBlockchain().getHeight()) + "==========");
	        	
	        	Logger.logDebugMessage("========== ALL PEERS ==========");
	        	if (allPeers != null) {
		        	Logger.logDebugMessage("address   |   state   |   isBlacklisted");
		        	allPeers.forEach(eachPeer -> Logger.logDebugMessage(eachPeer.getAnnouncedAddress() + " | " 
	        	+ eachPeer.getState() + " | " + eachPeer.isBlacklisted()));
	        	}
	        	Logger.logDebugMessage("========== ACTIVE PEERS ==========");
	        	if (activePeers!=null) {
	        		activePeers.forEach(eachPeer -> Logger.logDebugMessage(eachPeer.getAnnouncedAddress() + " | " 
	        	+ eachPeer.getState() + " | " + eachPeer.isBlacklisted()));
	        	}
	        	
	        	Logger.logDebugMessage("========== ALL FORGERS ==========");
	        if (forgers != null) {
	        		forgers.forEach(eachForger -> Logger.logDebugMessage(Crypto.rsEncode(eachForger)));
	        }
        } 

    };
    
    
    static void init() {
    		ThreadPool.scheduleThread("ScanningThread", DebugScanner.scanningThread, 10);
    }
	
	
	
}