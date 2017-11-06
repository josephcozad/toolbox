package com.jc.app.view;

public class TableDataLoadException extends Exception {

   private static final long serialVersionUID = -4804514439010326114L;

   private final String loadErrorMsg;
   private final String countErrorMsg;

   public TableDataLoadException(String loadErrorMsg, String countErrorMsg) {
      super();
      this.loadErrorMsg = loadErrorMsg;
      this.countErrorMsg = countErrorMsg;
   }

   public boolean hasTableDataCountError() {
      return countErrorMsg != null && !countErrorMsg.isEmpty();
   }

   public String getTableDataCountError() {
      return countErrorMsg;
   }

   public boolean hasTableDataLoadError() {
      return loadErrorMsg != null && !loadErrorMsg.isEmpty();
   }

   public String getTableDataLoadError() {
      return loadErrorMsg;
   }
}
