package com.jc.shiro;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.web.filter.PathMatchingFilter;
import org.apache.shiro.web.util.WebUtils;

public abstract class CORSFilter extends PathMatchingFilter {

   @Override
   protected boolean onPreHandle(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {

      boolean continueChain = true;

      String acReqestHeaders = ((HttpServletRequest) request).getHeader("access-control-request-headers");
      String acRequestMethod = ((HttpServletRequest) request).getHeader("access-control-request-method");

      if (acReqestHeaders != null && acRequestMethod != null) {
         // assume this is a "preflight" call for OPTIONS, and allow by not continuing chain...
         continueChain = false;
      }

      WebUtils.toHttp(response).setHeader("Access-Control-Allow-Origin", "*");
      WebUtils.toHttp(response).setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");

      String authenticationHeaderKey = getAuthenticateHeaderKey();
      WebUtils.toHttp(response).setHeader("Access-Control-Allow-Headers", "origin, content-type, accept, authorization, " + authenticationHeaderKey);

      return continueChain;
   }

   public abstract String getAuthenticateHeaderKey() throws Exception;
}
