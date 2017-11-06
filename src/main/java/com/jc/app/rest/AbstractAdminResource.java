package com.jc.app.rest;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;

import org.json.JSONObject;

import com.jc.app.data.domain.AppLogRec;
import com.jc.app.service.AppServiceErrorCode;
import com.jc.app.service.ApplicationServiceManager;
import com.jc.command.task.Job;
import com.jc.command.task.Job.JobStatus;
import com.jc.command.task.JobQueue;
import com.jc.exception.SystemInfoException;
import com.jc.log.Logger;

public abstract class AbstractAdminResource extends RESTResource {

   @GET
   @Path("applog")
   @Produces("application/json")
   public Response getApplicationLogData(@Context SecurityContext sc, @QueryParam("fromDate") String fromDateStr, @QueryParam("toDate") String toDateStr) {
      try {
         Date fromDate = new Date();
         Date toDate = new Date();
         DateFormat df = new SimpleDateFormat("yyyy-MM-dd"); // NOTE: Incoming date string format follows ISO 8601 'yyyy-MM-dd'.

         try {
            if (fromDateStr != null && !fromDateStr.isEmpty()) {
               fromDate = df.parse(fromDateStr);
            }

            if (toDateStr != null && !toDateStr.isEmpty()) {
               toDate = df.parse(toDateStr);
            }
         }
         catch (ParseException pex) {
            String message = "Unable to parse the supplied date strings fromDate '" + fromDateStr + "' and toDate '" + toDateStr
                  + "', format must follow 'yyyy-MM-dd'.";
            throw new SystemInfoException(Level.SEVERE, AppServiceErrorCode.INVALID_DATE_FORMAT, message);
         }

         if (fromDate.after(toDate)) {
            String message = "The from date, " + df.format(fromDate) + ", cannot be after the to date " + df.format(toDate) + ".";
            throw new SystemInfoException(Level.SEVERE, AppServiceErrorCode.INVALID_FROM_DATE, message);
         }

         List<AppLogRec> data = ApplicationServiceManager.readAppLog(fromDate, toDate);
         JSONObject json = convertReportDataToJSONObject("applog", data, AppLogRec.class);
         return Response.ok(json.toString(), MediaType.APPLICATION_JSON).build();
      }
      catch (Exception ex) {
         return createExceptionResponse(getClass(), ex);
      }
   }

   @GET
   @Path("jobQueue")
   @Produces("application/json")
   public Response getJobQueueInfo(@Context SecurityContext sc, @QueryParam("jobId") String jobId, @QueryParam("jobQueueId") String jobQueueId) {
      try {

         JobQueue jobQueue = getApplicationJobQueue(jobQueueId);
         if (jobQueue != null) {
            JSONObject jobIdStatsJson = new JSONObject();
            if (jobId != null && !jobId.isEmpty()) {
               Vector<Job> jobVec = jobQueue.getJob(jobId);
               if (jobVec != null && !jobVec.isEmpty()) {

                  double amountCompleted = jobQueue.getAmountCompleted(jobId);
                  long estimatedRuntime = jobQueue.getEstimatedRuntime(jobId);
                  boolean isProcess = jobQueue.isAProcess(jobId);
                  JobStatus jobStatus = jobQueue.getJobStatus(jobId);
                  String logData = jobQueue.getLogData(jobId);

                  jobIdStatsJson.put("jobId", jobId);
                  jobIdStatsJson.put("amountCompleted", amountCompleted);
                  jobIdStatsJson.put("estimatedRuntime", estimatedRuntime);
                  jobIdStatsJson.put("process", isProcess);
                  jobIdStatsJson.put("status", jobStatus.toString().toLowerCase());
                  jobIdStatsJson.put("logData", logData);
               }

               return Response.ok(jobIdStatsJson.toString(), MediaType.APPLICATION_JSON).build();
            }
            else {
               JSONObject statistics = jobQueue.getStatistics();
               String timestampValue = RESTUtils.DATE_FORMAT_ISO8601.format(new Date());
               statistics.put("timestamp", timestampValue);
               return Response.ok(statistics.toString(), MediaType.APPLICATION_JSON).build();
            }
         }
         else {
            ResponseBuilder builder = Response.status(Status.NOT_IMPLEMENTED);
            return builder.build();
         }
      }
      catch (Exception ex) {
         return createExceptionResponse(getClass(), ex);
      }
   }

   @GET
   @Path("jobQueue/clear")
   public Response clearJobQueue(@Context SecurityContext sc, @QueryParam("jobId") String jobId, @QueryParam("jobQueueId") String jobQueueId) {
      try {
         JobQueue jobQueue = getApplicationJobQueue(jobQueueId);
         if (jobQueue != null) {
            if (jobId != null && !jobId.isEmpty()) {
               Vector<Job> jobVec = jobQueue.getJob(jobId);
               if (jobVec != null && !jobVec.isEmpty()) {
                  jobQueue.cancelJob(jobId);
                  Thread.sleep((30000)); // wait 30 seconds for job to stop.
                  jobQueue.removeJob(jobId);
               }
            }
            else {
               clearAllJobsFromJobQueue(jobQueue);
            }
            ResponseBuilder builder = Response.status(Status.NO_CONTENT);
            return builder.build();
         }
         else {
            ResponseBuilder builder = Response.status(Status.NOT_IMPLEMENTED);
            return builder.build();
         }
      }
      catch (Exception ex) {
         return createExceptionResponse(getClass(), ex);
      }
   }

   protected JobQueue getApplicationJobQueue(String jobQueueId) throws Exception {
      return null;
   }

   protected JSONObject convertReportDataToJSONObject(String logName, List<?> logDataList, Class<?> logRecType) throws Exception {
      List<Map<String, Object>> logEntries = new ArrayList<>();

      if (logDataList.size() > 0) {
         Object header = logDataList.get(0);
         Map<Field, String> reportFields = new LinkedHashMap<>();
         Map<String, Object> logHeaderEntry = new LinkedHashMap<>();

         // Assign list object field's to the equivalent report fields from the header which 
         // is assumed to be the first record...
         Field[] objfields = logRecType.getDeclaredFields();
         for (Field field : objfields) {
            field.setAccessible(true);
            Object headerValue = field.get(header);
            reportFields.put(field, (String) headerValue);
            logHeaderEntry.put((String) headerValue, headerValue);
         }

         logEntries.add(logHeaderEntry); // adds headers to the log entries...

         for (int i = 1; i < logDataList.size(); i++) {
            Object item = logDataList.get(i);
            Map<String, Object> logEntry = new LinkedHashMap<>();

            for (Field field : objfields) {
               field.setAccessible(true);

               String headerValue = reportFields.get(field); // get report header column name...
               String value = (String) field.get(item);

               // Sometimes the log file may have the header values at other line number other than the first; skip 'em.
               if (!value.equalsIgnoreCase(headerValue)) {
                  logEntry.put(headerValue, value);
               }
            }

            logEntries.add(logEntry);
         }
      }

      Map<String, Object> logData = new HashMap<>();
      logData.put(logName, logEntries);

      JSONObject jsonObj = RESTUtils.createJSONObjectFromMap(logData);
      return jsonObj;
   }

   private void clearAllJobsFromJobQueue(JobQueue jobQueue) throws Exception {
      // Cancel everything...
      jobQueue.cancelAllJobs();
      Thread.sleep((30000)); // wait 30 seconds for jobs to stop.

      // Remove all jobs regardless of if they are part of a process or not...
      List<String> jobIdList = jobQueue.getAllJobIds();
      if (!jobIdList.isEmpty()) {
         for (String jobId : jobIdList) {
            JobStatus jobStatus = jobQueue.getJobStatus(jobId);
            if (jobStatus.isDone() || jobStatus.isErrored() || jobStatus.isStopped()) {
               jobQueue.removeJob(jobId);
            }
            else {
               // try canceling again...
               jobQueue.cancelJob(jobId);
               Thread.sleep((30000)); // wait 30 seconds for jobs to stop.
               jobStatus = jobQueue.getStatusForProcess(jobId);
               if (jobStatus.isDone() || jobStatus.isErrored() || jobStatus.isStopped()) {
                  jobQueue.removeProcess(jobId);
               }
               else {
                  Logger.log(getClass(), Level.WARNING, "While trying to clear the jobQueue, unable to stop job with id of '" + jobId + "'.");
                  //            logMessage(Level.WARNING, "While trying to clear the jobQueue, unable to stop job with id of '" + jobId + "'.");
               }
            }
         }
      }
   }
}
