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
public @interface Id {

   Attribute addData() default Attribute.FIELD_READ_ONLY;

   Attribute modifyData() default Attribute.FIELD_REQUIRED_READ_ONLY;

   SubMetadata subMetadataAttributes() default @SubMetadata(addData = Attribute.FIELD_REQUIRED_READ_ONLY, modifyData = Attribute.FIELD_REQUIRED_READ_ONLY);
}
