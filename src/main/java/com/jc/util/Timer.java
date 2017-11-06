package com.jc.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.jc.command.Command;

/*
 * This class runs a timer in it's own thread for a specified amount of time.
 * When the specified amount of time is up, all TimerListeners registered will 
 * be notified that the time has lapsed.
 */

public class Timer implements Runnable {

   public final static long ONE_SECOND_INTERVAL = 1000;
   public final static long ONE_MINUTE_INTERVAL = ONE_SECOND_INTERVAL * 60;

   private static int INSTANCE_NUM = 0;

   private boolean LogMessages;

   private Thread MyThread;
   private int ThreadID;

   private final List<TimerListener> Listeners;

   private final long WakeUpTime;
   private final long LastWakeUpTime;
   private final boolean WakeUpOnce;

   private long BeginTime;
   private long EndTime;

   private Command MyCommand;

   private final String Name;

   public Timer(long time) {
      this(time, 0, true);
   }

   public Timer(long maxtime, long totime) {
      this(maxtime, totime, false);
   }

   private Timer(long maxtime, long totime, boolean wakeUpOnce) {
      WakeUpTime = maxtime;
      LastWakeUpTime = totime;
      WakeUpOnce = wakeUpOnce;
      LogMessages = false;
      Name = "Timer" + INSTANCE_NUM++;
      Listeners = new CopyOnWriteArrayList<TimerListener>();
   }

   public void logMessages(boolean log) {
      LogMessages = log;
   }

   // Starts the Timer in its own thread.
   public synchronized void start() {
      MyThread = new Thread(this, "Thread" + ThreadID);
      logMessage("Starting timer...");
      MyThread.start();
      BeginTime = System.currentTimeMillis();
      ThreadID++;
   }

   // Interrupts the timer, stopping it from what it's doing.
   public synchronized void stop() {
      if (MyThread != null) {
         logMessage("Stopping timer (forced)...");
         MyThread.interrupt();
         try {
            Thread.sleep(300);
         }
         catch (InterruptedException e) {
            logMessage("WARNING: Interrupted when sleeping -- " + e.getMessage());
         }
         EndTime = System.currentTimeMillis();
      }
      else {
         logMessage("ERROR: When stopping, MyThread was null.");
      }
   }

   public boolean isRunning() {
      return MyThread != null && MyThread.isAlive();
   }

   public void addTimerListener(TimerListener listener) {
      Listeners.add(listener);
   }

   public void removeTimerListener(TimerListener listener) {
      Listeners.remove(listener);
   }

   public long getTotalRunTime() {
      if (BeginTime > 0 && EndTime > BeginTime) {
         return EndTime - BeginTime;
      }
      return -1;
   }

   public void addCommand(Command command) {
      MyCommand = command;
   }

   @Override
   public void run() {
      while (true) {
         try {
            logMessage("Sleeping...");
            Thread.sleep(WakeUpTime);
            logMessage("Awake...");
            notifyListeners();

            if (MyCommand != null) {
               logMessage("Running command...");
               MyCommand.execute();
            }

            if (WakeUpOnce || LastWakeUpTime <= System.currentTimeMillis()) {
               logMessage("Done...");
               break;
            }
            logMessage("Going back to sleep...");
         }
         catch (InterruptedException e) {
            logMessage("Thread interrupted...");
            break;
         }
         catch (Exception ex) {
            logMessage(ex.getMessage());
            break;
         }
      }
      cleanUp();
   }

   protected void notifyListeners() {
      if (Listeners.size() > 0) {
         logMessage("Notifying [" + Listeners.size() + "] listeners...");
         for (TimerListener listener : Listeners) {
            listener.timeExpired();
         }
      }
   }

   protected List<TimerListener> getTimerListeners() {
      return Listeners;
   }

   private void cleanUp() {
      logMessage("Thread clean up...");
      MyThread = null;
   }

   protected void logMessage(String message) {
      if (LogMessages) {
         String thread_name = "NullThread";
         if (MyThread != null) {
            thread_name = MyThread.getName();
         }
         System.out.println(Name + "[" + thread_name + "]: " + message);
      }
   }

   public interface TimerListener {

      public void timeExpired();
   }
}
