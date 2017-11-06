package com.jc.app.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * This annotation is used by the REST services to generate metadata related to 
 * domain objects.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RDMetadata {

   public enum BOOLEAN {
      YES, NO, UNKNOWN
   }

   public enum Attribute {
      FIELD_READ_ONLY(0), FIELD_OPTIONAL_MODIFIABLE(1), FIELD_REQUIRED_MODIFIABLE(2), FIELD_REQUIRED_READ_ONLY(3);

      private int code;

      private Attribute(int code) {
         this.code = code;
      }

      public int getCode() {
         return code;
      }

      public static Attribute fromCode(final int code) {
         for (Attribute t : Attribute.values()) {
            if (t.code == code) {
               return t;
            }
         }
         return null;
      }

   }

   Attribute addData() default Attribute.FIELD_OPTIONAL_MODIFIABLE;

   Attribute modifyData() default Attribute.FIELD_OPTIONAL_MODIFIABLE;

   SubMetadata subMetadataAttributes() default @SubMetadata();

   String[] extendingClasses() default {};

   /**
    * (Optional) Indicates whether an entity field can be used in a filter/search RESTful operation. Unless otherwise annotated, all non-readOnly (writable) fields are by default marked filterable.
    */
   BOOLEAN filterable() default BOOLEAN.UNKNOWN;

   /**
    * (Optional) Indicates whether an entity field can be used in a sort RESTful operation.
    */
   BOOLEAN sortable() default BOOLEAN.UNKNOWN;

   /**
    * (Optional) A human readable description about the entity field.
    */
   String description() default "";

   /**
    * (Optional) Indicates a default value that is applied to the entity field if no other value is specified.
    */
   String defaultValue() default "";

   /**
    * (Optional) A list of possible values that can be supplied as a valid entity field value.
    */
   String[] valueOptions() default {};

   /**
    * (Optional) The name of the class that will return a String[] of value options. Class must extend ValueOptionsLoader.
    */
   String valueOptionLoaderClass() default "";

}
