package com.jc.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jc.command.task.Task;
import com.jc.log.ExceptionMessageHandler;

public abstract class LoadDataTask extends Task {

   private static final long serialVersionUID = -5622568162536151571L;

   private String ErrorMessage;

   static {
      int max_processes = 10;
      String classname = LoadDataTask.class.getName();
      setMaxActiveTaskLimit(classname, max_processes);
   }

   public LoadDataTask() {
      String classname = getClass().getName();
      classname = classname.substring(classname.lastIndexOf(".") + 1);
      setIDPrefix(classname + "_");
   }

   protected abstract String getQuery();

   protected abstract Object processResult(DBResult result) throws Exception;

   protected String getDatasource() {
      return (null);
   }

   protected boolean byStoredProcedure() {
      return (false);
   }

   protected Object[] getStoredProcedureInParams() {
      return (null);
   }

   protected String getStoredProcedureName() {
      return (null);
   }

   @Override
   public void doTask() throws Exception {
      try {
         DBConnection conn = null;
         String datasource = getDatasource();
         if (datasource == null) { // Use default datasource...
            conn = DBConnection.getInstance();
         }
         else {
            conn = DBConnection.getInstance(datasource);
         }

         DBResult result = null;
         if (byStoredProcedure()) {
            // Object[] in_params = getStoredProcedureInParams();
            // int[] out_params = {
            // DBConnection.ORACLE_TYPE_CURSOR
            // };
            // String stored_proc_name = getStoredProcedureName();
            // result = conn.executeStoredProcedure(stored_proc_name, in_params, out_params);
         }
         else {
            result = conn.executeSQLQuery(getQuery());
         }

         if (result.hasErrors()) {
            throw new SQLException(result.getErrorMessage());
         }

         Object task_result = null;
         if (result.getNumRowsRetrieved() > 0) {
            task_result = processResult(result);
         }
         else {
            // no rows retrieved...
         }

         setTaskResult(task_result);
      }
      catch (Exception ex) {
         ErrorMessage = ExceptionMessageHandler.formatExceptionMessage(ex);
         throw ex;
      }
   }

   @Override
   protected void updateStatus() {}

   public String getErrorMessage() {
      return ErrorMessage;
   }

   // Combines all the List object results from each task into one master List.
   public static <T> List<T> loadDataToList(List<? extends LoadDataTask> tasks) throws Exception {
      loadData(tasks);

      List<T> data = new ArrayList<T>();
      for (LoadDataTask task : tasks) {
         if (task.isErrored()) {
            throw new Exception(task.getID() + " errored while trying to load data to a List. " + task.getErrorMessage());
         }
         List<T> task_result = (List<T>) task.getResult();
         if (task_result != null) {
            data.addAll(task_result);
         }
      }
      return data;
   }

   // Combines all the Map object results from each task into one master Map. All
   // keys must be unique.
   public static <K, V> Map<K, V> loadDataToMap(List<? extends LoadDataTask> tasks) throws Exception {
      loadData(tasks);

      Map<K, V> data = new HashMap<K, V>();

      for (LoadDataTask task : tasks) {
         if (task.isErrored()) {
            throw new Exception(task.getID() + " errored while trying to load data to a Map. " + task.getErrorMessage());
         }
         Map<K, V> task_result = (Map<K, V>) task.getResult();
         if (task_result != null) {
            data.putAll(task_result);
         }
      }

      return data;
   }

   private static void loadData(List<? extends LoadDataTask> tasks) {
      for (LoadDataTask task : tasks) {
         task.start();
      }

      while (tasksRunning(tasks)) {
         // wait until all tasks are done or have errored
      }
   }

   private static boolean tasksRunning(List<? extends LoadDataTask> tasks) {
      for (LoadDataTask task : tasks) {
         if (!(task.isDone() || task.isErrored())) {
            return true;
         }
      }

      return false;
   }
}
