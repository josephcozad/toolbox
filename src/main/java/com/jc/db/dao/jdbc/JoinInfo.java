package com.jc.db.dao.jdbc;

public class JoinInfo {

   public final static int ALIAS_SIZE = 4;

   String pSchema;
   String pTableName;
   String pTableAlias; // Table Alias joining to
   String pColumnName; // Column Name joining to

   String jSchema;
   String jTableName; // Table Name joining on
   String jTableAlias; // Table Alias joining on
   String jColumnName; // Table Alias joining on

   String xrefSchema;
   String xrefTableName;
   String xrefTableAlias;
   String xrefSrcColumn;
   String xrefDestColumn;

   public JoinInfo(EntityInfo entityInfo) {
      pSchema = entityInfo.getSchemaName();
      pTableName = entityInfo.getTableName().toLowerCase();
      pTableAlias = pTableName.substring(0, JoinInfo.ALIAS_SIZE);
   }

   public JoinInfo(EntityInfo entityInfo, FieldInfo fieldInfo, String columnProp) {
      if (columnProp != null && !columnProp.isEmpty()) {
         EntityInfo subEntityInfo = fieldInfo.getEntityInfo();

         pSchema = entityInfo.getSchemaName();
         pTableName = columnProp.substring(0, columnProp.indexOf("."));
         pTableAlias = pTableName.toLowerCase().substring(0, JoinInfo.ALIAS_SIZE);
         pColumnName = columnProp.substring(columnProp.indexOf(".") + 1, columnProp.length());
         pColumnName = pColumnName.toUpperCase();

         jSchema = subEntityInfo.getSchemaName();
         jTableName = subEntityInfo.getTableName().toUpperCase();
         jTableAlias = jTableName.toLowerCase().substring(0, JoinInfo.ALIAS_SIZE);

         String jColumnNameX = null;
         String refColumnName = fieldInfo.getRefColumnName();
         if (refColumnName != null && !refColumnName.isEmpty()) {
            jColumnNameX = refColumnName.toUpperCase();
         }
         else {
            // By default use the column that is marked as @id...
            String idColProp = subEntityInfo.getIdFieldInfo().getColumnProperty();
            jColumnNameX = idColProp.substring(idColProp.indexOf(".") + 1, idColProp.length());
            jColumnNameX = jColumnNameX.toUpperCase();
         }
         jColumnName = jColumnNameX;

         if (fieldInfo.hasXREFTable()) {
            xrefSchema = fieldInfo.getXREFSchemaName();
            xrefTableName = fieldInfo.getXREFTableName();
            xrefTableAlias = xrefTableName.toLowerCase().substring(0, JoinInfo.ALIAS_SIZE);
            xrefSrcColumn = fieldInfo.getXREFSrcColumn();
            xrefDestColumn = fieldInfo.getXREFDestColumn();

            // reassign pColumnName to entityInfo's id column
            columnProp = entityInfo.getIdFieldInfo().getColumnProperty();
            pColumnName = columnProp.substring(columnProp.indexOf(".") + 1, columnProp.length());
            pColumnName = pColumnName.toUpperCase();
         }
      }
   }

   public String getPTableName() {
      String tableName = pTableName;
      if (pSchema != null && !pSchema.isEmpty()) {
         tableName = pSchema + "." + pTableName;
      }
      return tableName.toLowerCase();
   }

   public String getJTableName() {
      String tableName = jTableName;
      if (jSchema != null && !jSchema.isEmpty()) {
         tableName = jSchema + "." + jTableName;
      }
      return tableName.toLowerCase();
   }

   public String getXTableName() {
      String tableName = xrefTableName;
      if (xrefSchema != null && !xrefSchema.isEmpty()) {
         tableName = xrefSchema + "." + xrefTableName;
      }
      return tableName.toLowerCase();
   }

   @Override
   public String toString() {
      String statement = "";
      if (pColumnName == null || pColumnName.isEmpty()) {
         statement = "SELECT ALIAS_LIST FROM " + getPTableName() + " " + pTableAlias.toLowerCase();
      }
      else if (xrefTableName != null && !xrefTableName.isEmpty()) {
         String xrefStatement = " LEFT OUTER JOIN " + getXTableName() + " " + xrefTableAlias.toLowerCase() + " ON " + xrefTableAlias.toLowerCase() + "."
               + xrefSrcColumn.toLowerCase() + " = " + pTableAlias.toLowerCase() + "." + pColumnName.toLowerCase();

         statement = xrefStatement + " LEFT OUTER JOIN " + getJTableName() + " " + jTableAlias.toLowerCase() + " ON " + jTableAlias.toLowerCase() + "."
               + jColumnName.toLowerCase() + " = " + xrefTableAlias.toLowerCase() + "." + xrefDestColumn.toLowerCase();
      }
      else {
         statement = " LEFT OUTER JOIN " + getJTableName() + " " + jTableAlias.toLowerCase() + " ON " + jTableAlias.toLowerCase() + "."
               + jColumnName.toLowerCase() + " = " + pTableAlias.toLowerCase() + "." + pColumnName.toLowerCase();
      }
      //      System.out.println(statement);
      return statement;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((jColumnName == null) ? 0 : jColumnName.hashCode());
      result = prime * result + ((jTableAlias == null) ? 0 : jTableAlias.hashCode());
      result = prime * result + ((jTableName == null) ? 0 : jTableName.hashCode());
      result = prime * result + ((pColumnName == null) ? 0 : pColumnName.hashCode());
      result = prime * result + ((pTableAlias == null) ? 0 : pTableAlias.hashCode());
      result = prime * result + ((pTableName == null) ? 0 : pTableName.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof JoinInfo)) {
         return false;
      }
      JoinInfo other = (JoinInfo) obj;
      if (jColumnName == null) {
         if (other.jColumnName != null) {
            return false;
         }
      }
      else if (!jColumnName.equals(other.jColumnName)) {
         return false;
      }
      if (jTableAlias == null) {
         if (other.jTableAlias != null) {
            return false;
         }
      }
      else if (!jTableAlias.equals(other.jTableAlias)) {
         return false;
      }
      if (jTableName == null) {
         if (other.jTableName != null) {
            return false;
         }
      }
      else if (!jTableName.equals(other.jTableName)) {
         return false;
      }
      if (pColumnName == null) {
         if (other.pColumnName != null) {
            return false;
         }
      }
      else if (!pColumnName.equals(other.pColumnName)) {
         return false;
      }
      if (pTableAlias == null) {
         if (other.pTableAlias != null) {
            return false;
         }
      }
      else if (!pTableAlias.equals(other.pTableAlias)) {
         return false;
      }
      if (pTableName == null) {
         if (other.pTableName != null) {
            return false;
         }
      }
      else if (!pTableName.equals(other.pTableName)) {
         return false;
      }
      return true;
   }
}
