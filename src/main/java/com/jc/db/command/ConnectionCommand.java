package com.jc.db.command;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.NamingException;

public interface ConnectionCommand {

   public Connection createConnection() throws ClassNotFoundException, FileNotFoundException, NamingException, SQLException;
}
