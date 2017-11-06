package com.jc.app.view;

import java.util.ArrayList;
import java.util.List;

public abstract class ColumnOptionsManager extends SelectOptionsManager {

   private static final long serialVersionUID = 7363106137330719492L;

   public List<ColumnModel> getColumnInfo() {
      List<ColumnModel> columnInfoList = new ArrayList<ColumnModel>();
      List<SelectOption> selectedOptions = getSelectedOptions();
      for (SelectOption option : selectedOptions) {
         ColumnModel columnModel = (ColumnModel) option.getObject();
         columnInfoList.add(columnModel);
      }
      return columnInfoList;
   }

   public void reset() {
      super.clearSelectedOptions();

      List<SelectOption> columnOptionsList = getOptions();
      List<SelectOption> selectedOptions = new ArrayList<SelectOption>();
      selectedOptions.addAll(columnOptionsList); // all are selected by default...
      setSelectedOptions(selectedOptions);
   }

   public class ExcelColumnOption extends SelectOption {

      private static final long serialVersionUID = -1547855595706264161L;

      public ExcelColumnOption(String columnHeader, String property) {
         super(columnHeader, property);
         setObject(new ColumnModel(columnHeader, property, ""));
      }
   }
}
