package com.jc.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public enum LogFieldsEnum {

   NANO_SECS(0), //
   DATE_STAMP(1),
   LOG_LEVEL(2),
   CLASS_FIELD_INFO(3),
   MESSAGE(4),
   UNKNOWN(5);

   private int code = 0;

   LogFieldsEnum(int code) {
      this.code = code;
   }

   public static LogFieldsEnum get(String value) {
      value = value.toUpperCase();
      if (NANO_SECS.toString().equals(value)) {
         return NANO_SECS;
      }
      else if (DATE_STAMP.toString().equals(value)) {
         return DATE_STAMP;
      }
      else if (LOG_LEVEL.toString().equals(value)) {
         return LOG_LEVEL;
      }
      else if (CLASS_FIELD_INFO.toString().equals(value)) {
         return CLASS_FIELD_INFO;
      }
      else if (MESSAGE.toString().equals(value)) {
         return MESSAGE;
      }
      else {
         return UNKNOWN;
      }
   }

   public static int getNumberOfFields() {
      return LogFieldsEnum.values().length - 1; // - 1 because UNKNOWN is not a field value
   }

   public List<LogFieldsEnum> getOrderedListOfFields() {
      LogFieldsEnum[] values = LogFieldsEnum.values();
      List<LogFieldsEnum> valuesList = new ArrayList<>(Arrays.asList(values));
      valuesList.remove(LogFieldsEnum.UNKNOWN);

      // sort based by enum code...
      Collections.sort(valuesList, new Comparator<LogFieldsEnum>() {

         @Override
         public int compare(LogFieldsEnum enum1, LogFieldsEnum enum2) {
            int code1 = enum1.getCode();
            int code2 = enum2.getCode();

            if (code1 == code2) {
               return 0;
            }
            else {
               if (code1 > code2) {
                  return 1; // o1 is higher priority than o2
               }
               else {
                  return -1; // o1 is lower priority than o2
               }
            }
         }
      });

      return valuesList;
   }

   public int getCode() {
      return code;
   }

   public boolean equals(LogFieldsEnum enumObj) {
      return code == enumObj.getCode();
   }

   public static LogFieldsEnum fromCode(final int code) {
      for (LogFieldsEnum t : LogFieldsEnum.values()) {
         if (t.code == code) {
            return t;
         }
      }
      return null;
   }
}
