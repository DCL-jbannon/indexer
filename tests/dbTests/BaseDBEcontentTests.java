package dbTests;

import java.sql.SQLException;

import org.dbunit.DBTestCase;

public abstract class BaseDBEcontentTests extends BaseDBTests
{
	public String getDatabaseName() 
	{
		return "testdclecontent";
	}
	
	public String getConnectionString()
	{
		return "jdbc:mysql://localhost/?user=&password=&useUnicode=yes&characterEncoding=UTF-8";
	}
	
	public String getSQLFileName()
	{
		return "C:/projects/VuFind-Plus/test/sql/testEcontent.sql";
	}	
}