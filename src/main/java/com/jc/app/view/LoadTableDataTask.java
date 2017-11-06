package com.jc.app.view;

import java.util.List;
import java.util.Map;

import com.jc.command.task.Task;
import com.jc.db.dao.FilterInfo;

public class LoadTableDataTask extends Task {

   private static final long serialVersionUID = -8297467395927509053L;

   private final static String TASKPREFIX = "LoadDataTask";

   private final AppLazyDataModel<?> dataModel;
   private final int first;
   private final int page_size;
   private final Map<String, String> sortOn;
   private final FilterInfo filterOn;

   private double amountCompleted;
   private String errorMessage;

   public LoadTableDataTask(AppLazyDataModel<?> dataModel, int first, int page_size, Map<String, String> sortOn, FilterInfo filterOn) {

      this.dataModel = dataModel;
      this.first = first;
      this.page_size = page_size;
      this.sortOn = sortOn;
      this.filterOn = filterOn;

      amountCompleted = 0;

      setIDPrefix(TASKPREFIX);
   }

   @Override
   public void doTask() throws Exception {
      updateStatusMessage("Starting task(" + getID() + ")."); // For job logging...

      amountCompleted = 0.5d;

      List<?> tabledata = dataModel.getTableData(first, page_size, sortOn, filterOn);
      tabledata = dataModel.processLoadedData(tabledata);
      setTaskResult(tabledata);

      amountCompleted = 1.0d;
      updateStatusMessage("Completed task."); // For job logging...
   }

   @Override
   protected void updateStatus() {}

   @Override
   protected void updateStatusMessage(String message) {

      // For job logging...
      if (message.contains(INTERRUPTED_TASK_STATUS_MSG) && amountCompleted > 0) {
         String pattern = INTERRUPTED_TASK_STATUS_MSG + getID() + " - ";
         message = "Task stopped: " + message.replace(pattern, "");
      }
      else if (message.contains(ERRORED_TASK_STATUS_MSG)) {
         String pattern = ERRORED_TASK_STATUS_MSG + getID() + " - ";
         errorMessage = message.replace(pattern, "");
         message = "Task errored: " + errorMessage;
      }

      super.updateStatusMessage(message);
   }

   String getErrorMessage() {
      return errorMessage;
   }
}
