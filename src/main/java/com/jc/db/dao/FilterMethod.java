package com.jc.db.dao;

import java.util.HashMap;
import java.util.Map;

public enum FilterMethod {
   MATCH_FROM_START(0), //
   MATCH_ANYWHERE(1),
   MATCH_FROM_END(2),
   MATCH_EXACT(3),
   MATCH_OTHER_THAN(4),

   IN_LIST(5),
   NOT_IN_LIST(6),

   BETWEEN(7),
   NOT_BETWEEN(8),

   LESSTHAN(9),
   GREATERTHAN(10),
   LESSTHAN_EQUALTO(11),
   GREATERTHAN_EQUALTO(12),

   IS_NULL(13),
   IS_NOT_NULL(14),

   AND_FIELD_GROUP(15),
   OR_FIELD_GROUP(16),

   DOES_NOT_MATCH_FROM_START(17),
   DOES_NOT_MATCH_ANYWHERE(18),
   DOES_NOT_MATCH_FROM_END(19);

   private int code;
   private static Map<Integer, FilterMethod> codeToStatusMapping;

   private FilterMethod(int code) {
      this.code = code;
   }

   public int getCode() {
      return code;
   }

   public static FilterMethod getFilterMethod(int i) {
      if (codeToStatusMapping == null) {
         initMapping();
      }
      return codeToStatusMapping.get(i);
   }

   private static void initMapping() {
      codeToStatusMapping = new HashMap<>();
      for (FilterMethod s : values()) {
         codeToStatusMapping.put(s.code, s);
      }
   }

   public boolean isANDedMethodType() {
      return (equals(AND_FIELD_GROUP));
   }

   public boolean isNull() {
      return (equals(IS_NULL));
   }

   public boolean isNotNull() {
      return (equals(IS_NOT_NULL));
   }

   public boolean isListMethod() {
      return (equals(IN_LIST) || equals(NOT_IN_LIST));
   }

   public boolean isLikeMethod() {
      return (equals(MATCH_FROM_START) || equals(MATCH_ANYWHERE) || equals(MATCH_FROM_END));
   }

   public boolean isBetweenMethod() {
      return (equals(BETWEEN) || equals(NOT_BETWEEN));
   }
}
