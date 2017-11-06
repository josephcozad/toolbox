package com.jc;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import com.jc.util.ConfigInfo;
import com.jc.util.FileSystem;

public class FakeDataGenerator {

   public final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 2017-02-25 11:27:44

   public static String FAKEDATADIR;

   private final static Random NumberGen = new Random(System.currentTimeMillis());

   private final static String[][] STREET_TYPE = {
         {
               "Road", "Rd."
         }, {
               "Lane", "Ln."
         }, {
               "Avenue", "Ave."
         }, {
               "Place", "Pl."
         }, {
               "Boulevard", "Blvd."
         }, {
               "Parkway", "Pkwy."
         }, {
               "Court", "Ct."
         }, {
               "Circle", "Cir."
         }, {
               "Street", "St."
         }, {
               "Drive", "Dr."
         }, {
               "Highway", "Hwy."
         }, {
               "Expressway", "Expy."
         }
   };

   private final static String[][] STREET_DIRECTION = {
         {
               "North", "N."
         }, {
               "South", "S"
         }, {
               "East", "E."
         }, {
               "West", "W."
         }, {
               "Northwest", "N.W."
         }, {
               "Northeast", "N.E."
         }, {
               "Southwest", "S.W."
         }, {
               "Southeast", "S.E."
         }
   };

   private static List<String> firstNamesFemaleList;
   private static List<String> firstNamesMaleList;
   private static List<String> lastNameList;
   private static List<CityData> cityDataList;
   private static List<String> streetNamesList;

   private static Map<String, String> uniqueNamesMap = new HashMap<>();

   static {
      try {
         ConfigInfo configInfo = ConfigInfo.getInstance();
         if (configInfo.hasProperty(CommonsTestUtils.TEST_RESOURCE_DIR)) {
            String testResourceDir = configInfo.getProperty(CommonsTestUtils.TEST_RESOURCE_DIR);
            FAKEDATADIR = testResourceDir + "\\fakedata\\";
         }
         else {
            System.err.println("WARNING: no fakedata directory has been intialized because no value could be found in ConfigInfo for property: "
                  + CommonsTestUtils.TEST_RESOURCE_DIR);
         }
      }
      catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public static boolean runRandom(double percentage) {
      boolean runRandom = false;
      if (percentage > 0.5) {
         percentage = 1.0 - percentage;
         int value = (int) Math.round((100 / percentage) / 100);
         if (getRandomInteger(1, 10000) % value != 0) {
            runRandom = true;
         }
      }
      else {
         int value = (int) Math.round((100 / percentage) / 100);
         if (getRandomInteger(1, 10000) % value == 0) {
            runRandom = true;
         }
      }
      return runRandom;
   }

   /*
    * Returns an integer between the supplied lower_limit and the upper_limit. The returned
    * value can include the lower_limit and upper_limit value.
    */
   public static int getRandomInteger(int lowerLimit, int upperLimit) {
      int number = -1;
      if (lowerLimit < upperLimit) {
         boolean found = false;
         while (!found) {
            number = NumberGen.nextInt(upperLimit);
            if ((number >= lowerLimit) && (number <= upperLimit)) {
               found = true;
            }
         }
      }
      return (number);
   }

   // if lowerLimit == null, then upperLimit or before...
   // if upperLimit == null, then lowerLimit or after...
   // otherwise from lowerLimit to upperLimit, inclusive...
   public static Date generateRandomDate(Date lowerLimit, Date upperLimit) {
      return new Date();
   }

   public static String generateRandomString(int length) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < length; i++) {
         char c = ' ';
         if (runRandom(0.5d)) { // run 50% of the time...
            c = (char) (NumberGen.nextInt(26) + 'A');
         }
         else {
            c = (char) (NumberGen.nextInt(26) + 'a');
         }
         sb.append(c);
      }
      return sb.toString();
   }

   public static String generateRandomNonsenseSentence(int length) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < length; i++) {
         char c = ' ';
         if (runRandom(0.5d)) { // run 50% of the time...
            c = (char) (NumberGen.nextInt(26) + 'A');
         }
         else {
            c = (char) (NumberGen.nextInt(26) + 'a');
         }
         sb.append(c);
      }
      sb.append(".");
      return sb.toString();
   }

   //   public static Date[] getDateRange() { //"2017-02-25 11:27:44"
   //      String year = "" + getRandomInteger(2010, 2016);
   //      String month = "" + getRandomInteger(1, 12);
   //      String date = "" + getRandomInteger(1, 28);
   //      String hour = "" + getRandomInteger(1, 24);
   //      String min = "" + getRandomInteger(1, 59);
   //      String sec = "" + getRandomInteger(1, 59);
   //
   //   }

   // -----------------------------------------------------------------------

   public static String generateAreaCode() {
      String areaCode;
      do {
         areaCode = "" + getRandomInteger(100, 999);
         int middleValue = getRandomInteger(0, 2);
         areaCode = areaCode.charAt(0) + ("" + middleValue) + areaCode.charAt(2);
      } while (areaCode.equals("800"));

      return areaCode;
   }

   public static String generatePhoneNumber() {
      String exchangeCode = "" + getRandomInteger(111, 999);
      String subscriberNumber = "" + getRandomInteger(1111, 9999);
      return exchangeCode + "-" + subscriberNumber;
   }

   public static String generateMiddleInitial() {
      char initial = (char) (NumberGen.nextInt(26) + 'A');
      String value = initial + ".";
      return value;
   }

   public static NameData getUniqueFirstLast() {
      boolean male = true;
      if (runRandom(0.5)) {
         male = false;
      }

      NameData nameInfo;
      if (male) {
         nameInfo = getUniqueFirstLastMale();
      }
      else {
         nameInfo = getUniqueFirstLastFemale();
      }
      return nameInfo;
   }

   public static NameData getUniqueFirstLastMale() {
      NameData nameInfo = new NameData();
      nameInfo.male = true;

      int numRecs = lastNameList.size() - 1;
      int index = getRandomInteger(0, numRecs);
      nameInfo.last = lastNameList.get(index);

      numRecs = firstNamesMaleList.size() - 1;
      index = getRandomInteger(0, numRecs);
      nameInfo.first = firstNamesMaleList.get(index);

      if (FakeDataGenerator.runRandom(0.25)) { // ~25% of the time.
         nameInfo.middle = FakeDataGenerator.generateMiddleInitial();
      }

      String middle = nameInfo.middle != null ? nameInfo.middle : "";

      String key = nameInfo.first + "|" + middle + "|" + nameInfo.last;
      if (uniqueNamesMap.containsKey(key)) {
         boolean found = false;
         while (!found) {
            index = getRandomInteger(0, numRecs);
            nameInfo.first = firstNamesMaleList.get(index);

            key = nameInfo.first + "|" + middle + "|" + nameInfo.last;
            found = uniqueNamesMap.containsKey(key);
         }
      }

      uniqueNamesMap.put(key, "");

      return nameInfo;
   }

   public static NameData getUniqueFirstLastFemale() {
      NameData nameInfo = new NameData();
      nameInfo.male = false;

      int numRecs = lastNameList.size() - 1;
      int index = getRandomInteger(0, numRecs);
      nameInfo.last = lastNameList.get(index);

      numRecs = firstNamesFemaleList.size() - 1;
      index = getRandomInteger(0, numRecs);
      nameInfo.first = firstNamesFemaleList.get(index);

      String key = nameInfo.first + "|" + nameInfo.last;
      if (uniqueNamesMap.containsKey(key)) {
         boolean found = false;
         while (!found) {
            index = getRandomInteger(0, numRecs);
            nameInfo.first = firstNamesFemaleList.get(index);

            key = nameInfo.first + "|" + nameInfo.last;
            found = uniqueNamesMap.containsKey(key);
         }
      }

      uniqueNamesMap.put(key, "");

      return nameInfo;
   }

   public static CityData getRandomCityData() {
      int numRecs = cityDataList.size() - 1;
      int index = getRandomInteger(0, numRecs);
      CityData cityData = cityDataList.get(index);
      return cityData;
   }

   public static String[] getStreetAddress() {
      String[] streetInfo = new String[2];

      int streetNumber = getRandomInteger(50, 15999);

      String direction = "";
      int dirLocation = 99;
      if (runRandom(0.2)) { // ~20% of the time.
         int index = getRandomInteger(0, STREET_DIRECTION.length - 1);
         int subIndex = 99;
         if (runRandom(0.5)) {
            subIndex = 0;
         }
         else {
            subIndex = 1;
         }
         direction = STREET_DIRECTION[index][subIndex];
         if (runRandom(0.5)) {
            dirLocation = 0;
         }
         else {
            dirLocation = 1;
         }
      }

      int numRecs = streetNamesList.size() - 1;
      int index = getRandomInteger(0, numRecs);
      String streetName = streetNamesList.get(index);

      index = getRandomInteger(0, STREET_TYPE.length - 1);
      int subIndex = getRandomInteger(0, 1);
      String streetType = STREET_TYPE[index][subIndex];

      StringBuilder sb = new StringBuilder();
      sb.append(streetNumber + " ");
      if (!direction.isEmpty()) {
         if (dirLocation == 0) { // Before street name...
            sb.append(direction + " ");
         }
         sb.append(streetName + " ");
         sb.append(streetType + " ");
         if (dirLocation == 1) { // After street name...
            sb.append(direction + " ");
         }
      }
      else {
         sb.append(streetName + " ");
         sb.append(streetType + " ");
      }
      streetInfo[0] = sb.toString();
      streetInfo[0] = streetInfo[0].trim();

      if (runRandom(0.12)) { // ~12% of the time;
         int suiteNumber = getRandomInteger(100, 999);
         streetInfo[1] = "Suite " + suiteNumber;
         streetInfo[1] = streetInfo[1].trim();

      }
      else {
         streetInfo[1] = null;
      }

      return streetInfo;
   }

   public static void loadFakeData() throws IOException {
      firstNamesFemaleList = new ArrayList<>();
      String firstNamesFemaleFile = "firstNamesFemale.txt";
      String[] lines = FileSystem.readInFile(FAKEDATADIR + firstNamesFemaleFile);
      for (int i = 1; i < lines.length; i++) {
         String line = lines[i].trim();
         firstNamesFemaleList.add(line);
      }

      firstNamesMaleList = new ArrayList<>();
      String firstNamesMaleFile = "firstNamesMale.txt";
      lines = FileSystem.readInFile(FAKEDATADIR + firstNamesMaleFile);
      for (int i = 1; i < lines.length; i++) {
         String line = lines[i].trim();
         firstNamesMaleList.add(line);
      }

      lastNameList = new ArrayList<>();
      String lastNameFile = "lastNames.txt";
      lines = FileSystem.readInFile(FAKEDATADIR + lastNameFile);
      for (int i = 1; i < lines.length; i++) {
         String line = lines[i].trim();
         lastNameList.add(line);
      }

      streetNamesList = new ArrayList<>();
      String streetNamesFile = "streets.txt";
      lines = FileSystem.readInFile(FAKEDATADIR + streetNamesFile);
      for (int i = 1; i < lines.length; i++) {
         String line = lines[i].trim();
         streetNamesList.add(line);
      }

      cityDataList = new ArrayList<>();
      String cityDataFile = "cities.txt";
      lines = FileSystem.readInFile(FAKEDATADIR + cityDataFile);
      for (int i = 1; i < lines.length; i++) {
         String line = lines[i].trim();
         String[] values = line.split("\\|", -1);
         CityData data = new CityData();
         data.city = values[0];
         data.state = values[1];
         data.zip = values[2];

         cityDataList.add(data);
      }
   }

   @Test
   @Ignore
   public void testFakeDataGen() throws Exception {
      FakeDataGenerator.loadFakeData();

      List<String> uniqueNames = new ArrayList<>();
      int uniqueMatches = 0;

      for (int i = 0; i < 100000; i++) {
         String[] streetInfo = FakeDataGenerator.getStreetAddress();
         //        System.out.println(streetInfo[0]);
         if (uniqueNames.contains(streetInfo[0])) {
            uniqueMatches++;
            if (uniqueMatches == 1) { // 13
               System.out.println(i);
               break;
            }
         }
         uniqueNames.add(streetInfo[0]);
      }
   }

   @Test
   @Ignore
   public void loadFakeDataFile() throws Exception {

      List<String> dataList = new ArrayList<>();

      String outDataFile = "streets.txt";
      String dataFile = "streeDataRaw.txt";
      String[] lines = FileSystem.readInFile(FakeDataGenerator.FAKEDATADIR + dataFile);

      String[] columns = lines[0].split("\\|", -1);
      int numColumns = columns.length;

      List<String> uniqueNames = new ArrayList<>();

      for (int i = 1; i < lines.length; i++) {
         String line = lines[i].trim();
         String[] values = line.split("\\|", -1);
         if (values.length < numColumns) {
            i++;
            String nextLine = lines[i].trim();
            line += nextLine;
            values = line.split("\\|", -1);
         }

         if (values.length == numColumns) {
            // ----------------------------------------------------

            String streetName = values[0].trim();
            if (!uniqueNames.contains(streetName)) {
               dataList.add(streetName);
            }

            // ----------------------------------------------------
         }
         else {
            System.out.println("Skipped[" + dataFile + ":" + i + "]  numParsedValues[" + values.length + "] != numColumns[" + numColumns + "].");
            System.out.println("       " + line);
         }
      }

      String resultsFile = FakeDataGenerator.FAKEDATADIR + outDataFile;
      StringBuilder content = new StringBuilder();
      for (String line : dataList) {
         content.append(line + FileSystem.NEWLINE);
      }

      FileSystem.writeContentOutToFile(resultsFile, content);

      System.out.println("Done..... results file at: " + resultsFile);
   }

   public static class CityData {

      public String city;
      public String state;
      public String zip;
   }

   public static class NameData {

      public String first;
      public String last;
      public String middle;
      public boolean male;
   }
}
