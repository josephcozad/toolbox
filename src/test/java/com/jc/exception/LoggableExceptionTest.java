package com.jc.exception;

import java.util.logging.Level;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.jc.CommonsTestUtils;
import com.jc.app.rest.exceptions.UnauthorizedException;
import com.jc.db.dao.jpa.EntityManagerCreationException;
import com.jc.db.dao.jpa.JPATransactionException;
import com.jc.log.ExceptionMessageHandler;
import com.jc.logtst.TestCustomException;
import com.jc.util.ConfigInfo;

public class LoggableExceptionTest {

   @Rule
   public final ExpectedException exceptionThrown = ExpectedException.none();

   @BeforeClass
   public static void setUp() throws Exception {
      ConfigInfo info = ConfigInfo.getInstance();
      info.addProperty("log.app.directory", CommonsTestUtils.APPLICTION_LOG_DIR);
   }

   @Test
   //  @Ignore
   public void testExceptionMessageHandler() throws Exception {

      Exception ex = new Exception("Something terrible just happened.");
      String formattedException = ExceptionMessageHandler.formatExceptionMessage(ex);
      System.out.println(formattedException);

      LoggableException lex = LoggableException.createLoggableException(getClass(), Level.SEVERE, ex);
      formattedException = ExceptionMessageHandler.formatExceptionMessage(lex);
      System.out.println(formattedException);

      String additionalMessage = "This is an additional message.";
      lex = LoggableException.createLoggableException(getClass(), Level.SEVERE, additionalMessage, ex);
      formattedException = ExceptionMessageHandler.formatExceptionMessage(lex);
      System.out.println(formattedException);

      // Test logging a NullPointerException...
      Long nullValue = null;
      try {
         int byteValue = nullValue.byteValue();
      }
      catch (Exception npex) {
         formattedException = ExceptionMessageHandler.formatExceptionMessage(npex);
         System.out.println(formattedException);
      }

      // Test logging a custom created exception...
      TestCustomException tcex = new TestCustomException();
      formattedException = ExceptionMessageHandler.formatExceptionMessage(tcex);
      System.out.println(formattedException);

      // Test logging an IllegalArgumentException, following code should throw one.
      try {
         Character.toChars(-1); // IllegalArgumentException without additional message...
      }
      catch (Exception ex2) {
         formattedException = ExceptionMessageHandler.formatExceptionMessage(ex2);
         System.out.println(formattedException);
      }

      try {
         "".wait(-1); // IllegalArgumentException with additional message...
      }
      catch (Exception ex3) {
         formattedException = ExceptionMessageHandler.formatExceptionMessage(ex3);
         System.out.println(formattedException);
      }
   }

   @Test
   //   @Ignore
   public void testLoggableException() throws Exception {

      String userMessage = "This is a user message.";
      String additionalMessage = "This is the additional message.";

      LoggableException lex = new LoggableException(getClass(), Level.SEVERE, "Loggable message.");

      Exception ex = new Exception("Something terrible just happened.");

      lex = LoggableException.createLoggableException(getClass(), Level.SEVERE, ex);
      lex = LoggableException.createLoggableException(getClass(), Level.SEVERE, additionalMessage, ex);

      lex.setErrorCode(TestErrorCode.TEST_ERROR_CODE);

      if (!lex.hasErrorCode()) {
         Assert.fail("Expected LoggableException to have an error code.");
      }
      else {
         ErrorCode errorCode = lex.getErrorCode();
         if (!errorCode.equals(TestErrorCode.TEST_ERROR_CODE)) {
            Assert.fail("Error code from LoggableException " + errorCode + "'' != '" + TestErrorCode.TEST_ERROR_CODE + "'");
         }
      }

      //      String lexMessage = lex.getMessage();
      //      String formattedMessage = ExceptionMessageHandler.formatExceptionMessage(lex);
      //
      //      String lexUserMessage = lex.getUserMessage();
      //      Level severityLevel = lex.getSeverityLevel();
      //
      //   if the userMessage is null then use the message....

      // Create a LoggableException from a LoggableException, no additional logging should take place.
      LoggableException lex2 = LoggableException.createLoggableException(getClass(), Level.SEVERE, lex);
      lex2 = LoggableException.createLoggableException(getClass(), Level.SEVERE, additionalMessage, lex);

      // Test logging a NullPointerException...
      Long nullValue = null;
      try {
         int byteValue = nullValue.byteValue();
      }
      catch (Exception npex) {
         lex = LoggableException.createLoggableException(getClass(), Level.SEVERE, npex);
         lex = LoggableException.createLoggableException(getClass(), Level.SEVERE, additionalMessage, npex);
      }

      // Test logging a custom created exception...
      TestCustomException tcex = new TestCustomException();
      lex = LoggableException.createLoggableException(getClass(), Level.SEVERE, tcex);
      lex = LoggableException.createLoggableException(getClass(), Level.SEVERE, additionalMessage, tcex);

      // Test logging an IllegalArgumentException, following code should throw one.
      try {
         Character.toChars(-1); // IllegalArgumentException without additional message...
      }
      catch (Exception ex2) {
         lex = LoggableException.createLoggableException(getClass(), Level.SEVERE, ex2);
         lex = LoggableException.createLoggableException(getClass(), Level.SEVERE, additionalMessage, ex2);
      }

      try {
         "".wait(-1); // IllegalArgumentException with additional message...
      }
      catch (Exception ex3) {
         lex = LoggableException.createLoggableException(getClass(), Level.SEVERE, ex3);
         lex = LoggableException.createLoggableException(getClass(), Level.SEVERE, additionalMessage, ex3);
      }

      // Supply 'null' exception object...
      lex = LoggableException.createLoggableException(getClass(), Level.SEVERE, null);
      lex = LoggableException.createLoggableException(getClass(), Level.SEVERE, additionalMessage, null);

      String loggableMessage = "This is a loggable message.";
      String serviceId = "XXX";

      PrefixedLoggableException plex = new PrefixedLoggableException(getClass(), Level.SEVERE, loggableMessage, userMessage, serviceId);
      // TODO: verify the format of the user message...
      System.out.println("MSG --> " + plex.getMessage());
      System.out.println("UMSG --> " + plex.getUserMessage());

      Exception ex2 = new Exception("Something else terrible just happened.");
      plex = new PrefixedLoggableException(getClass(), Level.SEVERE, ex2, additionalMessage, userMessage, serviceId);
      // TODO: verify the format of the user message...
      System.out.println("MSG --> " + plex.getMessage());
      System.out.println("UMSG --> " + plex.getUserMessage());
   }

   @Test
   //   @Ignore
   public void testCustomLoggableExceptions() throws Exception {

      String url = "http://some.url.com";
      String inputParams = "some input parameters";
      String message = "This is a message.";
      String permission = "some permission";
      String parameterName = "parameterName";
      String userName = "userName";

      UnauthorizedException uaex = new UnauthorizedException(getClass(), permission, userName);

      JPATransactionException jpatex = new JPATransactionException("aDatasource");
      EntityManagerCreationException emcex = new EntityManagerCreationException("aDatasource");

      DatabaseSessionConnectionException dscex = new DatabaseSessionConnectionException(getClass());

      // These only have a user message and do not log...
      SystemInfoException siex2 = new SystemInfoException(Level.SEVERE, TestErrorCode.TEST_ERROR_CODE, "Don't panic something happened.");
   }

   @Test
   @Ignore
   public void testDatabaseThrownLoggableExceptions() throws Exception {

      // Test chained exceptions with database constraint violation; this test will
      //    verify how chained exceptions are handled.
      runDatabaseFieldConstrainViolation();

      // Test logging database issue where entity can't be created because of bad data.
      runDatabaseBadDataEntityNotFoundViolation();
   }

   private void runDatabaseFieldConstrainViolation() throws Exception {
      //      OnBoardDAOFactory daoFactory = new OnBoardDAOFactory(datasource);
      //      OrganizationDao dao = daoFactory.getOrganizationDAO();
      //
      //      long id = FakeDataGenerator.getRandomInteger(1, DomainTestSuite.numOfOrgs);
      //      Organization data = dao.findById(id);
      //
      //      data.setHcIdentifier("213744549"); // Violates constraints on field.
      //
      //      List<Organization> dataList = new ArrayList<Organization>();
      //      dataList.add(data);
      //
      //      SystemInfoException siex = null;
      //      try {
      //         dataList = dao.updateData(dataList);
      //      }
      //      catch (Exception ex) {
      //         if (!(ex instanceof SystemInfoException)) {
      //            Assert.fail("Expected SystemInfoException... exception thrown was " + ex.getClass() + " " + ex.getMessage());
      //         }
      //
      //         siex = (SystemInfoException) ex;
      //      }
   }

   /*
    * DEV NOTE: Manually change the value that organization vendor id points to in
    *    the database, to a value that references a vendor id that doesn't exist.
    */
   private void runDatabaseBadDataEntityNotFoundViolation() throws Exception {
      //      OnBoardDAOFactory daoFactory = new OnBoardDAOFactory(datasource);
      //      OrganizationDao dao = daoFactory.getOrganizationDAO();
      //
      //      LoggableException lex = null;
      //      try {
      //         long id = 15l;
      //         Organization data = dao.findById(id);
      //      }
      //      catch (Exception ex) {
      //         if (!(ex instanceof LoggableException)) {
      //            Assert.fail("Expected LoggableException... exception thrown was " + ex.getClass() + " " + ex.getMessage());
      //         }
      //
      //         lex = (LoggableException) ex;
      //      }
   }

}
