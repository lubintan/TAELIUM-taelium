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

package nxt;
//seen.
import nxt.db.BasicDb;
import nxt.db.TransactionalDb;

public final class Db {

    public static final String PREFIX = Constants.isTestnet ? "tael.testDb" : "tael.db";
    public static final TransactionalDb db = new TransactionalDb(new BasicDb.DbProperties()
            .maxCacheSize(Taelium.getIntProperty("tael.dbCacheKB"))
            .dbUrl(Taelium.getStringProperty(PREFIX + "Url"))
            .dbType(Taelium.getStringProperty(PREFIX + "Type"))
            .dbDir(Taelium.getStringProperty(PREFIX + "Dir"))
            .dbParams(Taelium.getStringProperty(PREFIX + "Params"))
            .dbUsername(Taelium.getStringProperty(PREFIX + "Username"))
            .dbPassword(Taelium.getStringProperty(PREFIX + "Password", null, true))
            .maxConnections(Taelium.getIntProperty("tael.maxDbConnections"))
            .loginTimeout(Taelium.getIntProperty("tael.dbLoginTimeout"))
            .defaultLockTimeout(Taelium.getIntProperty("tael.dbDefaultLockTimeout") * 1000)
            .maxMemoryRows(Taelium.getIntProperty("tael.dbMaxMemoryRows"))
    );

    public static void init() {
        db.init(new NxtDbVersion());
    }

    static void shutdown() {
        db.shutdown();
    }

    private Db() {} // never

}
