package com.jc.shiro;

import java.io.IOException;
import java.util.logging.Level;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authz.RolesAuthorizationFilter;

import com.jc.log.Logger;

/**
 * Allows access if current user has at least one role of the specified list.
 */
public class AnyRoleAuthorizationFilter extends RolesAuthorizationFilter {

   @Override
   public boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws IOException {
      boolean matched = false;

      Subject subject = getSubject(request, response);
      String[] rolesArray = (String[]) mappedValue;

      if (rolesArray == null || rolesArray.length == 0) {
         //no roles specified, so nothing to check - allow access.
         matched = true;
      }
      else {
         // Matches any role in the rolesArray...
         for (String roleName : rolesArray) {
            if (subject.hasRole(roleName)) {
               matched = true;
               break;
            }
         }

         String requestURI = ((HttpServletRequest) request).getRequestURI();
         boolean restServiceRequest = requestURI.contains("services");

         if (!matched && !restServiceRequest) {
            // set up unauthorized URL message...
            int code = AuthorizationErrorCodes.UNAUTHORIZED_ACCESS;

            try {
               ShiroUser user = AccessControlService.getUser();
               if (user.getRoles().isEmpty()) {
                  code = AuthorizationErrorCodes.SYSERR_NO_ROLES_ASSIGNED;
               }
            }
            catch (Exception ex) {
               Logger.log(getClass(), Level.SEVERE, ex);
               code = AuthorizationErrorCodes.SYSERR_GENERAL;
            }

            setUnauthorizedUrl("/loginError.xhtml?code=" + code);
         }
      }

      return matched;
   }
}
