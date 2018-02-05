package nxt;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import nxt.util.Logger;
import nxt.util.ThreadPool;

public class DebugDayTimer {
	
	static Date currentDate = todaysDate();
	
	private static Date todaysDate() {
		Date today;
		long maxDay = checkDb();
		if (maxDay == 0) {
			today = NtpTime.toDate("05-Feb-2018");
		}else {
			today = loadDate(maxDay);
		}
		
		return today;
	}
	
	
	
	
	private static Date addOneDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		calendar.setTime(date);
		calendar.add(Calendar.DATE, 1);
		date = calendar.getTime();
		return date;
	}
	
	private static long checkDb() {
		long dayFromDb = 0;
		
		try (Connection con = Db.db.getConnection();
	             PreparedStatement pstmt = con.prepareStatement("select MAX(DAY) from DAILY_DATA")) {
	            try(ResultSet rs = pstmt.executeQuery()){
	            	while (rs.next()) {
	                    dayFromDb = rs.getLong("MAX(DAY)");
	            	}
	        }
           
		}catch (SQLException e) {
			Logger.logDebugMessage(e.toString());
//            throw new RuntimeException(e.toString(), e);
        }
		return dayFromDb;
	}
	
	static Date loadDate(long day) {
		
		String dateString = null;
		
		try (Connection con = Db.db.getConnection();
	             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM daily_data where DAY = ?")) {
			pstmt.setLong(1, day);
	            try (ResultSet rs = pstmt.executeQuery()) {
	                if (rs.next()) {

				        try {
				             dateString = rs.getString("date");
				        	} catch (SQLException e) {
				            throw new RuntimeException(e.toString(), e);
				        		}
                		}
	            }
	        } catch (SQLException e) {
	            throw new RuntimeException(e.toString(), e);
        		}
		
		return NtpTime.toDate(dateString);
    }
	
	private static final Runnable debugDayThread = new Runnable() {

        @Override
        public void run() {
//        		currentDate = getDate();
        		//########## TEST SECTION #############
        		currentDate = addOneDay(currentDate);
    			//########## END TEST SECTION ############
//        		Logger.logDebugMessage("Retrieving Time: " + NtpTime.toString(currentDate));
        }};

    static void init() {
		ThreadPool.scheduleThread("DebugDayThread", DebugDayTimer.debugDayThread, 600);
    }
        
}
