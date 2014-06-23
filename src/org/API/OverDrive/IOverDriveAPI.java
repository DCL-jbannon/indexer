package org.API.OverDrive;

import org.json.simple.JSONObject;

public interface IOverDriveAPI {

	JSONObject login();

	JSONObject getLibraryInfo();

	JSONObject getDigitalCollection(int limit, int offset);

	JSONObject getItemMetadata(String overDriveId);

}
