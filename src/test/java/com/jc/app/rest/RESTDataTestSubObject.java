package com.jc.app.rest;

import java.util.Random;

import com.jc.FakeDataGenerator;

public class RESTDataTestSubObject extends RESTData {

   private static final long serialVersionUID = -2766012788845922079L;

   private String stringVar1;
   private String stringVar2;
   private Integer intVar;
   private RESTDataTestSubObject subObjectVar;

   public RESTDataTestSubObject() {}

   public String getStringVar1() {
      return stringVar1;
   }

   public void setStringVar1(String stringVar1) {
      this.stringVar1 = stringVar1;
   }

   public String getStringVar2() {
      return stringVar2;
   }

   public void setStringVar2(String stringVar2) {
      this.stringVar2 = stringVar2;
   }

   public Integer getIntVar() {
      return intVar;
   }

   public void setIntVar(Integer intVar) {
      this.intVar = intVar;
   }

   public RESTDataTestSubObject getSubObjectVar() {
      return subObjectVar;
   }

   public void setSubObjectVar(RESTDataTestSubObject subObjectVar) {
      this.subObjectVar = subObjectVar;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((intVar == null) ? 0 : intVar.hashCode());
      result = prime * result + ((stringVar1 == null) ? 0 : stringVar1.hashCode());
      result = prime * result + ((stringVar2 == null) ? 0 : stringVar2.hashCode());
      result = prime * result + ((subObjectVar == null) ? 0 : subObjectVar.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (!super.equals(obj)) {
         return false;
      }
      if (!(obj instanceof RESTDataTestSubObject)) {
         return false;
      }
      RESTDataTestSubObject other = (RESTDataTestSubObject) obj;
      if (intVar == null) {
         if (other.intVar != null) {
            return false;
         }
      }
      else if (!intVar.equals(other.intVar)) {
         return false;
      }
      if (stringVar1 == null) {
         if (other.stringVar1 != null) {
            return false;
         }
      }
      else if (!stringVar1.equals(other.stringVar1)) {
         return false;
      }
      if (stringVar2 == null) {
         if (other.stringVar2 != null) {
            return false;
         }
      }
      else if (!stringVar2.equals(other.stringVar2)) {
         return false;
      }
      if (subObjectVar == null) {
         if (other.subObjectVar != null) {
            return false;
         }
      }
      else if (!subObjectVar.equals(other.subObjectVar)) {
         return false;
      }
      return true;
   }

   void init() {
      Random NumberGen = new Random(System.nanoTime());

      this.stringVar1 = FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(15, 30));
      this.stringVar2 = FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(15, 30));
      this.intVar = NumberGen.nextInt();
   }

}
