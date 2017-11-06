package com.jc.logtst;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.jc.CommonsTestUtils;
import com.jc.log.LogMetadata;
import com.jc.log.Logger;
import com.jc.util.ConfigInfo;

public class LoggerTest {

   @Rule
   public final ExpectedException exceptionThrown = ExpectedException.none();

   @BeforeClass
   public static void setUp() throws Exception {
      ConfigInfo info = ConfigInfo.getInstance();
      info.addProperty("log.app.directory", CommonsTestUtils.APPLICTION_LOG_DIR);
   }

   @Test
   //  @Ignore
   public void testLogging() throws Exception {

      // Test turning console logging off, log just to the file...

      Logger.setConsoleLoggingOn(false);

      Logger.setLogLevelForClass(getClass(), Level.ALL);

      LogMetadata lmd = Logger.getLogMetadata(Logger.DEFAULT_LOG_ID);
      if (lmd.isLogToConsole()) {
         Assert.fail("Log to console was still set.");
      }

      String message = "This is a SEVERE error message.";
      Logger.log(getClass(), Level.SEVERE, message);

      message = "This is an INFO message.";
      Logger.log(getClass(), Level.INFO, message);

      message = "This is a CONFIG message.";
      Logger.log(getClass(), Level.CONFIG, message);

      message = "This is a FINER message.";
      Logger.log(getClass(), Level.FINER, message);

      // Test turning console logging on, log to the file and console...

      Logger.setConsoleLoggingOn(true);

      lmd = Logger.getLogMetadata(Logger.DEFAULT_LOG_ID);
      if (!lmd.isLogToConsole()) {
         Assert.fail("Log to console was not set.");
      }

      message = "I said... hello how are you?";
      Logger.log(getClass(), Level.SEVERE, message);

      // Test logging an exception....

      Exception ex = new Exception("Something terrible just happened!");
      Logger.log(getClass(), Level.SEVERE, ex);

      // Test logging a NullPointerException...
      Long nullValue = null;
      try {
         int byteValue = nullValue.byteValue();
      }
      catch (Exception npex) {
         Logger.log(getClass(), Level.SEVERE, npex);
      }

      // Test logging a custom created exception...
      TestCustomException tcex = new TestCustomException();
      Logger.log(getClass(), Level.SEVERE, tcex);

      // Test logging an exception, with an additional custom message....

      String additionalMessage = "Oh my goodness what was it?";
      ex = new Exception("Something else terrible just happened!");
      Logger.log(getClass(), Level.SEVERE, additionalMessage, ex);

      // Test logging a NullPointerException...
      additionalMessage = "Oh no a NPE!";
      try {
         int byteValue = nullValue.byteValue();
      }
      catch (Exception npex) {
         Logger.log(getClass(), Level.SEVERE, additionalMessage, npex);
      }

      // Test logging a custom created exception...
      additionalMessage = "This is just not right!";
      tcex = new TestCustomException();
      Logger.log(getClass(), Level.SEVERE, additionalMessage, tcex);

      // Test logging an IllegalArgumentException, following code should throw one.
      try {
         Character.toChars(-1); // IllegalArgumentException without additional message...
      }
      catch (Exception ex2) {
         Logger.log(getClass(), Level.SEVERE, ex2);
      }

      try {
         "".wait(-1); // IllegalArgumentException with additional message...
      }
      catch (Exception ex3) {
         Logger.log(getClass(), Level.SEVERE, ex3);
      }

      Logger.log(getClass(), Level.SEVERE, (Throwable) null);

      //      try {
      //         doJDBCConnection();
      //      }
      //      catch (Exception ex4) {
      //         Logger.log(getClass(), Level.SEVERE, ex4);
      //      }

   }

   // An attempt to generate a database related chained exception stack...
   private void doJDBCConnection() throws Exception {
      String DB_URL = "jdbc:mysql://localhost/EMP";
      String USER = "username";
      String PASS = "password";

      Class.forName("com.mysql.jdbc.Driver");
      Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
      Statement stmt = conn.createStatement();
      String sql = "SELECT id, first, last, age FROM Employees";
      ResultSet rs = stmt.executeQuery(sql);

      while (rs.next()) {}

      rs.close();
      stmt.close();
      conn.close();
   }

   // Improve test coverage by testing these things....
   // Also need to add programatic verification of the log file generated...

   //    add property 'logToConsole' to test with...
   //    add property 'datasourceClass' to test with... test logging to a database table...

   //      public static void flushAllLogs() {
   //      public static void flushLog(String logid) {

   //      public static void setDefaultLogging(boolean value) {

   //      test these with logid == "" or null.
   //      public static void log(String logid, Class<?> loggingClass, Level level, String msg) {
   //      public static void log(String logid, Class<?> loggingClass, Level level, String msg, Throwable ex) {

   //      public static String saveOutput(StringBuilder fileContent)
   //      public static String saveOutput(String logid, StringBuilder fileContent)

   //      public static String saveStackTrace(Throwable ex)
   //      public static String saveStackTrace(String logid, Throwable ex)

}
