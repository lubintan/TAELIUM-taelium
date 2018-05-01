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

package nxt;// every class to be included in "nxt" package needs this line as first line in the file.

import nxt.addons.AddOns;
import nxt.crypto.Crypto;
import nxt.env.DirProvider;
import nxt.env.RuntimeEnvironment;
import nxt.env.RuntimeMode;
import nxt.env.ServerStatus;
import nxt.http.API;
import nxt.http.APIProxy;
import nxt.peer.Peers;
import nxt.util.Convert;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import nxt.util.Time;
import nxt.DebugScanner;
import nxt.NtpTime;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Date;


public final class Nxt { //only public class in a Java file must also have the main method

    public static final String VERSION = "1.11.9";
    public static final String APPLICATION = "Taelium";

    private static volatile Time time = new Time.EpochTime();

    public static final String NXT_DEFAULT_PROPERTIES = "nxt-default.properties";
    public static final String NXT_PROPERTIES = "nxt.properties";
    public static final String NXT_INSTALLER_PROPERTIES = "nxt-installer.properties";
    public static final String CONFIG_DIR = "conf";

    private static final RuntimeMode runtimeMode;
    private static final DirProvider dirProvider;

    private static final Properties defaultProperties = new Properties(); //another one "properties" is declared below
    
/*  static = memory only allocated once
    the block below is initializing output and error logs and home folder and checking the properties file is the right version.	
*/    
    static {
        redirectSystemStreams("out");
        redirectSystemStreams("err");
        System.out.println("Initializing " + Nxt.APPLICATION + " server version " + Nxt.VERSION);
        printCommandLineArguments();
        runtimeMode = RuntimeEnvironment.getRuntimeMode();
        System.out.printf("Runtime mode %s\n", runtimeMode.getClass().getName());
        dirProvider = RuntimeEnvironment.getDirProvider();
        System.out.println("User home folder " + dirProvider.getUserHomeDir());
        loadProperties(defaultProperties, NXT_DEFAULT_PROPERTIES, true);
        if (!VERSION.equals(Nxt.defaultProperties.getProperty("nxt.version"))) {
            throw new RuntimeException("Using an nxt-default.properties file from a version other than " + VERSION + " is not supported!!!");
        }
    }

/*    this is used to print console output and error to files */
    private static void redirectSystemStreams(String streamName) {
        String isStandardRedirect = System.getProperty("nxt.redirect.system." + streamName);
        Path path = null;
        if (isStandardRedirect != null) {
            try {
                path = Files.createTempFile("nxt.system." + streamName + ".", ".log");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else {
            String explicitFileName = System.getProperty("nxt.system." + streamName);
            if (explicitFileName != null) {
                path = Paths.get(explicitFileName);
            }
        }
        if (path != null) {
            try {
                PrintStream stream = new PrintStream(Files.newOutputStream(path));
                if (streamName.equals("out")) {
                    System.setOut(new PrintStream(stream));
                } else {
                    System.setErr(new PrintStream(stream));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
//	different from "defaultProperties" above
    private static final Properties properties = new Properties(defaultProperties);

    static {
        loadProperties(properties, NXT_INSTALLER_PROPERTIES, true);
        loadProperties(properties, NXT_PROPERTIES, false);
    }

//    this loads property values from the properties files
    public static Properties loadProperties(Properties properties, String propertiesFile, boolean isDefault) {
        try {
            // Load properties from location specified as command line parameter
            String configFile = System.getProperty(propertiesFile);
            if (configFile != null) {
                System.out.printf("Loading %s from %s\n", propertiesFile, configFile);
                try (InputStream fis = new FileInputStream(configFile)) {
                    properties.load(fis);
                    return properties;
                } catch (IOException e) {
                    throw new IllegalArgumentException(String.format("Error loading %s from %s", propertiesFile, configFile));
                }
            } else {
                try (InputStream is = ClassLoader.getSystemResourceAsStream(propertiesFile)) {
                    // When running nxt.exe from a Windows installation we always have nxt.properties in the classpath but 
                		// this is not the nxt properties file.
                    // Therefore we first load it from the classpath and then look for the real nxt.properties in the user folder.
                    if (is != null) {
                        System.out.printf("Loading %s from classpath\n", propertiesFile);
                        properties.load(is);
                        if (isDefault) {
                            return properties;
                        }
                    }
                    // load non-default properties files from the user folder
                    if (!dirProvider.isLoadPropertyFileFromUserDir()) {
                        return properties;
                    }
                    String homeDir = dirProvider.getUserHomeDir();
                    if (!Files.isReadable(Paths.get(homeDir))) {
                        System.out.printf("Creating dir %s\n", homeDir);
                        try {
                            Files.createDirectory(Paths.get(homeDir));
                        } catch(Exception e) {
                            if (!(e instanceof NoSuchFileException)) {
                                throw e;
                            }
                            // Fix for WinXP and 2003 which does have a roaming sub folder
                            Files.createDirectory(Paths.get(homeDir).getParent());
                            Files.createDirectory(Paths.get(homeDir));
                        }
                    }
                    Path confDir = Paths.get(homeDir, CONFIG_DIR);
                    if (!Files.isReadable(confDir)) {
                        System.out.printf("Creating dir %s\n", confDir);
                        Files.createDirectory(confDir);
                    }
                    Path propPath = Paths.get(confDir.toString()).resolve(Paths.get(propertiesFile));
                    if (Files.isReadable(propPath)) {
                        System.out.printf("Loading %s from dir %s\n", propertiesFile, confDir);
                        properties.load(Files.newInputStream(propPath));
                    } else {
                        System.out.printf("Creating property file %s\n", propPath);
                        Files.createFile(propPath);
                        Files.write(propPath, Convert.toBytes("# use this file for workstation specific " + propertiesFile));
                    }
                    return properties;
                } catch (IOException e) {
                    throw new IllegalArgumentException("Error loading " + propertiesFile, e);
                }
            }
        } catch(IllegalArgumentException e) {
            e.printStackTrace(); // make sure we log this exception
            throw e;
        }
    }

    private static void printCommandLineArguments() {
        try {
            List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            if (inputArguments != null && inputArguments.size() > 0) {
                System.out.println("Command line arguments");
            } else {
                return;
            }
            inputArguments.forEach(System.out::println);
        } catch (AccessControlException e) {
            System.out.println("Cannot read input arguments " + e.getMessage());
        }
    }

    public static int getIntProperty(String name) {
        return getIntProperty(name, 0);
    }
//	if something in the properties files is an int, parse the number string into an int
    public static int getIntProperty(String name, int defaultValue) {
        try {
            int result = Integer.parseInt(properties.getProperty(name));
            Logger.logMessage(name + " = \"" + result + "\"");
            return result;
        } catch (NumberFormatException e) {
            Logger.logMessage(name + " not defined or not numeric, using default value " + defaultValue);
            return defaultValue;
        }
    }

    public static String getStringProperty(String name) {
        return getStringProperty(name, null, false);
    }

    public static String getStringProperty(String name, String defaultValue) {
        return getStringProperty(name, defaultValue, false);
    }

    public static String getStringProperty(String name, String defaultValue, boolean doNotLog) {
        return getStringProperty(name, defaultValue, doNotLog, null);
    }

    public static String getStringProperty(String name, String defaultValue, boolean doNotLog, String encoding) {
        String value = properties.getProperty(name);
        if (value != null && ! "".equals(value)) {
            Logger.logMessage(name + " = \"" + (doNotLog ? "{not logged}" : value) + "\"");
        } else {
            Logger.logMessage(name + " not defined");
            value = defaultValue;
        }
        if (encoding == null || value == null) {
            return value;
        }
        try {
            return new String(value.getBytes("ISO-8859-1"), encoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getStringListProperty(String name) {
        String value = getStringProperty(name);
        if (value == null || value.length() == 0) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String s : value.split(";")) {
            s = s.trim();
            if (s.length() > 0) {
                result.add(s);
            }
        }
        return result;
    }

    public static boolean getBooleanProperty(String name) {
        return getBooleanProperty(name, false);
    }

    public static boolean getBooleanProperty(String name, boolean defaultValue) {
        String value = properties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            Logger.logMessage(name + " = \"true\"");
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            Logger.logMessage(name + " = \"false\"");
            return false;
        }
        Logger.logMessage(name + " not defined, using default " + defaultValue);
        return defaultValue;
    }

    public static Blockchain getBlockchain() {
        return BlockchainImpl.getInstance();
    }

    public static BlockchainProcessor getBlockchainProcessor() {
        return BlockchainProcessorImpl.getInstance();
    }

    public static TransactionProcessor getTransactionProcessor() {
        return TransactionProcessorImpl.getInstance();
    }

    public static Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, BigInteger amountNQT, BigInteger feeNQT, short deadline, Attachment attachment) {
        return new TransactionImpl.BuilderImpl((byte)1, senderPublicKey, amountNQT, feeNQT, deadline, (Attachment.AbstractAttachment)attachment);
    }

    public static Transaction.Builder newTransactionBuilder(byte[] transactionBytes) throws NxtException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionBytes);
    }

    public static Transaction.Builder newTransactionBuilder(JSONObject transactionJSON) throws NxtException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionJSON);
    }

    public static Transaction.Builder newTransactionBuilder(byte[] transactionBytes, JSONObject prunableAttachments) throws NxtException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionBytes, prunableAttachments);
    }
//	timestamper!
    public static int getEpochTime() {
        return time.getTime();
    }

    static void setTime(Time time) {
        Nxt.time = time;
    }

    public static void main(String[] args) {
        try {
//        		Date date = NtpTime.getDate();
//        		Date date2 = new Date(NtpTime.getDateMs() + 86400000);
//        		Date date3 = new Date(NtpTime.getDateMs() + ((12/24) * 86400000));
//        		
//        		Logger.logDebugMessage(NtpTime.toString(date));
//        		Logger.logDebugMessage(NtpTime.toString(date2));
//        		Logger.logDebugMessage(NtpTime.toString(date3));
//        		
//        		Logger.logDebugMessage("compare: " + NtpTime.toDate(NtpTime.toString(date)).compareTo(NtpTime.toDate(NtpTime.toString(date3))));
//        		System.exit(0);
//        		date2 = NtpTime.getZeroTimeDate(date2);
//        		date3 = NtpTime.getZeroTimeDate(date3);
//        		
//        		SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//        		isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
//        		
//        		System.out.println(isoFormat.format(date));
//        		System.out.println(isoFormat.format(date2));
//        		System.out.println(isoFormat.format(date3));
//        		System.out.println(date.compareTo(date2));
//        		System.out.println(date.compareTo(date3));
//        		System.out.println(date.compareTo(date));
        		
        	
//        		ZonedDateTime date = Instant.ofEpochMilli(NtpTime.getDateMs())
//    	            .atZone(ZoneId.of("Z")); // Z: UTC Time
//        		ZonedDateTime date2 = Instant.ofEpochMilli(NtpTime.getDateMs() + 86400000)
//        	            .atZone(ZoneId.of("Z")); // Z: UTC Time
//        		ZonedDateTime date3 = Instant.ofEpochMilli(NtpTime.getDateMs() + (86400000/6))
//        	            .atZone(ZoneId.of("Z")); // Z: UTC Time
//        		
//        		System.out.println(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy hhmm")));
//        		System.out.println(date2.format(DateTimeFormatter.ofPattern("dd/MM/yyyy hhmm")));
//        		System.out.println(date3.format(DateTimeFormatter.ofPattern("dd/MM/yyyy hhmm")));
//        		System.out.println(date.compareTo(date2));
//        		System.out.println(date.compareTo(date3));
//        		System.out.println(date.compareTo(date));
        		
        		
//            System.out.println( machine + " says it is " + timestamp );
//            System.exit(0);
             // System.out.println(instant1);
        	
        	
            // Lambda: parameter -> expression body. 
        		// Eg. public class Java8Tester {

	//        	   final static String salutation = "Hello! ";
	//        	   
	//        	   public static void main(String args[]){
	//        	      GreetingService greetService1 = message -> 
	//        	      System.out.println(salutation + message);
	//        	      greetService1.sayMessage("Mahesh");
	//        	   }
	//        		
	//        	   interface GreetingService {
	//        	      void sayMessage(String message);
	//        	   }
	//        	}
        		
//        	/*~~~~~~~~~~~~~~~~~~~~~~~~ BEGIN Test/Debug Space 	~~~~~~~~~~~~~~~~~~~~~~~~~ */
//    		
//    		long acctID = Convert.parseAccountId("NXT-TND7-6ZQL-79D6-9SR8B");
//    		String acctRS = Convert.rsAccount(acctID);
//    		String pubKeyString = "b37c06a562ac79751408d4dec896ea154683c7e474b3d4c63bb46021131f4318";
//    		long acctID2 = 
//    				Convert.fullHashToId(Crypto.sha256().digest(Convert.parseHexString((pubKeyString))));
//    		
//    		System.out.println();
//    		System.out.println("/*~~~~~~~~~~~~~~~~~~~~~~~~ BEGIN Test/Debug Space 	~~~~~~~~~~~~~~~~~~~~~~~~~ */");
//    		System.out.println(acctID);
//    		System.out.println(acctRS);
//    		System.out.println(acctID2);
//    		System.out.println("/*~~~~~~~~~~~~~~~~~~~~~~~~ END Test/Debug Space 	~~~~~~~~~~~~~~~~~~~~~~~~~ */");
//    		System.out.println();
//    		System.exit(0);
//    		/*~~~~~~~~~~~~~~~~~~~~~~~~ END Test/Debug Space 		~~~~~~~~~~~~~~~~~~~~~~~~~ */
        	
        		Runtime.getRuntime().addShutdownHook(new Thread(Nxt::shutdown)); //shutdown() defined below
            init();
        } catch (Throwable t) {
            System.out.println("Fatal error: " + t.toString());
            t.printStackTrace();
        }
    }

    public static void init(Properties customProperties) {
        properties.putAll(customProperties);
        init();
    }

    public static void init() {
        Init.init();
    }

    public static void shutdown() {
        Logger.logShutdownMessage("Shutting down...");
        AddOns.shutdown();
        API.shutdown();
//        FundingMonitor.shutdown();
        ThreadPool.shutdown();
        BlockchainProcessorImpl.getInstance().shutdown();
        Peers.shutdown();
        Db.shutdown();
        Logger.logShutdownMessage(Nxt.APPLICATION + " server " + VERSION + " stopped.");
        Logger.shutdown();
        runtimeMode.shutdown();
    }

    private static class Init {
//	main() comes here
    	
        private static volatile boolean initialized = false;
        
//   static is to ensure all only use 1 space in memory, not repeated. This block gets executed when the class is loaded in the memory. 
//   A class can have multiple Static blocks, which will execute in the same sequence in which they have been written into the program.
//      ***TODO: list of classes and functions to go through here. ***
        static {
            try {
                long startTime = System.currentTimeMillis();
                Logger.init();
                setSystemProperties();
                logSystemProperties();
                runtimeMode.init();
                Thread secureRandomInitThread = initSecureRandom();
                setServerStatus(ServerStatus.BEFORE_DATABASE, null);
                Db.init();
                setServerStatus(ServerStatus.AFTER_DATABASE, null);
                
                TransactionProcessorImpl.getInstance();
                BlockchainProcessorImpl.getInstance();
                Account.init();
                AccountRestrictions.init();
                AccountLedger.init();
//                Alias.init();
//                Asset.init();
//                DigitalGoodsStore.init();
//                Order.init();
//                Poll.init();
//                PhasingPoll.init();
//                Trade.init();
//                AssetTransfer.init();
//                AssetDelete.init();
//                AssetDividend.init();
//                Vote.init();
//                PhasingVote.init();
//                Currency.init();
//                CurrencyBuyOffer.init();
//                CurrencySellOffer.init();
//                CurrencyFounder.init();
//                CurrencyMint.init();
//                CurrencyTransfer.init();
//                Exchange.init();
//                ExchangeRequest.init();
//                Shuffling.init();
//                ShufflingParticipant.init();
//                PrunableMessage.init();
                TaggedData.init();
                Peers.init();
                APIProxy.init();
                Generator.init();
                AddOns.init();
                API.init();
                CalculateInterestAndG.init();
//                DebugTrace.init();
//                DebugScanner.init();
//                DebugDayTimer.init();
                NtpTime.init();
                
                
                int timeMultiplier = (Constants.isTestnet && Constants.isOffline) ? Math.max(Nxt.getIntProperty("nxt.timeMultiplier"), 1) : 1;
                ThreadPool.start(timeMultiplier);
                if (timeMultiplier > 1) {
                    setTime(new Time.FasterTime(Math.max(getEpochTime(), Nxt.getBlockchain().getLastBlock().getTimestamp()), timeMultiplier));
                    Logger.logMessage("TIME WILL FLOW " + timeMultiplier + " TIMES FASTER!");
                }
                try {
                    secureRandomInitThread.join(10000);
                } catch (InterruptedException ignore) {}
                testSecureRandom();
                long currentTime = System.currentTimeMillis();
                Logger.logMessage("Initialization took " + (currentTime - startTime) / 1000 + " seconds");
                Logger.logMessage(Nxt.APPLICATION + " server " + VERSION + " started successfully.");
                Logger.logMessage("Copyright © 2013-2016 The Nxt Core Developers.");
                Logger.logMessage("Copyright © 2016-2017 Jelurida IP B.V.");
                Logger.logMessage("Distributed under the Jelurida Public License version 1.0 for the Nxt Public Blockchain Platform, with ABSOLUTELY NO WARRANTY.");
                if (API.getWelcomePageUri() != null) {
                    Logger.logMessage("Client UI is at " + API.getWelcomePageUri());
                }
                setServerStatus(ServerStatus.STARTED, API.getWelcomePageUri());
                if (isDesktopApplicationEnabled()) {
                    launchDesktopApplication();
                }
                
                // to run on testnet!
                if (Constants.isTestnet) {
                    Logger.logMessage("RUNNING ON TESTNET - DO NOT USE REAL ACCOUNTS!");
                }
            } catch (Exception e) {
                Logger.logErrorMessage(e.getMessage(), e);
                runtimeMode.alert(e.getMessage() + "\n" +
                        "See additional information in " + dirProvider.getLogFileDir() + System.getProperty("file.separator") + "nxt.log");
                System.exit(1);
            }
        }

        private static void init() {
            if (initialized) {
                throw new RuntimeException("Nxt.init has already been called");
            }
            initialized = true;
        }

//        	prevent initializing an instance of this class
        private Init() {} // never

    }

    private static void setSystemProperties() {
      // Override system settings that the user has define in nxt.properties file.
      String[] systemProperties = new String[] {
        "socksProxyHost",
        "socksProxyPort",
      };

      for (String propertyName : systemProperties) {
        String propertyValue;
        if ((propertyValue = getStringProperty(propertyName)) != null) {
          System.setProperty(propertyName, propertyValue);
        }
      }
    }

    private static void logSystemProperties() {
        String[] loggedProperties = new String[] {
                "java.version",
                "java.vm.version",
                "java.vm.name",
                "java.vendor",
                "java.vm.vendor",
                "java.home",
                "java.library.path",
                "java.class.path",
                "os.arch",
                "sun.arch.data.model",
                "os.name",
                "file.encoding",
                "java.security.policy",
                "java.security.manager",
                RuntimeEnvironment.RUNTIME_MODE_ARG,
                RuntimeEnvironment.DIRPROVIDER_ARG
        };
        for (String property : loggedProperties) {
            Logger.logDebugMessage(String.format("%s = %s", property, System.getProperty(property)));
        }
        Logger.logDebugMessage(String.format("availableProcessors = %s", Runtime.getRuntime().availableProcessors()));
        Logger.logDebugMessage(String.format("maxMemory = %s", Runtime.getRuntime().maxMemory()));
        Logger.logDebugMessage(String.format("processId = %s", getProcessId()));
    }

    private static Thread initSecureRandom() {
        Thread secureRandomInitThread = new Thread(() -> Crypto.getSecureRandom().nextBytes(new byte[1024]));
        secureRandomInitThread.setDaemon(true);
        secureRandomInitThread.start();
        return secureRandomInitThread;
    }

    private static void testSecureRandom() {
        Thread thread = new Thread(() -> Crypto.getSecureRandom().nextBytes(new byte[1024]));
        thread.setDaemon(true);
        thread.start();
        try {
            thread.join(2000);
            if (thread.isAlive()) {
                throw new RuntimeException("SecureRandom implementation too slow!!! " +
                        "Install haveged if on linux, or set nxt.useStrongSecureRandom=false.");
            }
        } catch (InterruptedException ignore) {}
    }

    public static String getProcessId() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        if (runtimeName == null) {
            return "";
        }
        String[] tokens = runtimeName.split("@");
        if (tokens.length == 2) {
            return tokens[0];
        }
        return "";
    }

    public static String getDbDir(String dbDir) {
        return dirProvider.getDbDir(dbDir);
    }

    public static void updateLogFileHandler(Properties loggingProperties) {
        dirProvider.updateLogFileHandler(loggingProperties);
    }

    public static String getUserHomeDir() {
        return dirProvider.getUserHomeDir();
    }

    public static File getConfDir() {
        return dirProvider.getConfDir();
    }

    private static void setServerStatus(ServerStatus status, URI wallet) {
        runtimeMode.setServerStatus(status, wallet, dirProvider.getLogFileDir());
    }

    public static boolean isDesktopApplicationEnabled() {
        return RuntimeEnvironment.isDesktopApplicationEnabled() && Nxt.getBooleanProperty("nxt.launchDesktopApplication");
    }

    private static void launchDesktopApplication() {
        runtimeMode.launchDesktopApplication();
    }
    
//    prevent initializing an instance of this class
    private Nxt() {} // never

}
