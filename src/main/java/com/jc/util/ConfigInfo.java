package com.jc.util;

/**
 * This class represents configuration information read in from a property file.
 * It can be called as a Singleton using the getInstance() method loading a default
 * property file defined by the private final static variable DEFAULT_PROPERTY_FILE.
 * The default name of the file is "configInfo.properties" and should exist in the
 * System.getProperty("user.dir") location.
 *
 * Multiple unrelated instances of the ConfigInfo object can also be created with
 * the static getConfigInfoFrom(String) method. Note that ConfigInfo objects
 * created with this method are completely unrelated to the singleton created with the
 * getInstance() method. In addition, subsets of configuration information may be
 * created by calling the getConfigInfoFor(String prop_path) method, creating "child"
 * instances of the parent ConfigInfo object. Each of these child instances have an
 * internal reference to its parent, and if a parent is a child it has a reference
 * to its parent and so on to the root ConfigInfo object.
 *
 * Every call to any of the defined getProperty() methods will check to see if config
 * information needs to be reloaded from the property file. In the case of child
 * ConfigInfo objects, the child requests its parent to reload if necessary, and then
 * refreshes itself if the parent did reload information.
 *
 * Properties can be added on an individual basis. Those added are considered "transient"
 * in that they are unaffected by a change in the root property file loaded, and once the
 * ConfigInfo object that contains the added properties is garbage collected, the property
 * will no longer exist. In other words, adding the property does not guarantee that
 * the associated file is updated with the new property.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;

import com.jc.log.Logger;

public class ConfigInfo {

   public final static String USER_DIR_PROPKEY = "user.dir";
   public final static String DEBUG_PROPKEY = "debug";
   public final static String RUN_LOCALLY_PROPKEY = "local";

   private static String PROPERTY_FILE_NAME = System.getProperty("user.dir") + File.separatorChar + "configInfo.properties";

   private static ConfigInfo SELF;

   private PropertyManager Manager;

   private ConfigInfo() {
      Manager = new PropertyManager();
      Manager.setAutoRefresh(true);
      Manager.addProperty(USER_DIR_PROPKEY, System.getProperty(USER_DIR_PROPKEY));
   }

   /**
    * Returns a single instance of a default ConfigInfo object, loaded with property information from a default property file.
    */
   public static ConfigInfo getInstance() throws FileNotFoundException {
      if (SELF == null) {
         SELF = getConfigInfoFrom(PROPERTY_FILE_NAME);
      }
      return SELF;
   }

   /**
    * Sets the location of the property file that is associated with an instance of the ConfigInfo object. All instances created after this method has been called will be associated with the supplied propfile_path until this method is called again.
    */
   public static void setPropertyFile(String propertyFileName) {
      PROPERTY_FILE_NAME = propertyFileName;
   }

   /**
    * Returns a ConfigInfo object for the supplied property file. Note that the returned ConfigInfo object by this method and the one returned by the getInstance() method are not the same object.
    */
   public static ConfigInfo getConfigInfoFrom(String path) throws FileNotFoundException {
      ConfigInfo conf_info = new ConfigInfo();

      File file = new File(path);
      if (file.exists()) {
         conf_info.load(path);
      }

      //  throw new FileNotFoundException("The file containing the configInfo could not be found at [" + path + "]");
      return conf_info;
   }

   /**
    * Returns a subset of property values, a child ConfigInfo object, for the supplied property path.
    */
   public ConfigInfo getConfigInfoFor(String path) {
      ConfigInfo confInfo = new ConfigInfo();

      PropertyManager propMgr = Manager.getManagerFor(path);
      if (propMgr == null) { // no properties for 'path'...
         propMgr = new PropertyManager(); // create an empty property manager for use by ConfigInfo.
         propMgr.setAutoRefresh(false); // Assume off since this was not created with a file.
      }

      confInfo.Manager = propMgr;
      confInfo.addProperty(USER_DIR_PROPKEY, getProperty(USER_DIR_PROPKEY));
      return confInfo;
   }

   /**
    * Clears all properties from the ConfigInfo.
    */
   public void removeAllProperties() {
      Manager.removeAllProperties();
   }

   /**
    * Returns whether the supplied property name exists in the config info.
    */
   public boolean hasProperty(String property) {
      return Manager.hasProperty(property);
   }

   /**
    * Adds the properties in the supplied property file to the current objects TransientProps.
    */
   public void addPropertiesFromFile(String property_file) throws FileNotFoundException {
      Manager.addPropertiesFromFile(property_file);
   }

   /**
    * Adds a given property value referenced by the path. For example the value "red" can be added to a path of "groceries.fruit.apples.color"; in a property file this would look like "groceries.fruit.apples.color = red".
    */
   public void addProperty(String path, String value) {
      Manager.addProperty(path, value);
   }

   /**
    * Removes a give property by path
    * 
    * @param path
    *           the path to remove
    */
   public void removeProperty(String path) {
      Manager.removeProperty(path);
   }

   /**
    * Adds the given system property
    * 
    * @param property
    */
   public void addSystemProperty(String property) {
      addProperty(property, System.getProperty(property));
   }

   /**
    * Returns a property associated with the given property. Default return value is null.
    */
   public String getProperty(String property) {
      if (Manager.hasProperty(property)) {
         return Manager.getProperty(property, null);
      }
      RuntimeException e = new PropertyManager.PropertyNotFoundException(property, PROPERTY_FILE_NAME);
      logMessage(e.getMessage());
      throw e;
   }

   public String getProperty(String property, String key) throws GeneralSecurityException {
      if (Manager.hasProperty(property)) {
         return Manager.getProperty(property, null, key);
      }
      RuntimeException e = new PropertyManager.PropertyNotFoundException(property, PROPERTY_FILE_NAME);
      logMessage(e.getMessage());
      throw e;
   }

   /**
    * Returns a property as an int, associated with the given property. Default return value is 0.
    */
   public int getPropertyAsInteger(String property) {
      if (Manager.hasProperty(property)) {
         return Manager.getPropertyAsInteger(property);
      }
      RuntimeException e = new PropertyManager.PropertyNotFoundException(property, PROPERTY_FILE_NAME);
      logMessage(e.getMessage());
      throw e;
   }

   public int getPropertyAsInteger(String property, String key) throws GeneralSecurityException {
      if (Manager.hasProperty(property)) {
         return Manager.getPropertyAsInteger(property, key);
      }
      RuntimeException e = new PropertyManager.PropertyNotFoundException(property, PROPERTY_FILE_NAME);
      logMessage(e.getMessage());
      throw e;
   }

   /**
    * Returns a property as a double, associated with the given property. Default return value is 0.0.
    */
   public double getPropertyAsDouble(String property) {
      if (Manager.hasProperty(property)) {
         return Manager.getPropertyAsDouble(property);
      }
      RuntimeException e = new PropertyManager.PropertyNotFoundException(property, PROPERTY_FILE_NAME);
      logMessage(e.getMessage());
      throw e;
   }

   public double getPropertyAsDouble(String property, String key) throws GeneralSecurityException {
      if (Manager.hasProperty(property)) {
         return Manager.getPropertyAsDouble(property, key);
      }
      RuntimeException e = new PropertyManager.PropertyNotFoundException(property, PROPERTY_FILE_NAME);
      logMessage(e.getMessage());
      throw e;
   }

   /**
    * Returns a property as a long, associated with the given property. Default return value is 0L.
    */
   public long getPropertyAsLong(String property) {
      if (Manager.hasProperty(property)) {
         return Manager.getPropertyAsLong(property);
      }
      RuntimeException e = new PropertyManager.PropertyNotFoundException(property, PROPERTY_FILE_NAME);
      logMessage(e.getMessage());
      throw e;
   }

   public long getPropertyAsLong(String property, String key) throws GeneralSecurityException {
      if (Manager.hasProperty(property)) {
         return Manager.getPropertyAsLong(property, key);
      }
      RuntimeException e = new PropertyManager.PropertyNotFoundException(property, PROPERTY_FILE_NAME);
      logMessage(e.getMessage());
      throw e;
   }

   /**
    * Returns a property as a float, associated with the given property. Default return value is 0.0.
    */
   public float getPropertyAsFloat(String property) {
      if (Manager.hasProperty(property)) {
         return Manager.getPropertyAsFloat(property);
      }
      RuntimeException e = new PropertyManager.PropertyNotFoundException(property, PROPERTY_FILE_NAME);
      logMessage(e.getMessage());
      throw e;
   }

   public float getPropertyAsFloat(String property, String key) throws GeneralSecurityException {
      if (Manager.hasProperty(property)) {
         return Manager.getPropertyAsFloat(property, key);
      }
      RuntimeException e = new PropertyManager.PropertyNotFoundException(property, PROPERTY_FILE_NAME);
      logMessage(e.getMessage());
      throw e;
   }

   /**
    * Returns a property as a byte, associated with the given property. Default return value is 0.
    */
   public byte getPropertyAsByte(String property) {
      if (Manager.hasProperty(property)) {
         return Manager.getPropertyAsByte(property);
      }
      RuntimeException e = new PropertyManager.PropertyNotFoundException(property, PROPERTY_FILE_NAME);
      logMessage(e.getMessage());
      throw e;
   }

   public byte getPropertyAsByte(String property, String key) throws GeneralSecurityException {
      if (Manager.hasProperty(property)) {
         return Manager.getPropertyAsByte(property, key);
      }
      RuntimeException e = new PropertyManager.PropertyNotFoundException(property, PROPERTY_FILE_NAME);
      logMessage(e.getMessage());
      throw e;
   }

   /**
    * Returns a property as a boolean, associated with the given property. Default return value is false.
    */
   public boolean getPropertyAsBoolean(String property) {
      if (Manager.hasProperty(property)) {
         return Manager.getPropertyAsBoolean(property, "false");
      }
      RuntimeException e = new PropertyManager.PropertyNotFoundException(property, PROPERTY_FILE_NAME);
      logMessage(e.getMessage());
      throw e;
   }

   public boolean getPropertyAsBoolean(String property, String key) throws GeneralSecurityException {
      if (Manager.hasProperty(property)) {
         return Manager.getPropertyAsBoolean(property, "false", key);
      }
      RuntimeException e = new PropertyManager.PropertyNotFoundException(property, PROPERTY_FILE_NAME);
      logMessage(e.getMessage());
      throw e;
   }

   @Override
   public String toString() {
      return Manager.toString();
   }

   private static void logMessage(String message) {
      Logger.log(ConfigInfo.class, Level.SEVERE, message);
   }

   private void load(String filepath) throws FileNotFoundException {
      if (Manager == null) {
         Manager = new PropertyManager();
         Manager.setAutoRefresh(true);
         Manager.addProperty(USER_DIR_PROPKEY, System.getProperty(USER_DIR_PROPKEY));
      }
      Manager.addPropertiesFromFile(filepath);
   }
}
