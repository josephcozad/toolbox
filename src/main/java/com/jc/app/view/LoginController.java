package com.jc.app.view;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;

import org.apache.shiro.authc.ExpiredCredentialsException;

import com.jc.app.User;
import com.jc.app.WebAppContextListener;
import com.jc.exception.LoggableException;
import com.jc.log.Logger;
import com.jc.shiro.AccessControlService;
import com.jc.shiro.AuthorizationErrorCodes;
import com.jc.util.ConfigInfo;

public abstract class LoginController implements Serializable {

   private static final long serialVersionUID = 2144901344094998664L;

   private String username;
   private String password;
   private String appName;
   private String appVersion;

   private String confirmPwd;
   private String newPassword;

   @PostConstruct
   public void init() {
      try {
         ConfigInfo info = ConfigInfo.getInstance();
         if (info.hasProperty(WebAppContextListener.INITIALIZATION_ERROR_PROPKEY)) {
            String initErrorMessage = info.getProperty(WebAppContextListener.INITIALIZATION_ERROR_PROPKEY);
            JSFUtils.addUserMessage(initErrorMessage, Level.SEVERE);
         }
      }
      catch (FileNotFoundException fnfex) {
         JSFUtils.handleException(getClass(), fnfex);
      }
   }

   public String login() {
      String fallbackUrl = getLoginPage();
      try {
         AccessControlService.loginUser(username, password);
         fallbackUrl = getLoginSuccessNextPage();
      }
      catch (Exception ex) {
         if (ex instanceof ExpiredCredentialsException) {
            fallbackUrl = getExpiredCredentialsPage();
         }
         else {
            JSFUtils.handleException(getClass(), ex);
         }
      }
      return fallbackUrl;
   }

   public String logout() {
      try {
         User user = JSFUtils.getUser();
         AccessControlService.logoutUser(user.getUsername());
         JSFUtils.clearAllObjectsFromSessionMap();
      }
      catch (Exception ex) {
         JSFUtils.handleException(getClass(), ex);
      }
      return getLogOutNextPage();
   }

   public String resetPassword() {
      String fallbackUrl = getPasswordResetPage();
      try {
         // confirm new passwords are the same...
         // change password and set reset password to false in db...
         if (newPasswordValid()) {
            AccessControlService.changePassword(username, password, newPassword);
            AccessControlService.loginUser(username, newPassword);
            fallbackUrl = getLoginSuccessNextPage();
         }
      }
      catch (Exception ex) {
         JSFUtils.handleException(getClass(), ex);
      }
      return fallbackUrl;
   }

   public void setCode(String code) {
      int codeValue = new Integer(code).intValue();

      if (codeValue == AuthorizationErrorCodes.UNAUTHORIZED_ACCESS) {
         JSFUtils.addUserMessage(JSFUtils.DEFAULT_JSF_MESSAGE_CLIENT_ID, FacesMessage.SEVERITY_ERROR, "Unauthorized Access", "");
      }
      else if (codeValue == AuthorizationErrorCodes.SYSERR_NO_ROLES_ASSIGNED) {
         JSFUtils.handleException(getClass(), new LoggableException(getClass(), Level.SEVERE, "No roles found for user."));
      }
      else if (codeValue == AuthorizationErrorCodes.SYSERR_GENERAL) {
         JSFUtils.addUserMessage(JSFUtils.DEFAULT_JSF_MESSAGE_CLIENT_ID, FacesMessage.SEVERITY_ERROR, "System error occured.", "");
      }
   }

   public String getCode() {
      return "";
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getConfirmPassword() {
      return confirmPwd;
   }

   public void setConfirmPassword(String password) {
      this.confirmPwd = password;
   }

   public String getNewPassword() {
      return newPassword;
   }

   public void setNewPassword(String password) {
      this.newPassword = password;
   }

   public String getAppName() {
      if (appName == null || appName.isEmpty()) {
         try {
            ConfigInfo info = ConfigInfo.getInstance();
            if (info.hasProperty("app.name")) {
               appName = info.getProperty("app.name");
            }
         }
         catch (Exception ex) {
            Logger.log(getClass(), Level.SEVERE, ex);
            JSFUtils.addMessage(getClass(), Level.SEVERE, "Error getting application name.");
         }
      }
      return appName;
   }

   public String getAppVersion() {
      if (appVersion == null || appVersion.isEmpty()) {
         try {
            ConfigInfo info = ConfigInfo.getInstance();
            appVersion = info.getProperty("app.version");
         }
         catch (Exception ex) {
            JSFUtils.handleException(getClass(), ex);
         }
      }
      return appVersion;
   }

   /*
    * Override this method to validate the new password.
    */
   protected boolean newPasswordValid() throws Exception {
      return true;
   }

   /*
    * Returns the URI to the login page.
    */
   protected abstract String getLoginPage();

   /*
    * Returns the URI to the next page after user has successfully logged in.
    */
   protected abstract String getLoginSuccessNextPage();

   /*
    * Returns the URI to the next page after user has 
    * unsuccessfully logged in with an expired account.
    */
   protected abstract String getExpiredCredentialsPage();

   /*
    * Returns the URI to the password reset page.
    */
   protected abstract String getPasswordResetPage();

   /*
    * Returns the URI to the next page after user has successfully logged out.
    */
   protected abstract String getLogOutNextPage();
}
