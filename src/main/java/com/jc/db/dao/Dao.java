package com.jc.db.dao;

import java.util.List;
import java.util.Map;

public interface Dao<K, E> {

   public final static int UPDATE = 0;
   public final static int SAVE = 1;

   public static boolean ROLLBACK_ON = false; // true for debugging and testing...

   public E addData(E data) throws Exception;

   public List<E> addData(List<E> dataList) throws Exception;

   public E updateData(E data) throws Exception;

   public List<E> updateData(List<E> dataList) throws Exception;

   public void removeData(List<E> dataList) throws Exception;

   public void removeData(E data) throws Exception;

   public E persistData(E data, int persistType) throws Exception;

   public E findById(K id) throws Exception;

   public List<E> findAll(int start, int length, Map<String, String> sortOn, FilterInfo filterOn) throws Exception;

   public long countAll(FilterInfo filterOn) throws Exception;

   public List<E> getDataListByField(String fieldname, Object fieldvalue, FilterMethod methodType) throws Exception;

   public E getDataByField(String fieldname, Object fieldvalue, FilterMethod methodType) throws Exception;
}
