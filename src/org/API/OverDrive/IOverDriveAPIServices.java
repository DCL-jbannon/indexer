package org.API.OverDrive;

import org.json.simple.JSONObject;

public interface IOverDriveAPIServices {

	public JSONObject getDigitalCollection(int limit, int offset);
	public JSONObject getItemMetadata(String overDriveId);

}
