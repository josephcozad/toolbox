package com.jc.logtst;

public class TestCustomException extends Exception {

   private static final long serialVersionUID = 5116432996272574269L;

   public TestCustomException() {
      super("Howdy ho ho... and hi!");
   }
}
