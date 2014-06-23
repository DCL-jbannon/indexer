package db;

import java.sql.SQLException;

import org.json.simple.JSONObject;

public interface IDBeContentRecordServices {

	public Boolean existOverDriveRecord(String overDriveId, String source) throws SQLException;
	public Boolean addOverDriveAPIItem(JSONObject item) throws SQLException;
	public Boolean deleteRecordById(String id) throws SQLException;
	public String selectRecordIdByOverDriveIdBySource(String overDriveID, String source) throws SQLException;
	public Boolean updateOverDriveAPIItem(String recordId, JSONObject item) throws SQLException;
}
