package com.jc.app.rest;

import com.jc.util.Node;

public class QueryParamInfo {

   private final Node MyNode;

   public QueryParamInfo() {
      MyNode = new Node();
   }

   private QueryParamInfo(Node node) {
      MyNode = node;
   }

   public boolean isEmpty() {
      return MyNode.hasNodes();
   }

   public boolean hasParameter(String path) {
      return MyNode.hasNode(path);
   }

   public void addParameter(String path) {
      MyNode.addNode(path);
   }

   // Returns an empty QueryParamInfo if path doesn't exist...
   public QueryParamInfo getQueryParamInfo(String path) {
      if (hasParameter(path)) {
         Node node = MyNode.getNode(path);
         return new QueryParamInfo(node);
      }
      else {
         return new QueryParamInfo();
      }
   }

   @Override
   public String toString() {
      return MyNode.toString();
   }
}
