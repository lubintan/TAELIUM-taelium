package nxt;

import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Logger;
import nxt.crypto.Crypto;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import nxt.GetAllForgersBalances;
import nxt.util.ThreadPool;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.NetworkInterface;
import java.net.SocketException;


public class DebugScanner{
	private DebugScanner(){}
	
    private static final Runnable scanningThread = new Runnable() {

        @Override
        public void run() {
//	        	Collection<? extends Peer> allPeers = Peers.getAllPeers();
//	        	List<Peer> activePeers = Peers.getActivePeers();
//	        	Set<Long> forgers = GetAllForgersBalances.allForgerIds;
//	        	
//	        	Logger.logDebugMessage("========== DebugScanner at Height: " + String.valueOf(Nxt.getBlockchain().getHeight()) + "==========");
//	        	
//	        	Logger.logDebugMessage("========== ALL PEERS ==========");
//	        	if (allPeers != null) {
//		        	Logger.logDebugMessage("address   |   state   |   isBlacklisted");
//		        	allPeers.forEach(eachPeer -> Logger.logDebugMessage(eachPeer.getAnnouncedAddress() + " | " 
//	        	+ eachPeer.getState() + " | " + eachPeer.isBlacklisted()));
//	        	}
//	        	Logger.logDebugMessage("========== ACTIVE PEERS ==========");
//	        	if (activePeers!=null) {
//	        		activePeers.forEach(eachPeer -> Logger.logDebugMessage(eachPeer.getAnnouncedAddress() + " | " 
//	        	+ eachPeer.getState() + " | " + eachPeer.isBlacklisted()));
//	        	}
//	        	
//	        	Logger.logDebugMessage("========== ALL FORGERS ==========");
//	        if (forgers != null) {
//	        		forgers.forEach(eachForger -> Logger.logDebugMessage(Crypto.rsEncode(eachForger)));
//	        }
        	
//        	try {
//        			for (NetworkInterface each: NetworkInterface.getNetworkInterfaces()) {
//        				
//        			}
//        		
//        			InetAddress ipAddr = InetAddress.getLocalHost();
//                Logger.logDebugMessage("###########################");
//                Logger.logDebugMessage(ipAddr.getHostAddress());
//                Logger.logDebugMessage(InetAddress.getLoopbackAddress().getHostAddress());
//                Logger.logDebugMessage("###########################");
//            } catch (UnknownHostException ex) {
//                ex.printStackTrace();
//            }
        	
            try
            {

//                InetAddress inetAddress = null;
                InetAddress myAddr = null;

                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                for (NetworkInterface singleInterface : Collections.list(nets))
                
//                for (Enumeration<NetworkInterface> networkInterface = NetworkInterface
//                        .getNetworkInterfaces(); networkInterface.hasMoreElements();)
                {

//                    NetworkInterface singleInterface = networkInterface
//                            .nextElement();

                		Enumeration<InetAddress> inetAddresses = singleInterface.getInetAddresses();
                    for (InetAddress inetAddress : Collections.list(inetAddresses))
//                    for (Enumeration<InetAddress> IpAddresses = singleInterface
//                            .getInetAddresses(); IpAddresses.hasMoreElements();)
                    {
//                        inetAddress = IpAddresses.nextElement();
                        
                        Logger.logDebugMessage("###########################");
                        Logger.logDebugMessage("display: " + singleInterface.getDisplayName());
                        Logger.logDebugMessage("myAddr: " + inetAddress.toString());
                       	Logger.logDebugMessage("host addr: " + inetAddress.getHostAddress());
                       	Logger.logDebugMessage("local host: " + inetAddress.getLocalHost().getHostAddress());
                       	Logger.logDebugMessage("reachable: " + String.valueOf(inetAddress.isReachable(3000)));
            				Logger.logDebugMessage("## ## ## ## ## ## ## ## ## ## ## ## ###");
                        
                        if (!inetAddress.isLoopbackAddress()
                                && (singleInterface.getDisplayName().contains(
                                        "wlan0") || singleInterface
                                        .getDisplayName().contains("eth0")))
                        {

                            myAddr = inetAddress;
                            Logger.logDebugMessage("NOT YOU SHALL NOT PASS");
                        }
                    }
                }
//                return myAddr;

              
        	  	
			

            }
            catch (SocketException ex)
            {
                Logger.logDebugMessage(ex.toString());
            } catch (UnknownHostException e) {
				// TODO Auto-generated catch block
            		Logger.logDebugMessage(e.toString());
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Logger.logDebugMessage(e.toString());
				e.printStackTrace();
			}
        }
    };
    
    
    static void init() {
    		ThreadPool.scheduleThread("ScanningThread", DebugScanner.scanningThread, 10);
    }
	
	
	
}