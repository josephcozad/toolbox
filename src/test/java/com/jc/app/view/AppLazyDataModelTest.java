package com.jc.app.view;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.primefaces.model.SortOrder;

public class AppLazyDataModelTest {

   //   @Test
   //   // @Ignore
   //   public void xxxxx() throws Exception {
   //      if (!conn.isClosed()) {
   //         fail("Expected DBConnection object to be closed.");
   //      }
   //   }

   //   public void setSelectedRows(List<?> selectedRows) {
   //   public void rowUnselected(Object rowData) {
   //   public Object getSelectedRow() {
   //   public boolean hasRowsThisPage() {
   //   public List<T> getRowsThisPage() {

   @Test
   // @Ignore
   public void xxxxx() throws Exception {

      TestLazyDataModel dataModel = new TestLazyDataModel();

      int first = 0;
      int pageSize = 0;
      String sortField = null;
      SortOrder sortOrder = null;
      Map<String, Object> filters = new HashMap<String, Object>();
      dataModel.load(first, pageSize, sortField, sortOrder, filters);

      List<Car> dataList = dataModel.getSelectedRows();
      if (dataList == null) {
         fail("Expected dataList to not be null.");
      }

      if (!dataList.isEmpty()) {
         fail("Expected dataList to be empty.");
      }

      // TEST Select All Rows -----------------------------------------------
      dataModel.selectAllRows();

      dataList = dataModel.getSelectedRows();
      if (dataList == null || dataList.isEmpty()) {
         fail("Expected dataList to not be null or empty.");
      }

      if (dataList.size() != CarService.TOTAL_NUMBER_CARS_CREATED) {
         fail("Expected dataList size to be '" + CarService.TOTAL_NUMBER_CARS_CREATED + "' and was '" + dataList.size() + "'.");
      }

      // TEST Deselect All Rows ---------------------------------------------
      dataModel.deselectAllRows();

      dataList = dataModel.getSelectedRows();
      if (!dataList.isEmpty()) {
         fail("Expected dataList to be empty.");
      }

      // TEST xxxxxxxxxx ----------------------------------------------------

      //      setSelectedRows(List<?> selectedRows)
      //      Object getSelectedRow()
   }

   //
   //
   //

   //   public abstract List<ColumnModel> getColumnModel();
   //   public abstract List<T> getTableData(int first, int page_size, Map<String, String> sortOn, FilterInfo filterOn) throws Exception;
   //   public abstract Long getDataCount(FilterInfo filterOn) throws Exception;
   //   public List<?> processLoadedData(List<?> tabledata) throws Exception {
   //   public List<T> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
   //   public void handleTableDataLoadException(String serviceId, TableDataLoadException tdlex) {

   //   public void setDataExporterCalled(boolean value) {
   //   public boolean dataExporterCalled() {
   //      
   //
   //   protected void setRowsThisPage(List<T> data) {
   //   protected Map<String, String> getSortOnInfo(List<SortMeta> sort_metadata) {
   //   protected FilterInfo getFilterOnInfo(Map<String, Object> filters) throws Exception {
   //   protected List<T> load(int first, int page_size, Map<String, String> sortOn, FilterInfo filterOn) throws Exception {
   //   protected List<String> getFilterKeys() {
}
