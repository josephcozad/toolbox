package com.jc.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The StringParser is a class of static methods that provides string parsing functionality.
 * 
 * @author Joseph Cozad
 * @version 1.0, 12/01/98
 */

public final class StringUtils {

   public static final String EMPTY = "";

   private static final Random NumGen = new Random(System.currentTimeMillis());
   private final static Pattern EMAIL_VALIDATION_PATTERN = Pattern.compile(
         "(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*:(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)(?:,\\s*(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*))*)?;\\s*)");

   public final static String STRINGS_VECTOR = "strings";
   public final static String PATTERNS_VECTOR = "patterns";

   private StringUtils() {}

   public static String formatPhoneNumber(String phoneNumber) throws Exception {
      if (phoneNumber.length() == 10) {
         phoneNumber = phoneNumber.replaceFirst("(\\d{3})(\\d{3})(\\d+)", "($1) $2-$3");
      }
      else if (phoneNumber.length() > 10) {
         phoneNumber = phoneNumber.replaceFirst("(\\d{3})(\\d{3})(\\d{4})(\\d+)", "($1) $2-$3 ext.$4");
      }
      else {
         throw new Exception("Error while trying to format a phonumber, the number '" + phoneNumber + "' is invalid length of '" + phoneNumber.length()
               + "', expected 10 digits or more.");
      }
      return phoneNumber;
   }

   public static boolean isEmailValid(String email) {
      return EMAIL_VALIDATION_PATTERN.matcher(email).matches();
   }

   /**
    * Returns an integer between the supplied lower_limit and the upper_limit. The returned value can include the lower_limit and upper_limit value.
    */
   public static int getRandomInteger(int lower_limit, int upper_limit) {
      int number = -1;
      if (lower_limit < upper_limit) {
         boolean found = false;
         while (!found) {
            number = NumGen.nextInt(upper_limit);
            if ((number >= lower_limit) && (number <= upper_limit)) {
               found = true;
            }
         }
      }
      return (number);
   }

   public static String lowerCaseFirstChar(String textStr) {
      char c[] = textStr.toCharArray();
      c[0] += 32;
      textStr = new String(c);
      return textStr;
   }

   public static String toInitialCap(String textStr) {
      if (textStr == null || textStr.isEmpty()) {
         return textStr;
      }

      textStr = textStr.toLowerCase();

      char[] buffer = textStr.toCharArray();
      boolean capitalizeNext = true;
      for (int i = 0; i < buffer.length; i++) {
         char ch = buffer[i];
         if (Character.isWhitespace(ch)) {
            capitalizeNext = true;
         }
         else if (capitalizeNext) {
            buffer[i] = Character.toTitleCase(ch);
            capitalizeNext = false;
         }
      }
      return new String(buffer);
   }

   /**
    * Converts the supplied byte array of file contents from a UNIX format file to one that can be read correctly by a Windows machine.
    */
   public static byte[] convertFromUnixToWindows(byte[] content) {
      String string = new String(content);

      if (string.contains(FileSystem.NEWLINE)) {
         return content;
      }

      String[] lines = string.split(String.valueOf((char) 10));
      StringBuilder sb = new StringBuilder();

      for (String line : lines) {
         sb.append(line).append(FileSystem.NEWLINE);
      }

      return sb.toString().getBytes();
   }

   /**
    * Returns a String representing the supplied number of spaces.
    */
   public static String getSpaces(int count) {
      return getSpacer(count, " ");
   }

   /**
    * Returns a String representing the supplied number of spaces using the supplied space_value.
    */
   public static String getSpacer(int count, String spaceValue) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < count; i++) {
         sb.append(spaceValue);
      }
      return sb.toString();
   }

   /**
    * Takes the supplied base(10) number converts it to a representation in the supplied base value.
    */
   public static String convertDecimalToBase(int number, int base) throws Exception {
      String trnas_value = "";
      int remainder = number;

      if (number >= base) {
         int value = number / base;
         trnas_value = convertDecimalToBase(value, base);
         remainder = number % base;
      }

      if (remainder > 9) {
         remainder -= 10;
         if (remainder > 26) {
            remainder -= 26;
            if (remainder > 26) {
               throw (new Exception("Error!!! Help me!."));
            }
            else {
               remainder += 97; // 97 is ASCII char a.
            }
         }
         else {
            remainder += 65; // 65 is ASCII char A.
         }
      }
      else {
         remainder += 48; // 48 is ASCII char 0.
      }
      return (trnas_value + ((char) remainder));
   }

   /**
    * Takes the supplied string array and partitions it into a 'partion_size' number of string array objects so that each new set of string arrays has a number of string elements equal to that of the supplied partion_size.
    */
   //   public static String[][] partitionStringArray(String[] strings, int partion_size) {
   //      if (strings == null || partion_size < 1) {
   //         return new String[0][];
   //      }
   //      else if (partion_size == 1) {
   //         Collection<String[]> c = new ArrayList<String[]>(strings.length);
   //         for (String s : strings) {
   //            c.add(new String[] {
   //               s
   //            });
   //         }
   //         return ArrayManager.toDoubleStringArray(c);
   //      }
   //      else if (strings.length <= partion_size) {
   //         return new String[][] {
   //            strings
   //         };
   //      }
   //
   //      int partitionCount = strings.length / partion_size;
   //
   //      if (strings.length % partion_size > 0) {
   //         partitionCount++;
   //      }
   //
   //      int start = 0;
   //      int end = partion_size;
   //      Collection<String[]> partitions = new NonNullList<String[]>();
   //
   //      for (int i = 0; i < partitionCount; i++) {
   //         String[] array = Arrays.copyOfRange(strings, start, Math.min(end, strings.length));
   //
   //         if (i + 1 < partitionCount) {
   //            start = end;
   //            end = end + partion_size;
   //         }
   //         else { // last one.
   //            Collection<String> c = Arrays.asList(array);
   //            array = ArrayManager.toStringArray(c);
   //         }
   //
   //         partitions.add(array);
   //      }
   //
   //      return ArrayManager.toDoubleStringArray(partitions);
   //   }

   /**
    * Returns all the words in a string that are all upper case.
    */
   public static List<String> getAllUpperCaseWords(String string) {
      if (string == null || string.isEmpty()) {
         return Collections.emptyList();
      }

      Matcher m = Pattern.compile("\\b[A-Z][A-Z0-9]*\\b").matcher(string);
      List<String> matches = new ArrayList<>();

      while (m.find()) {
         matches.add(m.group());
      }

      return (matches);
   }

   /**
    * Tests to see if the supplied String value is an integer value.
    */
   public static boolean isInteger(String value) {
      boolean int_value = false;
      try {
         Integer.parseInt(value);
         int_value = true;
      }
      catch (NumberFormatException ex) {}
      return (int_value);
   }

   /**
    * Tests to see if the supplied String value is an integer value.
    */
   public static boolean isLong(String value) {
      boolean int_value = false;
      try {
         Long.parseLong(value);
         int_value = true;
      }
      catch (NumberFormatException ex) {}
      return (int_value);
   }

   /**
    * Tests to see if the supplied String value is a double value.
    */
   public static boolean isDouble(String value) {
      boolean double_value = false;
      try {
         Double.parseDouble(value);
         double_value = true;
      }
      catch (NumberFormatException ex) {}
      return (double_value);
   }

   /**
    * Tests to see if the supplied String value is a Boolean value.
    */
   public static boolean isBoolean(String value) {
      boolean boolean_value = false;
      if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
         boolean_value = true;
      }
      return (boolean_value);
   }

   /**
    * Trims all the strings in the supplied array.
    */
   public static String[] trim(String[] strings) {
      if (strings != null) {
         for (int i = 0; i < strings.length; ++i) {
            if (strings[i] != null) {
               strings[i] = strings[i].trim();
            }
         }
      }
      return strings;
   }

   public static <T> String arrayToString(T[] array, char sep) {
      return arrayToString(array, String.valueOf(sep));
   }

   public static <T> String arrayToString(T[] array, String sep) {
      List<T> alist = Arrays.asList(array);
      return (collectionToString(alist, sep));
   }

   public static <T> String collectionToString(Collection<T> c, char sep) {
      return collectionToString(c, String.valueOf(sep));
   }

   public static <T> String collectionToString(Collection<T> c, String sep) {
      if (c == null) {
         return EMPTY;
      }
      StringBuilder sb = new StringBuilder();
      for (T item : c) {
         if (sb.length() > 0) {
            sb.append(sep);
         }
         sb.append(item);
      }
      return sb.toString();
   }

   // --------------------------------------------------------

   /**
    * Parses a string of text based on a supplied token string.
    * 
    * @param text
    *           The text string to parse.
    * @param token
    *           The token to use to parse the text string.
    * @return An array of strings that are the result of the parsing. If the first element of the array is empty then the token occurred at the very begining of the string parsed. Likewise if the last element of the string array is empty then the token occured at the very end of the string parsed. Strings contained in the array do not contain the token.
    */
   public static String[] parseString(String text, String token) {
      return (StringUtils.parseString(text, token, false, 0, true));
   }

   public static String[] parseString(String text, String token, boolean trim) {
      return (StringUtils.parseString(text, token, trim, 0, true));
   }

   /**
    * Splits a string into two strings based on the supplied token.
    * 
    * @param text
    *           The text string to split.
    * @param token
    *           The token to use to parse the text string.
    * @return An array of two strings that are the result of the splitting. If the first element of the array is empty then the token occurred at the very begining of the string parsed. Likewise if the last element of the string array is empty then the token occured at the very end of the string.
    */
   public static String[] splitString(String text, String token) {
      return (StringUtils.parseString(text, token, false, 0, false));
   }

   public static String[] splitString(String text, String token, boolean trim) {
      return (StringUtils.parseString(text, token, trim, 0, false));
   }

   /**
    * Parses a string based on a begining token and an ending token.
    * 
    * @param text
    *           The text string to parse.
    * @param beginToken
    *           The token to use to parse the text string.
    * @param endToken
    *           The token to use to parse the text string.
    * @return Text between every occurance of begining and ending tokens are returned in a java.util.Vector of patterns, and the text laying outside of these tokens are returned in a java.util.Vector of strings. These two vectors are returned in a java.util.Hashtable, keyed on "patterns" and "strings".
    */
   public static HashMap<String, ArrayList<String>> parseString(String text, String beginToken, String endToken, boolean save_tokens) {
      ArrayList<String> strings = new ArrayList<>();
      ArrayList<String> patterns = new ArrayList<>();
      String[] tmpResults = new String[2];
      boolean done = false;

      tmpResults[1] = text;
      while (!done) {
         tmpResults = StringUtils.splitString(tmpResults[1], beginToken);
         if (tmpResults != null && tmpResults.length > 0) {
            strings.add(tmpResults[0]);
            if (tmpResults.length > 1) {
               tmpResults = StringUtils.splitString(tmpResults[1], endToken);
               if (tmpResults != null && tmpResults.length > 0) {
                  if (save_tokens) {
                     patterns.add(beginToken + tmpResults[0] + endToken);
                  }
                  else {
                     patterns.add(tmpResults[0]);
                  }
               }
            }
         }

         if (tmpResults == null || tmpResults.length < 2 || tmpResults[1].equals("")) {
            done = true;
         }
      }

      HashMap<String, ArrayList<String>> result = new HashMap<>();
      result.put(STRINGS_VECTOR, strings);
      result.put(PATTERNS_VECTOR, patterns);
      return (result);
   }

   public static HashMap<String, ArrayList<String>> parseString(String text, String beginToken, String endToken) {
      return (parseString(text, beginToken, endToken, false));
   }

   private static String[] parseString(String text, String token, boolean trim, int times, boolean recurse) {
      String[] result;
      String part1, part2;
      int tknlen = token.length();
      int begin = 0;
      int end = text.indexOf(token, begin);

      if (end > -1) {
         part1 = text.substring(begin, end);
         begin = end + tknlen;
         part2 = text.substring(begin, text.length());
         if (trim) {
            part1 = part1.trim();
            part2 = part2.trim();
         }

         if (recurse) {
            result = StringUtils.parseString(part2, token, trim, times + 1, true);
            text = part1;
            result[times] = text;
            return (result);
         }
         else {
            result = new String[2];
            result[0] = part1;
            result[1] = part2;
            return (result);
         }
      }
      else {
         result = new String[times + 1];
         result[times] = text;
         return (result);
      }
   }
}
