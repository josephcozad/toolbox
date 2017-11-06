package com.jc.util;

import java.util.ArrayList;
import java.util.List;

public class Node {

   private final static String ROOT_KEY = "ROOT";

   private String Name;
   private Node ParentNode;
   private final List<Node> ChildNodes;

   public Node() {
      Name = Node.ROOT_KEY;
      ChildNodes = new ArrayList<>();
   }

   public boolean hasNodes() {
      return ChildNodes.isEmpty();
   }

   public boolean hasNode(String path) {
      Node node = getNode(path);
      return node != null;
   }

   public Node getNode(String path) {
      Node node = null;
      if (!path.contains(".")) {
         String nodeName = path;

         if (Name.equals(nodeName)) {
            node = this;
         }
         else {
            if (!ChildNodes.isEmpty()) { // search ChildNodes for nodeName...
               for (Node childNode : ChildNodes) {
                  String childNodeName = childNode.Name;
                  if (childNodeName.equals(nodeName)) {
                     node = childNode;
                     break;
                  }
               }
            }
            // do nothing and return null, not found...
         }
      }
      else {
         String nodeName = path.substring(0, path.indexOf("."));
         if (Name.equals(nodeName)) {
            path = path.substring(path.indexOf(".") + 1, path.length());
            node = getNode(path);
         }
         else {
            if (!ChildNodes.isEmpty()) { // search ChildNodes for nodeName...
               for (Node childNode : ChildNodes) {
                  String childNodeName = childNode.Name;
                  if (childNodeName.equals(nodeName)) {
                     path = path.substring(path.indexOf(".") + 1, path.length());
                     node = childNode.getNode(path);
                     break;
                  }
               }
            }
            // do nothing and return null, not found...
         }
      }
      return node;
   }

   public void addNode(String path) {
      if (!path.contains(".")) {
         if (!Name.equals(path)) {
            createChildNode(path);
         }
         // ignore this node is the same as path.
      }
      else {
         String nodeName = path.substring(0, path.indexOf("."));
         path = path.substring(path.indexOf(".") + 1, path.length());
         if (ChildNodes.isEmpty()) {
            // create a new node for nodeName and add path
            Node childNode = createChildNode(nodeName);
            childNode.addNode(path);
         }
         else {
            // search for nodeName.
            boolean found = false;
            for (Node childNode : ChildNodes) {
               String childNodeName = childNode.Name;
               if (childNodeName.equals(nodeName)) {
                  childNode.addNode(path); // add path to found child node
                  found = true;
                  break;
               }
            }

            if (!found) {
               // create a new node for nodeName and add path
               Node childNode = createChildNode(nodeName);
               childNode.addNode(path);
            }
         }
      }
   }

   @Override
   public String toString() {
      if (ParentNode == null) {
         return Name;
      }
      else {
         String parentPath = ParentNode.toString();
         if (!parentPath.equals(ROOT_KEY)) {
            return parentPath + "." + Name;
         }
         else {
            return Name;
         }
      }
   }

   private Node createChildNode(String nodeName) {
      Node childNode = new Node();
      childNode.Name = nodeName;
      childNode.ParentNode = this;
      ChildNodes.add(childNode);
      return childNode;
   }
}
