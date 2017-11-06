package com.jc.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public abstract class ExcelProcessor {

   public static final String MISSING_DATA = "Missing Data";

   public static ArrayList<ArrayList<String>> parseCSVDocument(InputStream inputFile) throws Exception {
      BufferedReader reader = null;
      try {
         ArrayList<ArrayList<String>> contents = new ArrayList<ArrayList<String>>();

         reader = new BufferedReader(new InputStreamReader(inputFile));
         String inputLine = null;
         int rowSize = -1;
         int lineNum = 0;
         do {
            inputLine = reader.readLine();
            lineNum++;
            if (inputLine != null && inputLine.length() > 0) {
               String[] fieldData = inputLine.split(",", -1);
               if (rowSize == -1) { // initialize rowSize
                  rowSize = fieldData.length;
               }
               else if (rowSize != fieldData.length) {
                  throw (new Exception("Error while reading CSV file, row " + lineNum + " was not the same size as previously read rows."));
               }

               int num_empty_cells = 0;

               ArrayList<String> rowData = new ArrayList<String>();
               for (String data : fieldData) {
                  data = cleanValueString(data.trim());

                  if (data.isEmpty()) {
                     num_empty_cells++;
                  }
                  rowData.add(data);
               }

               if (num_empty_cells < rowSize) {
                  contents.add(rowData);
               }
            }
         } while (inputLine != null);

         return contents;
      }
      catch (Exception ex) {
         throw ex;
      }
      finally {
         try {
            reader.close();
         }
         catch (IOException ex) {
            throw (ex);
         }
      }
   }

   public static ArrayList<ArrayList<String>> parseExcelDocument(InputStream inputFile) throws Exception {
      try {
         ArrayList<ArrayList<String>> contents = new ArrayList<ArrayList<String>>();

         Workbook wb = WorkbookFactory.create(inputFile);
         Sheet sheet = wb.getSheetAt(0);
         Row row = null;

         short minColIdx = 0;
         short maxColIdx = 0;

         for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            row = sheet.getRow(i);
            if (row != null) {
               ArrayList<String> rowdata = new ArrayList<String>();

               if (i == 0) {
                  minColIdx = row.getFirstCellNum();
                  maxColIdx = row.getLastCellNum();
               }

               int num_empty_cells = 0;

               for (short colIx = minColIdx; colIx < maxColIdx; colIx++) {
                  Cell cell = row.getCell(colIx);
                  if (cell != null) {
                     String value = null;

                     cell.setCellType(Cell.CELL_TYPE_STRING);
                     value = cell.getStringCellValue();
                     value = cleanValueString(value.trim());

                     if (value.isEmpty()) {
                        num_empty_cells++;
                     }

                     rowdata.add(value);
                  }
                  else {
                     num_empty_cells++;
                     rowdata.add("");
                  }
               }

               if (num_empty_cells < maxColIdx) {
                  contents.add(rowdata);
               }
            }
         }

         return (contents);
      }
      catch (Exception ex) {
         throw ex;
      }
      finally {
         try {
            inputFile.close();
         }
         catch (IOException ex) {
            throw ex;
         }
      }
   }

   //   protected static Map<Integer, Integer> getColumnMap(ArrayList<String> row) throws Exception {
   //      Map<Integer, Integer> colmap = new HashMap<Integer, Integer>();
   //
   //      //Required fields:  Organization Name, OrgAbbrev, Address1, City, State, Zip Code, Phone, TIN, XDEndpoint, Domain
   //      ArrayList<String> reqColList = new ArrayList<String>();
   //      for (String coltitle : REQUIRED_ORG_COLUMNS) {
   //         reqColList.add(coltitle);
   //      }
   //
   //      for (int counter = 0; counter < row.size(); counter++) {
   //         String value = row.get(counter);
   //         value = value.replaceAll(" ", "");
   //
   //         if ("OrganizationName".equalsIgnoreCase(value)) {
   //            colmap.put(ORGANIZATION_COLUMN_ORG_NAME, counter);
   //            reqColList.set(0, "");
   //         }
   //         else if ("OrgAbbrev".equalsIgnoreCase(value)) {
   //            colmap.put(ORGANIZATION_COLUMN_ORG_ABBREV, counter);
   //            reqColList.set(1, "");
   //         }
   //         else if ("Specialty".equalsIgnoreCase(value)) {
   //            colmap.put(ORGANIZATION_COLUMN_SPECIALTY, counter);
   //         }
   //         else if ("Address1".equalsIgnoreCase(value)) {
   //            colmap.put(ORGANIZATION_COLUMN_ADDRESS1, counter);
   //            reqColList.set(2, "");
   //         }
   //         else if ("Address2".equalsIgnoreCase(value)) {
   //            colmap.put(ORGANIZATION_COLUMN_ADDRESS2, counter);
   //         }
   //         else if ("City".equalsIgnoreCase(value)) {
   //            colmap.put(ORGANIZATION_COLUMN_CITY, counter);
   //            reqColList.set(3, "");
   //         }
   //         else if ("State".equalsIgnoreCase(value)) {
   //            colmap.put(ORGANIZATION_COLUMN_STATE, counter);
   //            reqColList.set(4, "");
   //         }
   //         else if ("ZipCode".equalsIgnoreCase(value) || "Zip".equalsIgnoreCase(value)) {
   //            colmap.put(ORGANIZATION_COLUMN_ZIP, counter);
   //            reqColList.set(5, "");
   //         }
   //         else if ("Phone".equalsIgnoreCase(value) || "Telephone".equalsIgnoreCase(value)) {
   //            colmap.put(ORGANIZATION_COLUMN_PHONE, counter);
   //            reqColList.set(6, "");
   //         }
   //         else if ("TIN".equalsIgnoreCase(value) || "CDH".equalsIgnoreCase(value)) {
   //            colmap.put(ORGANIZATION_COLUMN_TIN, counter);
   //            reqColList.set(7, "");
   //         }
   //         else if ("XDEndPoint".equalsIgnoreCase(value) || "PrimaryXDEndPoint".equalsIgnoreCase(value)) {
   //            colmap.put(ORGANIZATION_COLUMN_ENDPOINT, counter);
   //            reqColList.set(8, "");
   //         }
   //         else if ("OID".equalsIgnoreCase(value)) {
   //            colmap.put(ORGANIZATION_COLUMN_OID, counter);
   //         }
   //         else if ("Domain".equalsIgnoreCase(value)) {
   //            colmap.put(ORGANIZATION_COLUMN_DOMAIN, counter);
   //            reqColList.set(9, "");
   //         }
   //      }
   //
   //      //Check for required columns here....
   //      StringBuilder sb = new StringBuilder();
   //      for (String coltitle : reqColList) {
   //         if (!coltitle.isEmpty()) {
   //            sb.append("'" + coltitle + "', ");
   //         }
   //      }
   //
   //      if (sb.length() > 0) {
   //         String title_list = sb.toString();
   //         title_list = title_list.substring(0, title_list.length() - 2); // remove last two chars.
   //
   //         String message = "The following fields were not found in the organization upload file and are required: " + title_list;
   //         throw (new LoggableException(BulkLoadExcelProcessor.class, Level.SEVERE, message));
   //      }
   //
   //      return colmap;
   //   }

   protected static String getDataForPosition(ArrayList<String> row, Integer position) {
      String value = MISSING_DATA;
      if (position != null) {
         value = row.get(position);
         value = cleanValueString(value.trim());
      }

      return value;
   }

   protected static String cleanValueString(String value) {
      char[] chars = value.toCharArray();
      char[] newchars = new char[chars.length];
      for (int i = 0, j = 0; i < chars.length; i++) {
         if (chars[i] > 31 && chars[i] < 127) {
            newchars[j] = chars[i];
            j++;
         }
      }
      value = new String(newchars);
      return (value);
   }
}
