package com.jc.app.view;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import com.jc.app.User;
import com.jc.exception.PrefixedLoggableException;
import com.jc.exception.SystemInfoException;
import com.jc.log.Logger;
import com.jc.shiro.AccessControlService;
import com.jc.util.ConfigInfo;

/**
 * These methods are intended to be used as convenience methods when developing JSF-based code.
 */
public class JSFUtils {

   public final static String SYSTEM_SERVICE_ID = "SYS";

   public static User getUser() throws Exception {
      return (User) AccessControlService.getUser();
   }

   public static String getUsersDatasource() throws Exception {
      User user = getUser();
      if (user != null) {
         String datasource = user.getDatasource();
         return datasource;
      }
      else {
         throw new Exception("Error while trying to get user's datasource; no user object returned.");
      }
   }

   public static void sendBackContentAsFile(String fileName, byte[] contentBytes) throws Exception {
      FacesContext fc = FacesContext.getCurrentInstance();
      ExternalContext ec = fc.getExternalContext();

      String contentType = ec.getMimeType(fileName);
      int contentLength = contentBytes.length;

      ec.responseReset();
      ec.setResponseContentType(contentType);
      ec.setResponseContentLength(contentLength);
      ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

      OutputStream output = ec.getResponseOutputStream();
      output.write(contentBytes); // Write to output...

      fc.responseComplete();
   }

   public static void addObjectToSessionMap(String key, Object value) throws Exception {
      if (value != null) {
         FacesContext fc = FacesContext.getCurrentInstance();
         if (fc != null) {
            fc.getExternalContext().getSessionMap().put(key, value);
         }
         else {
            throw new Exception("No FacesContext available.");
         }
      }
   }

   public static boolean containsSessionKey(String key) throws Exception {
      FacesContext fc = FacesContext.getCurrentInstance();
      if (fc != null) {
         return fc.getExternalContext().getSessionMap().containsKey(key);
      }
      else {
         throw new Exception("No FacesContext available.");
      }
   }

   public static Object getObjectFromSessionMap(String key) throws Exception {
      FacesContext fc = FacesContext.getCurrentInstance();
      if (fc != null) {
         Map<String, Object> sessionMap = fc.getExternalContext().getSessionMap();
         if (sessionMap.containsKey(key)) {
            return sessionMap.get(key);
         }
         else {
            return null;
         }
      }
      else {
         throw new Exception("No FacesContext available.");
      }
   }

   public static void removeObjectFromSessionMap(String key) throws Exception {
      FacesContext fc = FacesContext.getCurrentInstance();
      if (fc != null) {
         Map<String, Object> sessionMap = fc.getExternalContext().getSessionMap();
         if (sessionMap.containsKey(key)) {
            sessionMap.remove(key);
         }
      }
      else {
         throw new Exception("No FacesContext available.");
      }
   }

   public static void clearAllObjectsFromSessionMap() throws Exception {
      FacesContext fc = FacesContext.getCurrentInstance();
      if (fc != null) {
         Map<String, Object> sessionMap = fc.getExternalContext().getSessionMap();
         if (!sessionMap.isEmpty()) {
            sessionMap.clear();
         }
      }
      else {
         throw new Exception("No FacesContext available.");
      }
   }

   public static void handleException(Class<?> calling_class, Exception ex) {
      handleException(calling_class, ex, (String) null, DEFAULT_JSF_MESSAGE_CLIENT_ID);
   }

   public static void handleException(Class<?> calling_class, Exception ex, String userMessage) {
      handleException(calling_class, ex, userMessage, DEFAULT_JSF_MESSAGE_CLIENT_ID);
   }

   public static void handleException(Class<?> callingClass, Exception ex, String userMessage, String clientId) {
      if (!(ex instanceof PrefixedLoggableException)) {
         if (!(ex instanceof SystemInfoException)) { // don't create PrefixedLoggableException for these, these are user consumable.
            String additionalMessage = null;
            ex = new PrefixedLoggableException(callingClass, Level.SEVERE, ex, additionalMessage, userMessage, SYSTEM_SERVICE_ID);
         }
      }
      addMessage(clientId, ex);
   }

   // ------------------- Add Message Methods --------------------
   public final static String DEFAULT_JSF_MESSAGE_CLIENT_ID = null;
   public final static String DEFAULT_DAILOG_MESSAGE_CLIENT_ID = "dialogMessages";

   public static void addMessage(Class<?> calling_class, Level level, String message, String userMessage) {
      addMessage(DEFAULT_JSF_MESSAGE_CLIENT_ID, calling_class, level, message, userMessage);
   }

   public static void addMessage(Class<?> calling_class, Level level, String message) {
      addMessage(DEFAULT_JSF_MESSAGE_CLIENT_ID, calling_class, level, message);
   }

   // Also logs message displayed to user.
   public static void addMessage(String clientId, Class<?> calling_class, Level level, String message, String userMessage) {
      addUserMessage(clientId, userMessage, level);
      if (message != null && !message.isEmpty()) {
         Logger.log(calling_class, level, message);
      }
   }

   public static void addMessage(String clientId, Class<?> calling_class, Level level, String message) {
      addUserMessage(clientId, message, level);
      Logger.log(calling_class, level, message);
   }

   public static void addUserMessage(String message, Level level) {
      addUserMessage(DEFAULT_JSF_MESSAGE_CLIENT_ID, message, level);
   }

   public static void addUserMessage(String clientId, String userMessage, Level level) {
      if (!level.equals(Level.ALL) && !level.equals(Level.CONFIG) && !level.equals(Level.FINE) && !level.equals(Level.FINER) && !level.equals(Level.FINEST)
            && !level.equals(Level.OFF)) {
         if (level.equals(Level.INFO)) {
            addUserMessage(clientId, FacesMessage.SEVERITY_INFO, "Info", userMessage);
         }
         else if (level.equals(Level.WARNING)) {
            addUserMessage(clientId, FacesMessage.SEVERITY_WARN, "Warn", userMessage);
         }
         else {
            addUserMessage(clientId, FacesMessage.SEVERITY_FATAL, "Error", userMessage);
         }
      }
      //      else {
      //         FacesMessage fmessage = null;
      //         fmessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error[2]", message);
      //         String client_id = null;
      //         FacesContext.getCurrentInstance().addMessage(client_id, fmessage);
      //      }
   }

   public static void addUserMessage(String clientId, Severity level, String summaryMsg, String detailMsg) {
      FacesContext facesContext = FacesContext.getCurrentInstance();
      Iterator<FacesMessage> facesMsgItr = facesContext.getMessages(clientId);
      boolean found = false;
      while (facesMsgItr.hasNext()) {
         FacesMessage tstMessage = facesMsgItr.next();
         if (tstMessage.getDetail().equals(detailMsg)) {
            found = true;
            break;
         }
      }

      if (!found) {
         FacesMessage fmessage = new FacesMessage(level, summaryMsg, detailMsg);
         facesContext.addMessage(clientId, fmessage);
      }
   }

   public static boolean hasMessages(String clientId) {
      List<FacesMessage> messages = FacesContext.getCurrentInstance().getMessageList(clientId);
      return !messages.isEmpty();
   }

   // ------------------------------------------------

   private static void addMessage(String clientId, Exception ex) {
      Level level = null;
      String message = null;

      if (ex instanceof SystemInfoException) {
         SystemInfoException siex = (SystemInfoException) ex;
         level = siex.getSeverityLevel();
         message = siex.getMessage();

      }
      else if (ex instanceof PrefixedLoggableException) {
         PrefixedLoggableException plex = (PrefixedLoggableException) ex;

         level = plex.getSeverityLevel();
         message = plex.getUserMessage();

         boolean debugging = false;
         try {
            ConfigInfo configInfo = ConfigInfo.getInstance();
            debugging = configInfo.hasProperty("debug");
         }
         catch (FileNotFoundException fnfex) {
            // eat it for now...
         }

         if (debugging) {
            message = plex.getMessage();
         }
      }
      else { // should not happen...
         level = Level.SEVERE;
         message = ex.getMessage();
      }

      addUserMessage(clientId, message, level);
   }
}
