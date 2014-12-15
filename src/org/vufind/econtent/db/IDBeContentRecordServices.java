package org.vufind.econtent.db;

import java.sql.SQLException;

import org.json.simple.JSONObject;

public interface IDBeContentRecordServices {

	public Boolean overDriveRecordExists(String overDriveId, String source) throws SQLException;
	public Boolean addOverDriveAPIItem(JSONObject item) throws SQLException;
	public Boolean deleteRecordById(String id) throws SQLException;
	public String getRecordId(String overDriveID, String source) throws SQLException;
	public Boolean updateOverDriveAPIItem(String recordId, JSONObject item) throws SQLException;
    public Boolean touch(String recordId) throws SQLException;
}
