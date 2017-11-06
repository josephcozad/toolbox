package com.jc.db;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

import javax.naming.NamingException;

import com.jc.db.command.ConnectionCommand;
import com.jc.db.command.JDBCConnection;
import com.jc.log.ExceptionMessageHandler;
import com.jc.log.Logger;
import com.jc.util.ConfigInfo;
import com.jc.util.Timer;
import com.jc.util.Timer.TimerListener;

public class DBConnection implements DatasourcePropKeys, TimerListener {

   private static int MAX_INACTIVE_MINUTES = 1; // each JDBC connection can be inactive for only these many minutes

   private final static Random RANDOM_GENERATOR = new Random();

   private static int INSTANCE_NUM = 0;

   private boolean LogQueryInfo;
   private boolean LogMessages;

   private Connection AConnection;
   private Timer ATimer;
   private boolean InUse; // Indicates if the DBConnection object is in the process of being used to run a SQL query.
   private boolean Active; // Indicates if the DBConnection has been given out by the DBConnection Pool for use. Active = true (yes); Active = false (no).

   private final String ConnectionId;

   private final int InstanceNum;

   ConnectionCommand ConnectionCommand;

   DBConnection(String connectionId, String connectionMethod) throws Exception {
      InstanceNum = INSTANCE_NUM++;
      ConnectionId = connectionId;

      logQueries(true);

      logMessages(true);
      logMessage(Level.INFO, "New DBConnection object created; method[" + connectionMethod + "].");
   }

   /**
    * Creates a DBConnection based on information in the ConfigInfo assigned to the "datasource property key that is assigned to "default.datasource".
    */
   public static DBConnection getInstance() throws FileNotFoundException, GeneralSecurityException, Exception {
      String dataSource = ConfigInfo.getInstance().getProperty(DEFAULT_DBDATASOURCE_PROPKEY);
      return (getInstance(dataSource));
   }

   /**
    * Creates a DBConnection based on information in the ConfigInfo assigned to the supplied "datasource" property key. Note that the password contained in the ConfigInfo is assumed to be unencrypted.
    */
   public static DBConnection getInstance(String datasource) throws FileNotFoundException, GeneralSecurityException, Exception {
      ConfigInfo info = ConfigInfo.getInstance();
      String username = info.getProperty(datasource + DOT + USERNAME_PROPKEY);

      String database = datasource;
      if (info.hasProperty("datasource" + '.' + DATASOURCE_NAME_PROPKEY)) {
         database = info.getProperty(datasource + DOT + DATASOURCE_NAME_PROPKEY);
      }

      DBConnection connection = DBConnectionPool.getDBConnection(username + ":" + database, "By Datasource");
      if (connection.ConnectionCommand == null) {
         ConfigInfo config_info = ConfigInfo.getInstance();
         String password = config_info.getProperty(datasource + DOT + PASSWORD_PROPKEY);
         String driver = config_info.getProperty(datasource + DOT + JDBC_DRIVER_PROPKEY);
         String url = config_info.getProperty(datasource + DOT + JDBC_DRIVER_URL_PROPKEY);
         connection.setConnectionCommand(new JDBCConnection(username, password, driver, url));
      }

      return connection;
   }

   /**
    * Creates a DBConnection based on information in the ConfigInfo assigned to the supplied "datasource" property key. The supplied securityKey is used to decrypt a password contained in the ConfigInfo for the supplied datasource.
    */
   public static DBConnection getInstance(String datasource, String securityKey) throws FileNotFoundException, GeneralSecurityException, Exception {
      ConfigInfo info = ConfigInfo.getInstance();
      String username = info.getProperty(datasource + DOT + USERNAME_PROPKEY);

      String database = datasource;
      if (info.hasProperty("datasource" + DOT + DATASOURCE_NAME_PROPKEY)) {
         database = info.getProperty(datasource + DOT + DATASOURCE_NAME_PROPKEY);
      }

      DBConnection connection = DBConnectionPool.getDBConnection(username + ":" + database, "By Datasource");
      if (connection.ConnectionCommand == null) {
         ConfigInfo config_info = ConfigInfo.getInstance();
         String password = config_info.getProperty(datasource + DOT + PASSWORD_PROPKEY, securityKey);
         String driver = config_info.getProperty(datasource + DOT + JDBC_DRIVER_PROPKEY);
         String url = config_info.getProperty(datasource + DOT + JDBC_DRIVER_URL_PROPKEY);
         connection.setConnectionCommand(new JDBCConnection(username, password, driver, url));
      }

      return connection;
   }

   /**
    * Creates a DBConnection based on information in the ConfigInfo assigned to the supplied "datasource" property key, with the supplied username and unencrypted password.
    */
   public static DBConnection getInstance(String datasource, String username, String password) throws FileNotFoundException, Exception {
      ConfigInfo info = ConfigInfo.getInstance();
      String database = datasource;
      if (info.hasProperty("datasource" + DOT + DATASOURCE_NAME_PROPKEY)) {
         database = info.getProperty(datasource + DOT + DATASOURCE_NAME_PROPKEY);
      }

      DBConnection connection = DBConnectionPool.getDBConnection(username + ":" + database, "By Datasource with user/pwd");
      if (connection.ConnectionCommand == null) {
         ConfigInfo config_info = ConfigInfo.getInstance();
         String driver = config_info.getProperty(datasource + DOT + JDBC_DRIVER_PROPKEY);
         String url = config_info.getProperty(datasource + DOT + JDBC_DRIVER_URL_PROPKEY);
         connection.setConnectionCommand(new JDBCConnection(username, password, driver, url));
      }

      return connection;
   }

   public static DBConnection getInstance(JDBCConnection jdbcConnection) throws FileNotFoundException, GeneralSecurityException, Exception {
      String username = jdbcConnection.getUserName();
      String datasource = jdbcConnection.getDatasource();

      DBConnection connection = DBConnectionPool.getDBConnection(username + ":" + datasource, "By JDBCConnection");
      if (connection.ConnectionCommand == null) {
         connection.setConnectionCommand(jdbcConnection);
      }

      return connection;
   }

   public static void setMaxInactiveMinutes(int num_mins) {
      MAX_INACTIVE_MINUTES = num_mins;
   }

   /**
    * Forces the connection closed, turning off the inactivity timer, and removing it from the cached connections.
    * 
    * @throws Exception
    */
   public void close(String value) throws Exception {
      if (!inUse()) {
         // First remove the connection store to assure that no stale connections are reused.
         DBConnectionPool.removeDBConnection(getID());
         if (AConnection != null && !AConnection.isClosed()) {
            AConnection.close();
            AConnection = null;
            stopTimer();
            logMessage(Level.INFO, "Closed JDBC connection at the request of " + value + ".");
         }
         else {
            logMessage(Level.INFO, value + " requested a close but this connection already closed.");
         }
      }
      else {
         logMessage(Level.INFO, value + " requested a close but this connection is still in use.");
      }
   }

   boolean isActive() {
      return (Active);
   }

   void setActive(boolean active) {
      Active = active;
   }

   private Connection openNewConnection() throws Exception {
      Connection connection = null;
      if (ConnectionCommand != null) {
         // If no open connections where found, try to create a new one.
         logDebugMessage(Level.INFO, "Begin creating a new connection.");

         int num_tries = 1; // Only try to create a new connection once...
         for (int i = 0; i < num_tries; ++i) {
            long time = System.currentTimeMillis();
            connection = ConnectionCommand.createConnection();
            time = System.currentTimeMillis() - time;

            if (connection != null) {
               ATimer = new Timer(Timer.ONE_MINUTE_INTERVAL * MAX_INACTIVE_MINUTES);
               ATimer.addTimerListener(this);
               // ATimer.logMessages(LOGMESSAGES);

               logDebugMessage(Level.INFO, "Created a new connection object, " + connection.hashCode() + " on " + (i + 1) + " try; time to create " + time
                     + ".");
            }
            else {
               logDebugMessage(Level.INFO, "Connection creation failed on " + i + " try.");
               try {
                  // sleep for 2.5 seconds and try again...
                  Thread.sleep(2500);
               }
               catch (InterruptedException e) {
                  logException(e);
               }
            }
         }
      }
      else {
         logDebugMessage(Level.SEVERE, "**** No connection command available to create database connection; Exception thrown.");
         throw (new Exception("No connection command available to create database connection."));
      }
      return (connection);
   }

   private Connection getConnection(int query_id) throws ClassNotFoundException, FileNotFoundException, NamingException, SQLException, Exception {
      if (isActive()) { // Must be in Active state....
         logDebugMessage(Level.INFO, "Getting JDBC connection for query id[" + query_id + "].");
         if (AConnection == null) {
            AConnection = openNewConnection();
            if (AConnection != null) {
               logDebugMessage(Level.INFO, "New JDBC connection opened, for query id[" + query_id + "]");
               InUse = true;
            }
            else {
               logDebugMessage(Level.SEVERE, "New JDBC connection was null; exception thrown, for query id[" + query_id + "]");
               throw (new Exception("Unable to open a connection to the database."));
            }
         }
         else { // Connection is not null...
            if (AConnection.isClosed()) { // Connection closed...
               if (inUse()) {
                  AConnection = null;
                  stopTimer();
                  InUse = false;
                  logDebugMessage(Level.SEVERE, "Connection was closed but marked as 'in use', for query id[" + query_id + "]");
                  throw (new Exception("Connection was closed but marked as 'in use'."));
               }
               else {
                  logDebugMessage(Level.INFO, "Stopping timer before opening new JDBC connection, for query id[" + query_id + "]");
                  stopTimer();
                  AConnection = openNewConnection();
                  if (AConnection != null) {
                     logDebugMessage(Level.INFO, "Changing InUse to true, for query id[" + query_id + "]");
                     InUse = true;
                     logDebugMessage(Level.INFO, "New JDBC connection opened, for query id[" + query_id + "]");
                  }
                  else {
                     logDebugMessage(Level.SEVERE, "New JDBC connection was null; Exception thrown, for query id[" + query_id + "]");
                     throw (new Exception("Unable to open a connection to the database."));
                  }
               }
            }
            else { // Connection open....
               if (!inUse()) {
                  if (ATimer != null && ATimer.isRunning()) {
                     logDebugMessage(Level.INFO, "Stopping timer, for query id[" + query_id + "]");
                     ATimer.stop();
                     logDebugMessage(Level.INFO, "Timer stopped, for query id[" + query_id + "]");
                  }
                  else {
                     logDebugMessage(Level.INFO, "Timer was null or not running, for query id[" + query_id + "]");
                  }
                  logDebugMessage(Level.INFO, "Changing InUse to true, for query id[" + query_id + "]");
                  InUse = true;
                  logDebugMessage(Level.INFO, "Marked DBConnection as 'IN USE', for query id[" + query_id + "]");
               }
               else {
                  logDebugMessage(Level.INFO, "DBConnection was already marked as IN USE, for query id[" + query_id + "]");
               }
               // Do nothing, all ok...
            }
         }
      }
      else {
         throw (new IllegalStateException(
               "DBConnection is INACTIVE while trying to get a new JDBC connection. Possible cause: DBConnection instance is being reused. Use DBConnection.getInstance() to get a new DBConnection with each use."));
      }
      return (AConnection);
   }

   private void stopTimer() {
      if (ATimer != null && ATimer.isRunning()) {
         ATimer.stop();
         ATimer.removeTimerListener(this);
      }
      ATimer = null;
   }

   private void releaseConnection() {
      if (!transactionStarted()) {
         InUse = false;
         if (ATimer != null) {
            ATimer.start(); // Use to time the connection's inactivity.
         }
         DBConnectionPool.releaseConnection(this);
      }
   }

   // Support for Transaction Processing ---------------------------------

   private boolean transactionStarted = false;

   public boolean transactionStarted() {
      return transactionStarted;
   }

   public void startTransaction() {
      try {
         int query_id = Math.abs(RANDOM_GENERATOR.nextInt());
         Connection conn = getConnection(query_id);
         conn.setAutoCommit(false);
         transactionStarted = true;
      }
      catch (Exception ex) {
         transactionStarted = false;
         releaseConnection();
         logException(ex);
      }
   }

   public void endTransaction() {
      try {
         AConnection.commit();
         AConnection.setAutoCommit(true);
      }
      catch (SQLException ex) {
         logException(ex);
      }
      finally {
         transactionStarted = false;
         releaseConnection();
      }
   }

   public void rollBackTransaction() {
      try {
         AConnection.rollback();
         AConnection.setAutoCommit(true);
      }
      catch (SQLException ex) {
         logException(ex);
      }
      finally {
         transactionStarted = false;
         releaseConnection();
      }
   }

   // -------------------------------------------------------

   /**
    * Executes the supplied SQL Update returning the number of rows updated in the DBResult object. This method assumes auto commit is on.
    * 
    * @throws Exception
    */
   public DBResult executeSQLUpdate(String query) throws Exception {
      return executeSQLUpdate(query, true);
   }

   /**
    * Executes the supplied SQL Update returning the number of rows updated in the DBResult object.
    * 
    * @throws Exception
    */
   public DBResult executeSQLUpdate(String query, boolean auto_commit) throws Exception {
      int query_id = Math.abs(RANDOM_GENERATOR.nextInt());
      DBResult db_result = new DBResult();
      String connection_hashcode = null;
      if (query != null && query.length() > 0) {
         try {
            Connection conn = getConnection(query_id);
            connection_hashcode = "" + conn.hashCode();
            logDebugMessage(Level.INFO, "Got connection " + connection_hashcode + " for query id " + query_id + ".");

            try {
               db_result.startPerfTracking();
               db_result.setQuery(query);
               if (!transactionStarted()) {
                  conn.setAutoCommit(auto_commit);
               }
               logDebugMessage(Level.INFO, "Start execute update, query id " + query_id + "...");
               long time = System.currentTimeMillis();
               Statement stmt = conn.createStatement();
               int num_updated = stmt.executeUpdate(query);
               logDebugMessage(Level.INFO, "Fished executing query id " + query_id + ", execution time: " + (System.currentTimeMillis() - time));

               if (!transactionStarted() && !auto_commit) {
                  conn.commit();
               }
               db_result.setNumRowsUpdated(num_updated);
               SQLWarning warning = stmt.getWarnings();
               if (warning != null) {
                  while (warning != null) {
                     db_result.addWarning(warning.getMessage());
                     warning = warning.getNextWarning();
                  }
               }
               stmt.close();
               if (!transactionStarted() && !auto_commit) {
                  conn.setAutoCommit(true);
               }
            }
            catch (SQLException sql_ex) {
               if (!transactionStarted() && !auto_commit) {
                  try {
                     conn.rollback();
                     conn.setAutoCommit(true);

                     String errorMessage = "SQL Update/Insert error occurred and a rollback was completed.";
                     db_result.addError(new Exception(errorMessage));
                  }
                  catch (SQLException sql_ex2) {
                     db_result.addError(sql_ex2);
                  }
               }
               else if (transactionStarted()) {
                  // don't rollback here... allow transaction manager to rollback.
                  String errorMessage = "SQL Update/Insert error occurred during a transaction, no rollback was completed.";
                  db_result.addError(new Exception(errorMessage));
               }
               db_result.addError(sql_ex);
            }
            db_result.stopPerfTracking();
         }
         catch (Exception conn_ex) {
            db_result.addError(conn_ex);
         }
         finally {
            releaseConnection();
            logDebugMessage(Level.INFO, "Returned connection " + connection_hashcode + " for query id " + query_id + ".");
         }
      }
      else {
         String errorMessage = "SQL query was blank or otherwise invalid.";
         db_result.addError(new Exception(errorMessage));
      }

      addToDBLog(db_result, connection_hashcode, query_id);

      return db_result;
   }

   public DBResult executeSQLUpdate(String query, List<Object> parameterList) throws Exception {
      return executeSQLUpdate(query, parameterList, true);
   }

   public DBResult executeSQLUpdate(String query, List<Object> parameterList, boolean auto_commit) throws Exception {
      int query_id = Math.abs(RANDOM_GENERATOR.nextInt());
      DBResult db_result = new DBResult();
      String connection_hashcode = null;
      if (query != null && query.length() > 0) {
         try {
            Connection conn = getConnection(query_id);
            connection_hashcode = "" + conn.hashCode();
            logDebugMessage(Level.INFO, "Got connection " + connection_hashcode + " for query id " + query_id + ".");

            try {
               db_result.startPerfTracking();
               db_result.setQuery(query);
               if (!transactionStarted()) {
                  conn.setAutoCommit(auto_commit);
               }

               logDebugMessage(Level.INFO, "Start execute update, query id " + query_id + "...");
               long time = System.currentTimeMillis();

               PreparedStatement stmt = conn.prepareStatement(query);
               stmt = addParametersToStatement(parameterList, stmt);

               int num_updated = stmt.executeUpdate();
               logDebugMessage(Level.INFO, "Fished executing query id " + query_id + ", execution time: " + (System.currentTimeMillis() - time));

               if (!transactionStarted() && !auto_commit) {
                  conn.commit();
               }
               db_result.setNumRowsUpdated(num_updated);
               SQLWarning warning = stmt.getWarnings();
               if (warning != null) {
                  while (warning != null) {
                     db_result.addWarning(warning.getMessage());
                     warning = warning.getNextWarning();
                  }
               }
               stmt.close();
               if (!transactionStarted() && !auto_commit) {
                  conn.setAutoCommit(true);
               }
            }
            catch (SQLException sql_ex) {
               if (!transactionStarted() && !auto_commit) {
                  try {
                     conn.rollback();
                     conn.setAutoCommit(true);

                     String errorMessage = "SQL Update/Insert error occurred and a rollback was completed.";
                     db_result.addError(new Exception(errorMessage));
                  }
                  catch (SQLException sql_ex2) {
                     db_result.addError(sql_ex2);
                  }
               }
               else if (transactionStarted()) {
                  // don't rollback here... allow transaction manager to rollback.
                  String errorMessage = "SQL Update/Insert error occurred during a transaction, no rollback was completed.";
                  db_result.addError(new Exception(errorMessage));
               }
               db_result.addError(sql_ex);
            }
            db_result.stopPerfTracking();
         }
         catch (Exception conn_ex) {
            db_result.addError(conn_ex);
         }
         finally {
            releaseConnection();
            logDebugMessage(Level.INFO, "Returned connection " + connection_hashcode + " for query id " + query_id + ".");
         }
      }
      else {
         String errorMessage = "SQL query was blank or otherwise invalid.";
         db_result.addError(new Exception(errorMessage));
      }

      addToDBLog(db_result, connection_hashcode, query_id);

      return db_result;
   }

   /**
    * Executes the supplied SQL query returning results.
    */
   public DBResult executeSQLQuery(String query) {
      DBResult db_result = new DBResult();
      db_result.setQuery(query);
      executeSQLQuery(db_result);
      return (db_result);
   }

   @SuppressWarnings("resource")
   public ResultSetProcessor executeSQLQuery(ResultSetProcessor processor) {
      String query = processor.getQuery();
      List<Object> parameterList = processor.getParameterList();

      int query_id = Math.abs(RANDOM_GENERATOR.nextInt());

      String connection_hashcode = null;
      if (query != null && query.length() > 0) {
         try {
            Connection conn = getConnection(query_id);
            if (!transactionStarted()) {
               conn.setAutoCommit(false);
            }
            connection_hashcode = "" + conn.hashCode();
            logDebugMessage(Level.INFO, "Got connection " + connection_hashcode + " for query id " + query_id + ".");

            processor.startPerfTracking();
            PreparedStatement stmt = null;
            ResultSet rs = null;

            try {
               stmt = conn.prepareStatement(query);
               stmt.setFetchSize(1000);

               if (parameterList != null && !parameterList.isEmpty()) {
                  stmt = addParametersToStatement(parameterList, stmt);
               }

               logDebugMessage(Level.INFO, "Start execute query id " + query_id + "...");
               long time = System.currentTimeMillis();
               rs = stmt.executeQuery();
               logDebugMessage(Level.INFO, "Fished executing query id " + query_id + ", execution time: " + (System.currentTimeMillis() - time));
               SQLWarning warning = stmt.getWarnings();
               if (warning != null) {
                  while (warning != null) {
                     processor.addWarning(warning.getMessage());
                     warning = warning.getNextWarning();
                  }
               }

               processor.processResultSet(rs);
               // while(cstmt.getMoreResults() != false) {
               // db_result.addResultSet(cstmt.getResultSet());
               // }

            }
            catch (SQLException sql_ex) {
               processor.addError(sql_ex);
            }
            finally {
               if (rs != null) {
                  try {
                     rs.close();
                  }
                  catch (SQLException e) {
                     processor.addError(e);
                  }
               }

               if (stmt != null) {
                  try {
                     stmt.close();
                  }
                  catch (SQLException sql_ex) {
                     processor.addError(sql_ex);
                  }
               }
               processor.stopPerfTracking();
            }

            if (!transactionStarted()) {
               conn.setAutoCommit(true);
            }
            logDebugMessage(Level.INFO, "Returned connection " + connection_hashcode + " for query id " + query_id + ".");
         }
         catch (Exception conn_ex) {
            processor.addError(conn_ex);
         }
         finally {
            releaseConnection();
         }
      }
      else {
         String errorMessage = "SQL query was blank or otherwise invalid.";
         processor.addError(new Exception(errorMessage));
      }

      addToDBLog(processor, connection_hashcode, query_id);
      return (processor);
   }

   /**
    * Executes the supplied named database stored procedure returning outputs that match the out_types supplied. out_types should be a java.sql.Types or vendor specific type, like oracle.jdbc.driver.OracleTypes. The resulting object returned will contain statistical and error info as well as the outputs.
    * 
    * @throws Exception
    */
   public DBResult executeStoredProcedure(String procedureName, Object[] inParams) throws Exception {
      return executeStoredProcedure(procedureName, inParams, null);
   }

   /**
    * Executes the supplied named database stored procedure using the supplied in_params and returning outputs that match the out_types supplied. in_params is an array of Objects where each Object matches the input type that should be supplied to the database function. out_types should be a java.sql.Types or vendor specific type, like oracle.jdbc.driver.OracleTypes. The resulting object returned will contain statistical and error info as well as the outputs.
    * 
    * @throws Exception
    */
   public DBResult executeStoredProcedure(String procedureName, Object[] inParams, int[] outTypes) throws Exception {
      int query_id = Math.abs(RANDOM_GENERATOR.nextInt());

      int numParams = 0;
      if (inParams != null) {
         numParams += inParams.length;
      }

      if (outTypes != null) {
         numParams += outTypes.length;
      }

      DBResult result = new DBResult();
      result.startPerfTracking();
      Object connection_hashcode = null;

      try {
         Connection conn = getConnection(query_id);
         connection_hashcode = "" + conn.hashCode();
         logDebugMessage(Level.INFO, "Got connection " + connection_hashcode + " for query id " + query_id + ".");

         String statementTemplate = "";

         try {
            statementTemplate = createCallProcTemplate(procedureName, numParams);
            CallableStatement statement = conn.prepareCall(statementTemplate);
            setStatementParameters(statement, inParams, outTypes, false);
            result = executeCallableStatement(statement, inParams, outTypes, false, query_id);
         }
         catch (SQLException e) {
            result.addError(e);
         }

         result.setProcedureInfo(statementTemplate, inParams);
         logDebugMessage(Level.INFO, "Returned connection " + connection_hashcode + " for query id " + query_id + ".");
      }
      catch (Exception ex) {
         result.addError(ex);
      }
      finally {
         releaseConnection();
         result.stopPerfTracking();
      }

      addToDBLog(result, connection_hashcode, query_id);

      return result;
   }

   // Creates a stored procedure template for a CallableStatement object.
   private String createCallProcTemplate(String procedureName, int numParams) {
      // call procedure_name(?, ?, ...)
      StringBuilder call = new StringBuilder("call " + procedureName + "(");

      // construct the template for the call statement
      for (int i = 0; i < numParams; ++i) {
         call.append("?");
         if (i + 1 < numParams) {
            call.append(", ");
         }
      }

      return call.append(")").toString();
   }

   // Given the in_params and out_types, this method sets them in the CallableStatement object.
   private void setStatementParameters(CallableStatement statement, Object[] inParams, int[] outTypes, boolean isFunction) throws SQLException {
      int index = 1;
      if (isFunction) {
         index = 2;
      }

      if (inParams != null) {
         for (Object param : inParams) {
            if (param instanceof String) {
               statement.setString(index, (String) param);
            }
            else if (param instanceof Integer) {
               statement.setInt(index, (Integer) param);
            }
            else if (param instanceof Double) {
               statement.setDouble(index, (Double) param);
            }
            else if (param instanceof Float) {
               statement.setFloat(index, (Float) param);
            }
            else if (param instanceof Date) {
               statement.setDate(index, (Date) param);
            }
            else if (param instanceof Timestamp) {
               statement.setTimestamp(index, (Timestamp) param);
            }
            else if (param instanceof Long) {
               statement.setLong(index, (Long) param);
            }
            else if (param instanceof Short) {
               statement.setShort(index, (Short) param);
            }
            else if (param instanceof Byte) {
               statement.setByte(index, (Byte) param);
            }
            else if (param instanceof Time) {
               statement.setTime(index, (Time) param);
            }
            else if (param instanceof BigDecimal) {
               statement.setBigDecimal(index, (BigDecimal) param);
            }
            else {
               throw new IllegalArgumentException("Input parameter " + param.getClass().getName()
                     + " for stored procedure [proc_name] not supported for this method.");
            }
            ++index;
         }
      }

      // this is a function and the first index has to be registered as an out param.
      if (isFunction) {
         if (outTypes == null) {
            throw new IllegalArgumentException("No out parameter types were supplied when calling to execute a database function.");
         }

         statement.registerOutParameter(1, outTypes[0]);
         for (int type : outTypes) {
            statement.registerOutParameter(index, type);
            ++index;
         }
      }
      else {
         if (outTypes != null) {
            for (int type : outTypes) {
               statement.registerOutParameter(index, type);
               ++index;
            }
         }
      }
   }

   // Executes the supplied CallableStatement object.
   private DBResult executeCallableStatement(CallableStatement statement, Object[] inParams, int[] outTypes, boolean isFunction, int query_id) throws Exception {
      DBResult result = new DBResult();
      result.startPerfTracking();

      ResultSet rs = null;
      try {
         logDebugMessage(Level.INFO, "Start execute of callable statement id " + query_id + "...");
         long time = System.currentTimeMillis();
         boolean result_set_exists = statement.execute();
         logDebugMessage(Level.INFO, "Fished executing callable statement id " + query_id + ", execution time: " + (System.currentTimeMillis() - time));

         SQLWarning warning = statement.getWarnings();

         if (warning != null) {
            while (warning != null) {
               result.addWarning(warning.getMessage());
               warning = warning.getNextWarning();
            }
         }

         if (result_set_exists) {
            rs = statement.getResultSet();
            result.startResultSetPerfTracking();
            result.processResultSet(rs);
            result.stopResultSetPerfTracking();
         }

         if (outTypes != null) {
            int num_inparams = 0;
            if (inParams != null) {
               num_inparams = inParams.length;
            }

            int outparam_index = num_inparams + 1;

            int index = 0;
            if (isFunction) {
               addOutParamValueTo(result, statement, outTypes[0], 1);
               index = 1; // skip the first out_types....
               outparam_index = num_inparams + 2;
            }

            for (int i = index; i < outTypes.length; ++i, ++outparam_index) {
               addOutParamValueTo(result, statement, outTypes[i], outparam_index);
            }
         }
      }
      catch (SQLException e) {
         result.addError(e);
      }
      finally {
         if (rs != null) {
            try {
               rs.close();
            }
            catch (SQLException e) {
               result.addError(e);
            }
         }

         try {
            statement.close();
         }
         catch (SQLException e) {
            result.addError(e);
         }
         result.stopPerfTracking();
      }

      return result;
   }

   // Adds an out parameter value from the CallableStatement object to the DBResult object.
   // Adds an out parameter value from the CallableStatement object to the DBResult object.
   // Adds an out parameter value from the CallableStatement object to the DBResult object.
   private void addOutParamValueTo(DBResult result, CallableStatement statement, int typeId, int index) throws SQLException {
      Object outParam = null;
      String columnName = "COLUMN_" + index; // Default column name...
      int sqlType = typeId;
      Class<?> cls = null;
      int columnIndex = index;

      switch (typeId) {
         case Types.ARRAY:
            outParam = statement.getArray(index);
            cls = Array.class;
            break;
         case Types.BIGINT:
            outParam = Long.valueOf(statement.getLong(index));
            cls = Long.class;
            break;
         case Types.BLOB:
            outParam = statement.getBlob(index);
            cls = Blob.class;
            break;
         case Types.BIT:
            outParam = Boolean.valueOf(statement.getBoolean(index));
            cls = Boolean.class;
            break;
         case Types.NUMERIC:
            outParam = statement.getBigDecimal(index);
            cls = BigDecimal.class;
            break;
         case Types.TINYINT:
            outParam = Byte.valueOf(statement.getByte(index));
            cls = Byte.class;
            break;
         case Types.SMALLINT:
            outParam = Short.valueOf(statement.getShort(index));
            cls = Short.class;
            break;
         case Types.CHAR:
         case Types.VARCHAR:
         case Types.LONGVARCHAR:
            outParam = statement.getString(index);
            cls = String.class;
            break;
         case Types.CLOB:
            outParam = statement.getClob(index);
            cls = Clob.class;
            break;
         case Types.DATE:
            outParam = statement.getDate(index);
            cls = Date.class;
            break;
         case Types.DOUBLE:
            outParam = Double.valueOf(statement.getDouble(index));
            cls = Double.class;
            break;
         case Types.FLOAT:
            outParam = Float.valueOf(statement.getFloat(index));
            cls = Float.class;
            break;
         case Types.INTEGER:
            outParam = Integer.valueOf(statement.getInt(index));
            cls = Integer.class;
            break;
         case Types.TIME:
            outParam = statement.getTime(index);
            cls = Time.class;
            break;
         case Types.TIMESTAMP:
            outParam = statement.getTimestamp(index);
            cls = Timestamp.class;
            break;
         default:
            logMessage(Level.SEVERE, "Unknown parameter type [" + typeId + "]");
            break;
      }
      result.setOutParameter(outParam, columnName, sqlType, cls, columnIndex);
   }

   private PreparedStatement addParametersToStatement(List<Object> parameterList, PreparedStatement stmt) throws Exception {
      int numParams = parameterList.size();
      for (int i = 0; i < numParams; i++) {
         Object parameter = parameterList.get(i);
         Class<?> objClass = parameter.getClass();

         int paramIndex = i + 1;

         if (objClass.equals(String.class)) {
            stmt.setString(paramIndex, (String) parameter);
         }
         else if (objClass.equals(Integer.class)) {
            stmt.setInt(paramIndex, (Integer) parameter);
         }
         else if (objClass.equals(Double.class)) {
            stmt.setDouble(paramIndex, (Double) parameter);
         }
         else if (objClass.equals(Long.class)) {
            stmt.setLong(paramIndex, (Long) parameter);
         }
         else if (objClass.equals(Float.class)) {
            stmt.setFloat(paramIndex, (Float) parameter);
         }
         else if (objClass.equals(Boolean.class)) {
            stmt.setBoolean(paramIndex, (Boolean) parameter);
         }
         else if (objClass.equals(java.util.Date.class)) {
            java.util.Date utilDate = (java.util.Date) parameter;

            //       java.sql.Date dateValue = new java.sql.Date(utilDate.getTime());
            //       stmt.setDate(paramIndex, dateValue);

            java.sql.Timestamp dateValue = new java.sql.Timestamp(utilDate.getTime());
            stmt.setTimestamp(paramIndex, dateValue);
         }
         else if (objClass.equals(Character.class)) {
            String stringValue = ((Character) parameter).toString();
            stmt.setString(paramIndex, stringValue);
         }
         else if (objClass.equals(Short.class)) {
            stmt.setShort(paramIndex, (Short) parameter);
         }
         else if (objClass.equals(Byte.class)) {
            stmt.setByte(paramIndex, (Byte) parameter);
         }
         else if (objClass.equals(BigInteger.class)) {
            long longValue = ((BigInteger) parameter).longValue();
            stmt.setLong(paramIndex, new Long(longValue));
         }
         else if (objClass.equals(BigDecimal.class)) {
            stmt.setBigDecimal(paramIndex, (BigDecimal) parameter);
         }
         else if (objClass.equals(Array.class)) {
            stmt.setArray(paramIndex, (Array) parameter);
         }
         else if (objClass.equals(Blob.class)) {
            stmt.setBlob(paramIndex, (Blob) parameter);
         }
         else {
            throw new Exception("Object class of " + objClass.getName() + " is unsupported.");
         }
      }
      return stmt;
   }

   // ----------------------------------------------------------------------------------

   @Override
   public String toString() {
      return getID();
   }

   void setConnectionCommand(ConnectionCommand command) {
      ConnectionCommand = command;
   }

   // -------------------------------- Logging Methods -------------------------------- //

   public void logMessages(boolean doit) {
      LogMessages = doit;
   }

   public void logQueries(boolean doit) {
      LogQueryInfo = doit;
   }

   String getJDBCConnectionStatus() {
      String conn_status = "CONN_OK";
      if (AConnection == null) {
         conn_status = "CONN_NULL";
      }
      else {
         try {
            if (AConnection.isClosed()) {
               conn_status = "CONN_CLOSED";
            }
         }
         catch (SQLException sqlex) {
            conn_status = "CONN_ERROR: " + ExceptionMessageHandler.formatExceptionMessage(sqlex);
         }
      }
      return (conn_status);
   }

   String getTimerStatus() {
      String timer_status = "TIMER_STOPPED";
      if (ATimer != null && ATimer.isRunning()) {
         timer_status = "TIMER_RUNNING";
      }
      return (timer_status);
   }

   String getUseStatus() {
      String in_use = "IN_USE";
      if (!inUse()) {
         in_use = "NOT_IN_USE";
      }
      return (in_use);
   }

   String getActiveStatus() {
      String active = "ACTIVE";
      if (!isActive()) {
         active = "INACTIVE";
      }
      return (active);
   }

   String getInstanceID() {
      return (this.ConnectionId);
   }

   private void addToDBLog(DBResult result, Object connectionKey, int query_id) throws Exception {
      String message;
      if (connectionKey == null) {
         connectionKey = "NO_CONNECTION_KEY";
      }

      if (LogQueryInfo && !result.hasErrors()) {
         message = "CONNECTION_KEY: " + connectionKey + " QUERY_ID: " + query_id + " " + result.getLogMessage().replaceAll("\\n", " ");
         logMessage(Level.INFO, message);
      }
      else if (result.hasErrors()) {
         message = result.getErrorMessage().replaceAll("\\n", " ");
         logMessage(Level.INFO, "CONNECTION_KEY: " + connectionKey + " QUERY_ID: " + query_id + " ERROR: " + message);
      }
   }

   private void addToDBLog(ResultSetProcessor processor, Object connectionKey, int query_id) {
      try {
         String message;
         if (connectionKey == null) {
            connectionKey = "NO_CONNECTION_KEY";
         }

         if (LogQueryInfo && !processor.hasErrors()) {
            message = "CONNECTION_KEY: " + connectionKey + " QUERY_ID: " + query_id + " " + processor.getLogMessage().replaceAll("\\n", " ");
            logMessage(Level.INFO, message);
         }
         else if (processor.hasErrors()) {
            message = processor.getErrorMessage().replaceAll("\\n", " ");
            logMessage(Level.INFO, "CONNECTION_KEY: " + connectionKey + " QUERY_ID: " + query_id + " ERROR: " + message);
         }
      }
      catch (Exception ex) {
         Logger.log(getClass(), Level.SEVERE, null, ex);
      }
   }

   /**
    * This method logs the message and Exception, by default, to the default implementation of the updateStatusMessage() as well as prints the exception to System.err.
    */
   private void logException(Throwable ex) {
      if (LogMessages || debugOn()) {
         try {
            Logger.log("db", getClass(), Level.SEVERE, getID(), ex);
         }
         catch (Exception exc) {
            Logger.log(getClass(), Level.SEVERE, getID(), ex);
         }
      }
   }

   /**
    * This method logs the message, by default, Standard out.
    */
   private void logMessage(Level level, String message) {
      if (LogMessages || debugOn()) {
         String statement = getLoggingStatement(message);
         Logger.log("db", getClass(), level, statement);
      }
   }

   /**
    * Outputs a "standard" formatted debugging statement that can be used in a spreadsheet.
    * 
    * @throws Exception
    */
   private void logDebugMessage(Level level, String message) throws Exception {
      logMessage(level, message);
   }

   private String getLoggingStatement(String message) {
      String conn_status = getJDBCConnectionStatus();
      String timer_status = getTimerStatus();
      String in_use = getUseStatus();
      String active = getActiveStatus();
      String instance_id = getInstanceID();

      String statement = instance_id + "|" + conn_status + "|" + timer_status + "|" + in_use + "|" + active + "|" + message;
      return (statement);
   }

   // Writes additional messages to the db log file.
   private boolean debugOn() {
      boolean debug = false;
      try {
         ConfigInfo info = ConfigInfo.getInstance();
         String property = "log.db.debug";

         if (info.hasProperty(property)) {
            debug = info.getPropertyAsBoolean(property);
         }
      }
      catch (FileNotFoundException e) {
         logException(e);
      }
      return (debug);
   }

   String getID() {
      return "DBConnection" + InstanceNum;
   }

   boolean inUse() {
      return (InUse);
   }

   boolean isClosed() throws Exception {
      boolean closed = AConnection == null ? true : false;
      if (!closed) {
         closed = AConnection.isClosed();
      }
      return (closed);
   }

   // ------------------------------- Implements TimerListener -------------------------------

   @Override
   public void timeExpired() {
      try {
         if (inUse()) {
            // This can only happen if the ATimer is being used to time
            // the connection's activity...
            ATimer.stop();
            ATimer.start();
            logDebugMessage(Level.INFO, "ActivityTimer for connection, " + AConnection.hashCode() + ", expired; restarting timer.");
         }
         else {
            // This can only happen if the conn_info.ATimer is being used to time
            // the connection's inactivity...
            try {
               AConnection.close();
               if (AConnection.isClosed()) {
                  AConnection = null;
               }
            }
            catch (SQLException ex) {
               logMessage(Level.SEVERE, "Error while trying to close the connection for " + AConnection.hashCode() + ".");
               logException(ex);
            }
            stopTimer();
            logDebugMessage(Level.INFO, "ActivityTimer for connection " + AConnection.hashCode() + " expired; closing connection.");
         }
      }
      catch (Exception ex) {}
   }
}
