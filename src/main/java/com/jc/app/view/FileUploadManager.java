package com.jc.app.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

public abstract class FileUploadManager implements Serializable {

   private static final long serialVersionUID = 5735073632008018814L;

   private final List<UploadedFileInfo> UploadedFileList;

   public FileUploadManager() {
      UploadedFileList = new ArrayList<UploadedFileInfo>();
   }

   public void handleFileUpload(FileUploadEvent event) {
      try {
         UploadedFile uploadedFile = event.getFile();
         UploadedFileInfo fileInfo = new UploadedFileInfo(uploadedFile);
         UploadedFileList.add(fileInfo);
      }
      catch (Exception ex) {
         JSFUtils.handleException(getClass(), ex);
      }
   }

   public boolean isFileUploaded() {
      return !UploadedFileList.isEmpty();
   }

   public List<UploadedFileInfo> getUploadedFileList() {
      return UploadedFileList;
   }

   public class UploadedFileInfo {

      private final String fileName;
      private final String fileType;
      private final byte[] fileContent;

      UploadedFileInfo(UploadedFile uploadedFile) {
         fileName = uploadedFile.getFileName();
         fileType = uploadedFile.getContentType();
         fileContent = uploadedFile.getContents();
      }

      public UploadedFileInfo(String filePath) throws Exception {
         File fileObj = new File(filePath);
         if (fileObj.exists()) {
            fileContent = new byte[(int) fileObj.length()];

            FileInputStream fileInputStream = new FileInputStream(fileObj);
            fileInputStream.read(fileContent);
            fileInputStream.close();

            fileName = fileObj.getName();
            fileType = null;
         }
         else {
            fileContent = new byte[0];
            fileName = "";
            fileType = null;
         }
      }

      public String getFileName() {
         return fileName;
      }

      public long getSize() {
         long fileSize = 0;
         if (fileContent != null && fileContent.length > 0) {
            fileSize = fileContent.length;
         }
         return fileSize;
      }

      public String getType() {
         return fileType;
      }

      public byte[] getFileContent() {
         return fileContent;
      }

      public void saveTo(String toFile) throws Exception {
         if (fileContent != null && fileContent.length > 0) {
            FileOutputStream fos = new FileOutputStream(toFile);
            fos.write(fileContent);
            fos.close();
         }
      }

   }
}
