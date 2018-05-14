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

package nxt.env;

import nxt.Taelium;
import nxt.util.Logger;

import java.nio.file.Paths;

public class UnixUserDirProvider extends DesktopUserDirProvider {
		
		private static final String NXT_USER_HOME = Paths.get(System.getProperty("user.home"), "Documents/Taelium Data").toString();
//    private static final String NXT_USER_HOME = Paths.get(System.getProperty("user.home"), "Library/Application Support/" + Taelium.APPLICATION.toUpperCase()).toString();
//    private static final String NXT_USER_HOME = Paths.get(System.getProperty("user.home"), Taelium.APPLICATION.toUpperCase()).toString();

    @Override
    public String getUserHomeDir() {
    	
    		
    		System.out.println("TAELIUM_USER_HOME: " + NXT_USER_HOME);
    	    	
        return NXT_USER_HOME;
    }
}
