package com.jc.db;

import java.util.Calendar;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Level;

import com.jc.log.Logger;
import com.jc.util.Timer;
import com.jc.util.Timer.TimerListener;

final class DBConnectionPool implements TimerListener {

   private final static int MAX_MASTER_TIME = 30; // number of minutes to set the master timer for before waking up to clean up closed connection.

   private static final Hashtable<String, Vector<DBConnection>> DBCONNECTION_STORE;

   private static DBConnectionPool SELF;

   static {
      DBCONNECTION_STORE = new Hashtable<String, Vector<DBConnection>>();
      SELF = new DBConnectionPool();
   }

   private Timer MasterTimer; // A timer to close down the DBConnection object before Midnight each night.

   private DBConnectionPool() {
      startTimer();
   }

   static void printConnectionInfo() throws Exception {
      String[] keys = DBCONNECTION_STORE.keySet().toArray(new String[DBCONNECTION_STORE.size()]);
      for (String key : keys) {
         printConnectionInfo(key);
      }
   }

   static void printConnectionInfo(String connection_id) throws Exception {
      if (DBCONNECTION_STORE.containsKey(connection_id)) {
         Vector<DBConnection> connections = DBCONNECTION_STORE.get(connection_id);
         int num_connections = connections.size();
         System.out.println("CONN_ID[" + connection_id + "] has " + num_connections + " connections.");
         for (int j = 0; j < num_connections; j++) {
            DBConnection conn = connections.get(j);
            boolean in_use = conn.inUse();
            System.out.println("     " + conn.getID() + " IN_USE[" + in_use + "] CLOSED[" + conn.isClosed() + "]");
         }
         System.out.println("-------------------------------------------------");
      }
   }

   static int getNumberOfConnections() throws Exception {
      int total_connections = 0;
      String[] keys = DBCONNECTION_STORE.keySet().toArray(new String[DBCONNECTION_STORE.size()]);
      for (String key : keys) {
         total_connections += getNumberOfConnections(key);
      }
      return (total_connections);
   }

   static int getNumberOfConnections(String connection_id) throws Exception {
      int num_connections = 0;
      if (DBCONNECTION_STORE.containsKey(connection_id)) {
         Vector<DBConnection> connections = DBCONNECTION_STORE.get(connection_id);
         num_connections = connections.size();
      }
      return (num_connections);
   }

   static int getNumberOfInUseConnections() throws Exception {
      int num_active = 0;
      String[] keys = DBCONNECTION_STORE.keySet().toArray(new String[DBCONNECTION_STORE.size()]);
      for (String key : keys) {
         num_active += getNumberOfInUseConnections(key);
      }
      return (num_active);
   }

   static int getNumberOfInUseConnections(String connection_id) throws Exception {
      int num_active = 0;
      if (DBCONNECTION_STORE.containsKey(connection_id)) {
         Vector<DBConnection> connections = DBCONNECTION_STORE.get(connection_id);
         int num_connections = connections.size();
         for (int j = 0; j < num_connections; j++) {
            DBConnection conn = connections.get(j);
            boolean in_use = conn.inUse();
            if (in_use) {
               num_active++;
            }
         }
      }
      return (num_active);
   }

   static int getNumberOfNotInUseConnections() throws Exception {
      int num_inactive = 0;
      String[] keys = DBCONNECTION_STORE.keySet().toArray(new String[DBCONNECTION_STORE.size()]);
      for (String key : keys) {
         num_inactive += getNumberOfNotInUseConnections(key);
      }
      return (num_inactive);
   }

   static int getNumberOfNotInUseConnections(String connection_id) throws Exception {
      int num_inactive = 0;
      if (DBCONNECTION_STORE.containsKey(connection_id)) {
         Vector<DBConnection> connections = DBCONNECTION_STORE.get(connection_id);
         int num_connections = connections.size();
         for (int j = 0; j < num_connections; j++) {
            DBConnection conn = connections.get(j);
            if (!conn.inUse()) {
               num_inactive++;
            }
         }
      }
      return (num_inactive);
   }

   static synchronized DBConnection getDBConnection(String connection_id, String connectionMethod) throws Exception {
      DBConnection connection = null;
      boolean found = false;
      if (DBCONNECTION_STORE.containsKey(connection_id)) {
         Vector<DBConnection> connvec = DBCONNECTION_STORE.get(connection_id);
         int num_connections = connvec.size();

         DBConnectionPool.logMessage("Connection pool contains " + num_connections + " DBConnection objects for " + connection_id + ".");

         for (int i = 0; i < num_connections && !found; i++) {
            connection = connvec.get(i);
            if (!connection.isActive() && !connection.inUse() && !connection.isClosed()) {
               connection.setActive(true);
               found = true;
               DBConnectionPool.logMessage("Found " + connection.getID() + " connection.", connection);
            }
         }

         if (!found) {
            DBConnectionPool.logMessage("No DBConnection object found that wasn't in use and had an open connection.");
            connection = new DBConnection(connection_id, connectionMethod);
            connection.setActive(true);
            connvec.add(connection);
            found = true;
            DBConnectionPool.logMessage("Created a new DBConnection object, " + connection.getID() + ".", connection);
         }
      }
      else {
         DBConnectionPool.logMessage("Connection pool does not have any DBConnection objects for " + connection_id + ".");
         Vector<DBConnection> connvec = new Vector<DBConnection>();
         connection = new DBConnection(connection_id, connectionMethod);
         connection.setActive(true);
         connvec.add(connection);
         found = true;
         DBCONNECTION_STORE.put(connection_id, connvec);
         DBConnectionPool.logMessage("Created a new DBConnection object, " + connection.getID() + ".", connection);
      }

      DBConnectionPool.logMessage("Returning DBConnection[" + connection.getID() + "].", connection);
      return (connection);
   }

   /*
    * This removes one DBConnection from the pool.
    */
   static synchronized void removeDBConnection(String dbconn_id) throws Exception {
      boolean found = false;
      String connection_id = "";

      String[] keys = DBCONNECTION_STORE.keySet().toArray(new String[DBCONNECTION_STORE.size()]);
      for (int i = 0; i < keys.length && !found; i++) {
         Vector<DBConnection> connections = DBCONNECTION_STORE.get(keys[i]);
         int num_connections = connections.size();
         for (int j = 0; j < num_connections && !found; j++) {
            DBConnection conn = connections.get(j);
            if (conn.getID().equals(dbconn_id)) {
               found = true;
               connections.remove(j);
               connection_id = keys[i];
            }
         }
      }

      DBConnectionPool.logMessage("Removed[" + found + "] " + dbconn_id + " from DBCONNECTION_STORE with connection id of '" + connection_id + "'.");
   }

   /*
    * This removes the set of DBConnections from the pool referenced by connection_id
    */
   static synchronized void remove(String connection_id) throws Exception {
      if (DBCONNECTION_STORE.containsKey(connection_id)) {
         Vector<DBConnection> connvec = DBCONNECTION_STORE.get(connection_id);
         connvec.clear();
         DBCONNECTION_STORE.remove(connection_id);
      }

      DBConnectionPool.logMessage("Removed all DBConnection objects from DBCONNECTION_STORE with key of '" + connection_id + "'.");

      if (DBCONNECTION_STORE.isEmpty()) {
         DBConnectionPool.logMessage("DBCONNECTION_STORE is empty.");
      }
      else {
         StringBuilder sb = new StringBuilder();
         if (!DBCONNECTION_STORE.isEmpty()) {
            sb.append("They are: ");
         }

         String[] keys = DBCONNECTION_STORE.keySet().toArray(new String[DBCONNECTION_STORE.size()]);
         for (Object key : keys) {
            if (sb.length() > 0) {
               sb.append(", ");
            }
            sb.append(key);
         }

         DBConnectionPool.logMessage("DBCONNECTION_STORE now has " + DBCONNECTION_STORE.size() + " connection object(s) left. " + sb.toString());
      }
   }

   static synchronized void removeAll() throws Exception {
      String[] keys = DBCONNECTION_STORE.keySet().toArray(new String[DBCONNECTION_STORE.size()]);
      for (String key : keys) {
         remove(key);
      }
   }

   static void releaseConnection(DBConnection connection) {
      connection.setActive(false);
   }

   private static void logMessage(String message) throws Exception {
      String statement = "|||||" + message;
      Logger.log("db", DBConnectionPool.class, Level.INFO, statement);
   }

   private static void logMessage(String message, DBConnection connection) throws Exception {
      String conn_status = connection.getJDBCConnectionStatus();
      String timer_status = connection.getTimerStatus();
      String in_use = connection.getUseStatus();
      String active = connection.getActiveStatus();
      String instance_id = connection.getInstanceID();

      String statement = instance_id + "|" + conn_status + "|" + timer_status + "|" + in_use + "|" + active + "|" + message;
      Logger.log("db", DBConnectionPool.class, Level.INFO, statement);
   }

   private void startTimer() {
      Calendar c = Calendar.getInstance();
      long now = c.getTimeInMillis();

      c.set(Calendar.HOUR_OF_DAY, 0);
      c.set(Calendar.MINUTE, 0);
      c.set(Calendar.SECOND, 0);
      c.set(Calendar.MILLISECOND, 0);
      c.add(Calendar.DAY_OF_MONTH, 1);
      long midnight = c.getTimeInMillis();

      // long runtime = midnight - now; // Run until midnight...

      long runtime = Timer.ONE_MINUTE_INTERVAL * MAX_MASTER_TIME; // Run for 30 minutes only...

      MasterTimer = new Timer(runtime);
      MasterTimer.addTimerListener(this);
      MasterTimer.logMessages(false);
      MasterTimer.start();
   }

   // ---------------------------------------- Implements TimerListener ---------------------------------------- //

   @Override
   public void timeExpired() {
      try {
         MasterTimer.stop();
         MasterTimer.removeTimerListener(this);
         MasterTimer = null;

         if (!DBCONNECTION_STORE.isEmpty()) {
            synchronized (DBCONNECTION_STORE) {
               int num_connections = DBConnectionPool.getNumberOfConnections();
               int num_closed = 0;

               String[] ids = DBCONNECTION_STORE.keySet().toArray(new String[DBCONNECTION_STORE.size()]);
               for (String id : ids) {
                  Vector<DBConnection> connvec = DBCONNECTION_STORE.get(id);
                  Vector<DBConnection> connvec_new = new Vector<DBConnection>();
                  DBConnection[] connray = connvec.toArray(new DBConnection[connvec.size()]);
                  for (DBConnection element : connray) {
                     if (element.isClosed()) {
                        DBConnectionPool.logMessage(id + "." + element.getID() + " was closed and not in use, removed from the DBConnectionPool.");
                        num_closed++;
                     }
                     else {
                        connvec_new.add(element);
                     }
                  }
                  DBCONNECTION_STORE.put(id, connvec_new);
               }

               if (num_connections != num_closed) {
                  DBConnectionPool.logMessage("DBConnection closed " + num_closed + " DBConnections out of " + num_connections + " total DBConnections.");
               }
            }
         }

         startTimer();

         // logDebugMessage("Master timer restarted.");
      }
      catch (Exception ex) {}
   }
}
