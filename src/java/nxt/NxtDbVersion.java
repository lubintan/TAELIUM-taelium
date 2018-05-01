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
import nxt.db.DbVersion;

class NxtDbVersion extends DbVersion {

    protected void update(int nextUpdate) {
        switch (nextUpdate) {
            case 1:
                apply("CREATE TABLE IF NOT EXISTS block (db_id IDENTITY, id BIGINT NOT NULL, version INT NOT NULL, "
                        + "timestamp INT NOT NULL, previous_block_id BIGINT, "
                        + "total_amount DECIMAL NOT NULL, "
                        + "total_fee DECIMAL NOT NULL, payload_length INT NOT NULL, "
                        + "previous_block_hash BINARY(32), cumulative_difficulty DECIMAL NOT NULL, base_target DECIMAL NOT NULL, "
                        + "next_block_id BIGINT, "
                        + "height INT NOT NULL, date CHAR NOT NULL, generation_signature BINARY(64) NOT NULL, "
                        + "block_signature BINARY(64) NOT NULL, payload_hash BINARY(32) NOT NULL, generator_id BIGINT NOT NULL,"
                        + "total_forging_holdings DECIMAL NOT NULL,"
                        + "latest_annual_interest_rate FLOAT NOT NULL,"
                        + "supply_current DECIMAL NOT NULL,"
                        + "block_reward DECIMAL NOT NULL,"
                        + "first_block_of_day BOOLEAN NOT NULL DEFAULT FALSE)");
            case 2:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_id_idx ON block (id)");
            case 3:
                apply("CREATE TABLE IF NOT EXISTS transaction (db_id IDENTITY, id BIGINT NOT NULL, "
                        + "deadline SMALLINT NOT NULL, recipient_id BIGINT, transaction_index SMALLINT NOT NULL, "
                        + "amount DECIMAL NOT NULL, fee DECIMAL NOT NULL, full_hash BINARY(32) NOT NULL, "
                        + "height INT NOT NULL, block_id BIGINT NOT NULL, FOREIGN KEY (block_id) REFERENCES block (id) ON DELETE CASCADE, "
                        + "signature BINARY(64) NOT NULL, timestamp INT NOT NULL, type TINYINT NOT NULL, subtype TINYINT NOT NULL, "
                        + "sender_id BIGINT NOT NULL, block_timestamp INT NOT NULL, referenced_transaction_full_hash BINARY(32), "
                        + "phased BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "attachment_bytes VARBINARY, version TINYINT NOT NULL, has_message BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "has_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE, has_public_key_announcement BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "ec_block_height INT DEFAULT NULL, ec_block_id BIGINT DEFAULT NULL, has_encrypttoself_message BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "has_prunable_message BOOLEAN NOT NULL DEFAULT FALSE, has_prunable_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "has_prunable_attachment BOOLEAN NOT NULL DEFAULT FALSE)");
            case 4:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS transaction_id_idx ON transaction (id)");
            case 5:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_height_idx ON block (height)");
            case 6:
                apply("CREATE INDEX IF NOT EXISTS block_generator_id_idx ON block (generator_id)");
            case 7:
                apply("CREATE INDEX IF NOT EXISTS transaction_sender_id_idx ON transaction (sender_id)");
            case 8:
                apply("CREATE INDEX IF NOT EXISTS transaction_recipient_id_idx ON transaction (recipient_id)");
            case 9:
                apply("CREATE TABLE IF NOT EXISTS peer (address VARCHAR PRIMARY KEY, last_updated INT, services BIGINT)");
            case 10:
                apply("CREATE INDEX IF NOT EXISTS transaction_block_timestamp_idx ON transaction (block_timestamp DESC)");
            case 11:
                apply("CREATE TABLE IF NOT EXISTS daily_data (db_id IDENTITY, block_id BIGINT NOT NULL, height BIGINT NOT NULL," 
                			+"day BIGINT NOT NULL, date CHAR NOT NULL, total_txed DECIMAL NOT NULL, average_holdings DECIMAL NOT NULL, "
                        + "deltaT DECIMAL NOT NULL, ma_delta_avg_holdings DECIMAL NOT NULL, "
                        + "x FLOAT NOT NULL, f_deltaT FLOAT NOT NULL, "
                        + "rYear FLOAT NOT NULL, supply_current DECIMAL NOT NULL)");
           
            case 12:
                apply("CREATE TABLE IF NOT EXISTS account (db_id IDENTITY, id BIGINT NOT NULL, "
                        + "balance BIGINT NOT NULL, unconfirmed_balance BIGINT NOT NULL, has_control_phasing BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "forged_balance BIGINT NOT NULL, active_lessee_id BIGINT, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 13:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_id_height_idx ON account (id, height DESC)");
            case 14:
                apply("CREATE TABLE IF NOT EXISTS account_guaranteed_balance (db_id IDENTITY, account_id BIGINT NOT NULL, "
                        + "additions DECIMAL NOT NULL, height INT NOT NULL)");
            case 15:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_guaranteed_balance_id_height_idx ON account_guaranteed_balance "
                        + "(account_id, height DESC)");
            case 16:
                apply("CREATE TABLE IF NOT EXISTS unconfirmed_transaction (db_id IDENTITY, id BIGINT NOT NULL, expiration INT NOT NULL, "
                        + "transaction_height INT NOT NULL, fee_per_byte BIGINT NOT NULL, arrival_timestamp BIGINT NOT NULL, "
                        + "transaction_bytes VARBINARY NOT NULL, prunable_json VARCHAR, height INT NOT NULL)");
            case 17:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS unconfirmed_transaction_id_idx ON unconfirmed_transaction (id)");

            case 18:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_timestamp_idx ON block (timestamp DESC)");
            case 19:
                apply("CREATE TABLE IF NOT EXISTS tag (db_id IDENTITY, tag VARCHAR NOT NULL, in_stock_count INT NOT NULL, "
                        + "total_count INT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 20:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS tag_tag_idx ON tag (tag, height DESC)");
            case 21:
                apply("CREATE INDEX IF NOT EXISTS tag_in_stock_count_idx ON tag (in_stock_count DESC, height DESC)");
            case 22:
                apply("CREATE INDEX IF NOT EXISTS unconfirmed_transaction_height_fee_timestamp_idx ON unconfirmed_transaction "
                        + "(transaction_height ASC, fee_per_byte DESC, arrival_timestamp ASC)");
            case 23:
                apply("CREATE TABLE IF NOT EXISTS scan (rescan BOOLEAN NOT NULL DEFAULT FALSE, height INT NOT NULL DEFAULT 0, "
                        + "validate BOOLEAN NOT NULL DEFAULT FALSE)");
            case 24:
                apply("INSERT INTO scan (rescan, height, validate) VALUES (false, 0, false)");

            case 25:
                apply("CREATE TABLE IF NOT EXISTS public_key (db_id IDENTITY, account_id BIGINT NOT NULL, "
                        + "public_key BINARY(32), height INT NOT NULL, FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 26:
                apply("CREATE INDEX IF NOT EXISTS account_guaranteed_balance_height_idx ON account_guaranteed_balance(height)");

            case 27:
                apply("CREATE TABLE IF NOT EXISTS account_info (db_id IDENTITY, account_id BIGINT NOT NULL, "
                        + "name VARCHAR, description VARCHAR, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 28:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_info_id_height_idx ON account_info (account_id, height DESC)");

            case 29:
                apply("CREATE TABLE IF NOT EXISTS tagged_data (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "name VARCHAR NOT NULL, description VARCHAR, tags VARCHAR, parsed_tags ARRAY, type VARCHAR, data VARBINARY NOT NULL, "
                        + "is_text BOOLEAN NOT NULL, channel VARCHAR, filename VARCHAR, block_timestamp INT NOT NULL, transaction_timestamp INT NOT NULL, "
                        + "height INT NOT NULL, FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 30:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS tagged_data_id_height_idx ON tagged_data (id, height DESC)");
            case 31:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_expiration_idx ON tagged_data (transaction_timestamp DESC)");
            case 32:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_account_id_height_idx ON tagged_data (account_id, height DESC)");
            case 33:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_block_timestamp_height_db_id_idx ON tagged_data (block_timestamp DESC, height DESC, db_id DESC)");
            case 34:
                apply("CREATE TABLE IF NOT EXISTS data_tag (db_id IDENTITY, tag VARCHAR NOT NULL, tag_count INT NOT NULL, "
                        + "height INT NOT NULL, FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 35:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS data_tag_tag_height_idx ON data_tag (tag, height DESC)");
            case 36:
                apply("CREATE INDEX IF NOT EXISTS data_tag_count_height_idx ON data_tag (tag_count DESC, height DESC)");
            case 37:
                apply("CREATE TABLE IF NOT EXISTS tagged_data_timestamp (db_id IDENTITY, id BIGINT NOT NULL, timestamp INT NOT NULL, "
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 38:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS tagged_data_timestamp_id_height_idx ON tagged_data_timestamp (id, height DESC)");
            case 39:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_channel_idx ON tagged_data (channel, height DESC)");
            case 40:
                apply("CREATE INDEX IF NOT EXISTS account_active_lessee_id_idx ON account (active_lessee_id)");

            case 41:
                apply("CREATE INDEX IF NOT EXISTS unconfirmed_transaction_expiration_idx ON unconfirmed_transaction (expiration DESC)");
            case 42:
                apply("CREATE INDEX IF NOT EXISTS account_height_id_idx ON account (height, id)");
            case 43:
                apply("CREATE INDEX IF NOT EXISTS tag_height_tag_idx ON tag (height, tag)");
            case 44:
                apply("CREATE INDEX IF NOT EXISTS account_info_height_id_idx ON account_info (height, account_id)");
            case 45:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_timestamp_height_id_idx ON tagged_data_timestamp (height, id)");

            case 46:
                apply("CREATE TABLE IF NOT EXISTS account_ledger (db_id IDENTITY, account_id BIGINT NOT NULL, "
                        + "event_type TINYINT NOT NULL, event_id BIGINT NOT NULL, holding_type TINYINT NOT NULL, "
                        + "holding_id BIGINT, change DECIMAL NOT NULL, balance DECIMAL NOT NULL, "
                        + "block_id BIGINT NOT NULL, height INT NOT NULL, timestamp INT NOT NULL)");
            case 47:
                apply("CREATE INDEX IF NOT EXISTS account_ledger_id_idx ON account_ledger(account_id, db_id)");
            case 48:
                apply("CREATE INDEX IF NOT EXISTS account_ledger_height_idx ON account_ledger(height)");
            case 49:
                apply("CREATE TABLE IF NOT EXISTS tagged_data_extend (db_id IDENTITY, id BIGINT NOT NULL, "
                        + "extend_id BIGINT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 50:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_extend_id_height_idx ON tagged_data_extend(id, height DESC)");
            case 51:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_extend_height_id_idx ON tagged_data_extend(height, id)");
            case 52:
                nxt.db.FullTextTrigger.init();
                apply(null);

            case 53:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS public_key_account_id_height_idx ON public_key (account_id, height DESC)");
            case 54:
                return;
            default:
                throw new RuntimeException("Blockchain database inconsistent with code, at update " + nextUpdate
                        + ", probably trying to run older code on newer database");
        }
    }
}
