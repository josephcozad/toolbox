package com.jc.command.task;

import com.jc.command.task.Job.JobStatus;

public interface JobStatusListener {

   public void jobStatusChanged(String jobId, JobStatus oldStatus, JobStatus newStatus);
}
