package com.jc.app.view;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.LazyDataModel;

@ManagedBean(name = "testListView")
@ViewScoped
public class TestListView extends DataTableListView implements Serializable {

   private static final long serialVersionUID = 2314303440912651716L;

   @Override
   @PostConstruct
   public void init() {
      TestLazyDataModel dataModel = new TestLazyDataModel();
      setDataModel(dataModel);
   }

   @Override
   public LazyDataModel<?> getRows() {
      LazyDataModel<?> dataModel = super.getRows();
      return dataModel;
   }

   @Override
   public String getPFDataTableId() {
      return "testTableWV";
   }

   @Override
   public String getDataTableId() {
      return "testTable";
   }

   @Override
   public void createNewRow() {}

   @Override
   public boolean isNewRow() {
      return false;
   }

   @Override
   public boolean isEditAllowed() {
      boolean editAllowed = false;
      return editAllowed;
   }

   @Override
   public String processRowEdit() {
      return null;
   }

   @Override
   public String deleteRow() {
      return null;
   }

   @Override
   public void reset() {
      deselectAllRows();
   }
}
