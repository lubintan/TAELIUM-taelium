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

package nxt.peer;
//seen.

import nxt.GetAllForgersBalances;
import nxt.util.Logger;

import java.util.Collections;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetPeersForgerIds extends PeerServlet.PeerRequestHandler {

    static final GetPeersForgerIds instance = new GetPeersForgerIds();

    private GetPeersForgerIds() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {
    	// this is what you give when a request is made.
        JSONObject response = new JSONObject();

        JSONArray allForgerIds = new JSONArray();
        
//        GetAllForgersBalances.getAllForgerIds();
        
//        for (long eachForgerId : GetAllForgersBalances.allForgerIds) {
//        		allForgerIds.add(String.valueOf(eachForgerId));
//        }
        
//        GetAllForgersBalances.allForgerIds.forEach(eachForgerId -> allForgerIds.add(eachForgerId));
        if (!GetAllForgersBalances.allForgerIds.isEmpty()) {
	        for (long eachForgerId : GetAllForgersBalances.allForgerIds) {
	        		
	        		allForgerIds.add(eachForgerId);
	        }
        }
        Logger.logDebugMessage("allForgerIds:" + allForgerIds);
        if ((allForgerIds == null) || (allForgerIds.size() <= 0)) {
        		return null;
        }
        
        response.put("forgerIds", allForgerIds);
        return response;
    }

    @Override
    boolean rejectWhileDownloading() {
        return true;
    }

}
