package com.jc.app.rest;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.jc.FakeDataGenerator;
import com.jc.app.rest.annotations.Transient;

public class RESTDataTestObject extends RESTData {

   private static final long serialVersionUID = 6391549843221176431L;

   private Long id;
   private String stringVar;
   private Integer intVar;
   private Double doubleVar;
   private Float floatVar;
   private Boolean booleanVar;
   private BigInteger bigIntVar;
   private BigDecimal bigDecimalVar;
   private Character charVar;
   private Byte byteVar;
   private Date dateVar;

   private List<Object> listVar;
   private Map<String, Object> mapVar;

   private RESTDataTestSubObject subObjectVar;
   private List<RESTDataTestSubObject> subObjectListVar;

   private List<List<Object>> listListVar;
   private List<Map<String, Object>> listMapVar;

   //   private Map<String, List<String>> mapListVar;

   @Transient
   private Double transDoubleVar;

   public RESTDataTestObject() {}

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public String getStringVar() {
      return stringVar;
   }

   public void setStringVar(String stringVar) {
      this.stringVar = stringVar;
   }

   public Integer getIntVar() {
      return intVar;
   }

   public void setIntVar(Integer intVar) {
      this.intVar = intVar;
   }

   public Double getDoubleVar() {
      return doubleVar;
   }

   public void setDoubleVar(Double doubleVar) {
      this.doubleVar = doubleVar;
   }

   public Float getFloatVar() {
      return floatVar;
   }

   public void setFloatVar(Float floatVar) {
      this.floatVar = floatVar;
   }

   public Boolean getBooleanVar() {
      return booleanVar;
   }

   public void setBooleanVar(Boolean booleanVar) {
      this.booleanVar = booleanVar;
   }

   public BigInteger getBigIntVar() {
      return bigIntVar;
   }

   public void setBigIntVar(BigInteger bigIntVar) {
      this.bigIntVar = bigIntVar;
   }

   public BigDecimal getBigDecimalVar() {
      return bigDecimalVar;
   }

   public void setBigDecimalVar(BigDecimal bigDecimalVar) {
      this.bigDecimalVar = bigDecimalVar;
   }

   public Character getCharVar() {
      return charVar;
   }

   public void setCharVar(Character charVar) {
      this.charVar = charVar;
   }

   public Byte getByteVar() {
      return byteVar;
   }

   public void setByteVar(Byte byteVar) {
      this.byteVar = byteVar;
   }

   public Date getDateVar() {
      return dateVar;
   }

   public void setDateVar(Date dateVar) {
      this.dateVar = dateVar;
   }

   public List<Object> getListVar() {
      return listVar;
   }

   public void setListVar(List<Object> listVar) {
      this.listVar = listVar;
   }

   public Map<String, Object> getMapVar() {
      return mapVar;
   }

   public void setMapVar(Map<String, Object> mapVar) {
      this.mapVar = mapVar;
   }

   public RESTDataTestSubObject getSubObjectVar() {
      return subObjectVar;
   }

   public void setSubObjectVar(RESTDataTestSubObject subObjectVar) {
      this.subObjectVar = subObjectVar;
   }

   public List<RESTDataTestSubObject> getSubObjectListVar() {
      return subObjectListVar;
   }

   public void setSubObjectListVar(List<RESTDataTestSubObject> subObjectListVar) {
      this.subObjectListVar = subObjectListVar;
   }

   public List<List<Object>> getListListVar() {
      return listListVar;
   }

   public void setListListVar(List<List<Object>> listListVar) {
      this.listListVar = listListVar;
   }

   public List<Map<String, Object>> getListMapVar() {
      return listMapVar;
   }

   public void setListMapVar(List<Map<String, Object>> listMapVar) {
      this.listMapVar = listMapVar;
   }

   //   public Map<String, List<String>> getMapListVar() {
   //      return mapListVar;
   //   }
   //
   //   public void setMapListVar(Map<String, List<String>> mapListVar) {
   //      this.mapListVar = mapListVar;
   //   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((bigDecimalVar == null) ? 0 : bigDecimalVar.hashCode());
      result = prime * result + ((bigIntVar == null) ? 0 : bigIntVar.hashCode());
      result = prime * result + ((booleanVar == null) ? 0 : booleanVar.hashCode());
      result = prime * result + ((byteVar == null) ? 0 : byteVar.hashCode());
      result = prime * result + ((charVar == null) ? 0 : charVar.hashCode());
      result = prime * result + ((dateVar == null) ? 0 : dateVar.hashCode());
      result = prime * result + ((doubleVar == null) ? 0 : doubleVar.hashCode());
      result = prime * result + ((floatVar == null) ? 0 : floatVar.hashCode());
      result = prime * result + ((id == null) ? 0 : id.hashCode());
      result = prime * result + ((intVar == null) ? 0 : intVar.hashCode());
      result = prime * result + ((listVar == null) ? 0 : listVar.hashCode());
      result = prime * result + ((mapVar == null) ? 0 : mapVar.hashCode());
      result = prime * result + ((stringVar == null) ? 0 : stringVar.hashCode());
      result = prime * result + ((subObjectVar == null) ? 0 : subObjectVar.hashCode());
      result = prime * result + ((subObjectListVar == null) ? 0 : subObjectListVar.hashCode());
      result = prime * result + ((listListVar == null) ? 0 : listListVar.hashCode());
      result = prime * result + ((listMapVar == null) ? 0 : listMapVar.hashCode());
      //    result = prime * result + ((mapListVar == null) ? 0 : mapListVar.hashCode());
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
      if (!(obj instanceof RESTDataTestObject)) {
         return false;
      }
      RESTDataTestObject other = (RESTDataTestObject) obj;
      if (bigDecimalVar == null) {
         if (other.bigDecimalVar != null) {
            return false;
         }
      }
      else if (!bigDecimalVar.equals(other.bigDecimalVar)) {
         return false;
      }
      if (bigIntVar == null) {
         if (other.bigIntVar != null) {
            return false;
         }
      }
      else if (!bigIntVar.equals(other.bigIntVar)) {
         return false;
      }
      if (booleanVar == null) {
         if (other.booleanVar != null) {
            return false;
         }
      }
      else if (!booleanVar.equals(other.booleanVar)) {
         return false;
      }
      if (byteVar == null) {
         if (other.byteVar != null) {
            return false;
         }
      }
      else if (!byteVar.equals(other.byteVar)) {
         return false;
      }
      if (charVar == null) {
         if (other.charVar != null) {
            return false;
         }
      }
      else if (!charVar.equals(other.charVar)) {
         return false;
      }
      //      if (dateVar == null) {
      //         if (other.dateVar != null) {
      //            return false;
      //         }
      //      }
      //      else if (dateVar.compareTo(other.dateVar) != 0) {
      //         return false;
      //      }
      if (doubleVar == null) {
         if (other.doubleVar != null) {
            return false;
         }
      }
      else if (!doubleVar.equals(other.doubleVar)) {
         return false;
      }
      if (floatVar == null) {
         if (other.floatVar != null) {
            return false;
         }
      }
      else if (!floatVar.equals(other.floatVar)) {
         return false;
      }
      if (id == null) {
         if (other.id != null) {
            return false;
         }
      }
      else if (!id.equals(other.id)) {
         return false;
      }
      if (intVar == null) {
         if (other.intVar != null) {
            return false;
         }
      }
      else if (!intVar.equals(other.intVar)) {
         return false;
      }
      if (listVar == null) {
         if (other.listVar != null) {
            return false;
         }
      }
      //      else if (!listVar.equals(other.listVar)) {  // TODO: need to add comparison of contents of list...
      //         return false;
      //      }
      //      if (mapVar == null) {
      //         if (other.mapVar != null) {
      //            return false;
      //         }
      //      }
      //      else if (!mapVar.equals(other.mapVar)) {
      //         return false;
      //      }
      if (stringVar == null) {
         if (other.stringVar != null) {
            return false;
         }
      }
      else if (!stringVar.equals(other.stringVar)) {
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
      if (subObjectListVar == null) {
         if (other.subObjectListVar != null) {
            return false;
         }
      }
      else if (!subObjectListVar.equals(other.subObjectListVar)) {
         return false;
      }
      if (listListVar == null) {
         if (other.listListVar != null) {
            return false;
         }
      }
      //      else if (!listListVar.equals(other.listListVar)) { // TODO: need to add comparison of contents of list...
      //         return false;
      //      }
      if (listMapVar == null) {
         if (other.listMapVar != null) {
            return false;
         }
      }
      //      else if (!listMapVar.equals(other.listMapVar)) { // TODO: need to add comparison of contents of list...
      //         return false;
      //      }
      //      if (mapListVar == null) {
      //         if (other.mapListVar != null) {
      //            return false;
      //         }
      //      }
      //      else if (!mapListVar.equals(other.mapListVar)) {
      //         return false;
      //      }

      return true;
   }

   void init() {

      Random NumberGen = new Random(System.nanoTime());

      setId(NumberGen.nextLong());
      setStringVar(FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(15, 30)));
      setIntVar(NumberGen.nextInt());
      setDoubleVar(NumberGen.nextDouble());
      setFloatVar(NumberGen.nextFloat());
      setBooleanVar(NumberGen.nextBoolean());
      setBigIntVar(new BigInteger("" + NumberGen.nextInt()));
      setBigDecimalVar(new BigDecimal("" + NumberGen.nextDouble()));
      setCharVar('f');
      setByteVar((byte) NumberGen.nextInt(Byte.MAX_VALUE));
      setDateVar(FakeDataGenerator.generateRandomDate(null, new Date())); // today or before...

      List<Object> listVar = createRandomListObject(NumberGen);
      setListVar(listVar);

      Map<String, Object> mapVar = createRandomMapObject(NumberGen);
      setMapVar(mapVar);

      subObjectVar = new RESTDataTestSubObject();
      subObjectVar.init();
      setSubObjectVar(subObjectVar);

      List<RESTDataTestSubObject> subObjectListVar = new ArrayList<>();
      for (int i = 0; i < 5; i++) {
         RESTDataTestSubObject solv = new RESTDataTestSubObject();
         solv.init();
         subObjectListVar.add(solv);
      }
      setSubObjectListVar(subObjectListVar);

      List<List<Object>> listListVar = new ArrayList<>();
      listListVar.add(createRandomListObject(NumberGen));
      listListVar.add(createRandomListObject(NumberGen));
      listListVar.add(createRandomListObject(NumberGen));
      listListVar.add(createRandomListObject(NumberGen));
      listListVar.add(createRandomListObject(NumberGen));
      setListListVar(listListVar);

      List<Map<String, Object>> listMapVar = new ArrayList<>();
      listMapVar.add(createRandomMapObject(NumberGen));
      listMapVar.add(createRandomMapObject(NumberGen));
      listMapVar.add(createRandomMapObject(NumberGen));
      listMapVar.add(createRandomMapObject(NumberGen));
      listMapVar.add(createRandomMapObject(NumberGen));
      setListMapVar(listMapVar);

      //      Map<String, List<String>> mapListVar = new HashMap<String, List<String>>();
      //      mapListVar.put(FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(5, 8)), createRandomListObject());
      //      mapListVar.put(FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(5, 8)), createRandomListObject());
      //      mapListVar.put(FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(5, 8)), createRandomListObject());
      //      mapListVar.put(FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(5, 8)), createRandomListObject());
      //      mapListVar.put(FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(5, 8)), createRandomListObject());
      //      setMapListVar(mapListVar);
   }

   private List<Object> createRandomListObject(Random NumberGen) {
      List<Object> listVar = new ArrayList<>();
      listVar.add(FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(5, 15)));
      listVar.add(NumberGen.nextBoolean());
      listVar.add(NumberGen.nextDouble());
      listVar.add(NumberGen.nextFloat());
      listVar.add(NumberGen.nextInt());
      listVar.add(NumberGen.nextLong());
      RESTDataTestSubObject subObjectVar = new RESTDataTestSubObject();
      subObjectVar.init();
      listVar.add(subObjectVar);
      return listVar;
   }

   private Map<String, Object> createRandomMapObject(Random NumberGen) {
      Map<String, Object> map = new HashMap<>();
      map.put(FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(5, 8)),
            FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(5, 15)));
      map.put(FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(5, 8)), NumberGen.nextBoolean());
      map.put(FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(5, 8)), NumberGen.nextDouble());
      map.put(FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(5, 8)), NumberGen.nextFloat());
      map.put(FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(5, 8)), NumberGen.nextInt());
      map.put(FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(5, 8)), NumberGen.nextLong());
      RESTDataTestSubObject subObjectVar = new RESTDataTestSubObject();
      subObjectVar.init();
      map.put(FakeDataGenerator.generateRandomString(FakeDataGenerator.getRandomInteger(5, 8)), subObjectVar);
      return map;
   }
}
