package com.jc.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

public final class FileSystem {

   public final static String WINDOWS_NEWLINE = "" + ((char) 13) + ((char) 10); // Windows specific
   public final static String NEWLINE = "" + ((char) 10); // UNIX friendly

   private FileSystem() {
      // do not instantiate
   }

   public static BufferedReader getFileReader(URL url) throws IOException {
      BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
      return reader;
   }

   public static BufferedReader getFileReader(String filename) throws IOException {
      BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
      return reader;
   }

   public static StringBuilder readInFile(String filename, boolean retain_format) throws IOException {
      return readInFile(filename, retain_format, null);
   }

   public static StringBuilder readInFile(String filename, boolean retain_format, String comment_marker) throws IOException {
      return readInFile(filename, retain_format, comment_marker, null);
   }

   public static StringBuilder readInFile(String filename, boolean retain_format, String comment_marker, LineProcessor processor) throws IOException {
      StringBuilder strbuf = new StringBuilder();
      BufferedReader reader = getFileReader(filename);

      String inputLine = null;
      int linenum = 0;
      do {
         inputLine = reader.readLine();
         if (inputLine != null && inputLine.length() > 0) {
            if (comment_marker == null || !inputLine.startsWith(comment_marker)) {
               if (processor != null) {
                  inputLine = processor.processLine(linenum, inputLine);
               }
               if (retain_format) {
                  strbuf.append(inputLine + NEWLINE);
               }
               else {
                  strbuf.append(inputLine.trim() + " ");
               }
            }
            else if (inputLine.startsWith(comment_marker)) {
               if (processor != null) {
                  processor.processCommentLine(linenum, inputLine);
               }
            }
         }
         linenum++;
      } while (inputLine != null);
      reader.close();
      return strbuf;
   }

   public static String[] readInFile(String filename) throws IOException {
      return readInFile(filename, null);
   }

   public static String[] readInFile(String filename, String comment_marker) throws IOException {
      ArrayList<String> file_content = readInFile(filename, comment_marker, null);
      return file_content.toArray(new String[file_content.size()]);
   }

   public static ArrayList<String> readInFile(String filename, String comment_marker, LineProcessor processor) throws IOException {
      ArrayList<String> file_content = new ArrayList<String>();
      BufferedReader reader = getFileReader(filename);
      String inputLine = null;
      int linenum = 0;
      do {
         inputLine = reader.readLine();
         if (inputLine != null && inputLine.length() > 0) {
            if (comment_marker == null || !inputLine.startsWith(comment_marker)) {
               if (processor != null) {
                  inputLine = processor.processLine(linenum, inputLine);
               }
               file_content.add(inputLine);
            }
            else if (inputLine.startsWith(comment_marker)) {
               if (processor != null) {
                  processor.processCommentLine(linenum, inputLine);
               }
            }
         }
         linenum++;
      } while (inputLine != null);
      reader.close();

      return file_content;
   }

   // Can only read in a 2Gb file.
   public static byte[] readInFile(File file) throws IOException {
      if (file.exists()) {
         InputStream is = new FileInputStream(file);
         long length = file.length();
         if (length <= Integer.MAX_VALUE) {
            // Create the byte array to hold the data
            byte[] bytes = new byte[(int) length];

            // Read in the bytes
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
               offset += numRead;
            }

            is.close();

            // Ensure all the bytes have been read in
            if (offset < bytes.length) {
               throw new IOException("Could not completely read file " + file.getName());
            }

            return bytes;
         }

         is.close();
         throw new IOException(file.getPath() + " file is too large.");
      }
      throw new IOException(file.getPath() + " file doesn't exist.");
   }

   public static File getTempRandomAccessFile(String filename) throws IOException {
      return getTempRandomAccessFile(null, filename);
   }

   public static File getTempRandomAccessFile(String tmpdir, String prefix) throws IOException {
      if (tmpdir == null) {
         tmpdir = System.getProperty("java.io.tmpdir");
         if (!(tmpdir.endsWith("/") || tmpdir.endsWith("\\"))) {
            tmpdir = tmpdir + System.getProperty("file.separator");
         }
      }

      File file = File.createTempFile(prefix, ".txt", new File(tmpdir));
      file.deleteOnExit();
      return file;
   }

   /**
    * Delete all files and empty sub directories from the specified directory. This method only cleans out the directory one level deep.
    */
   public static void deleteFilesFromDir(String dirpath) throws FileNotFoundException {
      deleteFilesFromDir(new File(dirpath));
   }

   /**
    * Delete all files and empty sub directories from the specified directory. This method only cleans out the directory one level deep.
    */
   public static void deleteFilesFromDir(File dir) throws FileNotFoundException {
      deleteFilesFromDir(dir, false);
   }

   /**
    * Delete all files and sub directories from the specified directory, only if 'complete' is true. Otherwise, only files and empty directories under the specified directory are deleted. This method will only completely clean out the specified directory if 'complete' is true.
    */
   public static void deleteFilesFromDir(String dirpath, boolean complete) throws FileNotFoundException {
      deleteFilesFromDir(new File(dirpath), complete);
   }

   /**
    * Delete all files and sub directories from the specified directory, only if 'complete' is true. Otherwise, only files and empty directories under the specified directory are deleted. This method will only completely clean out the specified directory if 'complete' is true.
    */
   public static void deleteFilesFromDir(File dir, boolean complete) throws FileNotFoundException {
      if (!dir.exists()) {
         throw new FileNotFoundException("Directory [" + dir.getAbsolutePath() + "] doesn't exist");
      }

      for (File file : dir.listFiles()) {
         if (complete && file.isDirectory()) {
            deleteFilesFromDir(file, complete);
         }
         file.delete();
      }
   }

   public static void deleteFile(String filename) {
      new File(filename).delete();
   }

   public static void writeContentOutToFile(String tofile, StringBuilder content_sb) throws IOException {
      String[] content = new String[1];
      content[0] = content_sb.toString();
      FileSystem.writeContentOutToFile(tofile, content);
   }

   public static void writeContentOutToFile(String tofile, String[] content) throws IOException {
      try {
         BufferedWriter out = new BufferedWriter(new FileWriter(tofile));
         for (String element : content) {
            out.write(element);
            out.newLine();
         }
         out.close();
      }
      catch (IOException e) {
         throw new IOException("Trouble writing to [" + tofile + "]: " + e.getMessage(), e);
      }
   }

   public static void appendContentToFile(String tofile, StringBuilder content_sb) throws IOException {
      String[] content = new String[1];
      content[0] = content_sb.toString();
      FileSystem.appendContentToFile(tofile, content);
   }

   public static void appendContentToFile(String tofile, String[] content) throws IOException {
      try {
         BufferedWriter out = new BufferedWriter(new FileWriter(tofile, true));
         for (String element : content) {
            out.write(element);
            out.newLine();
         }
         out.close();
      }
      catch (IOException e) {
         throw new IOException("Trouble writing to [" + tofile + "]", e);
      }
   }

   // doesn't copy hidden files...
   // both src and dest fully qualified filepaths to files...
   public static void copyFile(String src_filename, String dest_filename, boolean append) throws Exception {
      copyFile(src_filename, dest_filename, false, append);
   }

   // doesn't copy hidden files...
   // both src and dest fully qualified filepaths to files...
   public static void copyFile(File srcfile, File destfile, boolean append) throws Exception {
      copyFile(srcfile, destfile, false, append);
   }

   // copies hidden files only if hidden is true....
   // both src and dest fully qualified filepaths to files...
   public static void copyFile(String src_filename, String dest_filename, boolean hidden_ok, boolean append) throws Exception {
      File srcfile = new File(src_filename);
      File destfile = new File(dest_filename);
      copyFile(srcfile, destfile, hidden_ok, append);
   }

   // copies hidden files only if hidden is true....
   // both src and dest fully qualified filepaths to files...
   public static void copyFile(File srcfile, File destfile, boolean hidden_ok, boolean append) throws Exception {
      if (srcfile.isFile()) {
         if (srcfile.exists()) {
            if (!srcfile.isHidden() || (srcfile.isHidden() && hidden_ok)) {
               try {
                  FileInputStream in = new FileInputStream(srcfile);
                  FileOutputStream out = new FileOutputStream(destfile, append);

                  boolean done = false;
                  int available = in.available();
                  int bufsize = 51200000; // 50 Meg at a time.
                  if (available < bufsize) {
                     bufsize = available;
                  }

                  while (!done) {
                     byte[] data = new byte[bufsize];

                     in.read(data);
                     out.write(data);

                     available -= bufsize;
                     if (available < 1) {
                        done = true;
                     }
                     else if (available > 0 && available < bufsize) {
                        bufsize = available;
                     }
                  }

                  out.close();
                  in.close();
               }
               catch (FileNotFoundException ex) {
                  // ignore
               }
               catch (IOException ioex) {
                  throw ioex; // Just rethrow it.
               }
            }
            else {
               // Read file is hidden and cannot be copied.
            }
         }
         else {
            throw new FileNotFoundException(srcfile.getAbsolutePath() + " does not exist.");
         }
      }
      else {
         throw new FileNotFoundException(srcfile.getAbsolutePath() + " is not a file.");
      }
   }

   /**
    * Copies the contents of the src directory to the dest directory. Any hidden files in the src directory are not copied.
    */
   public static void copyDirectoryContents(String src_dirname, String dest_dirname) throws Exception {
      copyDirectoryContents(src_dirname, dest_dirname, false);
   }

   /**
    * Copies the contents of the src directory to the dest directory. Any hidden files in the src directory are not copied.
    */
   public static void copyDirectoryContents(File srcdir, File destdir) throws Exception {
      copyDirectoryContents(srcdir, destdir, false);
   }

   /**
    * Copies the contents of the src directory to the dest directory. Any hidden files in the src directory are copied only if 'hidden_ok' is true.
    */
   public static void copyDirectoryContents(String src_dirname, String dest_dirname, boolean hidden_ok) throws Exception {
      copyDirectoryContents(new File(src_dirname), new File(dest_dirname), hidden_ok);
   }

   /**
    * Copies the contents of the src directory to the dest directory. Any hidden files in the src directory are copied only if 'hidden_ok' is true. The srcdir can be either a file or a directory, but it MUST exist. [WARNING], destdir should be the name to a directory and not a file. If the destdir name does not exist, then it will be created as a directory. No distinction is made for the destdir name between a directory name or a file name; so that /a/b/c/d/xyz.txt if specified as the destdir name then the directory structure will be created and xyz.txt will be created as a directory, not the destination file. To copy a file to a file use the copyFile() method.
    * 
    * [WARNING]
    */
   public static void copyDirectoryContents(File srcdir, File destdir, boolean hidden_ok) throws Exception {
      if (srcdir.exists()) {
         if (!destdir.exists()) {
            destdir.mkdirs();
         }

         if (srcdir.isDirectory()) {
            if (destdir.isDirectory()) { // Copy directory contents to a directory...
               File[] from_contents = srcdir.listFiles();
               for (File from_content : from_contents) {
                  File newdest = destdir;
                  if (from_content.isDirectory()) {
                     String dirname = from_content.getName();
                     newdest = new File(destdir.getAbsolutePath() + File.separator + dirname);
                  }
                  copyDirectoryContents(from_content, newdest, hidden_ok);
               }
            }
            else {
               throw new Exception("Can't copy directory contents (" + srcdir.getAbsolutePath() + ") to a single file (" + destdir.getAbsolutePath() + ").");
            }
         }
         else { // From is a file...
            if (destdir.isDirectory()) {
               String filename = srcdir.getName();
               destdir = new File(destdir.getAbsolutePath() + File.separator + filename);
               copyFile(srcdir, destdir, hidden_ok, false); // Copy a file to a directory...
            }
            else { // To is a file...
               // Copy a file to a file...
               // Copy a file to a new file...
               // copyFile(srcdir, destdir, hidden_ok);
               throw new UnsupportedOperationException(
                     "Trying to copy src file to a dest file, this is unimplemented! See FileSystem.copyDirectoryContents() code.");
            }
         }
      }
      else {
         throw new FileNotFoundException(destdir.getAbsolutePath() + " doesn't exist");
      }
   }

   /**
    * Returns the relative path from the source to the target.
    */
   public static String getRelativePath(String srcpath, String target) {
      String[] srcdirs = srcpath.split(Pattern.quote(File.separator));
      String[] trgtdirs = target.split(Pattern.quote(File.separator));

      int matched_dirs = 0;
      boolean match = true;
      for (int i = 0; match && i < srcdirs.length && i < trgtdirs.length; i++) {
         if (srcdirs[i].equalsIgnoreCase(trgtdirs[i])) {
            matched_dirs++;
         }
         else {
            match = false; // first no match;
         }
      }

      int nomatch_dirs = 0;
      StringBuilder relative_path = new StringBuilder();
      if (srcdirs.length == trgtdirs.length) {
         // /a/b/c/d/e/f/g/src.txt 8 (total dirs) - 4 (match dirs) = 4 (nomatch dirs) (either)
         // /a/b/c/d/h/i/j/trg.txt

         // src.txt 1 (total src dirs) - 0 (match dirs) = 1 (nomatch dirs) (either)
         // trg.txt

         nomatch_dirs = srcdirs.length - matched_dirs - 1;
         if (nomatch_dirs > 0) {
            for (int i = 0; i < trgtdirs.length; i++) {
               if (i < nomatch_dirs) {
                  relative_path.append(".." + File.separator);
               }
               else if (i > (nomatch_dirs + 1)) {
                  relative_path.append(trgtdirs[i]);
                  if (i + 1 < trgtdirs.length) {
                     relative_path.append(File.separator);
                  }
               }
            }
         }
         else {
            relative_path.append(trgtdirs[trgtdirs.length - 1]);
         }
      }
      else if (srcdirs.length < trgtdirs.length) {
         // /a/b/c/d/e/f/g/src.txt 8 (total src dirs) - 4 (match dirs) = 4 (nomatch dirs) (trg)
         // /a/b/c/d/h/i/j/k/l/trg.txt

         nomatch_dirs = srcdirs.length - matched_dirs - 1;
         for (int i = 0; i < trgtdirs.length; i++) {
            if (i < nomatch_dirs) {
               relative_path.append(".." + File.separator);
            }
            else if (i > matched_dirs - 1) {
               relative_path.append(trgtdirs[i]);
               if (i + 1 < trgtdirs.length) {
                  relative_path.append(File.separator);
               }
            }
         }
      }
      else if (srcdirs.length > trgtdirs.length) {
         // /a/b/c/d/e/f/g/src.txt 8 (total src dirs) - 2 (match dirs) = 6 (nomatch dirs) (src)
         // /a/b/trg.txt

         nomatch_dirs = srcdirs.length - matched_dirs - 1;
         for (int i = 0; i < nomatch_dirs; i++) {
            relative_path.append(".." + File.separator);
         }

         for (int i = matched_dirs; i < trgtdirs.length; i++) {
            relative_path.append(trgtdirs[i]);
            if (i + 1 < trgtdirs.length) {
               relative_path.append(File.separator);
            }
         }
      }
      return relative_path.toString();
   }

   public static void addContentToTopOfFile(String fromfile, String tofile, String[] content) throws FileNotFoundException, IOException {
      File infile = new File(fromfile);
      if (infile.exists()) {
         try {
            BufferedWriter out = new BufferedWriter(new FileWriter(tofile));
            for (String element : content) {
               out.write(element);
               out.newLine();
            }

            String s = null;
            BufferedReader in = new BufferedReader(new FileReader(infile));
            while ((s = in.readLine()) != null) {
               out.write(s);
               out.newLine();
            }
            in.close();
            out.close();
         }
         catch (IOException e) {
            throw new IOException("Trouble adding content to [" + fromfile + "]", e);
         }
      }
      else {
         throw new FileNotFoundException(fromfile + " not found");
      }
   }

   /**
    * Objects implementing this interface will be notified as each line is read in, allowing the line to process the line read in before it is added to the resulting content that is read into memory. A LineProcessor is used by the readInFile() methods.
    */
   public interface LineProcessor {

      public String processLine(int linenum, String line);

      public void processCommentLine(int linenum, String line);
   }
}
