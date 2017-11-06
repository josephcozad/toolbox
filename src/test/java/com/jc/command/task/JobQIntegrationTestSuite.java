package com.jc.command.task;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
      TaskTester.class, //
      SingleTaskSingleThreadNonSlaveJobTest.class, //
      MultiTaskSingleThreadNonSlaveJobTest.class, //
      SingleTaskSingleThreadSlaveJobTest.class, //
      MultiTaskSingleThreadSlaveJobTest.class, //
      MultiTaskMultiThreadedNonSlaveJobTest.class, //
      MultiTaskMultiThreadedSlaveJobTest.class, //
      //   JobQueueTest.class, //
      //   ThreadMonitorTester.class

})
public class JobQIntegrationTestSuite {

}
