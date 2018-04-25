package nxt;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import nxt.util.Logger;
import nxt.util.ThreadPool;
import nxt.util.Time;


public class NtpTime {

	private NtpTime() {} //never
	

//    private static final int REFERENCE_TIME_OFFSET = 16;
    private static final int ORIGINATE_TIME_OFFSET = 24;
    private static final int RECEIVE_TIME_OFFSET = 32;
    private static final int TRANSMIT_TIME_OFFSET = 40;
    private static final int NTP_PACKET_SIZE = 48;

    private static final int NTP_PORT = 123;
    private static final int NTP_MODE_CLIENT = 3;
    private static final int NTP_VERSION = 3;

    // Number of seconds between Jan 1, 1900 and Jan 1, 1970
    // 70 years plus 17 leap days
    private static final long OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L;

    // system time computed from NTP server response
    private static long mNtpTime;

    // value of SystemClock.elapsedRealtime() corresponding to mNtpTime
    private static long mNtpTimeReference;

    // round trip time in milliseconds
    private static long mRoundTripTime;
    
    private static boolean connectionStatus = true;
    
    
    private static long utcTime = 0;
    private static long systemRef;

//    private static Date currentDate = getDate();
//    /**
//     * Sends an SNTP request to the given host and processes the response.
//     *
//     * @param host host name of the server.
//     * @param timeout network timeout in milliseconds.
//     * @return true if the transaction was successful.
//     */
//    
    public static Date getCurrentDate() {
    		return getDate();
    }
    
    private static final Runnable dateThread = new Runnable() {

        @Override
        public void run() {
//        		currentDate = getDate();
        		//########## TEST SECTION #############
        		utcTime = retrieveUtcTime();
        		
//        		currentDate = getZeroTimeDate(DebugDayTimer.currentDate);
    			//########## END TEST SECTION ############
//        		Logger.logDebugMessage("Retrieving Time: " + NtpTime.toString(currentDate));
        }};
        
        
    static void init() {
    		ThreadPool.scheduleThread("DateThread", NtpTime.dateThread, 20);
    }
    private static Date getDate() {
    		Date date = new Date(utcTime);
    	
    		if (utcTime==0){
    			date = new Date(retrieveUtcTime());
    		}
    		
    		date = getZeroTimeDate(date);
    		return date;
    }
    
    public static String toString(Date date) {
		date = getZeroTimeDate(date);
    		SimpleDateFormat isoFormat = new SimpleDateFormat(Constants.dateFormat);
		isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		return isoFormat.format(date);
    }
    
    public static Date toDate(String dateString) { //throws ParseException {
	    	SimpleDateFormat formatter = new SimpleDateFormat(Constants.dateFormat);
	    	formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	    	Date returnDate = null;
	    	try {
				returnDate = formatter.parse(dateString);
				returnDate = getZeroTimeDate(returnDate);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	return returnDate;
    }
    
    public static long getDateMs() {
//    		Logger.logDebugMessage("utc: " + utcTime);
//    		Logger.logDebugMessage("madeup: "+ (utcTime + System.currentTimeMillis() - systemRef));
    	
    		return utcTime + System.currentTimeMillis() - systemRef;
    }
    
    private static long retrieveUtcTime() {
    	String url = "time.google.com";
		int timeout = 15 * 1000; // in ms
		long epoch = NtpTime.getTime(url, timeout);
		
		return epoch;
    }
    
    private static long getTime(String url, int timeout) { //timeout in ms
    		long now = 0;
    		long current = 0;
    		boolean retrieved = false;
    	
//    		if (requestTime(url, timeout)) {
//    			now = getNtpTime() + (System.nanoTime()/1000000) - getNtpTimeReference();
//    		}
    		
    		if (requestTime(url, timeout, retrieved)) {
    			now = getNtpTime() + (System.nanoTime()/1000000) - getNtpTimeReference();
    			current = now;
//    			Logger.logDebugMessage("");
//    			Logger.logDebugMessage("getNtpTime: "+getNtpTime()); 
//    			Logger.logDebugMessage("System nano:"+System.nanoTime());
//    			Logger.logDebugMessage("timeref: " + getNtpTimeReference());
    			
////    			long xianZai = 1522909702850L;
////    			double secondsPerDay = 400; //400s in real life
////    			double currentDouble = (now - xianZai) * (86400/secondsPerDay);	//since 14 March 2018 UTC 0 
////    			currentDouble += xianZai;
////    			current = (long) currentDouble;
//    			
//    			Logger.logDebugMessage("now: " + now);
//    			Logger.logDebugMessage("current: " + current);
    			
    			
    		}  	 

    			
    		return current;
    }
    
    public static Date getZeroTimeDate(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		date = calendar.getTime();
		return date;
	}
    	 
    private static boolean requestTime(String host, int timeout, boolean retrieved) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(timeout);
            InetAddress address = InetAddress.getByName(host);
            byte[] buffer = new byte[NTP_PACKET_SIZE];
            DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, NTP_PORT);

            // set mode = 3 (client) and version = 3
            // mode is in low 3 bits of first byte
            // version is in bits 3-5 of first byte
            buffer[0] = NTP_MODE_CLIENT | (NTP_VERSION << 3);

            // get current time and write it to the request packet
            long requestTime = System.currentTimeMillis();
            long requestTicks = System.nanoTime() / 1000000; //ns to ms
            writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, requestTime);

            socket.send(request);

            // read the response
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            long responseTicks = System.nanoTime() / 1000000; //ns to ms
            long responseTime = requestTime + (responseTicks - requestTicks);

            // extract the results
            long originateTime = readTimeStamp(buffer, ORIGINATE_TIME_OFFSET);
            long receiveTime = readTimeStamp(buffer, RECEIVE_TIME_OFFSET);
            long transmitTime = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET);
            long roundTripTime = responseTicks - requestTicks - (transmitTime - receiveTime);

            long clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime))/2;

            mNtpTime = responseTime + clockOffset;
            mNtpTimeReference = responseTicks;
            mRoundTripTime = roundTripTime;
            
            connectionStatus = true;
            systemRef = System.currentTimeMillis();
//            Logger.logDebugMessage("time: " + mNtpTime);
            
        } catch (Exception e) {
            connectionStatus = false;
        		Logger.logDebugMessage("Could not retrieve time. Taelium runs on a global clock. Please make sure you are connected to the internet.");
        	
        		Logger.logDebugMessage("request time failed: " + e);
            return false;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }

        return true;
    }

    
    public static boolean getConnectionStatus() {
    		return connectionStatus;
    }
    /**
     * Returns the time computed from the NTP transaction.
     *
     * @return time value computed from NTP server response.
     */
    private static long getNtpTime() {
        return mNtpTime;
    }

    /**
     * Returns the reference clock value (value of SystemClock.elapsedRealtime())
     * corresponding to the NTP time.
     *
     * @return reference clock corresponding to the NTP time.
     */
    private static long getNtpTimeReference() {
        return mNtpTimeReference;
    }

    /**
     * Returns the round trip time of the NTP transaction
     *
     * @return round trip time in milliseconds.
     */
    private long getRoundTripTime() {
        return mRoundTripTime;
    }

    /**
     * Reads an unsigned 32 bit big endian number from the given offset in the buffer.
     */
    private static long read32(byte[] buffer, int offset) {
        byte b0 = buffer[offset];
        byte b1 = buffer[offset+1];
        byte b2 = buffer[offset+2];
        byte b3 = buffer[offset+3];

        // convert signed bytes to unsigned values
        int i0 = ((b0 & 0x80) == 0x80 ? (b0 & 0x7F) + 0x80 : b0);
        int i1 = ((b1 & 0x80) == 0x80 ? (b1 & 0x7F) + 0x80 : b1);
        int i2 = ((b2 & 0x80) == 0x80 ? (b2 & 0x7F) + 0x80 : b2);
        int i3 = ((b3 & 0x80) == 0x80 ? (b3 & 0x7F) + 0x80 : b3);

        return ((long)i0 << 24) + ((long)i1 << 16) + ((long)i2 << 8) + (long)i3;
    }

    /**
     * Reads the NTP time stamp at the given offset in the buffer and returns 
     * it as a system time (milliseconds since January 1, 1970).
     */    
    private static long readTimeStamp(byte[] buffer, int offset) {
        long seconds = read32(buffer, offset);
        long fraction = read32(buffer, offset + 4);
        return ((seconds - OFFSET_1900_TO_1970) * 1000) + ((fraction * 1000L) / 0x100000000L);        
    }

    /**
     * Writes system time (milliseconds since January 1, 1970) as an NTP time stamp 
     * at the given offset in the buffer.
     */    
    private static void writeTimeStamp(byte[] buffer, int offset, long time) {
        long seconds = time / 1000L;
        long milliseconds = time - seconds * 1000L;
        seconds += OFFSET_1900_TO_1970;

        // write seconds in big endian format
        buffer[offset++] = (byte)(seconds >> 24);
        buffer[offset++] = (byte)(seconds >> 16);
        buffer[offset++] = (byte)(seconds >> 8);
        buffer[offset++] = (byte)(seconds >> 0);

        long fraction = milliseconds * 0x100000000L / 1000L;
        // write fraction in big endian format
        buffer[offset++] = (byte)(fraction >> 24);
        buffer[offset++] = (byte)(fraction >> 16);
        buffer[offset++] = (byte)(fraction >> 8);
        // low order bits should be random data
        buffer[offset++] = (byte)(Math.random() * 255.0);
    }
}


	

