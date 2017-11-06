package com.jc.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * This class provides a static loop counter and timer that can be used in code 
 * to count the number of iterations per "tagged" item and/or the time it took to 
 * process that iteration. So for example you have a block of code that you want 
 * to know how long it took to do 5 different things. Using one SimpleTimer you can 
 * start a "clock" for thing #1 and stop that "clock" when thing #1 is done. 
 * Perhaps thing #2 starts mid-way through thing #1, you can start a "clock" for 
 * thing #2 and stop it when thing #2 is done. In addition you can add notes 
 * to a specific tagged point. Then at any time, all this information gathered 
 * can be output into report.
 */

public final class StopWatch {

   private final static String INTERNAL_TAG = "internalTag";

   private final Map<String, List<TagInfo>> infoMap;

   public StopWatch() {
      infoMap = new LinkedHashMap<String, List<TagInfo>>();
   }

   /**
    * Start a timer. Tag is the id string that will be associated with the time being started. The tag will be printed out in the report along with the total time associated with the tag.
    */
   public void start(String tag) throws Exception {

      TagInfo timer = new Timer();

      List<TagInfo> infoList = null;
      if (!infoMap.containsKey(tag)) {
         infoList = new ArrayList<TagInfo>();
      }
      else {
         infoList = infoMap.get(tag);

         // if timer already exists, throw exception that timer already exists...
         for (TagInfo info : infoList) {
            if (info instanceof Timer) {
               throw new Exception("Timer already started.");
            }
         }
      }

      infoList.add(timer);
   }

   public void start() throws Exception {
      start(INTERNAL_TAG);
   }

   /**
    * Stop a timer associated with the id string (tag) that is supplied.
    */
   public void stop(String tag) {

      if (infoMap.containsKey(tag)) {
         boolean timerFound = false;
         List<TagInfo> infoList = infoMap.get(tag);
         for (TagInfo info : infoList) {
            if (info instanceof Timer) {
               ((Timer) info).stop();
               timerFound = true;
               break;
            }
         }

         if (!timerFound) {
            addNote(tag, "Request to stop timer; timer not found.");
         }
      }
      // tag not found, ignore it...
   }

   public void stop() {
      stop(INTERNAL_TAG);
   }

   /**
    * Increment a loop counter associated with the supplied tag.
    */
   public void incLoopCount(String tag) {

      List<TagInfo> infoList = null;
      if (!infoMap.containsKey(tag)) { // no tag info exists yet, create a new info list...
         infoList = new ArrayList<TagInfo>();
         TagInfo counter = new Counter();
         infoList.add(counter);
      }
      else {
         infoList = infoMap.get(tag);

         // if counter exists, increment it...
         boolean counterFound = false;
         for (TagInfo info : infoList) {
            if (info instanceof Counter) {
               ((Counter) info).incCount();
               counterFound = true;
               break;
            }
         }

         // else create one...
         if (!counterFound) {
            TagInfo counter = new Counter();
            infoList.add(counter);
         }
      }

      infoMap.put(tag, infoList); // update it...
   }

   public void incLoopCount() {
      incLoopCount(INTERNAL_TAG);
   }

   /**
    * Add a note associated with the supplied tag.
    */
   public void addNote(String tag, String noteText) {
      TagInfo note = new Note(noteText);
      List<TagInfo> infoList = null;
      if (!infoMap.containsKey(tag)) {
         infoList = new ArrayList<TagInfo>();
      }
      else {
         infoList = infoMap.get(tag);
      }
      infoList.add(note);
   }

   public void addNote(String note) {
      addNote(INTERNAL_TAG, note);
   }

   /**
    * Resets everything.
    */
   public void reset() {
      infoMap.clear();
   }

   /**
    * Remove all info associated with the supplied tag.
    */
   public void removeTag(String tag) {
      if (infoMap.containsKey(tag)) {
         infoMap.remove(tag);
      }
   }

   public Map<String, List<TagInfo>> getInfo() {
      return infoMap;
   }

   /**
    * Reports the information gathered using the supplied title.
    */
   public String report(String title) throws Exception {
      StringBuilder report = new StringBuilder("---------------------- [" + title + "]" + '\n');
      for (Map.Entry<String, List<TagInfo>> entry : infoMap.entrySet()) {
         String tag = entry.getKey();
         List<TagInfo> infoList = entry.getValue();
         for (TagInfo info : infoList) {
            report.append("   ");
            if (info instanceof Timer) {
               long runTime = ((Timer) info).getTime();
               report.append(tag).append(' ').append(runTime);
            }
            else if (info instanceof Counter) {
               long count = ((Counter) info).getCount();
               report.append(tag).append(' ').append(count);
            }
            else if (info instanceof Note) {
               String note = ((Note) info).getNote();
               report.append("NOTE: ").append(note);
            }
            else {
               throw new Exception("Uknown tag type.");
            }
            report.append('\n');
         }
      }
      return report.append('\n').toString();
   }

   /**
    * Prints the report to the console.
    */
   public void printReport(String title) throws Exception {
      System.out.println(report(title));
   }

   /**
    * Prints the report to the specified file location.
    */
   public void printReport(String title, String fileName) throws Exception {
      try {
         String content = report(title);
         BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
         out.write(content);
         out.newLine();
         out.close();
      }
      catch (IOException e) {
         System.err.println("Trouble writing to [" + fileName + "]: " + e.getMessage());
      }
   }

   public long getTime(String tag) {
      long time = 0l;
      if (infoMap.containsKey(tag)) {
         List<TagInfo> infoList = infoMap.get(tag);
         for (TagInfo info : infoList) {
            if (info instanceof Timer) {
               time = ((Timer) info).getTime();
               break;
            }
         }
      }
      return time;
   }

   public long getTime() {
      return getTime(INTERNAL_TAG);
   }

   public List<String> getNotes(String tag) {
      List<String> notesList = new ArrayList<String>();
      if (infoMap.containsKey(tag)) {
         List<TagInfo> infoList = infoMap.get(tag);
         for (TagInfo info : infoList) {
            if (info instanceof Note) {
               String note = ((Note) info).getNote();
               notesList.add(note);
            }
         }
      }
      return notesList;
   }

   public List<String> getNotes() {
      return getNotes(INTERNAL_TAG);
   }

   public long getCount(String tag) {
      long time = 0l;
      if (infoMap.containsKey(tag)) {
         List<TagInfo> infoList = infoMap.get(tag);
         for (TagInfo info : infoList) {
            if (info instanceof Counter) {
               time = ((Counter) info).getCount();
               break;
            }
         }
      }
      return time;
   }

   public long getCount() {
      return getCount(INTERNAL_TAG);
   }

   public static final class Timer implements TagInfo {

      private boolean stopped;
      private long time;

      Timer() {
         time = System.currentTimeMillis();
         stopped = false;
      }

      public long getTime() {
         if (!stopped) {
            stop();
         }
         return time;
      }

      public void stop() {
         time = System.currentTimeMillis() - time;
         stopped = true;
      }
   }

   public static final class Note implements TagInfo {

      private final String note;

      Note(String note) {
         this.note = note;
      }

      public String getNote() {
         return note;
      }
   }

   public static final class Counter implements TagInfo {

      private long value;

      Counter() {
         this.value = 1l;
      }

      public void incCount() {
         value += value;
      }

      public long getCount() {
         return value;
      }
   }

   public interface TagInfo {}
}
