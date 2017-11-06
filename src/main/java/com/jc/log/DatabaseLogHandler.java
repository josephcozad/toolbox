package com.jc.log;

import java.io.IOException;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

public class DatabaseLogHandler extends Handler {

   private static final int STATE_FRESH = 0;
   private static final int STATE_PUBLISHED = 1;
   private static final int STATE_CLOSED = 2;

   private int streamState = STATE_FRESH;

   private final LogDatasource datasource;

   public DatabaseLogHandler(LogDatasource datasource) throws IOException, SecurityException {
      super();
      init();
      this.datasource = datasource;
   }

   @Override
   public void close() throws SecurityException {
      LogManager.getLogManager().checkAccess();

      try {
         checkOpen();

         if (datasource != null) {
            streamState = STATE_CLOSED;
            datasource.close();
         }
      }
      catch (Exception ex) {
         reportError(null, ex, ErrorManager.CLOSE_FAILURE);
      }
   }

   @Override
   public void flush() {
      // Not used in this implementation.
   }

   @Override
   public void publish(LogRecord record) {
      if (!isLoggable(record)) {
         return;
      }

      Formatter formatter = getFormatter();
      String formattedMessage;

      try {
         formattedMessage = formatter.format(record);
         formattedMessage = formattedMessage.substring(0, formattedMessage.length() - 1);
      }
      catch (Exception ex) {
         reportError(null, ex, ErrorManager.FORMAT_FAILURE);
         return;
      }

      try {
         int numFields = LogFieldsEnum.getNumberOfFields();

         String[] fields = formattedMessage.split("\\|", numFields);

         String head = formatter.getHead(this);
         head = head.substring(0, head.length() - 1);

         String[] fieldKeys = head.split("\\|", numFields);

         if (fieldKeys.length == fields.length) {
            for (int i = 0; i < fieldKeys.length; i++) {
               LogFieldsEnum fieldKey = LogFieldsEnum.get(fieldKeys[i]);
               switch (fieldKey) {
                  case NANO_SECS:
                     datasource.setNanoSecs(fields[i]);
                     break;
                  case DATE_STAMP:
                     datasource.setDateStamp(fields[i]);
                     break;
                  case LOG_LEVEL:
                     datasource.setLogLevel(fields[i]);
                     break;
                  case CLASS_FIELD_INFO:
                     datasource.setClassFieldInfo(fields[i]);
                     break;
                  case MESSAGE:
                     datasource.setMessage(fields[i]);
                     break;
                  default:
                     // Ignore if it's unknown.
                     break;
               }
            }

            datasource.persist();
         }
         else {
            throw (new IllegalArgumentException("Message fields and field keys are not the same number."));
         }
      }
      catch (Exception ex) {
         reportError(null, ex, ErrorManager.WRITE_FAILURE);
      }
   }

   private void checkOpen() {
      if (streamState == STATE_CLOSED) {
         throw new IllegalStateException(this.toString() + " has been closed");
      }
   }

   private void init() {
      LogFileFormatter formatterTxt = new LogFileFormatter();
      setFormatter(formatterTxt);

      LogFilter filter = new LogFilter();
      setFilter(filter);
   }
}
