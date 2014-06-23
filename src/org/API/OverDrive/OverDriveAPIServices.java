package org.API.OverDrive;

import org.json.simple.JSONObject;

public class OverDriveAPIServices implements IOverDriveAPIServices
{

	private IOverDriveAPI ova;
	
	public OverDriveAPIServices(String clientKey, String clientSecret, long libraryId, IOverDriveAPI overDriveAPI)
	{
		
		this.ova = overDriveAPI;
	}
	
	public OverDriveAPIServices (String clientKey, String clientSecret, long libraryId)
	{
		this(clientKey, clientSecret, libraryId, new OverDriveAPI(clientKey, clientSecret, libraryId));
	}

	/**
	 * Return a partial OverDrive Collection give the limit and time
	 * @param limit
	 * @param offset
	 * @return {@link JSONObject}
	 */
	public JSONObject getDigitalCollection(int limit, int offset)
	{
		this.ova.login();
		this.ova.getLibraryInfo();
		return this.ova.getDigitalCollection(limit, offset);
	}

	public JSONObject getItemMetadata(String overDriveId) 
	{
		this.ova.login();
		this.ova.getLibraryInfo();
		return this.ova.getItemMetadata(overDriveId);
	}

}
