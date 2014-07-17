package org.vufind.econtent.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import org.API.OverDrive.IOverDriveAPIUtils;
import org.API.OverDrive.OverDriveAPIUtils;
import org.json.simple.JSONObject;

public class DBeContentRecordServices implements IDBeContentRecordServices
{
	private static final String tableName = "econtent_record";
	private Connection conn;
	private IOverDriveAPIUtils overDriveApiUtils;
	
	public DBeContentRecordServices(Connection conn, IOverDriveAPIUtils overDriveApiUtils) 
	{
		this.conn = conn;
		this.overDriveApiUtils = overDriveApiUtils;
	}
	
	public DBeContentRecordServices(Connection conn) 
	{
		this(conn, new OverDriveAPIUtils());
	}

	public Boolean existOverDriveRecord(String overDriveID, String source) throws SQLException 
	{
		PreparedStatement stmt = this.conn.prepareStatement("SELECT * FROM " + DBeContentRecordServices.tableName + " WHERE source='" + source + "' and sourceUrl like '%" + overDriveID + "%'");
		ResultSet rs = stmt.executeQuery();
		if(rs.next())
		{
			return true;
		}
		return false;
 	}

	public String selectRecordIdByOverDriveIdBySource(String overDriveID, String source) throws SQLException
	{
		PreparedStatement stmt = this.conn.prepareStatement("SELECT id FROM " + DBeContentRecordServices.tableName + " WHERE source='" + source + "' and sourceUrl like '%" + overDriveID + "%'");
		ResultSet rs = stmt.executeQuery();
		if(rs.next())
		{
			return rs.getString("id");
		}
		return null;
	}

	public Boolean deleteRecordById(String id) throws SQLException 
	{
		PreparedStatement stmt = this.conn.prepareStatement("DELETE FROM " + DBeContentRecordServices.tableName + " WHERE id = " + id);
		stmt.execute();
		return true;
	}

	public Boolean addOverDriveAPIItem(JSONObject item) throws SQLException
	{
		long unixTime = System.currentTimeMillis() / 1000L;
		
		String overDriveId = (String) item.get("id");
		String title = (String) item.get("title");
		String subtitle = "" + (String) item.get("subtitle");
		String author = this.overDriveApiUtils.getAuthorFromAPIItem(item);
		String publisher = "" + (String) item.get("publisher");
		String isbn = ""+this.overDriveApiUtils.getISBNFromAPIItem(item);
		String mediaType = (String) item.get("mediaType");
		String sourceUrl = "http://www.emedia2go.org/ContentDetails.htm?ID="+overDriveId;
		
		String sql = "INSERT INTO " + DBeContentRecordServices.tableName + "(`id`,`title`,`subTitle`,`accessType`, `author`, `source`, `sourceUrl`, `date_added`, `publisher`, `isbn`, `genre`)";
			   sql += "VALUES (NULL,?, ?, 'free', ?, 'OverDriveAPI', ?, ?, ?, ?, ?)";
	   try
	   {
		   PreparedStatement stmt = this.conn.prepareStatement(sql);
		   stmt.setString(1, "" + title);
		   stmt.setString(2, "" + subtitle);
		   stmt.setString(3, "" + author);
		   stmt.setString(4, "" + sourceUrl);
		   stmt.setString(5, "" + unixTime);
		   stmt.setString(6, "" + publisher);
		   stmt.setString(7, "" + isbn);
		   stmt.setString(8, "" + mediaType);
		   stmt.execute();
	   }
	   catch(Exception e)
	   {
		   System.out.println(e.getMessage() + "  " + title);
		   throw new SQLException(e.getMessage() + "  " + title);
	   }
	   return true;
	}

	public Boolean updateOverDriveAPIItem(String recordId, JSONObject item) throws SQLException
	{
		long unixTime = System.currentTimeMillis() / 1000L;
		String overDriveId = (String) item.get("id");
		String title = (String) item.get("title");
		String subtitle = "" + (String) item.get("subtitle");
		String author = this.overDriveApiUtils.getAuthorFromAPIItem(item);
		String publisher = "" + (String) item.get("publisher");
		String sourceUrl = "http://www.emedia2go.org/ContentDetails.htm?ID="+overDriveId;
		String mediaType = (String) item.get("mediaType");
		String isbn = ""+this.overDriveApiUtils.getISBNFromAPIItem(item);
		
		String sql = "UPDATE `econtent_record` " +
				"	  SET `title` = ? "+
				"	  , `subTitle` = ? "+
				"	  , `author` = ? "+
				"	  , `sourceUrl` = ? "+
				"	  , `date_updated` = ? "+
				"	  , `publisher` = ? "+
				"	  , `genre` = ? "+
				"	  , `isbn` = ? "+
				"	  WHERE `id` = ?;";
		 
		try
		{
		  PreparedStatement stmt = this.conn.prepareStatement(sql);
		  stmt.setString(1, "" + title);
		  stmt.setString(2, "" + subtitle);
		  stmt.setString(3, "" + author);
		  stmt.setString(4, "" + sourceUrl);
		  stmt.setString(5, "" + unixTime);
		  stmt.setString(6, "" + publisher);
		  stmt.setString(7, "" + mediaType);
		  stmt.setString(8, "" + isbn);
		  stmt.setString(9, "" + recordId);
		  stmt.execute();
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage() + "  " + title);
			throw new SQLException(e.getMessage() + "  " + title);
		}
		return true;
	}
}