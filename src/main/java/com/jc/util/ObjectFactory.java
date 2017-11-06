package com.jc.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class ObjectFactory {

   private ObjectFactory() {}

   /**
    * Gets an instance of the class name supplied.
    *
    * @param className
    *           The name of the class for which an instance object will be created.
    * @return Returns an instance Object of the supplied class name.
    * @throws ObjectFactoryException
    */
   public static Object getInstance(String className) throws ObjectFactoryException {
      return getInstance(className, null);
   }

   public static Object getInstance(String className, Object param) throws ObjectFactoryException {
      if (param == null) {
         return getInstance(className, null, (Class[]) null);
      }

      Object[] params = new Object[] {
            param
      };

      return getInstance(className, params, (Class[]) null);
   }

   public static Object getInstance(String className, Object param, Class<?> cls) throws ObjectFactoryException {
      Class<?>[] types = new Class<?>[] {
            cls
      };
      Object[] params = new Object[] {
            param
      };

      return getInstance(className, params, types);
   }

   public static Object getInstance(String className, Object[] params, Class<?>[] types) throws ObjectFactoryException {
      Class<?>[] paramTypes = types;

      if (paramTypes == null) {
         paramTypes = getClassTypes(params);
      }

      try {
         Class<?> type = Class.forName(className);

         try {
            Constructor<?> constructor = type.getConstructor(paramTypes);

            try {
               return constructor.newInstance(params);
            }
            catch (Exception e) {
               throw new ObjectFactoryException("Exception Thrown on try [" + className + "].newInstance", e);
            }
         }
         catch (NoSuchMethodException e) {
            throw new ObjectFactoryException("NoSuchMethodException thrown on try [" + className + "].getConstructor", e);
         }
         catch (SecurityException e) {
            throw new ObjectFactoryException("SecurityException thrown on try [" + className + "].getConstructor", e);
         }
      }
      catch (ClassNotFoundException e) {
         throw new ObjectFactoryException("Class [" + className + "] not found", e);
      }
   }

   /**
    * Invokes a class' static method.
    *
    * @param className
    *           The name of the class whose static method is being invoked.
    * @param methodName
    *           The name of the static method being invoked.
    * @return Returns an Object representing the result, if any, of the invocation.
    * @throws ObjectFactoryException
    */
   public static Object invokeStaticMethod(String className, String methodName) throws ObjectFactoryException {
      return invokeMethod(className, null, methodName, (Object[]) null);
   }

   /**
    * Invokes a class' static method with parameters.
    *
    * @param className
    *           The name of the class whose static method is being invoked.
    * @param methodName
    *           The name of the static method being invoked.
    * @param params
    *           An Object array of method parameters.
    * @return Returns an Object representing the result, if any, of the invocation.
    * @throws ObjectFactoryException
    */
   public static Object invokeStaticMethod(String className, String methodName, Object[] params) throws ObjectFactoryException {
      return invokeMethod(className, null, methodName, params);
   }

   /**
    * Invokes an object's instance method.
    *
    * @param object
    *           A reference to the object whose method is being invoked.
    * @param methodName
    *           The name of the method being invoked.
    * @return Returns an Object representing the result, if any of the invocation.
    * @throws ObjectFactoryException
    */
   public static Object invokeMethod(Object object, String methodName) throws ObjectFactoryException {
      return invokeMethod(object, methodName, (Object[]) null);
   }

   /**
    * Invokes an object's instance method.
    *
    * @param object
    *           A reference to the object whose method is being invoked.
    * @param methodName
    *           The name of the method being invoked.
    * @param params
    *           An Object array of method parameters.
    * @return Returns an Object representing the result, if any of the invocation.
    * @throws ObjectFactoryException
    */
   public static Object invokeMethod(Object object, String methodName, Object[] params) throws ObjectFactoryException {
      return invokeMethod(object.getClass().getCanonicalName(), object, methodName, params);
   }

   private static Object invokeMethod(String className, Object object, String methodName, Object[] params) throws ObjectFactoryException {
      try {
         Class<?> cls = Class.forName(className);
         Class<?>[] types = getClassTypes(params);

         try {
            Method method = cls.getMethod(methodName, types);

            try {
               return method.invoke(object, params);
            }
            catch (Exception e) {
               throw new ObjectFactoryException("ObjectFactory.invokeMethod[" + methodName + "]", e);
            }
         }
         catch (NoSuchMethodException e) {
            throw new ObjectFactoryException("ObjectFactory.invokeMethod[" + methodName + "]: No Such Method ", e);
         }
      }
      catch (ClassNotFoundException e) {
         throw new ObjectFactoryException("No such class [" + className + "]", e);
      }
   }

   public static Object getValueForObjectProperty(Object obj, String objProp) throws ObjectFactoryException {
      Object value = null;

      objProp = objProp.toLowerCase(); // be sure this is lower case

      Class<?> aClass = obj.getClass();
      Method[] methods = aClass.getMethods();
      boolean found = false;
      for (int i = 0; i < methods.length && !found; i++) {
         Method method = methods[i];
         if (isGetter(method)) {
            String name = method.getName();
            String property = name.replace("get", "").toLowerCase();
            if (property.equalsIgnoreCase(objProp)) {
               value = invokeMethod(obj, name);
               found = true;
            }
         }
      }

      return value;
   }

   public static void setValueForObjectProperty(Object obj, String objProp, Object value) throws ObjectFactoryException {
      objProp = objProp.toLowerCase(); // be sure this is lower case

      Class<?> aClass = obj.getClass();
      Method[] methods = aClass.getMethods();
      boolean found = false;
      for (int i = 0; i < methods.length && !found; i++) {
         Method method = methods[i];
         if (isSetter(method)) {
            String name = method.getName();
            String property = name.replace("set", "").toLowerCase();
            if (property.equalsIgnoreCase(objProp)) {
               Object[] params = new Object[1];
               params[0] = value;
               invokeMethod(obj, name, params);
               found = true;
            }
         }
      }
   }

   public static boolean isInstanceOf(Class<?> srcClassObj, Class<?> destClassObj) {
      boolean value = false;
      while (srcClassObj != null) {
         if (srcClassObj.isInstance(destClassObj)) {
            value = true;
            break;
         }
         srcClassObj = srcClassObj.getSuperclass();
      }

      return value;
   }

   // does the source implement the dest?
   public static boolean implementsInterface(Class<?> srcClassObj, Class<?> destClassObj) {
      boolean value = false;
      Class<?>[] srcInterfaceClasses = srcClassObj.getInterfaces();
      for (Class<?> srcInterfaceClass : srcInterfaceClasses) {
         if (srcInterfaceClass.equals(destClassObj)) {
            value = true;
            break;
         }
      }
      return value;
   }

   public static boolean isGetter(Method method) {
      if (!method.getName().startsWith("get") && !method.getName().startsWith("is")) {
         return false;
      }
      if (method.getParameterTypes().length != 0) {
         return false;
      }
      if (void.class.equals(method.getReturnType())) {
         return false;
      }
      return true;
   }

   public static boolean isSetter(Method method) {
      if (!method.getName().startsWith("set")) {
         return false;
      }
      if (method.getParameterTypes().length != 1) {
         return false;
      }
      return true;
   }

   public static final class ObjectFactoryException extends Exception {

      private static final long serialVersionUID = 9141617205797848013L;

      public ObjectFactoryException(String message, Throwable e) {
         super(message, e);
      }
   }

   private static Class<?>[] getClassTypes(Object[] params) {
      if (params == null) {
         return null;
      }

      Class<?>[] class_types = new Class<?>[params.length];
      for (int i = 0; i < params.length; i++) {
         class_types[i] = params[i].getClass();
      }
      return class_types;
   }
}
