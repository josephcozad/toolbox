package com.jc.util;

import java.util.HashMap;
import java.util.Map;

public class PropertyNode {

   final static String ROOT_KEY = "ROOT";

   // Allow PropertyManager to manipulate these fields directly if neccessary.
   String FileId;
   String Key;
   String Value;
   PropertyNode ParentNode;

   private PropertyNode[] Children;

   private PropertyNode() {}

   PropertyNode(String key) {
      Key = key;
   }

   public static PropertyNode createCopy(PropertyNode node) {
      PropertyNode nodeCopy = new PropertyNode();
      nodeCopy.Key = node.Key;

      if (node.Value != null) {
         nodeCopy.Value = node.Value;
      }

      if (node.Children != null) {
         nodeCopy.Children = new PropertyNode[node.Children.length];
         for (int i = 0; i < node.Children.length; i++) {
            nodeCopy.Children[i] = PropertyNode.createCopy(node.Children[i]);
         }
      }

      return nodeCopy;
   }

   boolean hasChildren() {
      return Children != null && Children.length > 0;
   }

   // This indicates that the 'path' as a value associated with it.
   boolean hasProperty(String path) {
      PropertyNode node = getNode(path);
      return node != null && node.Value != null;
   }

   // This indicates that the 'path' is valid, and it may or may not have a value associated with it.
   boolean isValidPath(String path) {
      PropertyNode node = getNode(path);
      return node != null;
   }

   void addProperty(String path, String value, String fileid) {
      if (path.indexOf(".") < 0) {
         if (Key == null) {
            // Error... Key should always have been already set.
            throw new IllegalStateException("Error adding value, PropertyNode key was null; path[" + path + "] value[" + value + "].");
         }
         if (Key.equals(path)) {
            Value = value;
            FileId = fileid;
         }
         else {
            String key = path;

            // Find the child node if it exists...
            int child_indx = findChildIndexFor(key);
            PropertyNode child = getChildAt(child_indx, true);
            child.Key = key;
            child.addProperty(key, value, fileid);
         }
      }
      else {
         String key = path.substring(0, path.indexOf("."));
         path = path.substring(path.indexOf(".") + 1);

         // Find the child node if it exists...
         int child_indx = findChildIndexFor(key);
         PropertyNode child = getChildAt(child_indx, true);
         child.Key = key;
         child.addProperty(path, value, fileid);
      }
   }

   void removeProperty(String path) {
      PropertyNode node = getNode(path);
      if (node != null) {
         if (node.Children == null) {
            // remove node from parent...
            String parent_path = path.substring(0, path.lastIndexOf('.'));
            String child_key = path.substring(path.lastIndexOf('.') + 1, path.length());
            node = getNode(parent_path);
            if (node != null) {
               int child_indx = node.findChildIndexFor(child_key);
               node.removeChild(child_indx);
            }
         }
         else {
            node.Value = null; // remove value from node...
         }
      }
   }

   String getValue(String path) {
      PropertyNode node = getNode(path);
      return node == null ? null : node.Value;
   }

   void removeAllProperties() {
      if (Children != null) {
         for (int i = 0; i < Children.length; i++) {
            Children[i].removeAllProperties();
            Children[i] = null;
         }
      }
      if (!Key.equals(ROOT_KEY)) {
         Key = null;
      }
      Value = null;
      Children = null;
   }

   void removePropertiesFor(String fileid) {
      if (FileId != null && FileId.equals(fileid)) {
         if (Value != null) {
            Value = null;
         }
      }

      if (Children != null) {
         for (int i = 0; i < Children.length; i++) {
            Children[i].removePropertiesFor(fileid);
            if (Children[i].Value == null && Children[i].Children == null) {
               removeChild(i);
            }
         }
      }
   }

   @Override
   public String toString() {
      Map<String, String> propMap = getPropertyPairs();
      StringBuilder sb = new StringBuilder();
      for (Map.Entry<String, String> entry : propMap.entrySet()) {
         String key = entry.getKey();
         String value = entry.getValue();
         sb.append(key + " = " + value + FileSystem.NEWLINE);
      }
      return sb.toString();
   }

   String toString(String path) {
      PropertyNode node = getNode(path);
      return node == null ? StringUtils.EMPTY : node.toString();
   }

   private String getFullNodeHierarchyPath() {
      String path = "";
      if (!ParentNode.Key.equals(ROOT_KEY)) {
         path = ParentNode.getFullNodeHierarchyPath() + "." + Key;
         return path;
      }
      else {
         return Key;
      }
   }

   public Map<String, String> getPropertyPairs() {
      Map<String, String> propPairs = new HashMap<String, String>();
      if (Value != null) {
         if (ParentNode != null) { // null ParentNodeKey means this is the ROOT_KEY...
            propPairs.put(getFullNodeHierarchyPath(), Value);
         }
         else {
            propPairs.put(Key, Value);
         }
      }

      if (Children != null) {
         for (PropertyNode element : Children) {
            Map<String, String> elementPairs = element.getPropertyPairs();
            propPairs.putAll(elementPairs);
         }
      }

      return propPairs;
   }

   // --------------- //

   private int findChildIndexFor(String key) {
      int child_indx = -1;
      if (Children != null) {
         for (int i = 0; ((i < Children.length) && (child_indx == -1)); i++) {
            if (Children[i] != null && Children[i].Key != null && Children[i].Key.equals(key)) {
               child_indx = i;
            }
         }
      }
      return child_indx;
   }

   private PropertyNode getChildAt(int child_indx, boolean create_newnode) {
      PropertyNode child = null;
      if (child_indx > -1) { // This will over write the existing value.
         child = Children[child_indx];
      }
      else if (create_newnode) {
         // Add a new child to the list...
         int num_children = Children == null ? 0 : Children.length;
         PropertyNode[] nodes = Children;
         Children = new PropertyNode[num_children + 1];
         for (int i = 0; i < num_children; i++) {
            Children[i] = nodes[i];
         }
         Children[Children.length - 1] = new PropertyNode();
         Children[Children.length - 1].ParentNode = this;
         child = Children[Children.length - 1];
      }
      return child;
   }

   private void removeChild(int child_indx) {
      Children[child_indx] = null;
      if (Children.length == 0) {
         Children = null;
      }

      // Re-jigger the Children to remove the nulled space.
      int num_children = Children.length;
      PropertyNode[] nodes = Children;
      Children = new PropertyNode[num_children - 1];
      for (int i = 0, j = 0; i < num_children; i++) {
         if (nodes[i] != null) {
            Children[j] = nodes[i];
            j++;
         }
      }
   }

   PropertyNode getNode(String path) {
      PropertyNode node = null;

      String key = null;
      if (path.indexOf('.') > -1) {
         key = path.substring(0, path.indexOf("."));
         path = path.substring(path.indexOf(".") + 1, path.length());
      }
      else {
         key = path;
      }

      // Find the child node if it exists...
      int child_indx = findChildIndexFor(key);
      if (child_indx > -1) {
         node = getChildAt(child_indx, false);
         if (!path.equals(key)) {
            node = node.getNode(path);
         }
      }

      return node;
   }
}
