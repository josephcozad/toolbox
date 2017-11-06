package com.jc.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public final class CryptoUtils {

   public static final String AES = "AES"; // vs. "AES/CBC/PKCS5Padding"?

   private final static String VERSION = "2014V1";

   private CryptoUtils() {}

   public static String generateKey() throws GeneralSecurityException {
      KeyGenerator keyGen = KeyGenerator.getInstance(AES);
      keyGen.init(128);
      SecretKey sk = keyGen.generateKey();
      String key = byteArrayToHexString(sk.getEncoded());
      return key;
   }

   public static String encrypt(String value, String keyValue) throws GeneralSecurityException {
      Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, keyValue);
      byte[] encrypted = cipher.doFinal(value.getBytes());
      return byteArrayToHexString(encrypted);
   }

   public static String encrypt(String source) {
      String md5 = null;
      String pass = null;
      try {
         MessageDigest mdEnc = MessageDigest.getInstance("MD5"); // Encryption
         mdEnc.update(source.getBytes(), 0, source.length());
         md5 = new BigInteger(1, mdEnc.digest()).toString(16); // Encrypted
         pass = md5.substring(0, 10);
      }
      catch (Exception ex) {
         return null;
      }
      return pass;
   }

   public static String decrypt(String message, String keyValue) throws GeneralSecurityException {
      Cipher cipher = createCipher(Cipher.DECRYPT_MODE, keyValue);
      byte[] decrypted = cipher.doFinal(hexStringToByteArray(message));
      return new String(decrypted);
   }

   public static OutputStream encryptOutputStream(OutputStream out, String key_value) throws GeneralSecurityException {
      Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, key_value);
      out = new CipherOutputStream(out, cipher);
      return (out);
   }

   public static InputStream decryptInputStream(InputStream in, String key_value) throws GeneralSecurityException {
      Cipher cipher = createCipher(Cipher.DECRYPT_MODE, key_value);
      in = new CipherInputStream(in, cipher);
      return (in);
   }

   private static Cipher createCipher(int mode, String key_value) throws GeneralSecurityException {
      byte[] key = hexStringToByteArray(key_value);
      SecretKeySpec sks = new SecretKeySpec(key, AES);
      Cipher cipher = Cipher.getInstance(AES);
      cipher.init(mode, sks, cipher.getParameters());
      return (cipher);
   }

   private static byte[] hexStringToByteArray(String s) {
      byte[] b = new byte[s.length() / 2];
      for (int i = 0; i < b.length; i++) {
         int index = i * 2;
         int v = Integer.parseInt(s.substring(index, index + 2), 16);
         b[i] = (byte) v;
      }
      return b;
   }

   private static String byteArrayToHexString(byte[] b) {
      StringBuilder sb = new StringBuilder(b.length * 2);
      for (byte element : b) {
         int v = element & 0xff;
         if (v < 16) {
            sb.append('0');
         }
         sb.append(Integer.toHexString(v));
      }
      return sb.toString().toUpperCase();
   }

   private static String getHelpMessage() {
      StringBuilder sb = new StringBuilder();
      // 01234567890123456789012345678901234567890123456789012345678901234567890123456789
      sb.append("USAGE: CryptoUtils [-en <text to encrypt> -key <key> | -de <text to decrypt> -key <key>]" + FileSystem.NEWLINE);
      sb.append("For encryption, 'key' is optional." + FileSystem.NEWLINE);
      return sb.toString();
   }

   //   public static void main(String[] args) {
   //      CommandLineHelper helper = new CommandLineHelper(args);
   //
   //      if (helper.displayHelpMessage()) {
   //         CommandLineHelper.displayMessage(getHelpMessage());
   //      }
   //      else if (helper.displayVersion()) {
   //         CommandLineHelper.displayMessage("VERSION: " + VERSION);
   //      }
   //      else {
   //
   //         String text = null;
   //         String key = null;
   //         boolean encrypt = false;
   //         if (helper.isValidParameter("-en")) { //
   //            text = helper.getParameterValue("-en");
   //            encrypt = true;
   //            if (helper.isValidParameter("-key")) { // Optional
   //               key = helper.getParameterValue("-key");
   //            }
   //         }
   //         else if (helper.isValidParameter("-de")) {
   //            if (helper.isValidParameter("-key")) {
   //               text = helper.getParameterValue("-de");
   //               key = helper.getParameterValue("-key");
   //            }
   //            else {
   //               CommandLineHelper.displayMessage("ERROR: The -de parameter requires an associated -key parameter." + FileSystem.NEWLINE);
   //               CommandLineHelper.displayMessage(getHelpMessage());
   //            }
   //         }
   //         else {
   //            CommandLineHelper.displayMessage("ERROR: Unrecognizable parameter entered." + FileSystem.NEWLINE);
   //            CommandLineHelper.displayMessage(getHelpMessage());
   //         }
   //
   //         if (encrypt && text != null) {
   //            try {
   //               if (key == null) {
   //                  key = generateKey();
   //               }
   //               String encrypted_text = encrypt(text, key);
   //               System.out.println("KEY: " + key);
   //               System.out.println("ENCRYPTED TEXT: " + encrypted_text);
   //            }
   //            catch (GeneralSecurityException ex) {
   //               CommandLineHelper.displayExceptionInfo(ex);
   //               System.exit(-1);
   //            }
   //         }
   //         else if (!encrypt && text != null && key != null) {
   //            try {
   //               String decrypted_text = decrypt(text, key);
   //               System.out.println("TEXT: " + decrypted_text);
   //            }
   //            catch (GeneralSecurityException ex) {
   //               CommandLineHelper.displayExceptionInfo(ex);
   //               System.exit(-1);
   //            }
   //         }
   //         else {
   //            CommandLineHelper.displayMessage("ERROR: Unable to determine action required encrypt[" + encrypt + "] text[" + text + "] key[" + key + "]."
   //                  + FileSystem.NEWLINE);
   //            CommandLineHelper.displayMessage(getHelpMessage());
   //         }
   //      }
   //      System.exit(0);
   //   }
}
