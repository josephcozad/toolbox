package com.jc.shiro;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AccountException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

import com.jc.app.data.domain.AppUser;
import com.jc.app.service.ApplicationServiceManager;

public class DatabaseRealm extends AuthorizingRealm {

   @Override
   public boolean supports(AuthenticationToken token) {
      boolean supported = false;
      if (token != null) {
         if (token instanceof RESTAuthorizationToken) {
            supported = true;
         }
         else {
            supported = getAuthenticationTokenClass().isAssignableFrom(token.getClass());
         }
      }
      return supported;
   }

   @Override
   protected void assertCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) throws AuthenticationException {
      if (!(token instanceof RESTAuthorizationToken)) {
         super.assertCredentialsMatch(token, info);
      }
   }

   @Override
   protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
      SimpleAuthenticationInfo info = null;

      try {
         if (token instanceof RESTAuthorizationToken) {
            RESTAuthorizationToken restfulToken = (RESTAuthorizationToken) token;
            String applicationKey = (String) restfulToken.getPrincipal(); // the application key...

            // check the database to see if the application key is in the database...
            String principal = getPrincipalForApplicationKey(applicationKey);
            if (principal != null && !principal.isEmpty()) {
               String credential = "";
               info = new SimpleAuthenticationInfo(principal, credential.toCharArray(), getName());
            }
         }
         else {
            UsernamePasswordToken upToken = (UsernamePasswordToken) token;
            String principal = upToken.getUsername();
            if (principal == null) {
               throw new AccountException("Null usernames are not allowed by this realm.");
            }

            String credential = getPrincipalCredentials(principal);
            info = new SimpleAuthenticationInfo(principal, credential.toCharArray(), getName());

            //String salt = null;
            //                  if (salt != null) {
            //                     info.setCredentialsSalt(ByteSource.Util.bytes(salt));
            //                  }
         }
      }
      catch (Exception ex) {
         if (!(ex instanceof AuthenticationException)) {
            throw (new AuthenticationException(ex));
         }
         else {
            throw (AuthenticationException) ex;
         }
      }

      return info;
   }

   /**
    * This implementation of the interface expects the principals collection to return a String username keyed off of this realm's {@link #getName() name}
    *
    * @see #getAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection)
    */
   @Override
   protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {

      //a user without principals is invalid!
      if (principals == null) {
         throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
      }

      try {
         Subject currentUser = SecurityUtils.getSubject();
         org.apache.shiro.session.Session userSession = currentUser.getSession();

         ShiroUser user = null;
         Object value = userSession.getAttribute("user");
         if (value == null) {
            String userName = (String) principals.getPrimaryPrincipal();
            user = AccessControlService.createUser(userName);
            userSession.setAttribute("user", user);
         }
         else if (value instanceof ShiroUser) {
            user = (ShiroUser) value;
         }
         else {
            throw new AuthorizationException("Expected session value representing user to be of type " + ShiroUser.class.getName() + ".");
         }

         List<String> rolesList = user.getRoles();
         Set<String> roleNames = new HashSet<String>(rolesList);

         List<String> permissionsList = user.getPermissions();
         Set<String> permissions = new HashSet<String>(permissionsList);

         SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(roleNames);
         info.setStringPermissions(permissions);

         return info;
      }
      catch (Exception ex) {
         throw new AuthorizationException(ex);
      }
   }

   private String getPrincipalForApplicationKey(String applicationKey) throws Exception {
      String principal = null;
      ApplicationServiceManager srvmgr = ApplicationServiceManager.getServiceAccountServiceManager();
      if (srvmgr != null) {
         AppUser userData = srvmgr.getAppUserByAppKey(applicationKey);
         if (userData != null) {
            if (userData.isExpired()) {
               throw (new LockedAccountException());
            }
            principal = userData.getUserName();
         }
         else {
            throw (new UnknownAccountException("Unknown application key '" + applicationKey + "'."));
         }
         return principal;
      }
      else {
         throw new Exception("Unable to get user information because no DBApplicationServiceManager was available.");
      }
   }

   private String getPrincipalCredentials(String principal) throws Exception {
      String credentials = "";
      ApplicationServiceManager srvmgr = ApplicationServiceManager.getServiceAccountServiceManager();
      if (srvmgr != null) {
         AppUser userData = srvmgr.getAppUser(principal);
         if (userData != null) {
            if (userData.isExpired()) {
               throw (new LockedAccountException());
            }

            credentials = userData.getPassword();
         }
         else {
            throw (new UnknownAccountException("Unknown user '" + principal + "'."));
         }
         return credentials;
      }
      else {
         throw new Exception("Unable to get user information because no DBApplicationServiceManager was available.");
      }
   }
}
