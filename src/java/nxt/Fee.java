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

import java.math.BigInteger;


//seen.
public interface Fee {

    BigInteger getFee(TransactionImpl transaction, Appendix appendage);

    Fee DEFAULT_FEE = new Fee.ConstantFee(Constants.STD_FEE);

    Fee NONE = new Fee.ConstantFee(BigInteger.ZERO);

    final class ConstantFee implements Fee {

        private final BigInteger fee;

        public ConstantFee(BigInteger fee) {
            this.fee = fee;
        }

        @Override
        public BigInteger getFee(TransactionImpl transaction, Appendix appendage) {
            return fee;
        }

    }

    abstract class SizeBasedFee implements Fee {

        private final BigInteger constantFee;
        private final BigInteger feePerSize;
        private final int unitSize;

        public SizeBasedFee(BigInteger feePerSize) {
            this(BigInteger.ZERO, feePerSize);
        }

        public SizeBasedFee(BigInteger constantFee, BigInteger feePerSize) {
            this(constantFee, feePerSize, 1024);
        }

        public SizeBasedFee(BigInteger constantFee, BigInteger feePerSize, int unitSize) {
            this.constantFee = constantFee;
            this.feePerSize = feePerSize;
            this.unitSize = unitSize;
        }

        // the first size unit is free if constantFee is 0
        @Override
        public final BigInteger getFee(TransactionImpl transaction, Appendix appendage) {
            int size = getSize(transaction, appendage) - 1;
            if (size < 0) {
                return constantFee;
            }
            return constantFee.add(feePerSize.multiply(BigInteger.valueOf((long)(size/unitSize))));
//            return Math.addExact(constantFee, Math.multiplyExact((long) (size / unitSize), feePerSize));
        }

        public abstract int getSize(TransactionImpl transaction, Appendix appendage);

    }

}
