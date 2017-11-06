package com.jc.db;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/*
 * This class represents a data set retrieved from the database. The methods are similar in function to those methods in the java.sql.ResultSet class. This
 * class however, retains the data set in memory as opposed to an open persistent connection to the database. In addition this class contains error, warning,
 * and timing information related to the query run.
 */
public class DBResult extends ResultSetProcessor {

   // The data set retrieved from the ResultSet object for the query request.
   private final List<List<Object>> Data;

   private Map<String, ColumnMetaData> ColumnInfo;

   // The index into the Data collection.
   private int RowIndex = -1;

   private int NumRowsUpdated = 0;

   // If set, the request was a simple query.
   private String Query;

   public DBResult() {
      Data = new ArrayList<List<Object>>();
      ColumnInfo = new LinkedHashMap<String, ColumnMetaData>();
   }

   public Object getValue(int columnIndex) {
      if (RowIndex < 0) {
         RowIndex++;
      }
      return getDataAt(RowIndex, columnIndex - 1);
   }

   /**
    * Returns the data at the current row and given column as a String.
    *
    * @param columnIndex
    * @return the String value at the current row and given column
    *
    */
   public String getString(int columnIndex) {
      if (RowIndex < 0) {
         RowIndex++;
      }
      return getStringAt(RowIndex, columnIndex - 1);
   }

   /**
    * Returns the data at the current row and given column as a java.util.Date.
    *
    * @param columnIndex
    * @return the Date value at the current row and given column
    *
    */
   public java.util.Date getDate(int columnIndex) {
      if (RowIndex < 0) {
         RowIndex++;
      }
      return getDateAt(RowIndex, columnIndex - 1);
   }

   /**
    * Returns the data at the current row and given column as a double.
    *
    * @param columnIndex
    * @return the double value at the current row and given column
    *
    */
   public double getDouble(int columnIndex) {
      if (RowIndex < 0) {
         RowIndex++;
      }
      return getDoubleAt(RowIndex, columnIndex - 1);
   }

   /**
    * Returns the data at the current row and given column as a long.
    *
    * @param columnIndex
    * @return the long value at the current row and given column
    *
    */
   public long getLong(int columnIndex) throws NullValueException {
      if (RowIndex < 0) {
         RowIndex++;
      }
      return getLongAt(RowIndex, columnIndex - 1);
   }

   /**
    * Returns the data at the current row and given column as a float.
    *
    * @param columnIndex
    * @return the float value at the current row and given column
    *
    */
   public float getFloat(int columnIndex) {
      if (RowIndex < 0) {
         RowIndex++;
      }
      return getFloatAt(RowIndex, columnIndex - 1);
   }

   /**
    * Returns the data at the current row and given column as an integer.
    *
    * @param columnIndex
    * @return the integer value at the current row and given column
    *
    */
   public int getInt(int columnIndex) throws NullValueException {
      if (RowIndex < 0) {
         RowIndex++;
      }
      return getIntegerAt(RowIndex, columnIndex - 1);
   }

   /**
    * Returns the the data value at the current row location, using the next() method, for the column name specified, as a String object.
    */
   public String getString(String columnName) {
      try {
         return getStringAt(RowIndex, getColumnIdFor(columnName));
      }
      catch (IllegalArgumentException e) {
         throw new IllegalArgumentException("Unknown column \"" + columnName + "\"", e);
      }
   }

   /**
    * Returns the the data value at the current row location, using the next() method, for the column name specified, as a Date object.
    */
   public java.util.Date getDate(String columnName) {
      try {
         return getDateAt(RowIndex, getColumnIdFor(columnName));
      }
      catch (IllegalArgumentException e) {
         throw new IllegalArgumentException("Unknown column \"" + columnName + "\"", e);
      }
   }

   /**
    * Returns the the data value at the current row location, using the next() method, for the column name specified, as a double value. If the value cannot be returned as a double a NaN value is returned.
    */
   public double getDouble(String columnName) {
      int col_index = getColumnIdFor(columnName);
      return getDoubleAt(RowIndex, col_index);
   }

   /**
    * Returns the the data value at the current row location, using the next() method, for the column name specified, as a double value. If the value cannot be returned as a double a NaN value is returned.
    */
   public long getLong(String columnName) throws NullValueException {
      try {
         return getLongAt(RowIndex, getColumnIdFor(columnName));
      }
      catch (IllegalArgumentException e) {
         throw new IllegalArgumentException("Unknown column \"" + columnName + "\"", e);
      }
   }

   /**
    * Returns the the data value at the current row location, using the next() method, for the column name specified, as a float value. If the value cannot be returned as a double a NaN value is returned.
    */
   public float getFloat(String columnName) {
      try {
         return getFloatAt(RowIndex, getColumnIdFor(columnName));
      }
      catch (IllegalArgumentException e) {
         throw new IllegalArgumentException("Unknown column \"" + columnName + "\"", e);
      }
   }

   /**
    * Returns the the data value at the current row and given column as an int value.
    *
    * @param columnName
    *           the column name of the data
    * @return the integer value at the current row and given column
    *
    *         if the given column cannot be found
    */
   public int getInt(String columnName) throws NullValueException {
      try {
         return getIntegerAt(RowIndex, getColumnIdFor(columnName));
      }
      catch (IllegalArgumentException e) {
         throw new IllegalArgumentException("Unknown column \"" + columnName + "\"", e);
      }
   }

   /**
    * Moves the row index pointer one row from its current position. The row index pointer is initially positioned before the first row (1). The first call to this method makes the first row the current row, the second call makes the second row the current row and so on.
    */
   public boolean next() {
      if (RowIndex + 1 < Data.size()) {
         RowIndex++;
         return true;
      }
      return false;
   }

   /**
    * Resets the row index pointer to the beginning of the data set.
    */
   public void reset() {
      RowIndex = -1;
   }

   /**
    * Set the row index pointer to the row specified. If the supplied value is greater than the size of the data set, then the pointer is reset to the beginning of the data set. If the supplied value is less than 0 then the pointer is reset to the beginning of the data set.
    */
   public void setRowIndex(int index) {
      if (0 <= index && index < Data.size()) {
         RowIndex = index;
      }
      else {
         reset();
      }
   }

   // /**
   // * Returns the complete set of data as a double Object array. Each column of data is represented as a Java object, either String, Integer, Float, Double,
   // * etc. depending on what the value is in the database. Column types can be retrieved using the getColumnSQLTypes() or getColumnSQLType() methods.
   // */
   // public Object[][] getData() {
   // return Data;
   // }

   /**
    * returns true for an empty result set
    */
   public boolean isEmpty() {
      return Data.isEmpty();
   }

   /**
    * The number of rows that were updated during an update.
    */
   public int getNumRowsUpdated() {
      return NumRowsUpdated;
   }

   /**
    * Returns the query string that was sent to the database.
    */
   @Override
   public String getQuery() {
      return Query;
   }

   /**
    * Returns an array of column names for the resulting query.
    */
   public String[] getColumnNames() {
      List<String> columnNamesList = ColumnMetaData.getColumnNames(ColumnInfo);
      return columnNamesList.toArray(new String[columnNamesList.size()]);
   }

   /**
    * Returns the column names specified by the col id for the resulting query. The first column is referenced as '1'.
    */
   public String getColumnName(int column) {
      try {
         return getColumnNames()[column];
      }
      catch (ArrayIndexOutOfBoundsException e) {
         throw new IllegalArgumentException("Invalid column index " + column, e);
      }
   }

   /**
    * Returns an array of ints representing the SQLTypes (java.sql.Types) for each column for the resulting query.
    */
   public int[] getColumnSQLTypes() {
      return ColumnMetaData.getSqlTypes(ColumnInfo);
   }

   /**
    * Returns the SQLType (java.sql.Types) for the column specified by the supplied col id for the resulting query.
    */
   public int getColumnSQLType(int column) {
      try {
         return getColumnSQLTypes()[column];
      }
      catch (ArrayIndexOutOfBoundsException e) {
         throw new IllegalArgumentException("Invalid column index " + column, e);
      }
   }

   /**
    * Returns an array of java class names representing the data for each column in the resulting query.
    */
   public String[] getColumnClassNames() {
      List<String> classNamesList = ColumnMetaData.getClassNames(ColumnInfo);
      return classNamesList.toArray(new String[classNamesList.size()]);
   }

   /**
    * Returns the java class name representing the data for the column specified by the supplied col id in the resulting query.
    */
   public String getColumnClassName(int column) {
      try {
         return getColumnClassNames()[column];
      }
      catch (ArrayIndexOutOfBoundsException e) {
         throw new IllegalArgumentException("Invalid column index " + column, e);
      }
   }

   @Override
   public String toString() {
      if (isEmpty()) {
         return "No data exists, the table is empty.";
      }

      StringBuilder sb = new StringBuilder();
      for (Map.Entry<String, ColumnMetaData> entry : ColumnInfo.entrySet()) {
         ColumnMetaData column = entry.getValue();
         if (sb.length() > 0) {
            sb.append('\t');
         }
         sb.append(column.getColumnName());
      }
      sb.append('\n');

      int len = sb.length();

      for (List<Object> row : Data) {
         for (Object value : row) {
            if (sb.length() > len) {
               sb.append('\t');
            }
            sb.append(value);
         }
         sb.append('\n');
      }

      return sb.toString();
   }

   protected void setColumnNames(String[] columnNames) {
      if (ColumnInfo.isEmpty() || ColumnInfo.size() == columnNames.length) {
         ColumnInfo.clear();
         for (int i = 0; i < columnNames.length; i++) {
            ColumnInfo.put(columnNames[i].toLowerCase(), new ColumnMetaData(columnNames[i], i));
         }
      }
      else {
         // TODO: don't allow this to happen... throw exception??? or just ignore???
      }
   }

   protected void setData(Object[][] data) {
      Data.clear();
      for (Object[] rowData : data) {
         Data.add(Arrays.asList(rowData));
      }
   }

   // This method saves these values to use in error reporting.
   protected void setQuery(String query) {
      Query = query;
   }

   // ------------------------ Package Only Methods ------------------------ //

   // This method saves these values to use in error reporting.
   void setProcedureInfo(String statementTemplate, Object[] inParameters) {
      StringBuilder query = new StringBuilder(statementTemplate);
      int index = 0;

      if (inParameters != null && inParameters.length > 0) {
         for (Object param : inParameters) {
            index = statementTemplate.indexOf('?', index);
            query.delete(index, index + 1);
            if (param instanceof String) {
               query.insert(index, "'" + param + "'");
               query.replace(0, 0, "");
            }
            else {
               query.insert(index, param);
            }
            statementTemplate = query.toString();
         }
      }
      else {
         statementTemplate = statementTemplate.replace("?", "");
         statementTemplate = statementTemplate.replace(",", "");
         statementTemplate = statementTemplate.replace("  ", ""); // Replace two spaces with none
      }

      setQuery(statementTemplate);
   }

   /**
    * Given the row and column number the data value of that location in the data set is returned.
    */
   Object getDataAt(int row, int column) {
      if (checkArrayBound(row, Data.size()) && checkArrayBound(column, Data.get(0).size())) {
         List<Object> row_data = Data.get(row);
         Object value = row_data.get(column);
         return (value);
      }
      throw new IllegalArgumentException("No data at row " + row + ", col " + column);
   }

   /**
    * Returns the specified row of data from the table of results. First row is 0.
    */
   Object[] getDataRow(int index) {
      if (checkArrayBound(index, Data.size())) {
         List<Object> dataList = Data.get(index);
         return dataList.toArray(new Object[dataList.size()]);
      }
      throw new IllegalArgumentException("Invalid row index " + index);
   }

   void setNumRowsUpdated(int numRowsUpdated) {
      NumRowsUpdated = numRowsUpdated;
   }

   @Override
   public void processResultSet(ResultSet result) throws SQLException {
      ColumnInfo = DBResult.getColumnInfo(result, false);

      Map<String, ColumnMetaData> columnInfo = getColumnInfo();

      while (result.next()) {
         List<Object> columns = new ArrayList<Object>(columnInfo.size());

         for (Map.Entry<String, ColumnMetaData> entry : columnInfo.entrySet()) {
            ColumnMetaData column = entry.getValue();
            Object value = DBResult.getValueFromResultSet(column, result);
            columns.add(value);
         }
         addColumnData(columns);
         incrementRecordCount();
      }
   }

   Map<String, ColumnMetaData> getColumnInfo() {
      return (ColumnInfo);
   }

   void addColumnData(List<Object> column_data) {
      Data.add(column_data);
   }

   void setErrors(Throwable[] errorExceptions) {
      for (Throwable error : errorExceptions) {
         addError(error);
      }
   }

   void setWarnings(String[] warnings) {
      for (String message : warnings) {
         addWarning(message);
      }
   }

   void setOutParameter(Object value, String columnName, int sqlType, Class<?> cls, int columnIndex) {
      String className = cls == null ? null : cls.getName();
      ColumnMetaData metadata = new ColumnMetaData(columnName, sqlType, className, columnIndex);
      ColumnInfo.put(metadata.getColumnName().toLowerCase(), metadata);
      if (value != null) {
         // Assume that Data will have only one row...
         if (Data.size() > 1) {
            Data.clear();
         }

         if (Data.isEmpty()) {
            Data.add(new ArrayList<Object>());
         }

         List<Object> row = Data.get(0);

         if (row.size() < columnIndex + 1) {
            List<Object> tmp = new ArrayList<Object>(columnIndex + 1);

            // Transfer all the data from 'row' to the tmp object...
            for (Object element : row) {
               tmp.add(element);
            }

            // Add nulls up to and including the column we are going to add to.
            for (int i = row.size(); i < columnIndex; i++) {
               tmp.add(null);
            }

            // point 'row' to the tmp object...
            row = tmp;
         }

         row.set(columnIndex - 1, value);
         Data.set(0, row); // have to re-set it to location 0 because row object is not the same as was was retrieved from Data to begin with.
      }
   }

   private static boolean checkArrayBound(int index, int max) {
      return 0 <= index && index < max;
   }

   /**
    * Returns the column id for the supplied column name. If not found, then a -1 is returned.
    */
   private int getColumnIdFor(String columnName) {
      String key = columnName.toLowerCase();
      if (ColumnInfo.containsKey(key)) {
         return ColumnInfo.get(key).index() - 1;
      }
      else {
         throw (new IllegalArgumentException("Unknown column \"" + columnName + "\""));
      }
   }

   /**
    * Returns a String value at the row/column location specified. First row and first column are referenced as 0.
    *
    * @param row
    * @param column
    * @return the String value at (row, column)
    *
    *         if value at (row, column) can't be cast to an String (via String, Number, or Date)
    */
   private String getStringAt(int row, int column) {
      Object value = getDataAt(row, column);
      if (value == null) {
         return null;
      }
      else if (value instanceof String) {
         return (String) value;
      }
      else if (value instanceof Number) {
         return ((Number) value).toString();
      }
      else if (value instanceof java.util.Date) {
         return ((java.util.Date) value).toString();
      }
      else {
         return (value.toString());
      }
   }

   /**
    * Returns a Date value at the row/column location specified. First row and first column are referenced as 0.
    *
    * @param row
    * @param column
    * @return the java.util.Date value at (row, column)
    *
    *         if value at (row, column) can't be cast to a java.util.Date (via java.sql.Date or Timestamp)
    */
   private java.util.Date getDateAt(int row, int column) {
      Object value = getDataAt(row, column);
      if (value == null) {
         return null;
      }
      else if (value instanceof java.sql.Date || value instanceof Timestamp) {
         return (java.util.Date) value;
      }
      throw new IllegalArgumentException("No Date value at row " + row + ", column " + column);
   }

   /**
    * Returns a double value at the row/column location specified. First row and first column are referenced as 0.
    *
    * @param row
    * @param column
    * @return the double value at (row, column)
    * @throws IllegalArgumentException
    *            if the value at (row, column) can't be cast to a double
    */
   private double getDoubleAt(int row, int column) {
      try {
         Object data = getDataAt(row, column);
         if (data == null) {
            return (Double.NaN);
         }
         else if (data instanceof Number) {
            return (((Number) data).doubleValue());
         }
         else if (data instanceof String) {
            try {
               return (Double.parseDouble((String) data));
            }
            catch (NumberFormatException ex) {
               throw new IllegalArgumentException("No double value at row " + row + ", column " + column, ex);
            }
         }
         else {
            throw new IllegalArgumentException("No double value at row " + row + ", column " + column);
         }
      }
      catch (NullPointerException e) {
         return (Double.NaN);
      }
   }

   /**
    * Returns a long value at the row/column location specified. First row and first column are referenced as 0.
    *
    * @param row
    * @param column
    * @return the long value at (row, column)
    * @throws IllegalArgumentException
    *            if the value at (row, column) can't be cast to a long
    */
   private long getLongAt(int row, int column) throws NullValueException {
      try {
         Object data = getDataAt(row, column);
         if (data == null) {
            throw (new NullValueException());
         }
         else if (data instanceof Number) {
            return (((Number) data).longValue());
         }
         else if (data instanceof String) {
            try {
               return (Long.parseLong((String) data));
            }
            catch (NumberFormatException ex) {
               throw new IllegalArgumentException("No long value at row " + row + ", column " + column, ex);
            }
         }
         else {
            throw new IllegalArgumentException("No long value at row " + row + ", column " + column);
         }
      }
      catch (NullPointerException e) {
         throw (new NullValueException(e));
      }
   }

   /**
    * Returns a float value at the row/column location specified. First row and first column are referenced as 0.
    *
    * @param row
    * @param column
    * @return the float value at (row, column)
    * @throws IllegalArgumentException
    *            if the value at (row, column) can't be cast to a float
    */
   private float getFloatAt(int row, int column) {
      try {
         Object data = getDataAt(row, column);
         if (data == null) {
            return (Float.NaN);
         }
         else if (data instanceof Number) {
            return (((Number) data).floatValue());
         }
         else if (data instanceof String) {
            try {
               return (Float.parseFloat((String) data));
            }
            catch (NumberFormatException ex) {
               throw new IllegalArgumentException("No float value at row " + row + ", column " + column, ex);
            }
         }
         else {
            throw new IllegalArgumentException("No float value at row " + row + ", column " + column);
         }
      }
      catch (NullPointerException e) {
         return (Float.NaN);
      }
   }

   /**
    * Returns an integer value at the row/column location specified. First row and first column are referenced as 0.
    *
    * @param row
    * @param column
    * @return the integer value at (row, column)
    * @throws IllegalArgumentException
    *            if the value at (row, column) can't be cast to an integer
    */
   private int getIntegerAt(int row, int column) throws NullValueException {
      try {
         Object data = getDataAt(row, column);
         if (data == null) {
            throw (new NullValueException());
         }
         else if (data instanceof Number) {
            return (((Number) data).intValue());
         }
         else if (data instanceof String) {
            try {
               return (Integer.parseInt((String) data));
            }
            catch (NumberFormatException ex) {
               throw new IllegalArgumentException("No integer value at row " + row + ", column " + column, ex);
            }
         }
         else {
            throw new IllegalArgumentException("No integer value at row " + row + ", column " + column);
         }
      }
      catch (NullPointerException e) {
         throw (new NullValueException(e));
      }
   }

   public static Map<String, ColumnMetaData> getColumnInfo(ResultSet result, boolean includeTableName) throws SQLException {
      Map<String, ColumnMetaData> columnInfo = new LinkedHashMap<String, ColumnMetaData>();
      ResultSetMetaData meta_data = result.getMetaData();
      for (int i = 0; i < meta_data.getColumnCount(); ++i) {
         ColumnMetaData metadata = new ColumnMetaData(meta_data, i + 1);

         String columnProp = metadata.getColumnName().toLowerCase();
         if (includeTableName) {
            String tableName = metadata.getTableName().toLowerCase();
            columnProp = tableName + "." + columnProp;
         }

         columnInfo.put(columnProp, metadata);
      }
      return columnInfo;
   }

   public static Object getValueFromResultSet(ColumnMetaData column, ResultSet result) throws SQLException {
      Object value = null;

      Object obj = result.getObject(column.index());
      if (obj != null) {
         switch (column.getSqlType()) {
            case Types.ARRAY:
               value = result.getArray(column.index());
               break;
            case Types.BIT:
               value = result.getBoolean(column.index());
               break;
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
               value = result.getString(column.index());
               break;
            case Types.TIME:
               Time time = result.getTime(column.index());
               value = new Date(time.getTime());
               break;
            case Types.TIMESTAMP:
               Timestamp timestamp = result.getTimestamp(column.index());
               value = new Date(timestamp.getTime());
               break;
            case Types.DATE: // Check to make sure that this is really a DATE object.
               if (column.getClassName().equalsIgnoreCase("java.sql.Timestamp")) {
                  timestamp = result.getTimestamp(column.index());
                  value = new Date(timestamp.getTime());
               }
               else if (column.getClassName().equalsIgnoreCase("java.sql.Time")) {
                  time = result.getTime(column.index());
                  value = new Date(time.getTime());
               }
               else { // Date object
                  java.sql.Date date = result.getDate(column.index());
                  value = new Date(date.getTime());
               }
               break;
            case Types.NUMERIC:
               value = result.getBigDecimal(column.index());
               break;
            case Types.TINYINT:
               value = Byte.valueOf(result.getByte(column.index()));
               break;
            case Types.SMALLINT:
               value = Short.valueOf(result.getShort(column.index()));
               break;
            case Types.BIGINT:
               value = BigInteger.valueOf(result.getLong(column.index()));
               break;
            case Types.DOUBLE:
               value = Double.valueOf(result.getDouble(column.index()));
               break;
            case Types.DECIMAL:
               value = result.getBigDecimal(column.index());
               break;
            case Types.FLOAT:
               value = Float.valueOf(result.getFloat(column.index()));
               break;
            case Types.INTEGER:
               value = Integer.valueOf(result.getInt(column.index()));
               break;
            case Types.BLOB:
               value = result.getBlob(column.index());
               break;
            case Types.CLOB:
               // value = result.getClob(column.index());
               value = result.getString(column.index());
               break;
            default:
               //        addWarning("Unknown object type " + column.getSqlType() + " in column '" + (getColumnName(column.index())) + "'.");
               value = result.getString(column.index());
               break;
         }
      }

      return value;
   }

   public static final class ColumnMetaData {

      private final String ColumnName;
      private final int SqlType;
      private final String ClassName;
      private final int ColumnIndex;
      private String SchemaName;
      private String TableName;

      ColumnMetaData(String columnName, int column) {
         this(columnName, Types.OTHER, null, column);
      }

      ColumnMetaData(String columnName, int sqlType, String className, int column) {
         ColumnName = columnName.toLowerCase();
         SqlType = sqlType;
         ClassName = className;
         ColumnIndex = column;
      }

      ColumnMetaData(ResultSetMetaData metaData, int column) throws SQLException {
         this(metaData.getColumnLabel(column), metaData.getColumnType(column), metaData.getColumnClassName(column), column);
         SchemaName = metaData.getSchemaName(column);
         TableName = metaData.getTableName(column);
      }

      public String getColumnName() {
         return ColumnName;
      }

      public int getSqlType() {
         return SqlType;
      }

      public String getClassName() {
         return ClassName;
      }

      public int index() {
         return ColumnIndex;
      }

      public String getTableName() {
         return TableName;
      }

      public String getSchemaName() {
         return SchemaName;
      }

      public static List<String> getColumnNames(Map<String, ColumnMetaData> metadata) {
         List<String> columnNames = new ArrayList<String>(metadata.size());
         for (Entry<String, ColumnMetaData> entry : metadata.entrySet()) {
            ColumnMetaData column = entry.getValue();
            columnNames.add(column.getColumnName());
         }
         return columnNames;
      }

      public static int[] getSqlTypes(Map<String, ColumnMetaData> metadata) {
         int[] sqlTypes = new int[metadata.size()];
         int i = 0;
         for (Entry<String, ColumnMetaData> entry : metadata.entrySet()) {
            ColumnMetaData column = entry.getValue();
            sqlTypes[i] = column.getSqlType();
            ++i;
         }
         return sqlTypes;
      }

      public static List<String> getClassNames(Map<String, ColumnMetaData> metadata) {
         List<String> classNames = new ArrayList<String>(metadata.size());
         for (Entry<String, ColumnMetaData> entry : metadata.entrySet()) {
            ColumnMetaData column = entry.getValue();
            classNames.add(column.getClassName());
         }
         return classNames;
      }
   }
}
