package com.jc.app.rest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.jc.app.rest.annotations.RDMetadata.Attribute;

/*
 * This annotation is used by the REST services to generate metadata related to 
 * domain objects.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SubMetadata {

   Attribute addData() default Attribute.FIELD_OPTIONAL_MODIFIABLE;

   Attribute modifyData() default Attribute.FIELD_OPTIONAL_MODIFIABLE;
}
