package dbTests;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.io.IOUtils;
import org.dbunit.DBTestCase;
import org.dbunit.PropertiesBasedJdbcDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.ext.mysql.MySqlDataTypeFactory;
import org.vufind.Util;

import com.mysql.jdbc.PreparedStatement;

public abstract class BaseDBTests /*extends DBTestCase*/
{
    protected static Connection conn = null;
	public abstract String getDatabaseName();
	public abstract String getConnectionString();
	public abstract String getSQLFileName();
	
	/***
	 * NO COMMMENTS ALLOWED
	 */
	public BaseDBTests()
	{
		/*System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_DRIVER_CLASS, "com.mysql.jdbc.Driver" );
        System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL, "jdbc:mysql://localhost:3306/" + this.getDatabaseName() );
        System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_USERNAME, "root" );
        System.setProperty( PropertiesBasedJdbcDatabaseTester.DBUNIT_PASSWORD, "" );*/
		if (this.conn == null)
		{
			java.sql.PreparedStatement stmt = null;
			try
			{
				/*IDatabaseConnection connection = getConnection();
				connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new MySqlDataTypeFactory());*/
				
				this.conn = DriverManager.getConnection(this.getConnectionString());
				stmt = conn.prepareStatement("CREATE DATABASE IF NOT EXISTS " + this.getDatabaseName());
				stmt.execute();
				this.conn.setCatalog(this.getDatabaseName());
				
				/*
				 * http://www.java-examples.com/java-string-split-example
				 * Let's create the tables. Only structure
				 */
				String query = IOUtils.toString(new FileReader(this.getSQLFileName()));
				String[] querySplited = query.split(";");
				
				for(int i =0; i < querySplited.length ; i++)
				{
					stmt = this.conn.prepareStatement(querySplited[i]);
					stmt.execute();
				}
				stmt.close();
			}
			catch (Exception e) 
			{
				System.out.println("exception type: " + e.getClass() + " Exception Message: " + e.getMessage());
				System.exit(0);
			}
		}
	}
}