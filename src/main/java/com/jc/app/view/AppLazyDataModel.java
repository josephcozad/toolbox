package com.jc.app.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;

import com.jc.command.task.JobRunner;
import com.jc.command.task.Task;
import com.jc.db.dao.FilterInfo;
import com.jc.db.dao.FilterMethod;
import com.jc.log.Logger;

/*
 * This can be in one of two states, either everything is selected by default, or nothing 
 * is selected by default. The user chooses rows form the displayed table and depending on 
 * the state of this class, those chosen rows are either "unselected" rows (in the "nothing
 * is selected by default" state), or "selected" (in the "everything is selected by default
 * state). The initial default state of this class is always "nothing is selected".
 */

public abstract class AppLazyDataModel<T> extends LazyDataModel<T> {

   private static final long serialVersionUID = -1844578274000240044L;

   protected static final String ASCENDING = "asc";
   protected static final String DESCENDING = "desc";

   private final static String PRIMEFACES_GLOBAL_FIELD_FILTER_KEY = "globalFilter"; // Reserved PrimeFaces keyword.

   private String sortField;
   private SortOrder sortOrder;
   private Map<String, Object> filters;

   private boolean everythingSelected;
   private boolean loadingAllRowData;

   // Commented out as uneccessary -- JC 01/12/2017
   //   private boolean marker = false;
   //   private List<T> savedChosenRows = null;

   private final List<T> chosenRows; // these are the rows that the user has specifically chosen as "selected" or "unselected" depending on the value of 'everythingSelected'.
   private List<T> rowsThisPage; // these are the rows on the page display, regardless of selection or deselection.

   public AppLazyDataModel() {
      super();
      chosenRows = new ArrayList<>();
   }

   public abstract List<ColumnModel> getColumnModel();

   public abstract List<T> getTableData(int first, int page_size, Map<String, String> sortOn, FilterInfo filterOn) throws Exception;

   public abstract Long getDataCount(FilterInfo filterOn) throws Exception;

   public List<?> processLoadedData(List<?> tabledata) throws Exception {
      return tabledata;
   }

   @Override
   public List<T> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {

      // Save this info in case we do a data export of all rows in the datatable...
      this.sortField = sortField;
      this.sortOrder = sortOrder;
      this.filters = filters;

      SortMeta sortInfo = new SortMeta();
      sortInfo.setSortField(sortField);
      sortInfo.setSortOrder(sortOrder);

      List<SortMeta> sortMetadata = new ArrayList<>();
      sortMetadata.add(sortInfo);

      List<T> rowsData = load(first, pageSize, sortMetadata, filters);

      return (rowsData);
   }

   public void handleTableDataLoadException(String serviceId, TableDataLoadException tdlex) {
      StringBuilder loggableMessage = new StringBuilder();
      if (tdlex.hasTableDataLoadError()) {
         loggableMessage.append(tdlex.getTableDataLoadError());
      }

      if (loggableMessage.length() > 0 && tdlex.hasTableDataCountError()) {
         loggableMessage.append("; ");
      }

      if (tdlex.hasTableDataCountError()) {
         loggableMessage.append(tdlex.getTableDataCountError());
      }

      JSFUtils.addMessage(getClass(), Level.SEVERE, loggableMessage.toString(), "Table data could not be loaded.");
   }

   /*
    * This method creates a List of rowData that is a subset of all selected
    * rows (returned by getSelectedRows()) based on what's on rowsThisPage. 
    * This is generally called when filling the table for the displayed page.
    */
   public List<T> getSelectedRowsThisPage() {
      List<T> selectedRows = new ArrayList<>();
      if (!everythingSelected) { // "nothing selected by default"
         // use chosenRows to create a List of "selected rows" based on 
         // matches in rowsThisPage.
         if (!chosenRows.isEmpty()) { // only bother with this is there's something to work with.
            for (Object rowData : rowsThisPage) {
               if (chosenRows.contains(rowData)) {
                  selectedRows.add((T) rowData);
               }
            }
         }
      }
      else { // "everything selected by default"
         // create a duplicate List of "selected rows" based on the 
         // contents of rowsThisPage, and filter out matches based 
         // on chosenRows.
         selectedRows.addAll(rowsThisPage); // everything on page is selected.
         if (!chosenRows.isEmpty()) { // only bother with this is there's something to work with.
            for (Object rowData : rowsThisPage) { // filter out user chosen stuff as unselected.
               if (chosenRows.contains(rowData)) {
                  selectedRows.remove(rowData);
               }
            }
         }
      }
      return selectedRows;
   }

   /*
    * This method returns all the rowData selected across all pages in the displayed table.
    */
   public List<T> getSelectedRows() {
      List<T> selectedRows = new ArrayList<>();

      if (!everythingSelected) { // "nothing selected by default"
         // send back only all of the chosen rows...
         selectedRows.addAll(chosenRows);
      }
      else { // "everything selected by default"
         // send all rows, minus all the chosen rows (those chosen as unselected by user) ....
         loadingAllRowData = true;
         int totalNumDataTableRows = getRowCount();
         selectedRows = load(0, totalNumDataTableRows, sortField, sortOrder, filters);
         loadingAllRowData = false;
         if (!chosenRows.isEmpty()) {
            for (Object rowData : chosenRows) {
               if (selectedRows.contains(rowData)) {
                  selectedRows.remove(rowData);
               }
            }
         }
      }

      return selectedRows;
   }

   public void setSelectedRows(List<?> selectedRows) {
      // only add to chosenRows if in "nothing selected by default".
      if (!everythingSelected) {
         if (selectedRows != null && !selectedRows.isEmpty()) {
            if (!chosenRows.isEmpty()) {
               chosenRows.clear();
            }

            for (Object rowData : selectedRows) {
               if (!chosenRows.contains(rowData)) { // if it hasn't been chosen yet, add it.
                  chosenRows.add((T) rowData);
               }
            }
         }
         // else ....
      }
      else { // everything is selected...
         int totalRows = getRowCount();
         int chosenRowSize = chosenRows.size();
         int selectedRowSize = selectedRows.size();

         if (selectedRowSize == 1) {
            // Commented out as uneccessary -- JC 01/12/2017
            //            if ((chosenRowSize + selectedRowSize) == (totalRows - 1)) {
            //               marker = true;
            //               savedChosenRows = new ArrayList<T>();
            //               savedChosenRows.addAll(chosenRows); // save before clearing the choseRows with deselectAllRows();
            //
            //               deselectAllRows();
            //               if (!chosenRows.isEmpty()) {
            //                  chosenRows.clear();
            //               }
            //
            //               for (Object rowData : selectedRows) {
            //                  if (!chosenRows.contains(rowData)) { // if it hasn't been chosen yet, add it.
            //                     chosenRows.add((T) rowData);
            //                  }
            //               }
            //            }
            //            else {
            deselectAllRows();
            if (!chosenRows.isEmpty()) {
               chosenRows.clear();
            }

            for (Object rowData : selectedRows) {
               if (!chosenRows.contains(rowData)) { // if it hasn't been chosen yet, add it.
                  chosenRows.add((T) rowData);
               }
            }
            //            }
         }
         // else ignore...
      }
   }

   public void rowUnselected(Object rowData) {
      if (everythingSelected && rowData != null) { // "everything selected by default".
         if (!chosenRows.contains(rowData)) { // if it hasn't been chosen yet, add it.
            chosenRows.add((T) rowData); // contains a list of what's chosen as "unselected"
         }

         int totalRows = getRowCount();
         int chosenRowSize = chosenRows.size();
         if (totalRows == chosenRowSize) { // everything has been unselected individually...
            deselectAllRows();
         }
         // else ignore...
      }
      // Commented out as uneccessary -- JC 01/12/2017
      //      else if (marker) { // "everythingSelected" was true, but reset by setSelectedRows().
      //         everythingSelected = true;
      //         chosenRows = new ArrayList<T>(); // restore...
      //         chosenRows.addAll(savedChosenRows);
      //         chosenRows.add((T) rowData);
      //
      //         marker = false;
      //         savedChosenRows.clear();
      //         savedChosenRows = null;
      //      }
      // else "nothing selected by default"
   }

   public Object getSelectedRow() {
      Object selectedRow = null;
      if (!everythingSelected && chosenRows.size() == 1) {
         selectedRow = chosenRows.get(0);
      }
      // else everythingSelected, and more than one row is selected.
      return selectedRow;
   }

   /*
    * This method toggles the state of the data model to "everything selected by default"
    */
   public void selectAllRows() {
      // clear chosenRows of any previous user choices.
      if (chosenRows != null && !chosenRows.isEmpty()) {
         chosenRows.clear();
      }
      everythingSelected = true;
   }

   /*
    * This method toggles the state of the data model to "nothing selected by default"
    */
   public void deselectAllRows() {
      // clear chosenRows of any previous user choices.
      if (chosenRows != null && !chosenRows.isEmpty()) {
         chosenRows.clear();
      }
      everythingSelected = false;
   }

   public boolean hasRowsThisPage() {
      boolean hasRows = true;
      if (rowsThisPage == null || rowsThisPage.isEmpty()) {
         hasRows = false;
      }
      return hasRows;
   }

   public List<T> getRowsThisPage() {
      return rowsThisPage;
   }

   protected void setRowsThisPage(List<T> data) {
      // extending class calls this during a load(); if we are doing
      // a load() during an exporterCalled, don't change this.
      if (!loadingAllRowData) {
         rowsThisPage = data;
      }
   }

   protected Map<String, String> getSortOnInfo(List<SortMeta> sort_metadata) {
      HashMap<String, String> sorton_info = new HashMap<>();

      for (SortMeta sort_info : sort_metadata) {
         String sort_field = sort_info.getSortField();
         if (sort_field != null && !sort_field.isEmpty()) {
            String sort_by = ASCENDING;
            SortOrder sort_order = sort_info.getSortOrder();
            if (sort_order.equals(SortOrder.DESCENDING)) {
               sort_by = DESCENDING;
            }

            sorton_info.put(sort_field, sort_by);
         }
      }
      return (sorton_info);
   }

   protected FilterInfo getFilterOnInfo(Map<String, Object> filters) throws Exception {
      FilterInfo filterInfo = null;
      if (filters != null && !filters.isEmpty() && filters.size() > 0) {
         List<FilterInfo> searchFilterList = new ArrayList<>();
         if (filters.size() == 1 && filters.containsKey(PRIMEFACES_GLOBAL_FIELD_FILTER_KEY)) {
            Object value = filters.get(PRIMEFACES_GLOBAL_FIELD_FILTER_KEY);
            if (!(value instanceof String)) {
               throw new IllegalArgumentException("Filtering anywhere in a data value requires a String data type.");
            }

            String search_term = (String) value;
            List<String> filterKeys = getFilterKeys();
            for (String filterKey : filterKeys) {
               if (filterKey != null && !filterKey.isEmpty()) {
                  FilterInfo searchFilter = new FilterInfo(filterKey, search_term, FilterMethod.MATCH_ANYWHERE);
                  searchFilterList.add(searchFilter);
               }
            }
         }
         else {
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
               String filterKey = entry.getKey();
               if (!filterKey.equals(PRIMEFACES_GLOBAL_FIELD_FILTER_KEY)) {
                  Object value = filters.get(filterKey);
                  if (!(value instanceof String)) {
                     throw new IllegalArgumentException("Filtering " + filterKey + " anywhere in a data value requires a String data type.");
                  }

                  String search_term = (String) entry.getValue();
                  FilterInfo searchFilter = new FilterInfo(filterKey, search_term, FilterMethod.MATCH_ANYWHERE);
                  searchFilterList.add(searchFilter);
               }
               // else ignore it and do the next one...
            }
         }

         if (!searchFilterList.isEmpty()) {
            FilterInfo[] searchFilters = searchFilterList.toArray(new FilterInfo[searchFilterList.size()]);
            filterInfo = new FilterInfo(FilterMethod.OR_FIELD_GROUP, searchFilters);
         }
      }
      return filterInfo;
   }

   protected List<T> load(int first, int page_size, Map<String, String> sortOn, FilterInfo filterOn) throws Exception {
      List<Task> jobTaskList = new ArrayList<>();
      LoadTableDataTask loadDataTask = new LoadTableDataTask(this, first, page_size, sortOn, filterOn);
      jobTaskList.add(loadDataTask);
      CountTableDataTask countDataTask = new CountTableDataTask(this, filterOn);
      jobTaskList.add(countDataTask);

      JobRunner jobRunner = new JobRunner();
      jobRunner.setJobTasks(jobTaskList);
      List<?> jobResultList = jobRunner.run();

      if (!jobRunner.hasErrors()) {

         List<T> tabledata = new ArrayList<>();

         if (!jobResultList.isEmpty()) {

            if (jobResultList.size() != 2) {
               Object loadResult = loadDataTask.getResult();
               Object countResult = countDataTask.getResult();
               Logger.log(getClass(), Level.WARNING, "JobResultList.size(" + jobResultList.size() + ") != 2. LoadTableDataTask id[" + loadDataTask.getID()
                     + "] LoadResult[" + loadResult + "]; CountTableDataTask id[" + countDataTask.getID() + "] CountResult[" + countResult + "]");
            }

            for (Object result : jobResultList) {
               if (result instanceof List) {
                  tabledata = (List<T>) result;
                  setRowsThisPage(tabledata);
               }
               else if (result instanceof Number) {
                  setRowCount(((Number) result).intValue());
               }
               // else ????
            }
         }
         // else ????

         return tabledata;
      }
      else {
         String loadErrorMsg = loadDataTask.getErrorMessage();
         String countErrorMsg = countDataTask.getErrorMessage();
         throw new TableDataLoadException(loadErrorMsg, countErrorMsg);
      }
   }

   protected List<String> getFilterKeys() {
      List<ColumnModel> modelList = getColumnModel();
      List<String> filterKeys = new ArrayList<>();
      for (ColumnModel model : modelList) {
         String filterKey = model.getFilterby();
         if (filterKey != null && !filterKey.isEmpty()) {
            filterKeys.add(filterKey);
         }
      }
      return filterKeys;
   }
}
