package com.jc.shiro;

import org.apache.shiro.authc.HostAuthenticationToken;
import org.apache.shiro.authc.RememberMeAuthenticationToken;

public class RESTAuthorizationToken implements HostAuthenticationToken, RememberMeAuthenticationToken {

   private static final long serialVersionUID = 2816172845764476197L;

   private final boolean rememberMe;
   private final String host;
   private final String authorizationValue;

   public RESTAuthorizationToken(String authorizationValue, boolean rememberMe, String host) {
      this.rememberMe = rememberMe;
      this.host = host;
      this.authorizationValue = authorizationValue;
   }

   @Override
   public Object getPrincipal() {
      return authorizationValue;
   }

   @Override
   public Object getCredentials() {
      return "";
   }

   @Override
   public boolean isRememberMe() {
      return rememberMe;
   }

   @Override
   public String getHost() {
      return host;
   }
}
