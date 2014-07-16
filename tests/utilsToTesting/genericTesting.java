package utilsToTesting;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;

import org.API.OverDrive.IOverDriveAPIServices;
import org.API.OverDrive.OverDriveAPIServices;
import org.API.OverDrive.OverDriveCollectionIterator;
import org.vufind.econtent.PopulateSolrOverDriveAPIItems;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.solr.SolrWrapper;

public class genericTesting
{
	//@Test
	public void test1() throws SQLException
	{
		SolrWrapper solrWrapper = new SolrWrapper("vufind-dev:8080/solr/testEcontent");
		OverDriveCollectionIterator odci = new OverDriveCollectionIterator("", "", 1344);
		//String databaseConnectionInfo = "jdbc:mysql://localhost/dclecontent?user=&password=&useUnicode=yes&characterEncoding=UTF-8";
		String databaseConnectionInfo = "jdbc:mysql://10.254.2.93/dclecontent_test?user=&password=&useUnicode=yes&characterEncoding=UTF-8";
		Connection conn = DriverManager.getConnection(databaseConnectionInfo);		
		IOverDriveAPIServices overDriveAPI = new OverDriveAPIServices("", "", 1344);
		PopulateSolrOverDriveAPIItems service = new PopulateSolrOverDriveAPIItems(odci, conn, solrWrapper, overDriveAPI);
		service.execute();
	}
	
	//@Test
	public void test()
	{
		OverDriveCollectionIterator test = new OverDriveCollectionIterator("", "", 1344);
		int i = 1;
		while (test.hasNext() && i<2)
		{
			JSONObject result = test.next();
			JSONArray a = (JSONArray) result.get("products");
			Iterator b = a.iterator();
			while(b.hasNext())
			{
				JSONObject c = (JSONObject) b.next();
				System.out.println(c.toJSONString());
			}
			i++;
		}
	}
}