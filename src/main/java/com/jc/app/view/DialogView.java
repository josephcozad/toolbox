package com.jc.app.view;

import java.io.Serializable;

import org.primefaces.context.RequestContext;

public abstract class DialogView implements Serializable {

   private static final long serialVersionUID = 2314980617349630252L;

   public void init() {}

   // Called to reset the dialog without closing it.
   public abstract void reset();

   // Returns the PrimeFaces 'widgetVar' string for the dialog box.
   public abstract String getPFDialogId();

   // Returns the id string for the dialog box.
   public abstract String getDialogId();

   // Opens the dialog window
   public void showDialog() {
      RequestContext context = RequestContext.getCurrentInstance();
      context.execute("PF('" + getPFDialogId() + "').show();");
   }

   // Closes the dialog window
   public void hideDialog() {
      RequestContext context = RequestContext.getCurrentInstance();
      context.execute("PF('" + getPFDialogId() + "').hide();");
   }

   // Called to reset the dialog and close it. (see reset()).
   public void cancel() {
      reset();
      hideDialog();
   }
}
