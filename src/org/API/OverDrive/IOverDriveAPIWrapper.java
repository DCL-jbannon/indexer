package org.API.OverDrive;

import org.json.simple.JSONObject;

public interface IOverDriveAPIWrapper 
{

	JSONObject login(String clientKey, String clientSecret) throws Exception;

	JSONObject getInfoDCLLibrary(String accessToken, long libraryId) throws Exception;

	JSONObject getDigitalCollection(String accessToken, String productsUrl, int limit, int offset) throws Exception;

	JSONObject getItemMetadata(String accessToken, String productsUrl, String overDriveId) throws Exception;

}
