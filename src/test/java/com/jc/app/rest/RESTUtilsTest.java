package com.jc.app.rest;

import static org.junit.Assert.fail;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.jc.CommonsTestUtils;

public class RESTUtilsTest {

   @Rule
   public final ExpectedException exceptionThrown = ExpectedException.none();

   //   private final String datasource = HISPEnum.JCTST.getDatasource();

   @BeforeClass
   public static void setUp() throws Exception {
      try {
         CommonsTestUtils.loadConfigInfo();
      }
      catch (Exception ex) {
         Assert.fail(ex.getMessage());
      }
   }

   // -------------------------------------------------------------------------------------

   @Test
   //   @Ignore
   public void testCreateObjectFromJSON() throws Exception {

      RESTDataTestObject testData = new RESTDataTestObject();
      testData.init();

      // testing setting long with an "int" value
      //   testData.setId(123l);

      // testing with empty map object
      //   Map<String, Object> emptyMap = new HashMap<String, Object>();
      //  testData.setMapVar(emptyMap);

      // testing with emtpy list object
      //List<String> emptyList = new ArrayList<String>();
      //testData.setListVar(emptyList);

      JSONObject jsonObj = RESTUtils.createJSONObjectFromObject(testData);

      RESTDataTestObject dataObj = RESTUtils.createObjectFromJSONObject(jsonObj, RESTDataTestObject.class);

      if (!dataObj.equals(testData)) {
         fail("Inflated data object did not equal test data object.");
      }
   }
}
