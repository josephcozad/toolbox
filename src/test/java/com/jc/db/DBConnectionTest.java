package com.jc.db;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.logging.Level;

import org.junit.Ignore;
import org.junit.Test;

import com.jc.log.Logger;

public class DBConnectionTest {

   @Test
   // @Ignore
   public void testGetInstance() throws Exception {
      String datasource = "devDb";
      String username = "JUNIT";
      String password = "password";

      DBConnectionPool.removeAll();

      // Default datasource...
      DBConnection connection = DBConnection.getInstance();
      DBResult result = connection.executeSQLQuery("select trunc(sysdate) from dual");
      if (result.hasErrors()) {
         fail(result.getErrorMessage());
      }

      // Valid datasource...
      connection = DBConnection.getInstance(datasource);
      result = connection.executeSQLQuery("select trunc(sysdate) from dual");
      if (result.hasErrors()) {
         fail(result.getErrorMessage());
      }

      // Invalid datasource...
      try {
         connection = DBConnection.getInstance("caca");
         fail("DBConnection was created with an invalid datasource name of 'caca'.");
      }
      catch (Exception ex) {
         // ignore
      }

      // ----------------------------

      // Valid datasource, password, and username ...
      connection = DBConnection.getInstance(datasource, username, password);
      result = connection.executeSQLQuery("select trunc(sysdate) from dual");
      if (result.hasErrors()) {
         fail(result.getErrorMessage());
      }

      // Valid datasource and password, invalid username ...
      connection = DBConnection.getInstance(datasource, "caca", password);
      result = connection.executeSQLQuery("select trunc(sysdate) from dual");
      if (result.hasErrors()) {
         String errors = result.getErrorMessage();
         assertTrue("Database connection established with invalid username.", errors.contains("invalid username/password"));
      }

      // Valid datasource and username, invalid password ...
      connection = DBConnection.getInstance(datasource, username, "caca");
      result = connection.executeSQLQuery("select trunc(sysdate) from dual");
      if (result.hasErrors()) {
         String errors = result.getErrorMessage();
         assertTrue("Database connection established with invalid password.", errors.contains("invalid username/password"));
      }

      // ----------------------------

      // Valid User object... (Requires a "weblogicHostName" property in the ConfigInfo).
      //
      // ConfigInfo.getInstance().addProperty("weblogicHostName", "localhost:7001");
      //
      // User user = User.authenticate("devDb", "cozad_dev", "password");
      // connection = DBConnection.getInstance(user);
      // result = connection.executeSQLQuery("select trunc(sysdate) from dual");
      // if (result.hasErrors()) {
      // fail(result.getErrorMessage());
      // }
   }

   @Test
   // @Ignore
   public void testDBConnectionInactivityTimer() throws Exception {
      /*
       * Tests to see if the DBConnection object is closed after the inactivity timer expires.
       */

      DBConnectionPool.removeAll();

      DBConnection.setMaxInactiveMinutes(1); // set the inactivity timer to 60 secs.

      long length_of_task = 15000; // run the stored procedure for 15 secs.

      Object[] inParams = new Object[] {
         length_of_task
      };

      int[] outParams = new int[] {
      //        DBConnection.ORACLE_TYPE_CURSOR
      };

      DBConnection conn = DBConnection.getInstance();
      DBResult result = conn.executeStoredProcedure("RUN_FOR_MILLISECS", inParams, outParams);
      if (!result.hasErrors()) {
         // get result here....
      }
      else {
         throw (new Exception(result.getErrorMessage()));
      }

      if (conn.isClosed()) {
         fail("Expected DBConnection object to be open.");
      }

      Thread.sleep(70000); // wait 70 seconds so that the inactivity timer will time out and close the connection

      if (!conn.isClosed()) {
         fail("Expected DBConnection object to be closed.");
      }
   }

   @Test
   // @Ignore
   public void testMultipleInactiveDBConnections() throws Exception {
      DBConnectionPool.removeAll();

      DBConnection connection1 = DBConnection.getInstance();
      String connection_id1 = connection1.toString();

      if (connection1.inUse()) {
         fail("First connection was not marked as in use.");
      }

      DBConnection connection2 = DBConnection.getInstance();
      String connection_id2 = connection2.toString();

      if (connection2.inUse()) {
         fail("Second connection was not marked as in use.");
      }

      if (connection_id1.equals(connection_id2)) {
         fail("First connection was reused, and not marked as in use.");
      }

      int num_inactive = DBConnectionPool.getNumberOfNotInUseConnections();
      if (num_inactive != 2) {
         fail("DBConnectionPool reported incorrect number of inactive connections, should be 2 was " + num_inactive + ".");
      }

      int num_active = DBConnectionPool.getNumberOfInUseConnections();
      if (num_active != 0) {
         fail("DBConnectionPool reported incorrect number of active connections, should be 0 was " + num_active + ".");
      }
   }

   @Test
   // @Ignore
   public void testMultipleDBConnections() throws Exception {
      /*
       * This test should create 4 unique JDBC connections, each connection should return normally,
       * some of the connections should be reused to complete all 10 database requests.
       */
      DBConnectionPool.removeAll();

      DBConnection.setMaxInactiveMinutes(1); // set the inactivity timer to 60 seconds

      ArrayList<DBConnectionTestWorker> workers = new ArrayList<DBConnectionTestWorker>();
      for (int i = 0; i < 10; ++i) {
         DBConnectionTestWorker worker = new DBConnectionTestWorker("Worker_" + i, 20000);
         workers.add(worker);
         worker.start();
         Thread.sleep(7000);
      }

      int active_connections = DBConnectionPool.getNumberOfInUseConnections();
      if (active_connections == 0) {
         DBConnectionPool.printConnectionInfo();
         fail("No active connections were detected in the DBConnectionPool.");
      }
      else if (active_connections > 4) {
         DBConnectionPool.printConnectionInfo();
         fail("More than 4 active connections were detected in the DBConnectionPool.");
      }

      boolean done = false;
      while (!done) {
         Thread.sleep(500);

         int num_done = 0;
         for (DBConnectionTestWorker worker : workers) {
            if (worker.isDone()) {
               num_done++;
            }
         }

         if (num_done == workers.size()) {
            done = true;
         }
      }

      active_connections = DBConnectionPool.getNumberOfInUseConnections();
      if (active_connections != 0) {
         DBConnectionPool.printConnectionInfo();
         fail("Active connections were detected in the DBConnectionPool.");
      }
   }

   @Test
   // @Ignore
   public void testClose() throws Exception {
      DBConnectionPool.removeAll();

      DBConnection connection1 = DBConnection.getInstance();
      DBConnection connection2 = DBConnection.getInstance();
      DBConnection connection3 = DBConnection.getInstance();
      DBConnection connection4 = DBConnection.getInstance();

      DBResult result1 = connection1.executeSQLQuery("select trunc(sysdate) from dual");
      DBResult result2 = connection2.executeSQLQuery("select trunc(sysdate) from dual");
      DBResult result3 = connection3.executeSQLQuery("select trunc(sysdate) from dual");
      DBResult result4 = connection4.executeSQLQuery("select trunc(sysdate) from dual");

      int num_conn = DBConnectionPool.getNumberOfConnections();
      if (num_conn != 4) {
         fail("Expected 4 DBConnection objects in the pool and there were " + num_conn + ".");
      }

      connection3.close("DBConnectionTest.testClose()");

      num_conn = DBConnectionPool.getNumberOfConnections();
      if (num_conn != 3) {
         fail("Expected 3 DBConnection objects in the pool and there were " + num_conn + ".");
      }
   }

   @Test
   // @Ignore
   public void testDBConnectionDeadlock() throws Exception {
      /*
       * Tries to simulate a deadlock possible situation in which a number of DBConnectionWorkers
       * all request DBConnection objects at the same time.
       */
      DBConnectionPool.removeAll();

      DBConnectionTestWorker.turnOnDeadlockTesting();

      ArrayList<DBConnectionTestWorker> workers = new ArrayList<DBConnectionTestWorker>();
      for (int i = 0; i < 200; ++i) {
         DBConnectionTestWorker worker = new DBConnectionTestWorker("Worker_" + i, 60000);
         workers.add(worker);
         worker.start();
      }

      int num_workers = workers.size();

      Thread.sleep(5000); // wait before starting test

      DBConnectionTestWorker.releaseAllWorkers(); // Release them....

      Thread.sleep(15000); // wait before starting test

      int active_connections = DBConnectionPool.getNumberOfInUseConnections();
      if (active_connections == 0) {
         DBConnectionPool.printConnectionInfo();
         fail("No active connections were detected in the DBConnectionPool.");
      }
      else if (active_connections > num_workers) {
         DBConnectionPool.printConnectionInfo();
         fail("More than " + num_workers + " active connections were detected in the DBConnectionPool.");
      }

      boolean done = false;
      while (!done) {
         Thread.sleep(500);

         int num_done = 0;
         for (DBConnectionTestWorker worker : workers) {
            if (worker.isDone()) {
               num_done++;
            }
         }

         if (num_done == workers.size()) {
            done = true;
         }
      }

      Thread.sleep(10000); // wait before starting test

      active_connections = DBConnectionPool.getNumberOfInUseConnections();
      if (active_connections != 0) {
         DBConnectionPool.printConnectionInfo();
         fail("Active connections were detected in the DBConnectionPool.");
      }
   }

   @Test
   @Ignore("Run manually to monitor output. This test assumes that DBConnection.MAX_INACTIVE_TIME = 1 and DBConnection.MAX_MASTER_TIME = 2.")
   public void testDBConnectionMasterTimer() throws Exception {

      DBConnectionPool.removeAll();

      DBConnection.setMaxInactiveMinutes(1); // set the inactivity timer to 60 secs.

      // Create a bunch of DBConnection objects and use them...
      ArrayList<DBConnectionTestWorker> workers = new ArrayList<DBConnectionTestWorker>();
      for (int i = 0; i < 5; ++i) {
         DBConnectionTestWorker worker = new DBConnectionTestWorker("Worker_" + i, 15000);
         workers.add(worker);
         worker.start();
         Thread.sleep(500);
         System.out.println("15: " + worker.getID());
      }

      for (int i = 0; i < 5; ++i) {
         DBConnectionTestWorker worker = new DBConnectionTestWorker("Worker_" + i, 210000);
         workers.add(worker);
         worker.start();
         Thread.sleep(500);
         System.out.println("210: " + worker.getID());
      }

      Thread.sleep(5000);

      System.out.println("----------------------------------------------");

      int num_active1 = DBConnectionPool.getNumberOfInUseConnections();
      int num_inactive1 = DBConnectionPool.getNumberOfNotInUseConnections();

      System.out.println("num_active1[" + num_active1 + "]  num_inactive1[" + num_inactive1 + "]");

      Thread.sleep(160000);

      int num_active2 = DBConnectionPool.getNumberOfInUseConnections();
      int num_inactive2 = DBConnectionPool.getNumberOfNotInUseConnections();

      System.out.println("num_active2[" + num_active2 + "]  num_inactive2[" + num_inactive2 + "]");

      System.out.println("----------------------------------------------");

      DBConnectionPool.printConnectionInfo();

   }

   /**
    * This method logs the message, by default, Standard out.
    */
   private void writeToDBLog(String message) throws Exception {
      System.out.println(message);
      String statement = message + "|||";
      Logger.log("db", this.getClass(), Level.INFO, statement);
   }
}
