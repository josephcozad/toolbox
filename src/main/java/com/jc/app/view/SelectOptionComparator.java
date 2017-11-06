package com.jc.app.view;

import java.util.Comparator;

public class SelectOptionComparator implements Comparator<SelectOption> {

   public final static int ASCENDING = 0;
   public final static int DESCENDING = 1;

   private final int orderBy;
   private SelectOption ignoreOption;

   public SelectOptionComparator() {
      this.orderBy = ASCENDING;
   }

   public SelectOptionComparator(int orderBy) {
      this.orderBy = orderBy;
   }

   public void ignoreSelectOption(SelectOption ignoreOption) {
      this.ignoreOption = ignoreOption;
   }

   @Override
   public int compare(SelectOption o1, SelectOption o2) {
      String label1 = o1.getLabel();
      String label2 = o2.getLabel();

      if (o1.equals(ignoreOption) || o2.equals(ignoreOption)) {
         return 1; // always first...
      }
      else {
         int value = label1.compareToIgnoreCase(label2);
         if (orderBy == DESCENDING) {
            value = label2.compareToIgnoreCase(label1);
         }
         return value;
      }
   }
}
