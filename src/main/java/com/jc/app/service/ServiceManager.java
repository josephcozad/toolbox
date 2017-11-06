package com.jc.app.service;

import java.util.ArrayList;
import java.util.List;

/*
 * This class provides high level services for extending service managers.
 */

public abstract class ServiceManager {

   public final static String SERVICE_MANAGER_PROPKEY = "app.serviceManagerClass";

   protected ServiceManager() throws Exception {}

   public abstract Object addData(Object data) throws Exception;

   public List<?> addData(List<?> dataList) throws Exception {
      List<Object> updatedList = new ArrayList<Object>();
      for (Object data : dataList) {
         Object updatedData = addData(data);
         updatedList.add(updatedData);
      }
      return updatedList;
   }

   public abstract Object updateData(Object data) throws Exception;

   public List<?> updateData(List<?> dataList) throws Exception {
      List<Object> updatedList = new ArrayList<Object>();
      for (Object data : dataList) {
         Object updatedData = updateData(data);
         updatedList.add(updatedData);
      }
      return updatedList;
   }

   public abstract void removeData(Object data) throws Exception;

   public void removeData(List<?> dataList) throws Exception {
      for (Object data : dataList) {
         removeData(data);
      }
   }
}
