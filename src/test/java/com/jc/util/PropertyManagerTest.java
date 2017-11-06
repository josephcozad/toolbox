package com.jc.util;

import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.Random;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class PropertyManagerTest {

   private static final String SECURITY_KEY = "4677242A562B5C8F76FBC75D188DE9C2";

   private static final Random RANDOM = new Random(System.currentTimeMillis());

   private String key;

   private static final String TEST_PROPERTY_FILE_A = "C:\\Workspace\\Development\\projects\\ma-commons\\1.1\\src\\test\\resources\\TestPropertyFileA.properties";
   private static final String TEST_PROPERTY_FILE_B = "C:\\Workspace\\Development\\projects\\ma-commons\\1.1\\src\\test\\resources\\TestPropertyFileB.properties";

   private static final String BOGUS_PROPERTY_NAME = "bogus.property.name";
   private static final String ENC = ".encrypted";

   @Test
   @Ignore
   public void testGetPropertiesForPath() {
      PropertyManager propMgr = getPropertyManager(PropertyManagerTest.TEST_PROPERTY_FILE_A);
      Properties propObj = propMgr.getProperties("abc.def");

      if (propObj == null) {
         Assert.fail("Properties object was null.");
      }

      for (Object subKey : propObj.keySet()) {
         key = "def." + (String) subKey;
         if (!propMgr.hasProperty(key)) {
            Assert.fail("Parent/child PropertyManager property key mis-match.");
         }
      }
   }

   @Test
   @Ignore
   public void testGetManagerFor() {
      PropertyManager propMgr = getPropertyManager(PropertyManagerTest.TEST_PROPERTY_FILE_A);

      //    Properties properties = propMgr.getProperties();
      //   Properties properties2 = propMgr.getProperties("log.app");
      PropertyManager childPropMgr = propMgr.getManagerFor("log.app");

      System.out.println("----");

      //      PropertyManager childPropMgr = propMgr.getManagerFor("log.app");
      //
      //      if (childPropMgr == null) {
      //         Assert.fail("Child property manager was null.");
      //      }
      //
      //      if (!childPropMgr.hasProperties()) {
      //         Assert.fail("Child property manager didn't have properties.");
      //      }
      //
      //      if (!childPropMgr.hasProperty("def.propB")) {
      //         Assert.fail("Child property manager didn't have a property for 'abc.def'.");
      //      }
   }

   //   /*
   //    * This test checks the functionality of the auto-refresh features of the 
   //    * PropertyManager and should be run in debug mode to allow for interactive 
   //    * changes to the property file as needed. Place break points in appropriate 
   //    * places to stop the test to make the needed change to the property file, 
   //    * and then continue the test.
   //    */
   //   @Test
   //   @Ignore
   //   public void testAutoRefresh() throws Exception {
   //      // Test default with autorefresh off...
   //      PropertyManager propMgr = getPropertyManager(TEST_PROPERTY_FILE_A);
   //      String before = propMgr.getProperty("auto.refresh.test.valueA");
   //
   //      // PUT BREAK POINT HERE and change property value.
   //      System.out.println("Change file...");
   //
   //      String after = propMgr.getProperty("auto.refresh.test.valueA");
   //      if (!before.equals(after)) {
   //         Assert.fail("Default auto-refresh off failed; before[" + before + "] != after[" + after + "].");
   //      }
   //
   //      // Test default with autorefresh on...
   //      propMgr.setAutoRefresh(true);
   //
   //      before = propMgr.getProperty("auto.refresh.test.valueA");
   //      
   //      // PUT BREAK POINT HERE and change property value.
   //      System.out.println("Change file...");
   //
   //      after = propMgr.getProperty("auto.refresh.test.valueA");
   //      if (before.equals(after)) {
   //         Assert.fail("Default auto-refresh on failed; before[" + before + "] == after[" + after + "].");
   //      }
   //
   //      // With autorefresh on... remove property and test if turned off...
   //      before = propMgr.getProperty("auto.refresh.test.valueA");
   //
   //      // PUT BREAK POINT HERE and change property value.
   //      System.out.println("Change file...");
   //
   //      propMgr.removeProperty("property2.valueB");
   //      after = propMgr.getProperty("auto.refresh.test.valueA");
   //      
   //      Assert.assertEquals("Remove property with auto-refresh on failed.", before, after);
   //
   //      // With autorefresh on... add properties from second file and test if turned off...
   //      propMgr.setAutoRefresh(true);
   //
   //      before = propMgr.getProperty("auto.refresh.test.valueA");
   //      propMgr.addPropertiesFromFile(TEST_PROPERTY_FILE_A);
   //
   //      // PUT BREAK POINT HERE and change property value.
   //      System.out.println("Change file...");
   //
   //      after = propMgr.getProperty("auto.refresh.test.valueA");
   //      Assert.assertNotEquals("Add properties from file with auto-refresh on failed.", before, after);
   //   }
   //
   //   /**
   //    * This test case tests all the basic functionality; add, modify, remove, and test for existence of a property. It does not read properties in from an external source like the other tests in this test case. Also fully tests the functionality of the PropertyNode inner class.
   //    */
   //   @Test
   //   public void testBasicFunctionality() throws Exception {
   //      PropertyManager propMgr = new PropertyManager();
   //
   //      // Adds values...
   //      propMgr.addProperty("property12.valueD.value7", "Value D7");
   //      propMgr.addProperty("property12.valueD.value1", "Value D1");
   //      propMgr.addProperty("property12.valueD.value5", "Value D5");
   //      propMgr.addProperty("property12.valueD", "Value D");
   //
   //      // Do properties exist...
   //      Assert.assertTrue("failed to add any properties.", propMgr.hasProperties());
   //
   //      // Output as String
   //      String str_value = propMgr.toString();
   //      System.out.println(str_value);
   //
   //      // Overwrites value added.
   //      key = "property12.valueD.value1";
   //      String bfr = propMgr.getProperty(key);
   //      propMgr.addProperty(key, "Value D11");
   //      String aftr = propMgr.getProperty(key);
   //
   //      Assert.assertFalse("failed to modify property.", bfr.equals(aftr));
   //      Assert.assertTrue("failed to modify property correctly.", aftr.equals("Value D11"));
   //
   //      // Check if the property exists...
   //      key = "property12.valueD.value5";
   //      Assert.assertTrue("failed to add 'property12.valueD.value5'.", propMgr.hasProperty(key));
   //
   //      // Check if non-existent property exists...
   //      key = "property12.valueE.value5";
   //      Assert.assertFalse("failed verify if non-existent property exists.", propMgr.hasProperty(key));
   //
   //      // Remove existing property....
   //      key = "property12.valueD.value7";
   //      Assert.assertTrue("failed to find property [" + key + "]", propMgr.hasProperty(key));
   //      propMgr.removeProperty(key);
   //      Assert.assertFalse("failed to remove property [" + key + "].", propMgr.hasProperty(key));
   //
   //      key = "property12.valueD";
   //      Assert.assertTrue("failed to find property [" + key + "]", propMgr.hasProperty(key));
   //      TestObject.removeProperty(key);
   //      Assert.assertFalse("failed to remove property [" + key + "].", propMgr.hasProperty(key));
   //
   //      // Remove non-existent property...
   //      key = "property12.valueQ.value7";
   //      Assert.assertFalse("found unexpected property [" + key + "]", propMgr.hasProperty(key));
   //      propMgr.removeProperty(key);
   //      Assert.assertFalse("failed to remove non-existent property [" + key + "].", propMgr.hasProperty(key));
   //
   //      // Remove all properties...
   //      Assert.assertTrue("failed to find any properties", propMgr.hasProperties());
   //      propMgr.removeAllProperties();
   //      Assert.assertFalse("failed to remove all properties.", propMgr.hasProperties());
   //   }
   //
   //   @Test
   //   public void testAddProperties() throws Exception {
   //      PropertyManager propMgr = new PropertyManager();
   //      Assert.assertFalse("New PropertyManager is not empty.", propMgr.hasProperties());
   //
   //      // Create a Properties Object to add....
   //      Properties prop_obj = new Properties();
   //      FileInputStream stream = new FileInputStream(getConfigFilePath(CONFIG_FILE_NAME));
   //
   //      prop_obj.load(stream);
   //      stream.close();
   //
   //      propMgr.addProperties(prop_obj);
   //      Assert.assertTrue("PropertyManager has no properties.", propMgr.hasProperties());
   //   }
   //
   //   @Test
   //   public void testGetAllProperties() {
   //      Properties prop_obj = propMgr.getProperties();
   //
   //      Assert.assertNotNull("Get loaded properties as a property object failed.", prop_obj); 
   //
   //      // TODO: Need to verify that that properties object returned and the PropertyManager are the same properties.
   //   }
   //
   //   @Test
   //   public void testRemoveAllProperties() {
   //      TestObject.removeAllProperties();
   //
   //      Assert.assertFalse("failed to remove all properties.", TestObject.hasProperties());
   //   }
   //
   //   @Test
   //   public void testAddProperty() throws Exception {
   //
   //      // Add property and value to property tree already in existence
   //      key = "property2.valueC.value4";
   //      TestObject.addProperty(key, "Value C4");
   //      Assert.assertTrue("Property [" + key + "] not added.", TestObject.hasProperty(key));
   //      Assert.assertEquals("Property added to extent prop tree not correct.", "Value C4", TestObject.getProperty(key));
   //
   //      // Add property and value to non-existent property tree
   //      key = "property12.valueD.value7";
   //      TestObject.addProperty(key, "Value D7");
   //      Assert.assertTrue("Property [" + key + "] not added.", TestObject.hasProperty(key));
   //      Assert.assertEquals("Property added to non-existent prop tree not correct.", "Value D7", TestObject.getProperty(key));
   //   }
   //
   //   @Test
   //   public void testRemoveProperty() {
   //      // Remove property and value
   //      key = "property2.valueC.value2";
   //      TestObject.removeProperty(key);
   //      Assert.assertFalse("Property [" + key + "] not removed.", TestObject.hasProperty(key));
   //   }
   //
   //   @Test(expected = FileNotFoundException.class)
   //   public void testAddPropertiesFromNonExistentFile() throws Exception {
   //      TestObject = new PropertyManager();
   //      TestObject.addPropertiesFromFile("BogusFileName.properties");
   //      Assert.fail("PropertyManager did not throw an exception when a file is not found.");
   //   }
   //
   //   @Test
   //   public void testAddPropertiesFromFile() throws Exception {
   //      // Existing property file....
   //      TestObject = new PropertyManager();
   //      TestObject.addPropertiesFromFile(getConfigFilePath(CONFIG_FILE_NAME));
   //      Assert.assertTrue("No properties in existing property test file.", TestObject.hasProperties());
   //
   //      // Add properties from existing file....
   //      TestObject = getPropertyManager(getConfigFilePath(CONFIG_FILE_NAME));
   //      TestObject.addPropertiesFromFile(getConfigFilePath(OTHER_CONFIG_FILE));
   //
   //      // Test if same property from secondary file overwrites property in destination.
   //      key = "property1.valueA";
   //      Assert.assertTrue("Same property as in destination not added from secondary file.", TestObject.hasProperty(key));
   //      Assert.assertEquals("Property added from secondary file, but not correct.", "Value 23", TestObject.getProperty(key));
   //
   //      // Add new properties from secondary file.
   //      for (int i = 4; i < 7; ++i) {
   //         key = "property2.valueC.value" + i;
   //         Assert.assertTrue("New property [" + key + "] not added from secondary file.", TestObject.hasProperty(key));
   //      }
   //
   //      // Add new property tree from secondary file.
   //      key = "property5.valueR";
   //      Assert.assertTrue("New property tree (root) not added from secondary file.", TestObject.hasProperty(key));
   //
   //      for (int i = 1; i < 4; ++i) {
   //         key = "property5.valueR.value" + i;
   //         Assert.assertTrue("New property tree value [" + key + "]not added from secondary file.", TestObject.hasProperty(key));
   //      }
   //   }
   //
   //   @Test
   //   public void testGetProperty() throws Exception {
   //      String property = "property3.stringValue";
   //      String defaultValue = (String) getRandomDefaultValue(String.class);
   //      String propertyValue = "a string"; // from cps-commons-test.properties
   //      String value;
   //
   //      // Get in-range property value with default.
   //      value = TestObject.getProperty(property, defaultValue);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareStrings();
   //
   //      // Get non-existent property value with default.
   //      value = TestObject.getProperty(BOGUS_PROPERTY_NAME, defaultValue);
   //      setExpectedValue(defaultValue);
   //      setActualValue(value);
   //      compareStrings();
   //
   //      // Get in-range property value without default.
   //      value = TestObject.getProperty(property);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareStrings();
   //
   //      // Get non-existent property value without default.
   //      value = TestObject.getProperty(BOGUS_PROPERTY_NAME);
   //      setExpectedValue(null);
   //      setActualValue(value);
   //      compareStrings();
   //
   //      // Get encrypted in-range property value with default
   //      value = TestObject.getProperty(property + ENC, defaultValue, SECURITY_KEY);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareStrings();
   //
   //      // Get non-existent encrypted property value with default
   //      value = TestObject.getProperty(BOGUS_PROPERTY_NAME, defaultValue, SECURITY_KEY);
   //      setExpectedValue(defaultValue);
   //      setActualValue(value);
   //      compareStrings();
   //
   //      // Get encrypted in-range property value without default
   //      // **** NO CORRESPONDING METHOD ****
   //
   //      // Get non-existent encrypted property value without default.
   //      // **** NO CORRESPONDING METHOD ****
   //   }

   // -------------------------------------------------------------
   //   
   //   private static <T> Object getRandomDefaultValue(Class<T> cls) throws Exception {
   //      if (byte.class.isAssignableFrom(cls)) {
   //         byte[] array = new byte[1];
   //         RANDOM.nextBytes(array);
   //         return array[0];
   //      }
   //      else if (short.class.isAssignableFrom(cls)) {
   //         return (short) RANDOM.nextInt(Short.MAX_VALUE);
   //      }
   //      else if (int.class.isAssignableFrom(cls)) {
   //         return RANDOM.nextInt();
   //      }
   //      else if (long.class.isAssignableFrom(cls)) {
   //         return RANDOM.nextLong();
   //      }
   //      else if (float.class.isAssignableFrom(cls)) {
   //         return RANDOM.nextFloat() * (RANDOM.nextBoolean() ? Float.MAX_VALUE : Float.MIN_VALUE);
   //      }
   //      else if (double.class.isAssignableFrom(cls)) {
   //         return RANDOM.nextDouble();
   //      }
   //      else if (boolean.class.isAssignableFrom(cls)) {
   //         return String.valueOf(RANDOM.nextBoolean());
   //      }
   //      else if (String.class.isAssignableFrom(cls)) {
   //         StringBuilder sb = new StringBuilder();
   //         for (int i = 0; i < RANDOM.nextInt(100); ++i) {
   //            sb.append(Math.min(RANDOM.nextInt(Character.MAX_VALUE), 32));
   //         }
   //         System.out.println("Random string: " + sb);
   //         return sb.toString();
   //      }
   //
   //      throw new Exception("Don't know how to create a random " + cls);
   //   }
   //
   //   @Test
   //   public void testGetPropertyAsByte() throws Exception {
   //      String property = "property3.byteValue";
   //      byte defaultValue = (Byte) getRandomDefaultValue(byte.class);
   //      byte propertyValue = 113; // from cps-commons-test.properties
   //      byte value;
   //
   //      // Get in-range property value with default.
   //      value = TestObject.getPropertyAsByte(property, defaultValue);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareBytes();
   //
   //      // Get non-existent property value with default.
   //      value = TestObject.getPropertyAsByte(BOGUS_PROPERTY_NAME, defaultValue);
   //      setExpectedValue(defaultValue);
   //      setActualValue(value);
   //      compareBytes();
   //
   //      // TODO chokes on non-numeric input
   //      // Get non-byte property value with default.
   //      // key = "property3.stringValue";
   //      // value = props.getPropertyAsByte(key, a);
   //      // Assert.assertTrue("non-byte property [" + key + "] should be should be '97' by default.", value == a);
   //
   //      // Get in-range property value without default.
   //      value = TestObject.getPropertyAsByte(property);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareBytes();
   //
   //      // Get non-existent property value without default.
   //      value = TestObject.getPropertyAsByte(BOGUS_PROPERTY_NAME);
   //      setExpectedValue((byte) 0);
   //      setActualValue(value);
   //      compareBytes();
   //
   //      // TODO chokes on non-numeric input
   //      // Get non-byte property value without default.
   //      // key = "property3.stringValue";
   //      // value = props.getPropertyAsByte(key);
   //      // Assert.assertTrue("non-byte property [" + key + "] should be 0 by default.", value == 0);
   //
   //      // Get encrypted in-range property value with default
   //      value = TestObject.getPropertyAsByte(property + ENC, defaultValue, SECURITY_KEY);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareBytes();
   //
   //      // Get non-existent encrypted property value with default
   //      value = TestObject.getPropertyAsByte(BOGUS_PROPERTY_NAME, defaultValue, SECURITY_KEY);
   //      setExpectedValue(defaultValue);
   //      setActualValue(value);
   //      compareBytes();
   //
   //      // Get encrypted in-range property value without default
   //      value = TestObject.getPropertyAsByte(property + ENC, SECURITY_KEY);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareBytes();
   //
   //      // Get non-existent encrypted property value without default.
   //      value = TestObject.getPropertyAsByte(BOGUS_PROPERTY_NAME, SECURITY_KEY);
   //      setExpectedValue((byte) 0);
   //      setActualValue(value);
   //      compareBytes();
   //   }
   //
   //   @Test
   //   public void testGetPropertyAsShort() throws Exception {
   //      String property = "property3.shortValue";
   //      short defaultValue = (Short) getRandomDefaultValue(short.class);
   //      short propertyValue = -32467; // from cps-commons-test.properties
   //      short value;
   //
   //      // Get in-range property value with default.
   //      value = TestObject.getPropertyAsShort(property, defaultValue);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareShorts();
   //
   //      // Get non-existent property value with default.
   //      value = TestObject.getPropertyAsShort(BOGUS_PROPERTY_NAME, defaultValue);
   //      setExpectedValue(defaultValue);
   //      setActualValue(value);
   //      compareShorts();
   //
   //      // Get in-range property value without default.
   //      value = TestObject.getPropertyAsShort(property);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareShorts();
   //
   //      // Get non-existent property value without default.
   //      value = TestObject.getPropertyAsShort(BOGUS_PROPERTY_NAME);
   //      setExpectedValue((short) 0);
   //      setActualValue(value);
   //      compareShorts();
   //
   //      // Get encrypted in-range property value with default
   //      value = TestObject.getPropertyAsShort(property + ENC, defaultValue, SECURITY_KEY);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareShorts();
   //
   //      // Get non-existent encrypted property value with default
   //      value = TestObject.getPropertyAsShort(BOGUS_PROPERTY_NAME, defaultValue, SECURITY_KEY);
   //      setExpectedValue(defaultValue);
   //      setActualValue(value);
   //      compareShorts();
   //
   //      // Get encrypted in-range property value without default
   //      value = TestObject.getPropertyAsShort(property + ENC, SECURITY_KEY);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareShorts();
   //
   //      // Get non-existent encrypted property value without default.
   //      value = TestObject.getPropertyAsShort(BOGUS_PROPERTY_NAME, SECURITY_KEY);
   //      setExpectedValue((short) 0);
   //      setActualValue(value);
   //      compareShorts();
   //   }
   //
   //   @Test
   //   public void testGetPropertyAsInteger() throws Exception {
   //      String property = "property3.integerValue";
   //      int defaultValue = (Integer) getRandomDefaultValue(int.class);
   //      int propertyValue = 2147443647; // from cps-commons-test.properties
   //      int value;
   //
   //      // Get in-range property value with default.
   //      value = TestObject.getPropertyAsInteger(property, defaultValue);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareIntegers();
   //
   //      // Get non-existent property value with default.
   //      value = TestObject.getPropertyAsInteger(BOGUS_PROPERTY_NAME, defaultValue);
   //      setExpectedValue(defaultValue);
   //      setActualValue(value);
   //      compareIntegers();
   //
   //      // Get in-range property value without default.
   //      value = TestObject.getPropertyAsInteger(property);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareIntegers();
   //
   //      // Get non-existent property value without default.
   //      value = TestObject.getPropertyAsInteger(BOGUS_PROPERTY_NAME);
   //      setExpectedValue(0);
   //      setActualValue(value);
   //      compareIntegers();
   //
   //      // Get encrypted in-range property value with default
   //      value = TestObject.getPropertyAsInteger(property + ENC, defaultValue, SECURITY_KEY);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareIntegers();
   //
   //      // Get non-existent encrypted property value with default
   //      value = TestObject.getPropertyAsInteger(BOGUS_PROPERTY_NAME, defaultValue, SECURITY_KEY);
   //      setExpectedValue(defaultValue);
   //      setActualValue(value);
   //      compareIntegers();
   //
   //      // Get encrypted in-range property value without default
   //      value = TestObject.getPropertyAsInteger(property + ENC, SECURITY_KEY);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareIntegers();
   //
   //      // Get non-existent encrypted property value without default.
   //      value = TestObject.getPropertyAsInteger(BOGUS_PROPERTY_NAME, SECURITY_KEY);
   //      setExpectedValue(0);
   //      setActualValue(value);
   //      compareIntegers();
   //   }
   //
   //   @Test
   //   public void testGetPropertyAsLong() throws Exception {
   //      String property = "property3.longValue";
   //      long defaultValue = (Long) getRandomDefaultValue(long.class);
   //      long propertyValue = 9223312036854775808l; // from cps-commons-test.properties
   //      long value;
   //
   //      // Get in-range property value with default.
   //      value = TestObject.getPropertyAsLong(property, defaultValue);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareLongs();
   //
   //      // Get non-existent property value with default.
   //      value = TestObject.getPropertyAsLong(BOGUS_PROPERTY_NAME, defaultValue);
   //      setExpectedValue(defaultValue);
   //      setActualValue(value);
   //      compareLongs();
   //
   //      // Get in-range property value without default.
   //      value = TestObject.getPropertyAsLong(property);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareLongs();
   //
   //      // Get non-existent property value without default.
   //      value = TestObject.getPropertyAsLong(BOGUS_PROPERTY_NAME);
   //      setExpectedValue(0l);
   //      setActualValue(value);
   //      compareLongs();
   //
   //      // Get encrypted in-range property value with default
   //      value = TestObject.getPropertyAsLong(property + ENC, defaultValue, SECURITY_KEY);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareLongs();
   //
   //      // Get non-existent encrypted property value with default
   //      value = TestObject.getPropertyAsLong(BOGUS_PROPERTY_NAME, defaultValue, SECURITY_KEY);
   //      setExpectedValue(defaultValue);
   //      setActualValue(value);
   //      compareLongs();
   //
   //      // Get encrypted in-range property value without default
   //      value = TestObject.getPropertyAsLong(property + ENC, SECURITY_KEY);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareLongs();
   //
   //      // Get non-existent encrypted property value without default.
   //      value = TestObject.getPropertyAsLong(BOGUS_PROPERTY_NAME, SECURITY_KEY);
   //      setExpectedValue(0l);
   //      setActualValue(value);
   //      compareLongs();
   //   }
   //
   //   @Test
   //   public void testGetPropertyAsFloat() throws Exception {
   //      String property = "property3.floatValue";
   //      float defaultValue = (Float) getRandomDefaultValue(float.class);
   //      float propertyValue = 3.1415926535897932384626433832795f; // from cps-commons-test.properties
   //      float value;
   //
   //      // Get in-range property value with default.
   //      value = TestObject.getPropertyAsFloat(property, defaultValue);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareFloats();
   //
   //      // Get non-existent property value with default.
   //      value = TestObject.getPropertyAsFloat(BOGUS_PROPERTY_NAME, defaultValue);
   //      setExpectedValue(defaultValue);
   //      setActualValue(value);
   //      compareFloats();
   //
   //      // Get in-range property value without default.
   //      value = TestObject.getPropertyAsFloat(property);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareFloats();
   //
   //      // Get non-existent property value without default.
   //      value = TestObject.getPropertyAsShort(BOGUS_PROPERTY_NAME);
   //      setExpectedValue(0f);
   //      setActualValue(value);
   //      compareFloats();
   //
   //      // Get encrypted in-range property value with default
   //      value = TestObject.getPropertyAsFloat(property + ENC, defaultValue, SECURITY_KEY);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareFloats();
   //
   //      // Get non-existent encrypted property value with default
   //      value = TestObject.getPropertyAsFloat(BOGUS_PROPERTY_NAME, defaultValue, SECURITY_KEY);
   //      setExpectedValue(defaultValue);
   //      setActualValue(value);
   //      compareFloats();
   //
   //      // Get encrypted in-range property value without default
   //      value = TestObject.getPropertyAsFloat(property + ENC, SECURITY_KEY);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareFloats();
   //
   //      // Get non-existent encrypted property value without default.
   //      value = TestObject.getPropertyAsShort(BOGUS_PROPERTY_NAME, SECURITY_KEY);
   //      setExpectedValue(0f);
   //      setActualValue(value);
   //      compareFloats();
   //   }
   //
   //   @Test
   //   public void testGetPropertyAsDouble() throws Exception {
   //      String property = "property3.doubleValue";
   //      double defaultValue = (Double) getRandomDefaultValue(double.class);
   //      double propertyValue = 1e308; // from cps-commons-test.properties
   //      double value;
   //
   //      // Get in-range property value with default.
   //      value = TestObject.getPropertyAsDouble(property, defaultValue);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareDoubles();
   //
   //      // Get non-existent property value with default.
   //      value = TestObject.getPropertyAsDouble(BOGUS_PROPERTY_NAME, defaultValue);
   //      setExpectedValue(defaultValue);
   //      setActualValue(value);
   //      compareDoubles();
   //
   //      // Get in-range property value without default.
   //      value = TestObject.getPropertyAsDouble(property);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareDoubles();
   //
   //      // Get non-existent property value without default.
   //      value = TestObject.getPropertyAsDouble(BOGUS_PROPERTY_NAME);
   //      setExpectedValue(0d);
   //      setActualValue(value);
   //      compareDoubles();
   //
   //      // Get encrypted in-range property value with default
   //      value = TestObject.getPropertyAsDouble(property + ENC, defaultValue, SECURITY_KEY);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareDoubles();
   //
   //      // Get non-existent encrypted property value with default
   //      value = TestObject.getPropertyAsDouble(BOGUS_PROPERTY_NAME, defaultValue, SECURITY_KEY);
   //      setExpectedValue(defaultValue);
   //      setActualValue(value);
   //      compareDoubles();
   //
   //      // Get encrypted in-range property value without default
   //      value = TestObject.getPropertyAsDouble(property + ENC, SECURITY_KEY);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareDoubles();
   //
   //      // Get non-existent encrypted property value without default.
   //      value = TestObject.getPropertyAsDouble(BOGUS_PROPERTY_NAME, SECURITY_KEY);
   //      setExpectedValue(0d);
   //      setActualValue(value);
   //      compareDoubles();
   //   }
   //
   //   @Test
   //   public void testGetPropertyAsBoolean() throws Exception {
   //      String property = "property3.booleanValue";
   //      String defaultValue = (String) getRandomDefaultValue(boolean.class);
   //      boolean propertyValue = true; // from cps-commons-test.properties
   //      boolean value;
   //
   //      // Get in-range property value with default.
   //      value = TestObject.getPropertyAsBoolean(property, defaultValue);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareBooleans();
   //
   //      // Get non-existent property value with default.
   //      value = TestObject.getPropertyAsBoolean(BOGUS_PROPERTY_NAME, defaultValue);
   //      setExpectedValue(Boolean.valueOf(defaultValue));
   //      setActualValue(value);
   //      compareBooleans();
   //
   //      // Get non-boolean property value with default.
   //      value = TestObject.getPropertyAsBoolean(property.replace("boolean", "int"), defaultValue);
   //      setExpectedValue(Boolean.valueOf(defaultValue));
   //      setActualValue(value);
   //      compareBooleans();
   //
   //      // Get in-range property value without default.
   //      value = TestObject.getPropertyAsBoolean(property);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareBooleans();
   //
   //      // Get non-existent property value without default.
   //      value = TestObject.getPropertyAsBoolean(BOGUS_PROPERTY_NAME);
   //      setExpectedValue(false);
   //      setActualValue(value);
   //      compareBooleans();
   //
   //      // Get non-boolean property value without default.
   //      key = "property3.stringValue";
   //      value = TestObject.getPropertyAsBoolean(key);
   //      Assert.assertFalse("non-boolean property [" + key + "] should be false.", value);
   //
   //      // Get encrypted in-range property value with default
   //      value = TestObject.getPropertyAsBoolean(property + ENC, defaultValue, SECURITY_KEY);
   //      setExpectedValue(propertyValue);
   //      setActualValue(value);
   //      compareBooleans();
   //
   //      // Get non-existent encrypted property value with default
   //      value = TestObject.getPropertyAsBoolean(BOGUS_PROPERTY_NAME, defaultValue, SECURITY_KEY);
   //      setExpectedValue(Boolean.valueOf(defaultValue));
   //      setActualValue(value);
   //      compareBooleans();
   //
   //      // Get encrypted in-range property value without default
   //      // **** NO CORRESPONDING METHOD ****
   //
   //      // Get non-existent encrypted property value without default.
   //      // **** NO CORRESPONDING METHOD ****
   //   }
   //
   private static PropertyManager getPropertyManager(String filename) {
      PropertyManager props = new PropertyManager();
      try {
         props.addPropertiesFromFile(filename);
      }
      catch (FileNotFoundException ex) {
         Assert.fail(ex.getMessage());
      }
      return props;
   }
   //
   //   private static String getConfigFilePath(String fileName) {
   //      URL url = PropertyManagerTest.class.getResource("/" + fileName);
   //      try {
   //         return new File(url.toURI()).getAbsolutePath();
   //      }
   //      catch (URISyntaxException e) {
   //         throw new IllegalArgumentException(e);
   //      }
   //   }
}
