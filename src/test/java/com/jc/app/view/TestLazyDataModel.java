package com.jc.app.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.primefaces.model.SortMeta;

import com.jc.db.dao.FilterInfo;
import com.jc.exception.LoggableException;

public class TestLazyDataModel extends AppLazyDataModel<Car> {

   private static final long serialVersionUID = -4998390244971944920L;

   private CarService carService;

   public TestLazyDataModel() {
      try {
         carService = new CarService();
      }
      catch (Exception ex) {
         JSFUtils.handleException(getClass(), ex);
      }
   }

   @Override
   public Car getRowData(String rowKey) {
      try {
         Car car = carService.getCar(rowKey);
         return car;
      }
      catch (Exception ex) {
         LoggableException obex = LoggableException.createLoggableException(getClass(), Level.SEVERE, ex);
         JSFUtils.handleException(getClass(), obex);
         return null;
      }
   }

   @Override
   public Object getRowKey(Car rowdata) {
      return rowdata.getId();
   }

   @Override
   public List<Car> load(int first, int page_size, List<SortMeta> sortMetadata, Map<String, Object> filters) {
      try {
         Map<String, String> sortOn = getSortOnInfo(sortMetadata);
         FilterInfo filterOn = getFilterOnInfo(filters);

         // Get all the organizations that have been assigned to the admin...
         List<Car> tabledata = load(first, page_size, sortOn, filterOn);

         return tabledata;
      }
      catch (TableDataLoadException tdlex) {
         handleTableDataLoadException(JSFUtils.SYSTEM_SERVICE_ID, tdlex);
         return new ArrayList<Car>();
      }
      catch (Exception ex) {
         JSFUtils.handleException(getClass(), ex);
         return new ArrayList<Car>();
      }
   }

   @Override
   public List<ColumnModel> getColumnModel() {
      return new ArrayList<ColumnModel>();
   }

   @Override
   public List<Car> getTableData(int first, int length, Map<String, String> sortOn, FilterInfo filterOn) throws Exception {
      List<Car> tableDataList = carService.getCars(first, length, sortOn, filterOn);
      return tableDataList;
   }

   @Override
   public Long getDataCount(FilterInfo filterOn) throws Exception {
      long count = carService.getTotalNumberOfAddressNPIs(filterOn);
      return count;
   }
}
