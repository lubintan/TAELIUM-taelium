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

package nxt.http;

import nxt.AccountLedger;
import nxt.Block;
import nxt.BlockchainProcessor;
import nxt.Constants;
import nxt.NtpTime;
import nxt.Taelium;
import nxt.peer.Peer;
import nxt.peer.Peers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public final class GetBlockchainStatus extends APIServlet.APIRequestHandler {

    static final GetBlockchainStatus instance = new GetBlockchainStatus();

    private GetBlockchainStatus() {
        super(new APITag[] {APITag.BLOCKS, APITag.INFO});
    }

    @Override
    protected JSONObject processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        response.put("application", Taelium.APPLICATION);
        response.put("version", Taelium.VERSION);
        response.put("time", Taelium.getEpochTime());
        Block lastBlock = Taelium.getBlockchain().getLastBlock();
        response.put("lastBlock", lastBlock.getStringId());
        response.put("cumulativeDifficulty", lastBlock.getCumulativeDifficulty().toString());
        response.put("numberOfBlocks", lastBlock.getHeight() + 1);
        BlockchainProcessor blockchainProcessor = Taelium.getBlockchainProcessor();
        Peer lastBlockchainFeeder = blockchainProcessor.getLastBlockchainFeeder();
        response.put("lastBlockchainFeeder", lastBlockchainFeeder == null ? null : lastBlockchainFeeder.getAnnouncedAddress());
        response.put("lastBlockchainFeederHeight", blockchainProcessor.getLastBlockchainFeederHeight());
        response.put("isScanning", blockchainProcessor.isScanning());
        response.put("isDownloading", blockchainProcessor.isDownloading());
        response.put("maxRollback", Constants.MAX_ROLLBACK);
        response.put("currentMinRollbackHeight", Taelium.getBlockchainProcessor().getMinRollbackHeight());
        response.put("isTestnet", Constants.isTestnet);
        response.put("maxPrunableLifetime", Constants.MAX_PRUNABLE_LIFETIME);
        response.put("includeExpiredPrunable", Constants.INCLUDE_EXPIRED_PRUNABLE);
        response.put("correctInvalidFees", Constants.correctInvalidFees);
        response.put("ledgerTrimKeep", AccountLedger.trimKeep);
        response.put("rYear", lastBlock.getLatestRYear() * 100); // x 100 to convert to %.
        response.put("supplyCurrent", Constants.divideDec(lastBlock.getSupplyCurrent(), BigInteger.valueOf(100000000)));
        response.put("blockReward", Constants.divideDec(lastBlock.getBlockReward(), BigInteger.valueOf(100000000)));
        response.put("date", NtpTime.toString(lastBlock.getDate()));
        response.put("totalForgingHoldings", Constants.divideDec(lastBlock.getTotalForgingHoldings(), BigInteger.valueOf(100000000)));
        response.put("connectionStatus", NtpTime.getConnectionStatus());
        
//        Compute average time here instead of in html.
        double avgBlockTime = 0;
        double sumAvgBlockTime = 0;
        int currHeight = lastBlock.getHeight();
        
        if (currHeight > (Constants.DAILY_BLOCKS+1)) {
        		for(int i=currHeight; i>(currHeight-Constants.DAILY_BLOCKS); i--) {
        			sumAvgBlockTime += Taelium.getBlockchain().getBlockAtHeight(i).getTimestamp() - 
        					Taelium.getBlockchain().getBlockAtHeight(i-1).getTimestamp();
        		}
        		
        		avgBlockTime = sumAvgBlockTime/Constants.DAILY_BLOCKS;
        }
        else if (currHeight!=1){
        		for(int i=currHeight; i>1; i--) {
        			sumAvgBlockTime += Taelium.getBlockchain().getBlockAtHeight(i).getTimestamp() - 
        					Taelium.getBlockchain().getBlockAtHeight(i-1).getTimestamp();
        		}
        		
        		avgBlockTime = sumAvgBlockTime/(currHeight-1);
        }
//        else avgBlockTime = 0
        
        
        response.put("avgBlockTime", avgBlockTime);
        
        
        JSONArray servicesArray = new JSONArray();
        Peers.getServices().forEach(service -> servicesArray.add(service.name()));
        response.put("services", servicesArray);
        if (APIProxy.isActivated()) {
            String servingPeer = APIProxy.getInstance().getMainPeerAnnouncedAddress();
            response.put("apiProxy", true);
            response.put("apiProxyPeer", servingPeer);
        } else {
            response.put("apiProxy", false);
        }
        response.put("isLightClient", Constants.isLightClient);
        response.put("maxAPIRecords", API.maxRecords);
        response.put("blockchainState", Peers.getMyBlockchainState());
        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

}
