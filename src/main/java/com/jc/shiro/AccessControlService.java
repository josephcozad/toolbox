package com.jc.shiro;

import java.util.logging.Level;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.ExpiredCredentialsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.subject.Subject;

import com.jc.app.User;
import com.jc.app.data.domain.AppUser;
import com.jc.app.service.ApplicationServiceManager;
import com.jc.exception.SystemInfoException;
import com.jc.log.Logger;

public class AccessControlService {

   public static String encryptPassword(String password) {
      DefaultHashService hashService = new DefaultHashService();
      hashService.setHashAlgorithmName(DefaultPasswordService.DEFAULT_HASH_ALGORITHM);
      hashService.setHashIterations(1);
      hashService.setGeneratePublicSalt(false);

      DefaultPasswordService passwordService = new DefaultPasswordService();
      passwordService.setHashService(hashService);

      String encPwd = passwordService.encryptPassword(password);
      //      System.out.println(encPwd);

      encPwd = encPwd.substring(encPwd.lastIndexOf("$") + 1, encPwd.length());
      //      System.out.println(encPwd);

      //      encPwd = new String(Hex.encode(encPwd.getBytes()));
      //      System.out.println(encPwd);

      return encPwd;
   }

   public static void loginUser(String username, String password) throws Exception {
      try {
         UsernamePasswordToken token = new UsernamePasswordToken(username, password);

         Subject currentUser = SecurityUtils.getSubject();
         currentUser.login(token);

         ShiroUser user = createUser(username);

         if (user.resetPassword()) {
            currentUser.logout(); // log them out first...
            throw (new ExpiredCredentialsException());
         }

         org.apache.shiro.session.Session userSession = currentUser.getSession();
         userSession.setAttribute("user", user);

         //         if (!user.resetPassword()) {
         //            DBApplicationServiceManager.addToAuditLog(AccessControlService.class, "login");
         //         }
      }
      catch (UnknownAccountException uae) {
         // for logging purposes....
         Logger.log(AccessControlService.class, Level.INFO, uae.getMessage());
         String message = "User id or password incorrect.";
         throw (new SystemInfoException(Level.SEVERE, SecurityErrorCode.UNKNOWN_USER_ACCOUNT, message));
      }
      catch (IncorrectCredentialsException ice) {
         incrementNumFailedLogins(username);
         String message = "User id or password incorrect.";
         throw (new SystemInfoException(Level.SEVERE, SecurityErrorCode.INVALID_CREDENTIALS, message));
      }
      catch (LockedAccountException lae) {
         String message = "User account locked please contact the system admin to have your account unlocked.";
         throw (new SystemInfoException(Level.SEVERE, SecurityErrorCode.USER_ACCOUNT_LOCKED, message));
      }
      catch (ExpiredCredentialsException ecex) {
         throw ecex; // forces user to change their password...
      }
   }

   public static void logoutUser(String username) throws Exception {
      Subject currentUser = SecurityUtils.getSubject();
      currentUser.logout();
   }

   public static void changePassword(String username, String oldPassword, String newPassword) throws Exception {
      validatePassword(oldPassword, newPassword); // throws OnBoardingExcepiton if not valid...
      newPassword = encryptPassword(newPassword);
      setPassword(username, newPassword, false);
   }

   public static ShiroUser getUser() throws Exception {
      ShiroUser user = null;
      String errorMsg = "";

      try {
         Subject currentUser = SecurityUtils.getSubject();
         org.apache.shiro.session.Session userSession = currentUser.getSession();

         Object value = userSession.getAttribute("user");
         if (value instanceof ShiroUser) {
            user = (ShiroUser) value;
         }
         else if (value == null) {
            errorMsg = "No user object was found in session.";
         }
         else {
            errorMsg = "Expected session value representing user to be of type " + ShiroUser.class.getName() + ".";
         }
      }
      catch (InvalidSessionException isex) {
         String userMessage = "Session already invalidated InvalidSessionException thrown.";
         throw new SystemInfoException(Level.WARNING, SecurityErrorCode.INVALID_SESSION, userMessage);
      }
      catch (Exception ex) {
         String outfile = Logger.saveStackTrace(ex);
         String message = ex.getClass().getSimpleName() + " thrown. See full stacktrace at: " + outfile;
         Logger.log(AccessControlService.class, Level.SEVERE, message, ex);
         throw new Exception(message);
      }

      // throw outside of try/catch... try/catch looking for any exceptions from Shiro itself.
      if (!errorMsg.isEmpty()) {
         throw new Exception(errorMsg);
      }
      else {
         return user;
      }
   }

   private static void validatePassword(String oldPassword, String newPassword) throws SystemInfoException {
      boolean valid = !newPassword.equals(oldPassword);
      if (!valid) {
         SystemInfoException obex = new SystemInfoException(Level.SEVERE, SecurityErrorCode.INVALID_PASSWORD, "Invalid new password.");
         throw (obex);
      }
   }

   static ShiroUser createUser(String username) throws Exception {
      ApplicationServiceManager srvmgr = ApplicationServiceManager.getServiceAccountServiceManager();
      if (srvmgr != null) {
         User user = srvmgr.createUser(username);
         return user;
      }
      else {
         throw new Exception("Unable to create user '" + username + "' because no DBApplicationServiceManager was available.");
      }
   }

   private static void incrementNumFailedLogins(String username) throws Exception {
      ApplicationServiceManager srvmgr = ApplicationServiceManager.getServiceAccountServiceManager();
      if (srvmgr != null) {
         AppUser user = srvmgr.getAppUser(username);
         int numFailed = user.getFailedLogins();
         numFailed += 1;

         user.setFailedLogins(numFailed);
         if (numFailed == 3) { // You get three tries... 0, 1, 2...
            user.setExpired(true);
         }

         srvmgr.updateData(user);
      }
      else {
         Logger.log(AccessControlService.class, Level.SEVERE,
               "Unable to increment number of failed user '" + username + "' logins because no DBApplicationServiceManager was available.");
      }
   }

   private static void setPassword(String username, String password, boolean resetPasswordRequired) throws Exception {
      ApplicationServiceManager srvmgr = ApplicationServiceManager.getServiceAccountServiceManager();
      if (srvmgr != null) {
         AppUser user = srvmgr.getAppUser(username);
         user.setPassword(password);
         user.setPasswordReset(resetPasswordRequired);
         srvmgr.updateData(user);
      }
      else {
         throw new Exception("Unable to set user '" + username + "' password because no DBApplicationServiceManager was available.");
      }
   }

   // ---------------------------------------------------

   //   public abstract ShiroUser getUser(String username);
   //
   //   public abstract void incrementNumFailedLogins(String username);
   //
   //   public abstract void setPassword(String username, String encryptedPassword, boolean value);

   //   public static void main(String[] args) {
   //      try {
   //
   //         String password = "password";
   //
   //         DefaultHashService hashService = new DefaultHashService();
   //         hashService.setHashAlgorithmName(DefaultPasswordService.DEFAULT_HASH_ALGORITHM);
   //         hashService.setHashIterations(1);
   //         hashService.setGeneratePublicSalt(false);
   //
   //         DefaultPasswordService passwordService = new DefaultPasswordService();
   //         passwordService.setHashService(hashService);
   //
   //         String encPwd = passwordService.encryptPassword(password);
   //         System.out.println(encPwd);
   //
   //         encPwd = encPwd.substring(encPwd.lastIndexOf("$") + 1, encPwd.length());
   //         System.out.println(encPwd);
   //
   //         encPwd = new String(Hex.encode(encPwd.getBytes()));
   //         System.out.println(encPwd);
   //
   //      }
   //      catch (Exception ex) {
   //         ex.printStackTrace();
   //      }
   //      System.exit(0);
   //   }
}
