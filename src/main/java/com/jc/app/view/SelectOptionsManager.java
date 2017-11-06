package com.jc.app.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import com.jc.log.ExceptionMessageHandler;

public abstract class SelectOptionsManager implements Converter, Serializable {

   private static final long serialVersionUID = 7265838382406618456L;

   private List<SelectOption> selectOptionsList;
   private List<SelectOption> selectedOptions;

   @Override
   public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
      if (value != null && value.trim().length() > 0) {
         try {
            SelectOption option = getSelectOption(value);
            if (option == null) {
               option = new SelectOption(value, value);
            }
            return option;
         }
         catch (Exception e) {
            String message = ExceptionMessageHandler.formatExceptionMessage(e);
            throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Conversion Error", message));
         }
      }
      else {
         return null;
      }
   }

   @Override
   public String getAsString(FacesContext fc, UIComponent uic, Object object) {
      if (object != null) {
         if (object instanceof SelectOption) {
            return String.valueOf(((SelectOption) object).getValue());
         }
         else {
            return String.valueOf(object);
         }
      }
      else {
         return null;
      }
   }

   public abstract void init() throws Exception;

   protected abstract SelectOption createSelectOption(Object data);

   protected abstract SelectOption getSelectOption(String value);

   protected abstract String getSelectedOptionErrorMsg();

   protected void setSelectedOptionTo(Object value) {
      // Optional and unimplemented, override to provide specific functionality.
   }

   public void clearSelectedOptions() {
      if (selectedOptions != null && !selectedOptions.isEmpty()) {
         selectedOptions.clear();
      }
   }

   public boolean isOptionSelected() {
      return selectedOptions != null && !selectedOptions.isEmpty();
   }

   public void setSelectedOption(SelectOption selectedOption) {
      if (selectedOption != null) {
         if (selectedOptions == null) {
            selectedOptions = new ArrayList<SelectOption>();
         }
         else if (!selectedOptions.isEmpty()) {
            selectedOptions.clear();
         }
         selectedOptions.add(selectedOption);
      }
   }

   public SelectOption getSelectedOption() {
      SelectOption selectedOption = null;
      if (selectedOptions != null && !selectedOptions.isEmpty()) {
         selectedOption = selectedOptions.get(0);
      }
      return selectedOption;
   }

   public List<SelectOption> getSelectedOptions() {
      return selectedOptions;
   }

   public void setSelectedOptions(List<SelectOption> selectedOptions) {
      this.selectedOptions = selectedOptions;
   }

   public int getNumOptions() {
      return selectOptionsList.size();
   }

   public List<SelectOption> getOptions() {
      return selectOptionsList;
   }

   /*
    * Override this method to use the optionValue to return options that match
    * either in whole or in part.
    */
   public List<SelectOption> getOptions(String optionValue) {
      return getOptions();
   }

   protected void setOptions(List<SelectOption> optionsList) {
      selectOptionsList = optionsList;
   }

   public int getNumDisplayCols() {
      return 1;
   }
}
