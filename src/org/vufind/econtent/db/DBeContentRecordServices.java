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


	public Boolean overDriveRecordExists(String overDriveID, String source) throws SQLException
	{
        return getRecordId(overDriveID, source) != null;
 	}

    private PreparedStatement selectRecordStatement = null;
	public String getRecordId(String overDriveID, String source) throws SQLException
	{
        if(selectRecordStatement==null) {
            selectRecordStatement = this.conn.prepareStatement(
                "SELECT id " +
                "FROM " + DBeContentRecordServices.tableName + " " +
                "WHERE " +
                    "sourceUrl LIKE ? " +
                    "AND source = ?");
        }

        selectRecordStatement.setString(1, "%"+overDriveID);
        selectRecordStatement.setString(2, source);

		ResultSet rs = selectRecordStatement.executeQuery();
		if(rs.next())
		{
			return rs.getString("id");
		}
		return null;
	}

    private PreparedStatement deleteRecordStatement = null;
	public Boolean deleteRecordById(String id) throws SQLException 
	{
        if(deleteRecordStatement==null) {
            deleteRecordStatement = this.conn.prepareStatement(
                    "DELETE FROM " + DBeContentRecordServices.tableName + " WHERE id = ?");
        }
        deleteRecordStatement.setString(1, id);
        deleteRecordStatement.execute();
		return true;
	}

    private PreparedStatement insertRecordStatement = null;
	public Boolean addOverDriveAPIItem(JSONObject item) throws SQLException
	{
        if(insertRecordStatement==null) {
            insertRecordStatement = this.conn.prepareStatement(
                "INSERT INTO " + DBeContentRecordServices.tableName + "(`title`,`subTitle`,`accessType`, `author`, `source`, `sourceUrl`, `date_added`, `publisher`, `isbn`, `genre`, `external_id`, `last_touched`) " +
                "VALUES (?, ?, 'free', ?, 'OverDriveAPI', ?, ?, ?, ?, ?, ?, NOW())");
        }

		long unixTime = System.currentTimeMillis() / 1000L;
		
		String overDriveId = (String) item.get("id");
		String title = (String) item.get("title");
		String subtitle = "" + (String) item.get("subtitle");
		String author = this.overDriveApiUtils.getAuthorFromAPIItem(item);
		String publisher = "" + (String) item.get("publisher");
		String isbn = ""+this.overDriveApiUtils.getISBNFromAPIItem(item);
		String mediaType = (String) item.get("mediaType");
		String sourceUrl = "http://www.emedia2go.org/ContentDetails.htm?ID="+overDriveId;

	   try
	   {
           insertRecordStatement.setString(1, "" + title);
           insertRecordStatement.setString(2, "" + subtitle);
           insertRecordStatement.setString(3, "" + author);
           insertRecordStatement.setString(4, "" + sourceUrl);
           insertRecordStatement.setString(5, "" + unixTime);
           insertRecordStatement.setString(6, "" + publisher);
           insertRecordStatement.setString(7, "" + isbn);
           insertRecordStatement.setString(8, "" + mediaType);
           insertRecordStatement.setString(9, "" + overDriveId);
           insertRecordStatement.execute();
	   }
	   catch(Exception e)
	   {
		   System.out.println(e.getMessage() + "  " + title);
		   throw new SQLException(e.getMessage() + "  " + title);
	   }
	   return true;
	}

    private PreparedStatement updateRecordStatement = null;
	public Boolean updateOverDriveAPIItem(String recordId, JSONObject item) throws SQLException
	{
        if(updateRecordStatement==null) {
            updateRecordStatement = this.conn.prepareStatement(
                "UPDATE `econtent_record` " +
                "	  SET `title` = ? "+
                "	  , `subTitle` = ? "+
                "	  , `author` = ? "+
                "	  , `sourceUrl` = ? "+
                "	  , `date_updated` = ? "+
                "	  , `publisher` = ? "+
                "	  , `genre` = ? "+
                "	  , `isbn` = ? "+
                "	  , `external_id` = ? "+
                "	  , `status` = ? " +
                "     , `last_touched` = NOW()"+
                "	  WHERE `id` = ?");
        }

		long unixTime = System.currentTimeMillis() / 1000L;
		String overDriveId = (String) item.get("id");

		String title = (String) item.get("title");
        boolean isOwned = false;
        //int copiesOwned = 0;
        try{
            //copiesOwned = Integer.parseInt(item.get("copiesOwned").toString());
            isOwned = Boolean.parseBoolean(item.get("isOwnedByCollections").toString());
        } catch(Exception e){
            e.printStackTrace();
        }
		String subtitle = "" + (String) item.get("subtitle");
		String author = this.overDriveApiUtils.getAuthorFromAPIItem(item);
		String publisher = "" + (String) item.get("publisher");
		String sourceUrl = "http://www.emedia2go.org/ContentDetails.htm?ID="+overDriveId;
		String mediaType = (String) item.get("mediaType");
		String isbn = ""+this.overDriveApiUtils.getISBNFromAPIItem(item);
		 
		try
		{
            updateRecordStatement.setString(1, "" + title);
            updateRecordStatement.setString(2, "" + subtitle);
            updateRecordStatement.setString(3, "" + author);
            updateRecordStatement.setString(4, "" + sourceUrl);
            updateRecordStatement.setString(5, "" + unixTime);
            updateRecordStatement.setString(6, "" + publisher);
            updateRecordStatement.setString(7, "" + mediaType);
            updateRecordStatement.setString(8, "" + isbn);
            updateRecordStatement.setString(9, "" + overDriveId);
            updateRecordStatement.setString(10, isOwned ? "active" : "deleted");
            updateRecordStatement.setString(11, "" + recordId);
            updateRecordStatement.execute();
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage() + "  " + title);
			throw new SQLException(e.getMessage() + "  " + title);
		}
		return true;
	}

    private PreparedStatement touchRecordStatement = null;
    public Boolean touch(String recordId, boolean isActive) throws SQLException {
        if (touchRecordStatement == null) {
            touchRecordStatement = this.conn.prepareStatement(
                    "UPDATE `econtent_record` " +
                            "	  SET `last_touched` = NOW(), `status`=?" +
                            "	  WHERE `id` = ?");
        }
        touchRecordStatement.setString(1, isActive ? "active" : "deleted");
        touchRecordStatement.setString(2, recordId);
        touchRecordStatement.execute();

        return true;
    }

}