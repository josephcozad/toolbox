package com.jc;

import com.jc.util.ConfigInfo;

public class CommonsTestUtils {

   public final static String TEST_RESOURCE_DIR = "test.resource.dir";

   public final static String WORKING_DIR = "<PATH_TO_WORKING_DIR>";
   public final static String TESTING_RESOURCES_DIR = "<PATH_TO_TESTING_RESOURCES>";
   public final static String APPLICTION_LOG_DIR = "<PATH_TO_APP_LOG_DIR>";
   public final static String DATABASE_LOG_DIR = "<PATH_TO_DATABASE_LOG_DIR>";
   public final static String JOBQUEUE_LOG_DIR = "<PATH_TO_JOBQUEUE_LOG_DIR>";

   public static void loadConfigInfo() throws Exception {

      ConfigInfo info = ConfigInfo.getInstance();

      info.addProperty(ConfigInfo.USER_DIR_PROPKEY, WORKING_DIR);

      info.addProperty(TEST_RESOURCE_DIR, TESTING_RESOURCES_DIR);

      info.addProperty("log.app.directory", APPLICTION_LOG_DIR);
      info.addProperty("log.app.namePrefix", "appLog");
      //         info.addProperty("log.app.logToConsole", "true");

      info.addProperty("log.jobQueue.directory", JOBQUEUE_LOG_DIR);
      info.addProperty("log.jobQueue.namePrefix", "jobQLog");
      //         info.addProperty("log.jobQueue.logToConsole", "true");

      info.addProperty("log.db.directory", DATABASE_LOG_DIR);
      info.addProperty("log.db.namePrefix", "dbLog");
      //         info.addProperty("log.db.logToConsole", "true");
   }
}
