package com.jc.app.rest;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class GenericList implements ParameterizedType {

   private final Class<?> wrapped;

   public GenericList(Class<?> wrapper) {
      this.wrapped = wrapper;
   }

   @Override
   public Type[] getActualTypeArguments() {
      return new Type[] {
         wrapped
      };
   }

   @Override
   public Type getRawType() {
      return List.class;
   }

   @Override
   public Type getOwnerType() {
      return null;
   }
}
