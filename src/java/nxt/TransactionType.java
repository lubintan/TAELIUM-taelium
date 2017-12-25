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
import nxt.Account.ControlType;
import nxt.AccountLedger.LedgerEvent;
import nxt.Attachment.AbstractAttachment;
import nxt.NxtException.ValidationException;
import nxt.util.Convert;
import nxt.util.Logger;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class TransactionType {

	private static final byte TYPE_REWARD = 10;
    private static final byte TYPE_PAYMENT = 0;
    private static final byte TYPE_MESSAGING = 1;
    private static final byte TYPE_COLORED_COINS = 2;
    private static final byte TYPE_DIGITAL_GOODS = 3;
    private static final byte TYPE_ACCOUNT_CONTROL = 4;
    static final byte TYPE_MONETARY_SYSTEM = 5;
    private static final byte TYPE_DATA = 6;
    static final byte TYPE_SHUFFLING = 7;

    private static final byte SUBTYPE_REWARD_ORDINARY_REWARD = 0;
    
    private static final byte SUBTYPE_PAYMENT_ORDINARY_PAYMENT = 0;

    private static final byte SUBTYPE_MESSAGING_ARBITRARY_MESSAGE = 0;
    private static final byte SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT = 1;
    private static final byte SUBTYPE_MESSAGING_POLL_CREATION = 2;
    private static final byte SUBTYPE_MESSAGING_VOTE_CASTING = 3;
    private static final byte SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT = 4;
    private static final byte SUBTYPE_MESSAGING_ACCOUNT_INFO = 5;
    private static final byte SUBTYPE_MESSAGING_ALIAS_SELL = 6;
    private static final byte SUBTYPE_MESSAGING_ALIAS_BUY = 7;
    private static final byte SUBTYPE_MESSAGING_ALIAS_DELETE = 8;
    private static final byte SUBTYPE_MESSAGING_PHASING_VOTE_CASTING = 9;
    private static final byte SUBTYPE_MESSAGING_ACCOUNT_PROPERTY = 10;
    private static final byte SUBTYPE_MESSAGING_ACCOUNT_PROPERTY_DELETE = 11;

    private static final byte SUBTYPE_COLORED_COINS_ASSET_ISSUANCE = 0;
    private static final byte SUBTYPE_COLORED_COINS_ASSET_TRANSFER = 1;
    private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT = 2;
    private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT = 3;
    private static final byte SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION = 4;
    private static final byte SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION = 5;
    private static final byte SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT = 6;
    private static final byte SUBTYPE_COLORED_COINS_ASSET_DELETE = 7;

    private static final byte SUBTYPE_DIGITAL_GOODS_LISTING = 0;
    private static final byte SUBTYPE_DIGITAL_GOODS_DELISTING = 1;
    private static final byte SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE = 2;
    private static final byte SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE = 3;
    private static final byte SUBTYPE_DIGITAL_GOODS_PURCHASE = 4;
    private static final byte SUBTYPE_DIGITAL_GOODS_DELIVERY = 5;
    private static final byte SUBTYPE_DIGITAL_GOODS_FEEDBACK = 6;
    private static final byte SUBTYPE_DIGITAL_GOODS_REFUND = 7;

    private static final byte SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING = 0;
    private static final byte SUBTYPE_ACCOUNT_CONTROL_PHASING_ONLY = 1;

    private static final byte SUBTYPE_DATA_TAGGED_DATA_UPLOAD = 0;
    private static final byte SUBTYPE_DATA_TAGGED_DATA_EXTEND = 1;

    public static TransactionType findTransactionType(byte type, byte subtype) {
        switch (type) {
        		case TYPE_REWARD:
        			switch (subtype) {
        				case SUBTYPE_REWARD_ORDINARY_REWARD:
        					return Reward.ORDINARY_REWARD;
        				default:
        					return null;
        			}
            case TYPE_PAYMENT:
                switch (subtype) {
                    case SUBTYPE_PAYMENT_ORDINARY_PAYMENT:
                        return Payment.ORDINARY;
                    default:
                        return null;
                }
            case TYPE_MESSAGING:
                switch (subtype) {
                    case SUBTYPE_MESSAGING_ARBITRARY_MESSAGE:
                        return Messaging.ARBITRARY_MESSAGE;
//                    case SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT:
//                        return Messaging.ALIAS_ASSIGNMENT;
//                    case SUBTYPE_MESSAGING_POLL_CREATION:
//                        return Messaging.POLL_CREATION;
//                    case SUBTYPE_MESSAGING_VOTE_CASTING:
//                        return Messaging.VOTE_CASTING;
                    case SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT:
                        throw new IllegalArgumentException("Hub Announcement no longer supported");
//                    case SUBTYPE_MESSAGING_ACCOUNT_INFO:
//                        return Messaging.ACCOUNT_INFO;
//                    case SUBTYPE_MESSAGING_ALIAS_SELL:
//                        return Messaging.ALIAS_SELL;
//                    case SUBTYPE_MESSAGING_ALIAS_BUY:
//                        return Messaging.ALIAS_BUY;
//                    case SUBTYPE_MESSAGING_ALIAS_DELETE:
//                        return Messaging.ALIAS_DELETE;
//                    case SUBTYPE_MESSAGING_PHASING_VOTE_CASTING:
//                        return Messaging.PHASING_VOTE_CASTING;
//                    case SUBTYPE_MESSAGING_ACCOUNT_PROPERTY:
//                        return Messaging.ACCOUNT_PROPERTY;
//                    case SUBTYPE_MESSAGING_ACCOUNT_PROPERTY_DELETE:
//                        return Messaging.ACCOUNT_PROPERTY_DELETE;
                    default:
                        return null;
                }


           
            case TYPE_DATA:
                switch (subtype) {
                    case SUBTYPE_DATA_TAGGED_DATA_UPLOAD:
                        return Data.TAGGED_DATA_UPLOAD;
                    case SUBTYPE_DATA_TAGGED_DATA_EXTEND:
                        return Data.TAGGED_DATA_EXTEND;
                    default:
                        return null;
                }
//            case TYPE_SHUFFLING:
//                return ShufflingTransaction.findTransactionType(subtype);
            default:
                return null;
        }
    }


    TransactionType() {}

    public abstract byte getType();

    public abstract byte getSubtype();

    public abstract LedgerEvent getLedgerEvent();

    abstract Attachment.AbstractAttachment parseAttachment(ByteBuffer buffer) throws NxtException.NotValidException;

    abstract Attachment.AbstractAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException;

    abstract void validateAttachment(Transaction transaction) throws NxtException.ValidationException;

    // return false iff double spending
    final boolean applyUnconfirmed(TransactionImpl transaction, Account senderAccount) {
        BigInteger amountNQT = transaction.getAmountNQT();
        BigInteger feeNQT = transaction.getFeeNQT();
        if (transaction.referencedTransactionFullHash() != null) {
            feeNQT = feeNQT.add(Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
            
        }
        BigInteger totalAmountNQT = amountNQT.add(feeNQT);
        if (senderAccount.getUnconfirmedBalanceNQT().compareTo(totalAmountNQT) < 0) {
            return false;
        }
        senderAccount.addToUnconfirmedBalanceNQT(getLedgerEvent(), transaction.getId(), amountNQT.negate(), feeNQT.negate());
        if (!applyAttachmentUnconfirmed(transaction, senderAccount)) {
            senderAccount.addToUnconfirmedBalanceNQT(getLedgerEvent(), transaction.getId(), amountNQT, feeNQT);
            return false;
        }
        return true;
    }

    abstract boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    final void apply(TransactionImpl transaction, Account senderAccount, Account recipientAccount) {
    		BigInteger amount = transaction.getAmountNQT();
        long transactionId = transaction.getId();
        if (!transaction.attachmentIsPhased()) {
            senderAccount.addToBalanceNQT(getLedgerEvent(), transactionId, amount.negate(), transaction.getFeeNQT().negate());
        } else {
            senderAccount.addToBalanceNQT(getLedgerEvent(), transactionId, amount.negate());
        }
        if (recipientAccount != null) {
            recipientAccount.addToBalanceAndUnconfirmedBalanceNQT(getLedgerEvent(), transactionId, amount);
        }
        applyAttachment(transaction, senderAccount, recipientAccount);
    }

    abstract void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount);

    final void undoUnconfirmed(TransactionImpl transaction, Account senderAccount) {
        undoAttachmentUnconfirmed(transaction, senderAccount);
        senderAccount.addToUnconfirmedBalanceNQT(getLedgerEvent(), transaction.getId(),
                transaction.getAmountNQT(), transaction.getFeeNQT());
        if (transaction.referencedTransactionFullHash() != null) {
            senderAccount.addToUnconfirmedBalanceNQT(getLedgerEvent(), transaction.getId(), BigInteger.ZERO,
                    Constants.UNCONFIRMED_POOL_DEPOSIT_NQT);
        }
    }

    abstract void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return false;
    }

    // isBlockDuplicate and isDuplicate share the same duplicates map, but isBlockDuplicate check is done first
    boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return false;
    }

    boolean isUnconfirmedDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        return false;
    }

    static boolean isDuplicate(TransactionType uniqueType, String key, Map<TransactionType, Map<String, Integer>> duplicates, boolean exclusive) {
        return isDuplicate(uniqueType, key, duplicates, exclusive ? 0 : Integer.MAX_VALUE);
    }

    static boolean isDuplicate(TransactionType uniqueType, String key, Map<TransactionType, Map<String, Integer>> duplicates, int maxCount) {
        Map<String,Integer> typeDuplicates = duplicates.get(uniqueType);
        if (typeDuplicates == null) {
            typeDuplicates = new HashMap<>();
            duplicates.put(uniqueType, typeDuplicates);
        }
        Integer currentCount = typeDuplicates.get(key);
        if (currentCount == null) {
            typeDuplicates.put(key, maxCount > 0 ? 1 : 0);
            return false;
        }
        if (currentCount == 0) {
            return true;
        }
        if (currentCount < maxCount) {
            typeDuplicates.put(key, currentCount + 1);
            return false;
        }
        return true;
    }

    boolean isPruned(long transactionId) {
        return false;
    }

    public abstract boolean canHaveRecipient();

    public boolean mustHaveRecipient() {
        return canHaveRecipient();
    }

    public abstract boolean isPhasingSafe();

    public boolean isPhasable() {
        return true;
    }

    Fee getBaselineFee(Transaction transaction) {
        return Fee.DEFAULT_FEE;
    }

    Fee getNextFee(Transaction transaction) {
        return getBaselineFee(transaction);
    }

    int getBaselineFeeHeight() {
        return 1;
    }

    int getNextFeeHeight() {
        return Integer.MAX_VALUE;
    }

    BigInteger[] getBackFees(Transaction transaction) {
        return Convert.EMPTY_BIGINT;
    }

    public abstract String getName();

    @Override
    public final String toString() {
        return getName() + " type: " + getType() + ", subtype: " + getSubtype();
    }
    
    public static abstract class Reward extends TransactionType {

        private Reward() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_REWARD;
        }

        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return false;
        }

        @Override
        final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            if (recipientAccount == null) {
                Account.getAccount(Genesis.CREATOR_ID).addToBalanceAndUnconfirmedBalanceNQT(getLedgerEvent(),
                        transaction.getId(), transaction.getAmountNQT());
            }
        }

        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public final boolean canHaveRecipient() {
            return true;
        }

        @Override
        public final boolean isPhasingSafe() {
            return false;
        }

        public static final TransactionType ORDINARY_REWARD = new Payment() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_REWARD_ORDINARY_REWARD;
            }

            @Override
            public final LedgerEvent getLedgerEvent() {
                return LedgerEvent.ORDINARY_REWARD;
            }

            @Override
            public String getName() {
                return "OrdinaryReward";
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(ByteBuffer buffer) throws NxtException.NotValidException {
                return Attachment.ORDINARY_REWARD;
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return Attachment.ORDINARY_REWARD;
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (transaction.getAmountNQT().compareTo(BigInteger.ZERO) <= 0 || 
                		transaction.getAmountNQT().compareTo(Constants.MAX_BALANCE_HAEDS) >= 0) {
                    throw new NxtException.NotValidException("Invalid ordinary reward");
                }
            }

        };

    }


    public static abstract class Payment extends TransactionType {

        private Payment() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_PAYMENT;
        }

        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            if (recipientAccount == null) {
                Account.getAccount(Genesis.CREATOR_ID).addToBalanceAndUnconfirmedBalanceNQT(getLedgerEvent(),
                        transaction.getId(), transaction.getAmountNQT());
            }
        }

        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public final boolean canHaveRecipient() {
            return true;
        }

        @Override
        public final boolean isPhasingSafe() {
            return true;
        }

        public static final TransactionType ORDINARY = new Payment() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_PAYMENT_ORDINARY_PAYMENT;
            }

            @Override
            public final LedgerEvent getLedgerEvent() {
                return LedgerEvent.ORDINARY_PAYMENT;
            }

            @Override
            public String getName() {
                return "OrdinaryPayment";
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(ByteBuffer buffer) throws NxtException.NotValidException {
                return Attachment.ORDINARY_PAYMENT;
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return Attachment.ORDINARY_PAYMENT;
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                if (transaction.getAmountNQT().compareTo(BigInteger.ZERO) <= 0 || 
                		transaction.getAmountNQT().compareTo(Constants.MAX_BALANCE_HAEDS) >= 0) {
                    throw new NxtException.NotValidException("Invalid ordinary payment");
                }
            }

        };

    }

    public static abstract class Messaging extends TransactionType {

        private Messaging() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_MESSAGING;
        }

        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        public final static TransactionType ARBITRARY_MESSAGE = new Messaging() {

            @Override
            public final byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ARBITRARY_MESSAGE;
            }

            @Override
            public String getName() {
                return "ArbitraryMessage";
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(ByteBuffer buffer) throws NxtException.NotValidException {
                return Attachment.ARBITRARY_MESSAGE;
            }

            @Override
            Attachment.EmptyAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return Attachment.ARBITRARY_MESSAGE;
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment attachment = transaction.getAttachment();
                if (transaction.getAmountNQT().compareTo(BigInteger.ZERO) != 0) {
                    throw new NxtException.NotValidException("Invalid arbitrary message: " + attachment.getJSONObject());
                }
                if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                    throw new NxtException.NotValidException("Sending messages to Genesis not allowed.");
                }
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean mustHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return false;
            }

        };
//
//        public static final TransactionType ALIAS_ASSIGNMENT = new Messaging() {
//
//            private final Fee ALIAS_FEE = new Fee.SizeBasedFee(2 * Constants.ONE_NXT, 2 * Constants.ONE_NXT, 32) {
//                @Override
//                public int getSize(TransactionImpl transaction, Appendix appendage) {
//                    Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
//                    return attachment.getAliasName().length() + attachment.getAliasURI().length();
//                }
//            };
//
//            @Override
//            public final byte getSubtype() {
//                return TransactionType.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT;
//            }
//
//            @Override
//            public LedgerEvent getLedgerEvent() {
//                return LedgerEvent.ALIAS_ASSIGNMENT;
//            }
//
//            @Override
//            public String getName() {
//                return "AliasAssignment";
//            }
//
//            @Override
//            Fee getBaselineFee(Transaction transaction) {
//                return ALIAS_FEE;
//            }
//
//            @Override
//            Attachment.MessagingAliasAssignment parseAttachment(ByteBuffer buffer) throws NxtException.NotValidException {
//                return new Attachment.MessagingAliasAssignment(buffer);
//            }
//
//            @Override
//            Attachment.MessagingAliasAssignment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
//                return new Attachment.MessagingAliasAssignment(attachmentData);
//            }
//
//            @Override
//            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
//                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
//                Alias.addOrUpdateAlias(transaction, attachment);
//            }
//
//            @Override
//            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
//                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
//                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
//            }
//
//            @Override
//            boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
//                return Alias.getAlias(((Attachment.MessagingAliasAssignment) transaction.getAttachment()).getAliasName()) == null
//                        && isDuplicate(Messaging.ALIAS_ASSIGNMENT, "", duplicates, true);
//            }
//
//            @Override
//            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
//                Attachment.MessagingAliasAssignment attachment = (Attachment.MessagingAliasAssignment) transaction.getAttachment();
//                if (attachment.getAliasName().length() == 0
//                        || attachment.getAliasName().length() > Constants.MAX_ALIAS_LENGTH
//                        || attachment.getAliasURI().length() > Constants.MAX_ALIAS_URI_LENGTH) {
//                    throw new NxtException.NotValidException("Invalid alias assignment: " + attachment.getJSONObject());
//                }
//                String normalizedAlias = attachment.getAliasName().toLowerCase();
//                for (int i = 0; i < normalizedAlias.length(); i++) {
//                    if (Constants.ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
//                        throw new NxtException.NotValidException("Invalid alias name: " + normalizedAlias);
//                    }
//                }
//                Alias alias = Alias.getAlias(normalizedAlias);
//                if (alias != null && alias.getAccountId() != transaction.getSenderId()) {
//                    throw new NxtException.NotCurrentlyValidException("Alias already owned by another account: " + normalizedAlias);
//                }
//            }
//
//            @Override
//            public boolean canHaveRecipient() {
//                return false;
//            }
//
//            @Override
//            public boolean isPhasingSafe() {
//                return false;
//            }
//
//        };
//
//        public static final TransactionType ALIAS_SELL = new Messaging() {
//
//            @Override
//            public final byte getSubtype() {
//                return TransactionType.SUBTYPE_MESSAGING_ALIAS_SELL;
//            }
//
//            @Override
//            public LedgerEvent getLedgerEvent() {
//                return LedgerEvent.ALIAS_SELL;
//            }
//            @Override
//            public String getName() {
//                return "AliasSell";
//            }
//
//            @Override
//            Attachment.MessagingAliasSell parseAttachment(ByteBuffer buffer) throws NxtException.NotValidException {
//                return new Attachment.MessagingAliasSell(buffer);
//            }
//
//            @Override
//            Attachment.MessagingAliasSell parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
//                return new Attachment.MessagingAliasSell(attachmentData);
//            }
//
//            @Override
//            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
//                Attachment.MessagingAliasSell attachment = (Attachment.MessagingAliasSell) transaction.getAttachment();
//                Alias.sellAlias(transaction, attachment);
//            }
//
//            @Override
//            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
//                Attachment.MessagingAliasSell attachment = (Attachment.MessagingAliasSell) transaction.getAttachment();
//                // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
//                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
//            }
//
//            @Override
//            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
//                if (transaction.getAmountNQT() != 0) {
//                    throw new NxtException.NotValidException("Invalid sell alias transaction: " +
//                            transaction.getJSONObject());
//                }
//                final Attachment.MessagingAliasSell attachment =
//                        (Attachment.MessagingAliasSell) transaction.getAttachment();
//                final String aliasName = attachment.getAliasName();
//                if (aliasName == null || aliasName.length() == 0) {
//                    throw new NxtException.NotValidException("Missing alias name");
//                }
//                long priceNQT = attachment.getPriceNQT();
//                if (priceNQT < 0 || priceNQT > Constants.MAX_BALANCE_HAEDS) {
//                    throw new NxtException.NotValidException("Invalid alias sell price: " + priceNQT);
//                }
//                if (priceNQT == 0) {
//                    if (Genesis.CREATOR_ID == transaction.getRecipientId()) {
//                        throw new NxtException.NotValidException("Transferring aliases to Genesis account not allowed");
//                    } else if (transaction.getRecipientId() == 0) {
//                        throw new NxtException.NotValidException("Missing alias transfer recipient");
//                    }
//                }
//                final Alias alias = Alias.getAlias(aliasName);
//                if (alias == null) {
//                    throw new NxtException.NotCurrentlyValidException("No such alias: " + aliasName);
//                } else if (alias.getAccountId() != transaction.getSenderId()) {
//                    throw new NxtException.NotCurrentlyValidException("Alias doesn't belong to sender: " + aliasName);
//                }
//                if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
//                    throw new NxtException.NotValidException("Selling alias to Genesis not allowed");
//                }
//            }
//
//            @Override
//            public boolean canHaveRecipient() {
//                return true;
//            }
//
//            @Override
//            public boolean mustHaveRecipient() {
//                return false;
//            }
//
//            @Override
//            public boolean isPhasingSafe() {
//                return false;
//            }
//
//        };
//
//        public static final TransactionType ALIAS_BUY = new Messaging() {
//
//            @Override
//            public final byte getSubtype() {
//                return TransactionType.SUBTYPE_MESSAGING_ALIAS_BUY;
//            }
//
//            @Override
//            public LedgerEvent getLedgerEvent() {
//                return LedgerEvent.ALIAS_BUY;
//            }
//
//            @Override
//            public String getName() {
//                return "AliasBuy";
//            }
//
//            @Override
//            Attachment.MessagingAliasBuy parseAttachment(ByteBuffer buffer) throws NxtException.NotValidException {
//                return new Attachment.MessagingAliasBuy(buffer);
//            }
//
//            @Override
//            Attachment.MessagingAliasBuy parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
//                return new Attachment.MessagingAliasBuy(attachmentData);
//            }
//
//            @Override
//            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
//                final Attachment.MessagingAliasBuy attachment =
//                        (Attachment.MessagingAliasBuy) transaction.getAttachment();
//                final String aliasName = attachment.getAliasName();
//                Alias.changeOwner(transaction.getSenderId(), aliasName);
//            }
//
//            @Override
//            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
//                Attachment.MessagingAliasBuy attachment = (Attachment.MessagingAliasBuy) transaction.getAttachment();
//                // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
//                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
//            }
//
//            @Override
//            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
//                final Attachment.MessagingAliasBuy attachment =
//                        (Attachment.MessagingAliasBuy) transaction.getAttachment();
//                final String aliasName = attachment.getAliasName();
//                final Alias alias = Alias.getAlias(aliasName);
//                if (alias == null) {
//                    throw new NxtException.NotCurrentlyValidException("No such alias: " + aliasName);
//                } else if (alias.getAccountId() != transaction.getRecipientId()) {
//                    throw new NxtException.NotCurrentlyValidException("Alias is owned by account other than recipient: "
//                            + Long.toUnsignedString(alias.getAccountId()));
//                }
//                Alias.Offer offer = Alias.getOffer(alias);
//                if (offer == null) {
//                    throw new NxtException.NotCurrentlyValidException("Alias is not for sale: " + aliasName);
//                }
//                if (transaction.getAmountNQT() < offer.getPriceNQT()) {
//                    String msg = "Price is too low for: " + aliasName + " ("
//                            + transaction.getAmountNQT() + " < " + offer.getPriceNQT() + ")";
//                    throw new NxtException.NotCurrentlyValidException(msg);
//                }
//                if (offer.getBuyerId() != 0 && offer.getBuyerId() != transaction.getSenderId()) {
//                    throw new NxtException.NotCurrentlyValidException("Wrong buyer for " + aliasName + ": "
//                            + Long.toUnsignedString(transaction.getSenderId()) + " expected: "
//                            + Long.toUnsignedString(offer.getBuyerId()));
//                }
//            }
//
//            @Override
//            public boolean canHaveRecipient() {
//                return true;
//            }
//
//            @Override
//            public boolean isPhasingSafe() {
//                return false;
//            }
//
//        };
//
//        public static final TransactionType ALIAS_DELETE = new Messaging() {
//
//            @Override
//            public final byte getSubtype() {
//                return TransactionType.SUBTYPE_MESSAGING_ALIAS_DELETE;
//            }
//
//            @Override
//            public LedgerEvent getLedgerEvent() {
//                return LedgerEvent.ALIAS_DELETE;
//            }
//
//            @Override
//            public String getName() {
//                return "AliasDelete";
//            }
//
//            @Override
//            Attachment.MessagingAliasDelete parseAttachment(final ByteBuffer buffer) throws NxtException.NotValidException {
//                return new Attachment.MessagingAliasDelete(buffer);
//            }
//
//            @Override
//            Attachment.MessagingAliasDelete parseAttachment(final JSONObject attachmentData) throws NxtException.NotValidException {
//                return new Attachment.MessagingAliasDelete(attachmentData);
//            }
//
//            @Override
//            void applyAttachment(final Transaction transaction, final Account senderAccount, final Account recipientAccount) {
//                final Attachment.MessagingAliasDelete attachment =
//                        (Attachment.MessagingAliasDelete) transaction.getAttachment();
//                Alias.deleteAlias(attachment.getAliasName());
//            }
//
//            @Override
//            boolean isDuplicate(final Transaction transaction, final Map<TransactionType, Map<String, Integer>> duplicates) {
//                Attachment.MessagingAliasDelete attachment = (Attachment.MessagingAliasDelete) transaction.getAttachment();
//                // not a bug, uniqueness is based on Messaging.ALIAS_ASSIGNMENT
//                return isDuplicate(Messaging.ALIAS_ASSIGNMENT, attachment.getAliasName().toLowerCase(), duplicates, true);
//            }
//
//            @Override
//            void validateAttachment(final Transaction transaction) throws NxtException.ValidationException {
//                final Attachment.MessagingAliasDelete attachment =
//                        (Attachment.MessagingAliasDelete) transaction.getAttachment();
//                final String aliasName = attachment.getAliasName();
//                if (aliasName == null || aliasName.length() == 0) {
//                    throw new NxtException.NotValidException("Missing alias name");
//                }
//                final Alias alias = Alias.getAlias(aliasName);
//                if (alias == null) {
//                    throw new NxtException.NotCurrentlyValidException("No such alias: " + aliasName);
//                } else if (alias.getAccountId() != transaction.getSenderId()) {
//                    throw new NxtException.NotCurrentlyValidException("Alias doesn't belong to sender: " + aliasName);
//                }
//            }
//
//            @Override
//            public boolean canHaveRecipient() {
//                return false;
//            }
//
//            @Override
//            public boolean isPhasingSafe() {
//                return false;
//            }
//
//        };
//
//        public final static TransactionType POLL_CREATION = new Messaging() {
//
//            private final Fee POLL_OPTIONS_FEE = new Fee.SizeBasedFee(10 * Constants.ONE_NXT, Constants.ONE_NXT, 1) {
//                @Override
//                public int getSize(TransactionImpl transaction, Appendix appendage) {
//                    int numOptions = ((Attachment.MessagingPollCreation)appendage).getPollOptions().length;
//                    return numOptions <= 19 ? 0 : numOptions - 19;
//                }
//            };
//
//            private final Fee POLL_SIZE_FEE = new Fee.SizeBasedFee(0, 2 * Constants.ONE_NXT, 32) {
//                @Override
//                public int getSize(TransactionImpl transaction, Appendix appendage) {
//                    Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation)appendage;
//                    int size = attachment.getPollName().length() + attachment.getPollDescription().length();
//                    for (String option : ((Attachment.MessagingPollCreation)appendage).getPollOptions()) {
//                        size += option.length();
//                    }
//                    return size <= 288 ? 0 : size - 288;
//                }
//            };
//
//            private final Fee POLL_FEE = (transaction, appendage) ->
//                    POLL_OPTIONS_FEE.getFee(transaction, appendage) + POLL_SIZE_FEE.getFee(transaction, appendage);
//
//            @Override
//            public final byte getSubtype() {
//                return TransactionType.SUBTYPE_MESSAGING_POLL_CREATION;
//            }
//
//            @Override
//            public LedgerEvent getLedgerEvent() {
//                return LedgerEvent.POLL_CREATION;
//            }
//
//            @Override
//            public String getName() {
//                return "PollCreation";
//            }
//
//            @Override
//            Fee getBaselineFee(Transaction transaction) {
//                return POLL_FEE;
//            }
//
//            @Override
//            Attachment.MessagingPollCreation parseAttachment(ByteBuffer buffer) throws NxtException.NotValidException {
//                return new Attachment.MessagingPollCreation(buffer);
//            }
//
//            @Override
//            Attachment.MessagingPollCreation parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
//                return new Attachment.MessagingPollCreation(attachmentData);
//            }
//
//            @Override
//            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
//                Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation) transaction.getAttachment();
//                Poll.addPoll(transaction, attachment);
//            }
//
//            @Override
//            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
//
//                Attachment.MessagingPollCreation attachment = (Attachment.MessagingPollCreation) transaction.getAttachment();
//
//                int optionsCount = attachment.getPollOptions().length;
//
//                if (attachment.getPollName().length() > Constants.MAX_POLL_NAME_LENGTH
//                        || attachment.getPollName().isEmpty()
//                        || attachment.getPollDescription().length() > Constants.MAX_POLL_DESCRIPTION_LENGTH
//                        || optionsCount > Constants.MAX_POLL_OPTION_COUNT
//                        || optionsCount == 0) {
//                    throw new NxtException.NotValidException("Invalid poll attachment: " + attachment.getJSONObject());
//                }
//
//                if (attachment.getMinNumberOfOptions() < 1
//                        || attachment.getMinNumberOfOptions() > optionsCount) {
//                    throw new NxtException.NotValidException("Invalid min number of options: " + attachment.getJSONObject());
//                }
//
//                if (attachment.getMaxNumberOfOptions() < 1
//                        || attachment.getMaxNumberOfOptions() < attachment.getMinNumberOfOptions()
//                        || attachment.getMaxNumberOfOptions() > optionsCount) {
//                    throw new NxtException.NotValidException("Invalid max number of options: " + attachment.getJSONObject());
//                }
//
//                for (int i = 0; i < optionsCount; i++) {
//                    if (attachment.getPollOptions()[i].length() > Constants.MAX_POLL_OPTION_LENGTH
//                            || attachment.getPollOptions()[i].isEmpty()) {
//                        throw new NxtException.NotValidException("Invalid poll options length: " + attachment.getJSONObject());
//                    }
//                }
//
//                if (attachment.getMinRangeValue() < Constants.MIN_VOTE_VALUE || attachment.getMaxRangeValue() > Constants.MAX_VOTE_VALUE
//                        || attachment.getMaxRangeValue() < attachment.getMinRangeValue()) {
//                    throw new NxtException.NotValidException("Invalid range: " + attachment.getJSONObject());
//                }
//
//                if (attachment.getFinishHeight() <= attachment.getFinishValidationHeight(transaction) + 1
//                        || attachment.getFinishHeight() >= attachment.getFinishValidationHeight(transaction) + Constants.MAX_POLL_DURATION) {
//                    throw new NxtException.NotCurrentlyValidException("Invalid finishing height" + attachment.getJSONObject());
//                }
//
//                if (! attachment.getVoteWeighting().acceptsVotes() || attachment.getVoteWeighting().getVotingModel() == VoteWeighting.VotingModel.HASH) {
//                    throw new NxtException.NotValidException("VotingModel " + attachment.getVoteWeighting().getVotingModel() + " not valid for regular polls");
//                }
//
//                attachment.getVoteWeighting().validate();
//
//            }
//
//            @Override
//            boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
//                return isDuplicate(Messaging.POLL_CREATION, getName(), duplicates, true);
//            }
//
//            @Override
//            public boolean canHaveRecipient() {
//                return false;
//            }
//
//            @Override
//            public boolean isPhasingSafe() {
//                return false;
//            }
//
//        };
//
//        public final static TransactionType VOTE_CASTING = new Messaging() {
//
//            @Override
//            public final byte getSubtype() {
//                return TransactionType.SUBTYPE_MESSAGING_VOTE_CASTING;
//            }
//
//            @Override
//            public LedgerEvent getLedgerEvent() {
//                return LedgerEvent.VOTE_CASTING;
//            }
//
//            @Override
//            public String getName() {
//                return "VoteCasting";
//            }
//
//            @Override
//            Attachment.MessagingVoteCasting parseAttachment(ByteBuffer buffer) throws NxtException.NotValidException {
//                return new Attachment.MessagingVoteCasting(buffer);
//            }
//
//            @Override
//            Attachment.MessagingVoteCasting parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
//                return new Attachment.MessagingVoteCasting(attachmentData);
//            }
//
//            @Override
//            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
//                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
//                Vote.addVote(transaction, attachment);
//            }
//
//            @Override
//            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
//
//                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
//                if (attachment.getPollId() == 0 || attachment.getPollVote() == null
//                        || attachment.getPollVote().length > Constants.MAX_POLL_OPTION_COUNT) {
//                    throw new NxtException.NotValidException("Invalid vote casting attachment: " + attachment.getJSONObject());
//                }
//
//                long pollId = attachment.getPollId();
//
//                Poll poll = Poll.getPoll(pollId);
//                if (poll == null) {
//                    throw new NxtException.NotCurrentlyValidException("Invalid poll: " + Long.toUnsignedString(attachment.getPollId()));
//                }
//
//                if (Vote.getVote(pollId, transaction.getSenderId()) != null) {
//                    throw new NxtException.NotCurrentlyValidException("Double voting attempt");
//                }
//
//                if (poll.getFinishHeight() <= attachment.getFinishValidationHeight(transaction)) {
//                    throw new NxtException.NotCurrentlyValidException("Voting for this poll finishes at " + poll.getFinishHeight());
//                }
//
//                byte[] votes = attachment.getPollVote();
//                int positiveCount = 0;
//                for (byte vote : votes) {
//                    if (vote != Constants.NO_VOTE_VALUE && (vote < poll.getMinRangeValue() || vote > poll.getMaxRangeValue())) {
//                        throw new NxtException.NotValidException(String.format("Invalid vote %d, vote must be between %d and %d",
//                                vote, poll.getMinRangeValue(), poll.getMaxRangeValue()));
//                    }
//                    if (vote != Constants.NO_VOTE_VALUE) {
//                        positiveCount++;
//                    }
//                }
//
//                if (positiveCount < poll.getMinNumberOfOptions() || positiveCount > poll.getMaxNumberOfOptions()) {
//                    throw new NxtException.NotValidException(String.format("Invalid num of choices %d, number of choices must be between %d and %d",
//                            positiveCount, poll.getMinNumberOfOptions(), poll.getMaxNumberOfOptions()));
//                }
//            }
//
//            @Override
//            boolean isDuplicate(final Transaction transaction, final Map<TransactionType, Map<String, Integer>> duplicates) {
//                Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
//                String key = Long.toUnsignedString(attachment.getPollId()) + ":" + Long.toUnsignedString(transaction.getSenderId());
//                return isDuplicate(Messaging.VOTE_CASTING, key, duplicates, true);
//            }
//
//            @Override
//            public boolean canHaveRecipient() {
//                return false;
//            }
//
//            @Override
//            public boolean isPhasingSafe() {
//                return false;
//            }
//
//        };
//
//        public static final TransactionType PHASING_VOTE_CASTING = new Messaging() {
//
//            private final Fee PHASING_VOTE_FEE = (transaction, appendage) -> {
//                Attachment.MessagingPhasingVoteCasting attachment = (Attachment.MessagingPhasingVoteCasting) transaction.getAttachment();
//                return attachment.getTransactionFullHashes().size() * Constants.ONE_NXT;
//            };
//
//            @Override
//            public final byte getSubtype() {
//                return TransactionType.SUBTYPE_MESSAGING_PHASING_VOTE_CASTING;
//            }
//
//            @Override
//            public LedgerEvent getLedgerEvent() {
//                return LedgerEvent.PHASING_VOTE_CASTING;
//            }
//
//            @Override
//            public String getName() {
//                return "PhasingVoteCasting";
//            }
//
//            @Override
//            Fee getBaselineFee(Transaction transaction) {
//                return PHASING_VOTE_FEE;
//            }
//
//            @Override
//            Attachment.MessagingPhasingVoteCasting parseAttachment(ByteBuffer buffer) throws NxtException.NotValidException {
//                return new Attachment.MessagingPhasingVoteCasting(buffer);
//            }
//
//            @Override
//            Attachment.MessagingPhasingVoteCasting parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
//                return new Attachment.MessagingPhasingVoteCasting(attachmentData);
//            }
//
//            @Override
//            public boolean canHaveRecipient() {
//                return false;
//            }
//
//            @Override
//            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
//
//                Attachment.MessagingPhasingVoteCasting attachment = (Attachment.MessagingPhasingVoteCasting) transaction.getAttachment();
//                byte[] revealedSecret = attachment.getRevealedSecret();
//                if (revealedSecret.length > Constants.MAX_PHASING_REVEALED_SECRET_LENGTH) {
//                    throw new NxtException.NotValidException("Invalid revealed secret length " + revealedSecret.length);
//                }
//                byte[] hashedSecret = null;
//                byte algorithm = 0;
//
//                List<byte[]> hashes = attachment.getTransactionFullHashes();
//                if (hashes.size() > Constants.MAX_PHASING_VOTE_TRANSACTIONS) {
//                    throw new NxtException.NotValidException("No more than " + Constants.MAX_PHASING_VOTE_TRANSACTIONS + " votes allowed for two-phased multi-voting");
//                }
//
//                long voterId = transaction.getSenderId();
//                for (byte[] hash : hashes) {
//                    long phasedTransactionId = Convert.fullHashToId(hash);
//                    if (phasedTransactionId == 0) {
//                        throw new NxtException.NotValidException("Invalid phased transactionFullHash " + Convert.toHexString(hash));
//                    }
//
//                    PhasingPoll poll = PhasingPoll.getPoll(phasedTransactionId);
//                    if (poll == null) {
//                        throw new NxtException.NotCurrentlyValidException("Invalid phased transaction " + Long.toUnsignedString(phasedTransactionId)
//                                + ", or phasing is finished");
//                    }
//                    if (! poll.getVoteWeighting().acceptsVotes()) {
//                        throw new NxtException.NotValidException("This phased transaction does not require or accept voting");
//                    }
//                    long[] whitelist = poll.getWhitelist();
//                    if (whitelist.length > 0 && Arrays.binarySearch(whitelist, voterId) < 0) {
//                        throw new NxtException.NotValidException("Voter is not in the phased transaction whitelist");
//                    }
//                    if (revealedSecret.length > 0) {
//                        if (poll.getVoteWeighting().getVotingModel() != VoteWeighting.VotingModel.HASH) {
//                            throw new NxtException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " does not accept by-hash voting");
//                        }
//                        if (hashedSecret != null && !Arrays.equals(poll.getHashedSecret(), hashedSecret)) {
//                            throw new NxtException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " is using a different hashedSecret");
//                        }
//                        if (algorithm != 0 && algorithm != poll.getAlgorithm()) {
//                            throw new NxtException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " is using a different hashedSecretAlgorithm");
//                        }
//                        if (hashedSecret == null && ! poll.verifySecret(revealedSecret)) {
//                            throw new NxtException.NotValidException("Revealed secret does not match phased transaction hashed secret");
//                        }
//                        hashedSecret = poll.getHashedSecret();
//                        algorithm = poll.getAlgorithm();
//                    } else if (poll.getVoteWeighting().getVotingModel() == VoteWeighting.VotingModel.HASH) {
//                        throw new NxtException.NotValidException("Phased transaction " + Long.toUnsignedString(phasedTransactionId) + " requires revealed secret for approval");
//                    }
//                    if (!Arrays.equals(poll.getFullHash(), hash)) {
//                        throw new NxtException.NotCurrentlyValidException("Phased transaction hash does not match hash in voting transaction");
//                    }
//                    if (poll.getFinishHeight() <= attachment.getFinishValidationHeight(transaction) + 1) {
//                        throw new NxtException.NotCurrentlyValidException(String.format("Phased transaction finishes at height %d which is not after approval transaction height %d",
//                                poll.getFinishHeight(), attachment.getFinishValidationHeight(transaction) + 1));
//                    }
//                }
//            }
//
//            @Override
//            final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
//                Attachment.MessagingPhasingVoteCasting attachment = (Attachment.MessagingPhasingVoteCasting) transaction.getAttachment();
//                List<byte[]> hashes = attachment.getTransactionFullHashes();
//                for (byte[] hash : hashes) {
//                    PhasingVote.addVote(transaction, senderAccount, Convert.fullHashToId(hash));
//                }
//            }
//
//            @Override
//            public boolean isPhasingSafe() {
//                return true;
//            }
//
//        };
//
        public static final Messaging ACCOUNT_INFO = new Messaging() {

            private final Fee ACCOUNT_INFO_FEE = new Fee.SizeBasedFee(Constants.ONE_TAEL, BigInteger.valueOf(2).multiply(Constants.ONE_TAEL), 32) {
                @Override
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    Attachment.MessagingAccountInfo attachment = (Attachment.MessagingAccountInfo) transaction.getAttachment();
                    return attachment.getName().length() + attachment.getDescription().length();
                }
            };

            @Override
            public byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ACCOUNT_INFO;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ACCOUNT_INFO;
            }

            @Override
            public String getName() {
                return "AccountInfo";
            }

            @Override
            Fee getBaselineFee(Transaction transaction) {
                return ACCOUNT_INFO_FEE;
            }

            @Override
            Attachment.MessagingAccountInfo parseAttachment(ByteBuffer buffer) throws NxtException.NotValidException {
                return new Attachment.MessagingAccountInfo(buffer);
            }

            @Override
            Attachment.MessagingAccountInfo parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingAccountInfo(attachmentData);
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.MessagingAccountInfo attachment = (Attachment.MessagingAccountInfo)transaction.getAttachment();
                if (attachment.getName().length() > Constants.MAX_ACCOUNT_NAME_LENGTH
                        || attachment.getDescription().length() > Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH) {
                    throw new NxtException.NotValidException("Invalid account info issuance: " + attachment.getJSONObject());
                }
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAccountInfo attachment = (Attachment.MessagingAccountInfo) transaction.getAttachment();
                senderAccount.setAccountInfo(attachment.getName(), attachment.getDescription());
            }

            @Override
            boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
                return isDuplicate(Messaging.ACCOUNT_INFO, getName(), duplicates, true);
            }

            @Override
            public boolean canHaveRecipient() {
                return false;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };
//
        public static final Messaging ACCOUNT_PROPERTY = new Messaging() {

            private final Fee ACCOUNT_PROPERTY_FEE = new Fee.SizeBasedFee(Constants.ONE_TAEL, Constants.ONE_TAEL, 32) {
                @Override
                public int getSize(TransactionImpl transaction, Appendix appendage) {
                    Attachment.MessagingAccountProperty attachment = (Attachment.MessagingAccountProperty) transaction.getAttachment();
                    return attachment.getValue().length();
                }
            };

            @Override
            public byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ACCOUNT_PROPERTY;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ACCOUNT_PROPERTY;
            }

            @Override
            public String getName() {
                return "AccountProperty";
            }

            @Override
            Fee getBaselineFee(Transaction transaction) {
                return ACCOUNT_PROPERTY_FEE;
            }

            @Override
            Attachment.MessagingAccountProperty parseAttachment(ByteBuffer buffer) throws NxtException.NotValidException {
                return new Attachment.MessagingAccountProperty(buffer);
            }

            @Override
            Attachment.MessagingAccountProperty parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingAccountProperty(attachmentData);
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.MessagingAccountProperty attachment = (Attachment.MessagingAccountProperty)transaction.getAttachment();
                if (attachment.getProperty().length() > Constants.MAX_ACCOUNT_PROPERTY_NAME_LENGTH
                        || attachment.getProperty().length() == 0
                        || attachment.getValue().length() > Constants.MAX_ACCOUNT_PROPERTY_VALUE_LENGTH) {
                    throw new NxtException.NotValidException("Invalid account property: " + attachment.getJSONObject());
                }
                if (transaction.getAmountNQT().compareTo(BigInteger.ZERO) != 0) {
                    throw new NxtException.NotValidException("Account property transaction cannot be used to send " + Constants.COIN_SYMBOL);
                }
                if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                    throw new NxtException.NotValidException("Setting Genesis account properties not allowed");
                }
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAccountProperty attachment = (Attachment.MessagingAccountProperty) transaction.getAttachment();
                recipientAccount.setProperty(transaction, senderAccount, attachment.getProperty(), attachment.getValue());
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

        public static final Messaging ACCOUNT_PROPERTY_DELETE = new Messaging() {

            @Override
            public byte getSubtype() {
                return TransactionType.SUBTYPE_MESSAGING_ACCOUNT_PROPERTY_DELETE;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.ACCOUNT_PROPERTY_DELETE;
            }

            @Override
            public String getName() {
                return "AccountPropertyDelete";
            }

            @Override
            Attachment.MessagingAccountPropertyDelete parseAttachment(ByteBuffer buffer) throws NxtException.NotValidException {
                return new Attachment.MessagingAccountPropertyDelete(buffer);
            }

            @Override
            Attachment.MessagingAccountPropertyDelete parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.MessagingAccountPropertyDelete(attachmentData);
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.MessagingAccountPropertyDelete attachment = (Attachment.MessagingAccountPropertyDelete)transaction.getAttachment();
                Account.AccountProperty accountProperty = Account.getProperty(attachment.getPropertyId());
                if (accountProperty == null) {
                    throw new NxtException.NotCurrentlyValidException("No such property " + Long.toUnsignedString(attachment.getPropertyId()));
                }
                if (accountProperty.getRecipientId() != transaction.getSenderId() && accountProperty.getSetterId() != transaction.getSenderId()) {
                    throw new NxtException.NotValidException("Account " + Long.toUnsignedString(transaction.getSenderId())
                            + " cannot delete property " + Long.toUnsignedString(attachment.getPropertyId()));
                }
                if (accountProperty.getRecipientId() != transaction.getRecipientId()) {
                    throw new NxtException.NotValidException("Account property " + Long.toUnsignedString(attachment.getPropertyId())
                            + " does not belong to " + Long.toUnsignedString(transaction.getRecipientId()));
                }
                if (transaction.getAmountNQT().compareTo(BigInteger.ZERO) != 0) {
                    throw new NxtException.NotValidException("Account property transaction cannot be used to send " + Constants.COIN_SYMBOL);
                }
                if (transaction.getRecipientId() == Genesis.CREATOR_ID) {
                    throw new NxtException.NotValidException("Deleting Genesis account properties not allowed");
                }
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.MessagingAccountPropertyDelete attachment = (Attachment.MessagingAccountPropertyDelete) transaction.getAttachment();
                senderAccount.deleteProperty(attachment.getPropertyId());
            }

            @Override
            public boolean canHaveRecipient() {
                return true;
            }

            @Override
            public boolean isPhasingSafe() {
                return true;
            }

        };

    }


    public static abstract class Data extends TransactionType {

        private static final Fee TAGGED_DATA_FEE = new Fee.SizeBasedFee(Constants.ONE_TAEL, Constants.ONE_TAEL.divide(BigInteger.TEN)) {
            @Override
            public int getSize(TransactionImpl transaction, Appendix appendix) {
                return appendix.getFullSize();
            }
        };

        private Data() {
        }

        @Override
        public final byte getType() {
            return TransactionType.TYPE_DATA;
        }

        @Override
        final Fee getBaselineFee(Transaction transaction) {
            return TAGGED_DATA_FEE;
        }

        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }

        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }

        @Override
        public final boolean canHaveRecipient() {
            return false;
        }

        @Override
        public final boolean isPhasingSafe() {
            return false;
        }

        @Override
        public final boolean isPhasable() {
            return false;
        }

        public static final TransactionType TAGGED_DATA_UPLOAD = new Data() {

            @Override
            public byte getSubtype() {
                return SUBTYPE_DATA_TAGGED_DATA_UPLOAD;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.TAGGED_DATA_UPLOAD;
            }

            @Override
            Attachment.TaggedDataUpload parseAttachment(ByteBuffer buffer) throws NxtException.NotValidException {
                return new Attachment.TaggedDataUpload(buffer);
            }

            @Override
            Attachment.TaggedDataUpload parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.TaggedDataUpload(attachmentData);
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.TaggedDataUpload attachment = (Attachment.TaggedDataUpload) transaction.getAttachment();
                if (attachment.getData() == null && Nxt.getEpochTime() - transaction.getTimestamp() < Constants.MIN_PRUNABLE_LIFETIME) {
                    throw new NxtException.NotCurrentlyValidException("Data has been pruned prematurely");
                }
                if (attachment.getData() != null) {
                    if (attachment.getName().length() == 0 || attachment.getName().length() > Constants.MAX_TAGGED_DATA_NAME_LENGTH) {
                        throw new NxtException.NotValidException("Invalid name length: " + attachment.getName().length());
                    }
                    if (attachment.getDescription().length() > Constants.MAX_TAGGED_DATA_DESCRIPTION_LENGTH) {
                        throw new NxtException.NotValidException("Invalid description length: " + attachment.getDescription().length());
                    }
                    if (attachment.getTags().length() > Constants.MAX_TAGGED_DATA_TAGS_LENGTH) {
                        throw new NxtException.NotValidException("Invalid tags length: " + attachment.getTags().length());
                    }
                    if (attachment.getType().length() > Constants.MAX_TAGGED_DATA_TYPE_LENGTH) {
                        throw new NxtException.NotValidException("Invalid type length: " + attachment.getType().length());
                    }
                    if (attachment.getChannel().length() > Constants.MAX_TAGGED_DATA_CHANNEL_LENGTH) {
                        throw new NxtException.NotValidException("Invalid channel length: " + attachment.getChannel().length());
                    }
                    if (attachment.getFilename().length() > Constants.MAX_TAGGED_DATA_FILENAME_LENGTH) {
                        throw new NxtException.NotValidException("Invalid filename length: " + attachment.getFilename().length());
                    }
                    if (attachment.getData().length == 0 || attachment.getData().length > Constants.MAX_TAGGED_DATA_DATA_LENGTH) {
                        throw new NxtException.NotValidException("Invalid data length: " + attachment.getData().length);
                    }
                }
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.TaggedDataUpload attachment = (Attachment.TaggedDataUpload) transaction.getAttachment();
                TaggedData.add((TransactionImpl)transaction, attachment);
            }

            @Override
            public String getName() {
                return "TaggedDataUpload";
            }

            @Override
            boolean isPruned(long transactionId) {
                return TaggedData.isPruned(transactionId);
            }

        };

        public static final TransactionType TAGGED_DATA_EXTEND = new Data() {

            @Override
            public byte getSubtype() {
                return SUBTYPE_DATA_TAGGED_DATA_EXTEND;
            }

            @Override
            public LedgerEvent getLedgerEvent() {
                return LedgerEvent.TAGGED_DATA_EXTEND;
            }

            @Override
            Attachment.TaggedDataExtend parseAttachment(ByteBuffer buffer) throws NxtException.NotValidException {
                return new Attachment.TaggedDataExtend(buffer);
            }

            @Override
            Attachment.TaggedDataExtend parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new Attachment.TaggedDataExtend(attachmentData);
            }

            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                Attachment.TaggedDataExtend attachment = (Attachment.TaggedDataExtend) transaction.getAttachment();
                if ((attachment.jsonIsPruned() || attachment.getData() == null) && Nxt.getEpochTime() - transaction.getTimestamp() < Constants.MIN_PRUNABLE_LIFETIME) {
                    throw new NxtException.NotCurrentlyValidException("Data has been pruned prematurely");
                }
                TransactionImpl uploadTransaction = TransactionDb.findTransaction(attachment.getTaggedDataId(), Nxt.getBlockchain().getHeight());
                if (uploadTransaction == null) {
                    throw new NxtException.NotCurrentlyValidException("No such tagged data upload " + Long.toUnsignedString(attachment.getTaggedDataId()));
                }
                if (uploadTransaction.getType() != TAGGED_DATA_UPLOAD) {
                    throw new NxtException.NotValidException("Transaction " + Long.toUnsignedString(attachment.getTaggedDataId())
                            + " is not a tagged data upload");
                }
                if (attachment.getData() != null) {
                    Attachment.TaggedDataUpload taggedDataUpload = (Attachment.TaggedDataUpload)uploadTransaction.getAttachment();
                    if (!Arrays.equals(attachment.getHash(), taggedDataUpload.getHash())) {
                        throw new NxtException.NotValidException("Hashes don't match! Extend hash: " + Convert.toHexString(attachment.getHash())
                                + " upload hash: " + Convert.toHexString(taggedDataUpload.getHash()));
                    }
                }
                TaggedData taggedData = TaggedData.getData(attachment.getTaggedDataId());
                if (taggedData != null && taggedData.getTransactionTimestamp() > Nxt.getEpochTime() + 6 * Constants.MIN_PRUNABLE_LIFETIME) {
                    throw new NxtException.NotCurrentlyValidException("Data already extended, timestamp is " + taggedData.getTransactionTimestamp());
                }
            }

            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                Attachment.TaggedDataExtend attachment = (Attachment.TaggedDataExtend) transaction.getAttachment();
                TaggedData.extend(transaction, attachment);
            }

            @Override
            public String getName() {
                return "TaggedDataExtend";
            }

            @Override
            boolean isPruned(long transactionId) {
                return false;
            }

        };

    }

}
