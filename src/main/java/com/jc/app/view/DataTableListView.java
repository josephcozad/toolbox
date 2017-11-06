package com.jc.app.view;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import com.jc.util.FileSystem;

public abstract class DataTableListView implements Serializable {

   private static final long serialVersionUID = -2077142926838513559L;

   private AppLazyDataModel<?> dataModel;

   private boolean editMode;
   private boolean tableDisplayed = true; // default value

   public abstract void init();

   // This method is used by the xhtml to call on the managed bean before the message 
   // component is rendered, so that any messages that occur PostConstruct can be displayed. 
   public void noOp() {}

   public LazyDataModel<?> getRows() {
      return (dataModel);
   }

   public Object getSelectedRow() {
      Object selectedRow = null;
      try {
         if (dataModel != null) {
            selectedRow = dataModel.getSelectedRow();
         }
      }
      catch (Exception ex) {
         JSFUtils.handleException(getClass(), ex);
      }
      return selectedRow;
   }

   public boolean isRowSelected() {
      return (getSelectedRow() != null);
   }

   public void setSelectedRow(Object selectedRow) {
      if (selectedRow != null) {
         List<Object> selected_rows = new ArrayList<>();
         selected_rows.add(selectedRow);
         dataModel.setSelectedRows(selected_rows);
      }
   }

   public List<?> getSelectedRowsThisPage() {
      return dataModel.getSelectedRowsThisPage();
   }

   public void setSelectedRowsThisPage(List<?> selected_rows) {
      dataModel.setSelectedRows(selected_rows);
   }

   public List<?> getSelectedRows() {
      return dataModel.getSelectedRows();
   }

   public void selectAllRows() {
      dataModel.selectAllRows();
   }

   public void deselectAllRows() {
      if (dataModel != null) {
         dataModel.deselectAllRows();
      }
   }

   public boolean isDataListEmpty() {
      boolean emptyDataList = false;
      if (!dataModel.hasRowsThisPage()) {
         emptyDataList = true;
      }
      return emptyDataList;
   }

   public boolean isAddNewDisabled() {
      return false;
   }

   public void onRowDblClicked(final SelectEvent event) {
      try {
         // System.out.println("START onRowDblClicked[--]");
         // when user double clicks a row, only that row can be selected.
         dataModel.deselectAllRows();

         Object rowData = event.getObject();
         setSelectedRow(rowData);
         setEditMode(false);

         rowDoubleClicked();
         // System.out.println("END onRowDblClicked[--]");
      }
      catch (Exception ex) {
         JSFUtils.handleException(getClass(), ex);
      }
   }

   public void onRowUnselected(final UnselectEvent event) {
      // System.out.println("START onRowUnselected[--]");
      Object rowData = event.getObject();
      dataModel.rowUnselected(rowData);
      // System.out.println("END onRowUnselected[--]");
   }

   public void onRowSelected(final SelectEvent event) {
      //      // System.out.println("START onRowSelected[--]");
      //      Object rowData = event.getObject();
      //      setSelectedRow(rowData);
      //      // System.out.println("END onRowSelected[--]");
   }

   public void exportToExcel(String fileName) {
      List<ColumnModel> columnInfoList = getColumns();
      exportToExcel(fileName, columnInfoList);
   }

   public void exportToExcel(String fileName, List<ColumnModel> columnInfoList) {
      try {

         List<Object> selectedRows = (List<Object>) getSelectedRows();
         if (selectedRows != null && !selectedRows.isEmpty()) {

            StringBuilder worksheetContent = new StringBuilder();
            StringBuilder headerContent = new StringBuilder();

            for (int i = 0; i < selectedRows.size(); i++) {
               Object row = selectedRows.get(i);
               StringBuilder rowContent = new StringBuilder();
               for (int j = 0; j < columnInfoList.size(); j++) {
                  ColumnModel columnInfo = columnInfoList.get(j);
                  String property = columnInfo.getProperty();
                  if (i == 0) { // first one, save to headerContent
                     String header = columnInfo.getHeader();
                     headerContent.append(header);
                     if (j + 1 < columnInfoList.size()) {
                        headerContent.append(",");
                     }
                  }

                  Object value = getValueForObjectProperty(row, property);
                  if (value == null) {
                     value = "";
                  }

                  rowContent.append("\"" + value + "\"");
                  if (j + 1 < columnInfoList.size()) {
                     rowContent.append(",");
                  }
               }

               // Add header content...
               if (headerContent != null && headerContent.length() > 0) {
                  worksheetContent.append(headerContent.toString() + FileSystem.NEWLINE);
                  headerContent.delete(0, headerContent.length());
                  headerContent = null;
               }

               // Add row content...
               worksheetContent.append(rowContent.toString() + FileSystem.NEWLINE);
            }

            fileName += ".csv";
            byte[] contentBytes = worksheetContent.toString().getBytes();
            JSFUtils.sendBackContentAsFile(fileName, contentBytes);

            deselectAllRows();
         }
         else {
            // No rows selected...
            JSFUtils.addUserMessage("No rows selected.", Level.WARNING);
         }
      }
      catch (Exception ex) {
         JSFUtils.handleException(getClass(), ex);
      }
   }

   // Start edit row action from UI.
   public void editRow() {
      editMode = true;
   }

   // Tells UI if editing a row.
   public boolean isEditMode() {
      return editMode;
   }

   // Returns the column metadata used to display the table.
   public List<ColumnModel> getColumns() {
      return getDataModel().getColumnModel();
   }

   public boolean renderShowTableToggleButton() {
      /* When tabled displayed don't render show table toggle button */
      boolean value = true;
      if (tableDisplayed) {
         value = false; // then don't render the show button...
      }
      return value;
   }

   public boolean renderHideTableToggleButton() {
      /* When tabled displayed don't render show table toggle button */
      boolean value = true;
      if (!tableDisplayed) {
         value = false; // then don't render the hide button...
      }
      return value;
   }

   public boolean displayTable() {
      return tableDisplayed;
   }

   public void setTableDisplayed(boolean tableDisplayed) {
      this.tableDisplayed = tableDisplayed;
   }

   public abstract void reset();

   // Start edit of a new row from UI.
   public abstract void createNewRow();

   // Tells UI if editing a new row.
   public abstract boolean isNewRow();

   // Tells UI if user has permission to edit rows.
   public abstract boolean isEditAllowed();

   public abstract String processRowEdit();

   public abstract String deleteRow();

   // Returns the PrimeFaces 'widgetVar' string for the data table.
   public abstract String getPFDataTableId();

   // Returns the id string for the data table.
   public abstract String getDataTableId();

   protected void setDataModel(AppLazyDataModel<?> dataModel) {
      this.dataModel = dataModel;

      // Run a test load of the dataModel to make sure it won't error on the first run,
      // and if it does, displays/logs the error message.
      List<SortMeta> sortMetadata = new ArrayList<>();
      Map<String, Object> filters = new HashMap<>();
      dataModel.load(0, 10, sortMetadata, filters);
   }

   protected AppLazyDataModel<?> getDataModel() {
      return dataModel;
   }

   protected void setEditMode(boolean value) {
      editMode = value;
   }

   // Convenience method to be overridden by inherited classes to initialize
   // state when a table's row is double clicked and about to be displayed
   // to the user.
   protected void rowDoubleClicked() throws Exception {}

   // -------------------------------------------------------------------------

   private Object getValueForObjectProperty(Object obj, String objProp) throws Exception {
      Object value = null;

      objProp = objProp.toLowerCase(); // be sure this is lower case

      Class<?> aClass = obj.getClass();
      Method[] methods = aClass.getMethods();
      boolean found = false;
      for (int i = 0; i < methods.length && !found; i++) {
         Method method = methods[i];
         if (isGetter(method)) {
            String name = method.getName();
            String property = name.replace("get", "").toLowerCase();
            if (property.equalsIgnoreCase(objProp)) {
               value = method.invoke(obj);
               found = true;
            }
         }
      }

      return value;
   }

   private void setValueForObjectProperty(Object obj, String objProp, Object value) throws Exception {
      objProp = objProp.toLowerCase(); // be sure this is lower case

      Class<?> aClass = obj.getClass();
      Method[] methods = aClass.getMethods();
      boolean found = false;
      for (int i = 0; i < methods.length && !found; i++) {
         Method method = methods[i];
         if (isSetter(method)) {
            String name = method.getName();
            String property = name.replace("set", "").toLowerCase();
            if (property.equalsIgnoreCase(objProp)) {
               method.invoke(obj, value);
               found = true;
            }
         }
      }
   }

   private static boolean isGetter(Method method) {
      if (!method.getName().startsWith("get")) {
         return false;
      }
      if (method.getParameterTypes().length != 0) {
         return false;
      }
      if (void.class.equals(method.getReturnType())) {
         return false;
      }
      return true;
   }

   private static boolean isSetter(Method method) {
      if (!method.getName().startsWith("set")) {
         return false;
      }
      if (method.getParameterTypes().length != 1) {
         return false;
      }
      return true;
   }

}
