package com.jc.app.view;

import java.io.Serializable;

public class ColumnModel implements Serializable {

   private static final long serialVersionUID = 1512985756649915168L;

   private final String header;
   private final String property;
   private final String style;

   private String sortBy;
   private String filterBy;
   private String filterByType;

   private boolean lazyDataModelMode = true;

   public ColumnModel(String header, String property, String style) {
      this.header = header;
      this.property = property;
      this.style = style;

      this.sortBy = "";
      this.filterBy = "";
   }

   public String getHeader() {
      return header;
   }

   public String getProperty() {
      return property;
   }

   public String getStyle() {
      return style;
   }

   public String getSortby() {
      if (!lazyDataModelMode && !sortBy.isEmpty()) {
         sortBy = "#{" + sortBy + "}";
      }
      return sortBy;
   }

   public void setSortBy(String sortBy) {
      this.sortBy = sortBy;
   }

   public String getFilterby() {
      if (!lazyDataModelMode && !filterBy.isEmpty()) {
         filterBy = "#{" + filterBy + "}";
      }
      return filterBy;
   }

   public String getFilterbyType() {
      return filterByType;
   }

   public void setFilterBy(String filterBy, String filterByType) {
      this.filterBy = filterBy;
      this.filterByType = filterByType;
   }

   public void turnOffLazyDataModelMode() {
      lazyDataModelMode = false;
   }
}
