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

package nxt.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import nxt.NtpTime;

public interface Time {

    int getTime();

    final class EpochTime implements Time {

        public int getTime() {
//            return Convert.toEpochTime(System.currentTimeMillis());
        	
//        SimpleDateFormat isoFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mmm:ss:SSS");
//		isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
//		String systemDateAndTime = isoFormat.format(new Date(System.currentTimeMillis()));
//		String ntpDateAndTime = isoFormat.format(new Date(NtpTime.getDateMs()));
//        	
//        	
//        	
//        	Logger.logDebugMessage("System  ms: " + systemDateAndTime);
//        	Logger.logDebugMessage("NTP  ms: " + ntpDateAndTime);
//        	
//        	Logger.logDebugMessage("System Epoch ms: " + Convert.toEpochTime(System.currentTimeMillis()));
//        	Logger.logDebugMessage("NTP Epoch ms: " + Convert.toEpochTime(NtpTime.getDateMs()));
//        	
//        	Logger.logDebugMessage("");
//        	
        	return Convert.toEpochTime(NtpTime.getDateMs());
        }

    }

    final class ConstantTime implements Time {

        private final int time;

        public ConstantTime(int time) {
            this.time = time;
        }

        public int getTime() {
            return time;
        }

    }

    final class FasterTime implements Time {

        private final int multiplier;
        private final long systemStartTime;
        private final int time;

        public FasterTime(int time, int multiplier) {
            if (multiplier > 1000 || multiplier <= 0) {
                throw new IllegalArgumentException("Time multiplier must be between 1 and 1000");
            }
            this.multiplier = multiplier;
            this.time = time;
            this.systemStartTime = System.currentTimeMillis();
        }

        public int getTime() {
            return time + (int)((System.currentTimeMillis() - systemStartTime) / (1000 / multiplier));
        }

    }

    final class CounterTime implements Time {

        private final AtomicInteger counter;

        public CounterTime(int time) {
            this.counter = new AtomicInteger(time);
        }

        public int getTime() {
            return counter.incrementAndGet();
        }

    }

}
