package com.jc.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import com.jc.log.Logger;

/**
 * This class manages a set of properties.
 */

public class PropertyManager {

   private final static String NO_FILEID_AVAILABLE = "NO_ID";
   private final static String PROPERTY_DEFAULT_UNDEFINED = "DEFAULT_UNDEFINED";

   private final Map<String, PropFileInfo> PropertyFileInfo;
   private final PropertyNode RootNode;

   private boolean AutoRefresh;

   public PropertyManager() {
      RootNode = new PropertyNode(PropertyNode.ROOT_KEY);
      PropertyFileInfo = new HashMap<String, PropFileInfo>();
   }

   // used to create a clone or subset of a PropertyManager object.
   private PropertyManager(PropertyNode rootNode, Map<String, PropFileInfo> propertyFileInfo, boolean autoRefresh) {
      this.RootNode = rootNode;
      this.PropertyFileInfo = propertyFileInfo;
      this.AutoRefresh = autoRefresh;
   }

   /**
    * Writes the given properties to the named file.
    **/
   public static void writePropertiesToFile(Properties props, String filename) {
      try {
         FileOutputStream out = new FileOutputStream(filename);
         props.store(out, "Properties Header Info.");
         out.close();
      }
      catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void setAutoRefresh(boolean auto_refresh_on) {
      AutoRefresh = auto_refresh_on;
   }

   /**
    * Adds properties from a given file.
    */
   public void addPropertiesFromFile(String filename) throws FileNotFoundException {
      PropFileInfo info = new PropFileInfo(filename);
      Properties props = info.load();
      addProperties(props, info.getId());
      PropertyFileInfo.put(info.getId(), info);
   }

   /**
    * Load properties from a given URL.
    * 
    * @throws IOException
    **/
   public void loadPropertiesFromURL(String url_str) throws IOException {
      Properties prop_obj = new Properties();
      URL url = new URL(url_str);
      prop_obj.load(url.openStream());
      addProperties(prop_obj);
   }

   /**
    * Returns all the properties loaded into the property manager as a Properties object.
    */
   public Properties getProperties() {
      Properties propObj = null;
      if (hasProperties()) {
         propObj = new Properties();
         PropertyNode node = RootNode;
         Map<String, String> propertyMap = node.getPropertyPairs();
         for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            propObj.put(key, value);
         }
      }
      return propObj;
   }

   /**
    * Retrieves all the properties from the supplied path down.
    */
   public Properties getProperties(String path) {
      Properties propObj = null;
      if (hasProperties()) {
         propObj = new Properties();
         PropertyNode node = RootNode;
         Map<String, String> propertyMap = node.getPropertyPairs();
         path += ".";
         for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(path)) {
               key = key.replace(path, "");
               String value = entry.getValue();
               propObj.put(key, value);
            }
         }
      }
      return propObj;
   }

   /*
    * Returns a PropertyManager object with the properties of the supplied path loaded into it. 
    * A null value is returned if there are no properties for the supplied path.
    */
   public PropertyManager getManagerFor(String path) {
      PropertyManager propMngr = null;
      if (isValidPath(path)) {
         PropertyNode rootNode = null;
         rootNode = RootNode.getNode(path);
         rootNode = PropertyNode.createCopy(rootNode); // Create a deep copy...

         rootNode.Key = PropertyNode.ROOT_KEY;
         rootNode.Value = null;
         rootNode.FileId = null;
         rootNode.ParentNode = null;
         propMngr = new PropertyManager(rootNode, PropertyFileInfo, AutoRefresh);
      }
      return propMngr;
   }

   /**
    * Adds properties from a given Property object.
    */
   public void addProperties(Properties prop_obj) {
      addProperties(prop_obj, NO_FILEID_AVAILABLE);
      // AutoRefresh = false;
   }

   /**
    * Indicates whether properties exist.
    **/
   public boolean hasProperties() {
      refresh();
      return RootNode.hasChildren();
   }

   /**
    * Indicates whether a given property exists.
    **/
   public boolean hasProperty(String propkey) {
      refresh();
      return RootNode.hasProperty(propkey);
   }

   /**
    * Removes supplied property. AutoRefresh if on is turned off after this operation.
    */
   public void removeProperty(String path) {
      RootNode.removeProperty(path);
      // AutoRefresh = false;
   }

   /**
    * Clears all properties. AutoRefresh if on is turned off after this operation.
    */
   public void removeAllProperties() {
      RootNode.removeAllProperties();
      // AutoRefresh = false;
   }

   /**
    * Adds a given property value referenced by the path. For example the value "red" can be added to a path of "groceries.fruit.apples.color"; in a property file this would look like "groceries.fruit.apples.color = red".
    */
   public void addProperty(String path, String value) {
      RootNode.addProperty(path, value, NO_FILEID_AVAILABLE);
   }

   /**
    * Returns a property associated with the given prop_name. Default return value is null.
    */
   public String getProperty(String prop_name) {
      return getProperty(prop_name, null);
   }

   /**
    * Returns a property associated with the given prop_name. Default return value is the given default_value.
    */
   public String getProperty(String path, String default_value) {
      refresh();
      String value_str = RootNode.getValue(path);
      return value_str == null ? default_value : value_str;
   }

   public String getProperty(String path, String default_value, String decrypt_key) throws GeneralSecurityException {
      refresh();

      String value_str = RootNode.getValue(path);

      if (value_str == null) {
         return default_value;
      }

      if (decrypt_key == null || decrypt_key.isEmpty()) {
         return value_str;
      }

      return CryptoUtils.decrypt(value_str, decrypt_key);
   }

   /**
    * Returns a property as an int, associated with the given prop_name. Default return value is 0.
    */
   public int getPropertyAsInteger(String prop_name) {
      return getPropertyAsInteger(prop_name, 0);
   }

   /**
    * Returns a property as an int, associated with the given prop_name. Default return value is default_value.
    */
   public int getPropertyAsInteger(String prop_name, int default_value) {
      String value_str = getProperty(prop_name, PROPERTY_DEFAULT_UNDEFINED);
      return PROPERTY_DEFAULT_UNDEFINED.equals(value_str) ? default_value : Integer.valueOf(value_str);
   }

   public int getPropertyAsInteger(String prop_name, String decrypt_key) throws GeneralSecurityException {
      return getPropertyAsInteger(prop_name, 0, decrypt_key);
   }

   public int getPropertyAsInteger(String prop_name, int default_value, String decrypt_key) throws GeneralSecurityException {
      String value_str = getProperty(prop_name, PROPERTY_DEFAULT_UNDEFINED, decrypt_key);
      return PROPERTY_DEFAULT_UNDEFINED.equals(value_str) ? default_value : Integer.valueOf(value_str);
   }

   /**
    * Returns a property as a double, associated with the given prop_name. Default return value is 0.0.
    */
   public double getPropertyAsDouble(String prop_name) {
      return getPropertyAsDouble(prop_name, 0.0);
   }

   /**
    * Returns a property as a double, associated with the given prop_name. Default return value is default_value.
    */
   public double getPropertyAsDouble(String prop_name, double default_value) {
      String value_str = getProperty(prop_name, PROPERTY_DEFAULT_UNDEFINED);
      return PROPERTY_DEFAULT_UNDEFINED.equals(value_str) ? default_value : Double.valueOf(value_str);
   }

   public double getPropertyAsDouble(String prop_name, String decrypt_key) throws GeneralSecurityException {
      return getPropertyAsDouble(prop_name, 0.0, decrypt_key);
   }

   public double getPropertyAsDouble(String prop_name, double default_value, String decrypt_key) throws GeneralSecurityException {
      String value_str = getProperty(prop_name, PROPERTY_DEFAULT_UNDEFINED, decrypt_key);
      return PROPERTY_DEFAULT_UNDEFINED.equals(value_str) ? default_value : Double.valueOf(value_str);
   }

   /**
    * Returns a property as a long, associated with the given prop_name. Default return value is 0L.
    */
   public long getPropertyAsLong(String prop_name) {
      return getPropertyAsLong(prop_name, 0);
   }

   /**
    * Returns a property as a long, associated with the given prop_name. Default return value is default_value.
    */
   public long getPropertyAsLong(String prop_name, long default_value) {
      String value_str = getProperty(prop_name, PROPERTY_DEFAULT_UNDEFINED);
      return PROPERTY_DEFAULT_UNDEFINED.equals(value_str) ? default_value : Long.valueOf(value_str);
   }

   public long getPropertyAsLong(String prop_name, String decrypt_key) throws GeneralSecurityException {
      return getPropertyAsLong(prop_name, 0, decrypt_key);
   }

   public long getPropertyAsLong(String prop_name, long default_value, String decrypt_key) throws GeneralSecurityException {
      String value_str = getProperty(prop_name, PROPERTY_DEFAULT_UNDEFINED, decrypt_key);
      return PROPERTY_DEFAULT_UNDEFINED.equals(value_str) ? default_value : Long.valueOf(value_str);
   }

   /**
    * Returns a property as a float, associated with the given prop_name. Default return value is 0.0.
    */
   public float getPropertyAsFloat(String prop_name) {
      return getPropertyAsFloat(prop_name, 0.0f);
   }

   /**
    * Returns a property as a float, associated with the given prop_name. Default return value is default_value.
    */
   public float getPropertyAsFloat(String prop_name, float default_value) {
      String value_str = getProperty(prop_name, PROPERTY_DEFAULT_UNDEFINED);
      return PROPERTY_DEFAULT_UNDEFINED.equals(value_str) ? default_value : Float.valueOf(value_str);
   }

   public float getPropertyAsFloat(String prop_name, String decrypt_key) throws GeneralSecurityException {
      return getPropertyAsFloat(prop_name, 0.0f, decrypt_key);
   }

   public float getPropertyAsFloat(String prop_name, float default_value, String decrypt_key) throws GeneralSecurityException {
      String value_str = getProperty(prop_name, PROPERTY_DEFAULT_UNDEFINED, decrypt_key);
      return PROPERTY_DEFAULT_UNDEFINED.equals(value_str) ? default_value : Float.valueOf(value_str);
   }

   /**
    * Returns a property as a byte, associated with the given prop_name. Default return value is 0.
    */
   public byte getPropertyAsByte(String prop_name) {
      return getPropertyAsByte(prop_name, (byte) 0);
   }

   /**
    * Returns a property as a byte, associated with the given prop_name. Default return value is default_value.
    */
   public byte getPropertyAsByte(String prop_name, byte default_value) {
      String value_str = getProperty(prop_name, PROPERTY_DEFAULT_UNDEFINED);
      return PROPERTY_DEFAULT_UNDEFINED.equals(value_str) ? default_value : Byte.valueOf(value_str);
   }

   public byte getPropertyAsByte(String prop_name, String decrypt_key) throws GeneralSecurityException {
      return getPropertyAsByte(prop_name, (byte) 0, decrypt_key);
   }

   public byte getPropertyAsByte(String prop_name, byte default_value, String decrypt_key) throws GeneralSecurityException {
      String value_str = getProperty(prop_name, PROPERTY_DEFAULT_UNDEFINED, decrypt_key);
      return PROPERTY_DEFAULT_UNDEFINED.equals(value_str) ? default_value : Byte.valueOf(value_str);
   }

   /**
    * Returns a property as a short, associated with the given prop_name. Default return value is 0.
    */
   public short getPropertyAsShort(String prop_name) {
      return getPropertyAsShort(prop_name, (short) 0);
   }

   /**
    * Returns a property as a byte, associated with the given prop_name. Default return value is default_value.
    */
   public short getPropertyAsShort(String prop_name, short default_value) {
      String value_str = getProperty(prop_name, PROPERTY_DEFAULT_UNDEFINED);
      return PROPERTY_DEFAULT_UNDEFINED.equals(value_str) ? default_value : Short.valueOf(value_str);
   }

   public short getPropertyAsShort(String prop_name, String decrypt_key) throws GeneralSecurityException {
      return getPropertyAsShort(prop_name, (short) 0, decrypt_key);
   }

   public short getPropertyAsShort(String prop_name, short default_value, String decrypt_key) throws GeneralSecurityException {
      String value_str = getProperty(prop_name, PROPERTY_DEFAULT_UNDEFINED, decrypt_key);
      return PROPERTY_DEFAULT_UNDEFINED.equals(value_str) ? default_value : Short.valueOf(value_str);
   }

   /**
    * Returns a property as a boolean, associated with the given prop_name. Default return value is false.
    */
   public boolean getPropertyAsBoolean(String prop_name) {
      return getPropertyAsBoolean(prop_name, "false");
   }

   /**
    * Returns a property as a boolean, associated with the given prop_name. Default return value is default_value.
    */
   public boolean getPropertyAsBoolean(String prop_name, String default_value) {
      String value_str = getProperty(prop_name, default_value);
      return Boolean.valueOf(value_str);
   }

   public boolean getPropertyAsBoolean(String prop_name, String default_value, String decrypt_key) throws GeneralSecurityException {
      String value_str = getProperty(prop_name, default_value, decrypt_key);
      return Boolean.valueOf(value_str);
   }

   @Override
   public String toString() {
      return hasProperties() ? RootNode.toString() : StringUtils.EMPTY;
   }

   public void refresh() {
      if (AutoRefresh) {
         for (PropFileInfo info : PropertyFileInfo.values()) {
            Properties props = info.load();
            if (props != null) {
               String fileid = info.getId();
               RootNode.removePropertiesFor(fileid);
               addProperties(props, fileid);
            }
         }
      }
   }

   /**
    * Adds properties from a given Property object.
    */
   private void addProperties(Properties prop_obj, String fileid) {
      Enumeration<Object> keys = prop_obj.keys();
      while (keys.hasMoreElements()) {
         String key = (String) keys.nextElement();
         String element = prop_obj.getProperty(key);
         RootNode.addProperty(key, element, fileid);
      }
   }

   private boolean isValidPath(String path) {
      refresh();
      return RootNode.isValidPath(path);
   }

   private static final class PropFileInfo {

      private String Id;
      private String Filename;
      private long LastModified;

      PropFileInfo(String filename) throws FileNotFoundException {
         File prop_file = new File(filename);
         if (prop_file.exists()) {
            Filename = filename;
            Id = String.valueOf(filename.hashCode());
         }
         else {
            throw new FileNotFoundException(filename + " does not exist");
         }
      }

      public Properties load() {
         Properties prop_obj = null;
         File prop_file = new File(Filename);
         if (reloadProperties(prop_file)) {
            try {
               LastModified = prop_file.lastModified();
               prop_obj = new Properties();
               prop_obj.load(new FileInputStream(Filename));
            }
            catch (Exception ex) {
               ex.printStackTrace();
            }
         }
         return prop_obj;
      }

      public String getId() {
         return Id;
      }

      private boolean reloadProperties(File prop_file) {
         boolean reload = false;
         if (prop_file.exists()) {
            long lastmod = prop_file.lastModified();
            if (lastmod > LastModified) {
               reload = true;
            }
         }
         else {
            Logger.log(getClass(), Level.SEVERE, "The property file, " + prop_file.getName() + ", was not found at [" + prop_file.getAbsolutePath() + "].");
         }
         return reload;
      }
   }

   public static class PropertyNotFoundException extends RuntimeException {

      private static final long serialVersionUID = 572969718943427071L;

      public PropertyNotFoundException(String property, String filename) {
         super("Could not find property [" + property + "] in property file" + (filename == null ? "" : " [" + filename + "]"));
      }

      public PropertyNotFoundException(String property) {
         this(property, null);
      }
   }
}
