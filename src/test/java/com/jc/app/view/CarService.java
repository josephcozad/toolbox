package com.jc.app.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import com.jc.db.dao.FilterInfo;

@ManagedBean(name = "carService")
@ApplicationScoped
public class CarService {

   public final static int TOTAL_NUMBER_CARS_CREATED = 2500;

   private final static String[] colors;

   private final static String[] brands;

   static {
      colors = new String[10];
      colors[0] = "Black";
      colors[1] = "White";
      colors[2] = "Green";
      colors[3] = "Red";
      colors[4] = "Blue";
      colors[5] = "Orange";
      colors[6] = "Silver";
      colors[7] = "Yellow";
      colors[8] = "Brown";
      colors[9] = "Maroon";

      brands = new String[10];
      brands[0] = "BMW";
      brands[1] = "Mercedes";
      brands[2] = "Volvo";
      brands[3] = "Audi";
      brands[4] = "Renault";
      brands[5] = "Fiat";
      brands[6] = "Volkswagen";
      brands[7] = "Honda";
      brands[8] = "Jaguar";
      brands[9] = "Ford";
   }

   private final List<Car> carList;

   public CarService() {
      carList = createCars(TOTAL_NUMBER_CARS_CREATED);
   }

   public List<Car> getCars(int first, int length, Map<String, String> sortOn, FilterInfo filterOn) {
      ArrayList<Car> cars = new ArrayList<Car>();

      if (length == 0) { // return all cars...
         cars.addAll(carList);
      }
      else {
         for (int i = first; i < (first + length) && i < carList.size(); i++) {
            Car item = carList.get(i);
            cars.add(item);
         }
      }
      return cars;
   }

   public int getTotalNumberOfAddressNPIs(FilterInfo filterOn) {
      return carList.size();
   }

   public Car getCar(String rowKey) {
      Car foundCar = null;

      for (Car car : carList) {
         String carId = car.getId();
         if (rowKey.equals(carId)) {
            foundCar = car;
            break;
         }
      }

      return foundCar;
   }

   // -------------------------------------

   public static List<Car> createCars(int size) {
      List<Car> list = new ArrayList<Car>();
      for (int i = 0; i < size; i++) {
         list.add(new Car(getRandomId(), getRandomBrand(), getRandomYear(), getRandomColor(), getRandomPrice(), getRandomSoldState()));
      }

      return list;
   }

   public static Car createCar() {
      return new Car(getRandomId(), getRandomBrand(), getRandomYear(), getRandomColor(), getRandomPrice(), getRandomSoldState());
   }

   private static String getRandomId() {
      return UUID.randomUUID().toString().substring(0, 8);
   }

   private static int getRandomYear() {
      return (int) (Math.random() * 50 + 1960);
   }

   private static String getRandomColor() {
      return colors[(int) (Math.random() * 10)];
   }

   private static String getRandomBrand() {
      return brands[(int) (Math.random() * 10)];
   }

   public static int getRandomPrice() {
      return (int) (Math.random() * 100000);
   }

   public static boolean getRandomSoldState() {
      return (Math.random() > 0.5) ? true : false;
   }

   public static List<String> getColors() {
      return Arrays.asList(colors);
   }

   public static List<String> getBrands() {
      return Arrays.asList(brands);
   }
}
