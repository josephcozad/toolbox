package com.jc.shiro;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.util.WebUtils;

public abstract class RESTAuthenticationFilter extends AuthenticatingFilter {

   @Override
   protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
      AuthenticationToken token = null;
      HttpServletRequest httpRequest = WebUtils.toHttp(request);
      String authorizationHeader = httpRequest.getHeader(getAuthenticateHeaderKey());
      if (authorizationHeader != null && authorizationHeader.length() > 0) {
         boolean rememberMe = isRememberMe(request);
         String host = getHost(request);
         token = new RESTAuthorizationToken(authorizationHeader, rememberMe, host);
      }

      return token;
   }

   @Override
   protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
      boolean loggedIn = false; //false by default or we wouldn't be in this method

      HttpServletRequest httpRequest = WebUtils.toHttp(request);
      String authorizationHeader = httpRequest.getHeader(getAuthenticateHeaderKey());
      if (authorizationHeader != null) {
         loggedIn = executeLogin(request, response);
      }

      if (!loggedIn) {
         HttpServletResponse httpResponse = WebUtils.toHttp(response);
         httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      }
      return loggedIn;
   }

   public abstract String getAuthenticateHeaderKey() throws Exception;
}
