package com.jc.app.rest;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import com.jc.log.ExceptionMessageHandler;

public abstract class XMLEntity {

   @Override
   public String toString() {
      try {
         return RESTUtils.format(this);
      }
      catch (Exception ex) {
         ex.printStackTrace();
         return ExceptionMessageHandler.formatExceptionMessage(ex);
      }
   }

   public static <T extends Object> T getEntity(Class<T> type, String xmlData) throws Exception {
      StringReader strReader = new StringReader(xmlData);
      JAXBContext jaxbContext = JAXBContext.newInstance(type);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
      return type.cast(jaxbUnmarshaller.unmarshal(strReader));
   }
}
