package com.jc.app.rest;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

public class GenericMap implements ParameterizedType {

   private final Class<?> keyWrapper;
   private final Class<?> valueWrapper;

   public GenericMap(Class<?> keyWrapper, Class<?> valueWrapper) {
      this.keyWrapper = keyWrapper;
      this.valueWrapper = valueWrapper;
   }

   @Override
   public Type[] getActualTypeArguments() {
      return new Type[] {
            keyWrapper, valueWrapper
      };
   }

   @Override
   public Type getRawType() {
      return Map.class;
   }

   @Override
   public Type getOwnerType() {
      return null;
   }
}
