package com.jc.command.task;

import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;

import com.jc.log.Logger;
import com.jc.util.ConfigInfo;
import com.jc.util.FileSystem;

public class ThreadMonitor implements Runnable {

   public final static String THREAD_ID_PREFIX = "THREAD_";

   private static Hashtable<String, ThreadVar> THREADS = new Hashtable<>();
   private static ThreadMonitor SELF;
   private static long MAX_THREAD_RUNTIME = 0;
   private static String MONITOR_THREAD_ID; // the id given by ThreadMonitor of the thread running the ThreadMonitor.

   private final static Random RANDOM_GENERATOR = new Random();

   static {
      SELF = new ThreadMonitor();

      try {
         ConfigInfo info = ConfigInfo.getInstance();
         if (info.hasProperty("threadMonitor.maxThreadRuntime")) {
            MAX_THREAD_RUNTIME = info.getPropertyAsLong("threadMonitor.maxThreadRuntime");
         }
      }
      catch (FileNotFoundException ex) {
         Logger.log(ThreadMonitor.class, Level.SEVERE, ex);
      }
   }

   private boolean Running;
   private final double SleepTime; // In minutes...

   private ThreadMonitor() {
      SleepTime = 1.5d;
      MONITOR_THREAD_ID = startNewThread(this, Thread.MIN_PRIORITY);
   }

   /**
    * Returns the ThreadMonitor's thread id assigned to it by the ThreadMonitor. Use this to query the ThreadMonitor about this ThreadMonitor.
    */
   public static String getThreadId() {
      return MONITOR_THREAD_ID;
   }

   public static String startNewThread(Runnable runnable, int priority) {
      //      String id = THREAD_ID_PREFIX + Math.abs(ThreadLocalRandom.current().nextInt());
      String id = THREAD_ID_PREFIX + Math.abs(RANDOM_GENERATOR.nextInt());

      // DEV NOTE: more unique id by using String uniqueID = UUID.randomUUID().toString();
      //     though less human readable for logging purposes...

      if (SELF != null) {
         SELF.logMessage(Level.INFO, "Starting new thread for " + id + " ....");
      }

      Thread t = new Thread(runnable);
      t.setName(id);
      t.setPriority(priority);
      if (SELF != null) {
         SELF.logMessage(Level.INFO, "Setting " + id + " to " + priority + " priority.");
      }
      ThreadVar a_thread = new ThreadVar(t);
      addThreadVar(id, a_thread);
      t.start();

      Thread.State tstate = t.getState();
      if (SELF != null) {
         SELF.logMessage(Level.INFO, id + " Started; thread state [" + tstate + "].");
      }

      return id;
   }

   public static void stopThread(String id) {
      ThreadVar a_thread = getThreadVar(id);
      if (a_thread != null) {
         a_thread.interrupt();
         try {
            if (ConfigInfo.getInstance().hasProperty("log." + JobQueue.LOG_ID + ".directory")) {
               String message = "Requested thread interrupt for " + id + ".";
               String statement = getLoggingStatement(message);
               Logger.log(JobQueue.LOG_ID, ThreadMonitor.class, Level.INFO, statement);
            }
         }
         catch (Exception ex) {
            Logger.log(ThreadMonitor.class, Level.SEVERE, ex);
         }
      }
   }

   @Override
   public void run() {
      Running = true;
      while (Running) {
         try {
            if (THREADS.size() > 1) {
               logMessage(Level.INFO, "Checking status of " + (THREADS.size() - 1) + " thread(s).");
               checkThreads();
            }
            Thread.sleep((long) (60000 * SleepTime)); // sleep number of minutes.
         }
         catch (InterruptedException iex) {

         }
      }
   }

   /**
    * Outputs a "standard" formatted debugging statement that can be used in a spreadsheet.
    */
   protected void logMessage(Level level, String message) {
      logMessage(level, message, null);
   }

   protected void logMessage(Level level, String message, Throwable exc) {
      try {
         if (ConfigInfo.getInstance().hasProperty("log." + JobQueue.LOG_ID + ".directory")) {
            String statement = getLoggingStatement(message);
            if (exc != null) {
               Logger.log(JobQueue.LOG_ID, getClass(), level, statement, exc);
            }
            else {
               Logger.log(JobQueue.LOG_ID, getClass(), level, statement);
            }
         }
         else {
            //            String statement = getDebuggingStatement(message);
            //            System.out.println(statement);
            //            if (exc != null) {
            //               System.out.println(LoggableException.formatExceptionMessage(exc));
            //            }
         }
      }
      catch (Exception ex) {
         Logger.log(getClass(), Level.SEVERE, ex);
      }
   }

   private static String getLoggingStatement(String message) {
      // DEV NOTE: statement must have the same number of fields as the statement in same method in JobQueue.java.
      String statement = "ThreadMonitor(" + MONITOR_THREAD_ID + ")" + Logger.LOGFILE_FIELD_SEPARATOR + message + Logger.LOGFILE_FIELD_SEPARATOR + "n/a";
      return statement;
   }

   private void checkThreads() {
      synchronized (THREADS) {
         Object[] keys = THREADS.keySet().toArray();
         if (keys.length > 1) {
            Hashtable<String, ThreadVar> blocked = new Hashtable<>();
            for (Object key : keys) {
               ThreadVar a_thread = THREADS.get(key);
               String thread_name = a_thread.getName();
               if (!thread_name.equals(MONITOR_THREAD_ID)) {
                  ThreadStatus status = a_thread.getStatus();
                  Thread.State state = a_thread.getState();
                  if (state.equals(Thread.State.BLOCKED)) {
                     String message = key + " status[" + status + "], state[" + state + "]";
                     logMessage(Level.INFO, message);
                     blocked.put(a_thread.getName(), a_thread);
                  }
                  else if (state.equals(Thread.State.TERMINATED)) {
                     a_thread.clear();
                     THREADS.remove(key);
                     String message = key + " was removed from the THREADS hashtable; status[" + status + "], state[" + state + "]";
                     logMessage(Level.INFO, message);
                  }
                  else {
                     String message = key + " status[" + status + "], state[" + state + "]";
                     logMessage(Level.INFO, message);
                  }
               }
               else {
                  // Don't do anything the the ThreadMonitor thread.
               }
            }

            if (blocked.size() > 0) {
               resolveBlockedThreads(blocked);
            }
         }
         // else {
         // logMessage("THREADS hashtable was empty.");
         // }
      }

      checkForDeadlocks();
   }

   private void checkForDeadlocks() {
      ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
      long[] ids = tmx.findDeadlockedThreads();
      if (ids != null) {
         ThreadInfo[] infos = tmx.getThreadInfo(ids, true, true);
         System.out.println("The following threads are deadlocked:");
         for (ThreadInfo ti : infos) {
            logMessage(Level.SEVERE, "DEADLOCKED THREAD: " + ti);
         }
      }
   }

   private void resolveBlockedThreads(Hashtable<String, ThreadVar> blocked) {
      StringBuilder sb = new StringBuilder();

      Vector<ThreadVar> blocking_threads = new Vector<>();

      Object[] keys = blocked.keySet().toArray();
      for (Object key : keys) {
         ThreadVar blocked_thread = blocked.get(key);
         ThreadVar blocking_thread = getBlockingThread(blocked_thread.MyThread);

         StackTraceElement[] elements = blocked_thread.MyThread.getStackTrace();
         String blocked_method = elements[0].getClassName() + "." + elements[0].getMethodName();

         if (blocking_thread != null) {
            StackTraceElement[] elements2 = blocking_thread.MyThread.getStackTrace();
            String blocking_method = elements2[0].getClassName() + "." + elements2[0].getMethodName();

            sb.append(blocked_thread.getName() + " is being blocked at: " + blocked_method + "; RT[" + blocked_thread.getRuntime() + "] STATE["
                  + blocked_thread.getState() + "]" + FileSystem.NEWLINE);
            sb.append("     " + blocking_thread.getName() + " is doing the blocking at: " + blocking_method + "; RT[" + blocking_thread.getRuntime()
                  + "] STATE[" + blocking_thread.getState() + "]" + FileSystem.NEWLINE);

            sb.append("     STACK TRACE[" + blocked_thread.getName() + "]: ----------------------------" + FileSystem.NEWLINE);
            for (StackTraceElement element : elements) {
               sb.append("          " + element + FileSystem.NEWLINE);
            }

            sb.append("     STACK TRACE[" + blocking_thread.getName() + "]: ----------------------------" + FileSystem.NEWLINE);
            for (StackTraceElement element : elements2) {
               sb.append("          " + element + FileSystem.NEWLINE);
            }
            sb.append(FileSystem.NEWLINE);

            if (!blocking_threads.contains(blocking_thread)) {
               blocking_threads.add(blocking_thread);
            }
         }
      }

      if (blocking_threads.size() > 0 && MAX_THREAD_RUNTIME > 0) {
         for (ThreadVar blocking_thread : blocking_threads) {
            blocking_thread.MyThread.interrupt();
            logMessage(Level.INFO, "Requested interrupt on " + blocking_thread.getName() + ".");
         }
      }

      if (blocking_threads.size() > 0) {
         String outfile = Logger.saveOutput(sb);
         String message = "BLOCKED threads detected with ThreadMonitor, see the outfile for more details: " + outfile;
         Logger.log(getClass(), Level.SEVERE, message);
      }
   }

   private ThreadVar getBlockingThread(Thread blocked_thread) {
      StackTraceElement[] elements = blocked_thread.getStackTrace();
      String blocked_method = elements[0].getClassName() + "." + elements[0].getMethodName();

      ThreadVar blocking_thread = null;
      boolean found = false;
      Object[] keys = THREADS.keySet().toArray();
      for (int i = 0; i < keys.length && !found; i++) {
         String key = (String) keys[i];

         ThreadVar a_thread = THREADS.get(key);
         if (!a_thread.getName().equals(MONITOR_THREAD_ID) && !a_thread.getName().equals(blocked_thread.getName())) {
            StackTraceElement[] stack = a_thread.MyThread.getStackTrace();
            Thread.State state = a_thread.getState();
            for (StackTraceElement element : stack) {
               String thread_location = element.getClassName() + "." + element.getMethodName();
               if (!state.equals(Thread.State.BLOCKED) && thread_location.equals(blocked_method) && a_thread.getRuntime() > MAX_THREAD_RUNTIME) {
                  blocking_thread = a_thread; // this is the blocker!
                  found = true;
               }
            }
         }
      }

      return (blocking_thread);
   }

   private synchronized static ThreadVar getThreadVar(String id) {
      ThreadVar a_thread = null;
      if (THREADS.containsKey(id)) {
         a_thread = THREADS.get(id);
      }
      else {
         // log threadvar not found for id
         if (SELF != null) {
            SELF.logMessage(Level.WARNING, "WARNING: No thread var was found for " + id + " while trying to retrieve it.");
         }
      }
      return (a_thread);
   }

   private synchronized static void addThreadVar(String id, ThreadVar a_thread) {
      if (!THREADS.containsKey(id)) {
         THREADS.put(id, a_thread);
         if (SELF != null) {
            SELF.logMessage(Level.INFO, "Added " + id + " to THREADS.");
         }
      }
      else {
         if (SELF != null) {
            ThreadVar existing_thread = THREADS.get(id);
            SELF.logMessage(Level.INFO,
                  "Duplicate thread, " + id + ", found; new[" + a_thread.get().getId() + "] existing[" + existing_thread.get().getId() + "].");
         }

         // duplicate..
         ThreadVar dup_thread = THREADS.get(id);
         if (!dup_thread.MyThread.isAlive()) {
            dup_thread.clear();
            THREADS.put(id, a_thread);
            if (SELF != null) {
               SELF.logMessage(Level.INFO, "Duplicate thread, " + id + ", not alive, replacing it with a new ThreadVar object.");
            }
         }
         else {
            // log duplicate was still alive...
            Exception ex = new Exception("Duplicate thread, " + id + ", alive.... ignoring.");
            String outfile = Logger.saveStackTrace(ex);
            String message = "Duplicate thread, " + id + ", alive.... ignoring. See the out file for more details: " + outfile;
            Logger.log(ThreadMonitor.class, Level.SEVERE, message);
         }
      }
   }

   /**
    * Class that maintains reference to current thread under separate synchronization control.
    */
   static class ThreadVar {

      private Thread MyThread;
      private final long StartTime;
      private boolean InterruptedRequested;
      private long TimeMarker;

      ThreadVar(Thread t) {
         MyThread = t;
         StartTime = System.currentTimeMillis();
      }

      synchronized Thread get() {
         return (MyThread);
      }

      synchronized void clear() {
         MyThread = null;
      }

      // This method show the amount of time since the thread was added to the monintor
      // and is not an indication of the amount of time the thread has been running.
      long getRuntime() {
         return (System.currentTimeMillis() - StartTime);
      }

      void markAsDying() {
         TimeMarker = System.currentTimeMillis();
      }

      long getTimeMarker() {
         return (TimeMarker);
      }

      String getName() {
         return (MyThread.getName());
      }

      Thread.State getState() {
         return (MyThread.getState());
      }

      ThreadStatus getStatus() {
         ThreadStatus status = ThreadStatus.ALIVE;
         boolean alive = MyThread.isAlive();
         boolean interrupted = MyThread.isInterrupted();
         boolean terminated = MyThread.getState().equals(Thread.State.TERMINATED);

         if ((!alive && !interrupted && !InterruptedRequested) || (!alive && !interrupted && InterruptedRequested)) {
            if (terminated) {
               status = ThreadStatus.DEAD;
            }
            else {
               status = ThreadStatus.UNKNOWN;
            }
         }
         else if ((!alive && interrupted && !InterruptedRequested) || (!alive && interrupted && InterruptedRequested)) { // DEAD...
            status = ThreadStatus.DEAD;
         }
         else if ((alive && interrupted && !InterruptedRequested) || (alive && interrupted && InterruptedRequested)) { // ZOMBIE
            status = ThreadStatus.ZOMBIE;
         }
         else if ((alive && !interrupted && InterruptedRequested)) { // DYING
            status = ThreadStatus.DYING;
         }
         // else it's ALIVE!!!! BWAAAAAHHHHHH.....

         return (status);
      }

      boolean interruptRequested() {
         return (InterruptedRequested);
      }

      void interrupt() {
         if (MyThread != null) {
            MyThread.interrupt();
            InterruptedRequested = true;
         }
      }
   }

   public static enum ThreadStatus {

      UNKNOWN("UNKNOWN"), DEAD("DEAD"), ALIVE("ALIVE"), ZOMBIE("ZOMBIE"), DYING("DYING");

      private String text;

      private ThreadStatus(String text) {
         this.text = text;
      }

      public static boolean isUnknown(ThreadStatus status) {
         return (UNKNOWN.equals(status));
      }

      public boolean isUnknown() {
         return (isUnknown(this));
      }

      public static boolean isDead(ThreadStatus status) {
         return (DEAD.equals(status));
      }

      public boolean isDead() {
         return (isDead(this));
      }

      public static boolean isAlive(ThreadStatus status) {
         return (ALIVE.equals(status));
      }

      public boolean isAlive() {
         return (isAlive(this));
      }

      // A "Zombie" is both "alive" and "interrupted".
      public static boolean isZombie(ThreadStatus status) {
         return (ZOMBIE.equals(status));
      }

      public boolean isZombie() {
         return (isZombie(this));
      }

      public static boolean isDying(ThreadStatus status) {
         return (DYING.equals(status));
      }

      public boolean isDying() {
         return (isDying(this));
      }

      @Override
      public String toString() {
         return text;
      }
   }
}
