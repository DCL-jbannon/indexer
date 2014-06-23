package utilsToTesting;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;

import org.API.OverDrive.OverDriveCollectionIterator;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.xml.FlatDtdDataSet;
import org.dbunit.ext.mysql.MySqlDataTypeFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;

import com.mysql.jdbc.Connection;

public class GenerateDTDFile
{

	@Test
	public void test_methodName_called_should() throws Exception 
	{
		
	        // database connection
	        Class driverClass = Class.forName("com.mysql.jdbc.Driver");
	        Connection jdbcConnection = (Connection) DriverManager.getConnection("jdbc:mysql://vufind-dev:3306/dclecontent", "root", "dev");
	        IDatabaseConnection connection = new DatabaseConnection(jdbcConnection);
	        connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new MySqlDataTypeFactory());
	        // write DTD file
	        FlatDtdDataSet.write(connection.createDataSet(), new FileOutputStream("test.dtd"));
	}

}