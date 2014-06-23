/**
 * 
 */
package org.API.OverDrive;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class OverDriveAPIWrapper implements IOverDriveAPIWrapper
{
	private final static String tokenUrl = "https://oauth.overdrive.com/token";
	private final static String baseAPIUrl = "https://api.overdrive.com";
	private final static String userAgent = "Douglas County Libraries OverDrive API Version 1.0 - JAVA";
	
	private String response = new String();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public JSONObject login(String clientKey, String clientSecret) throws Exception 
	{
		Base64 base64 = new Base64();
		String base64AuthString = base64.encodeToString( (clientKey + ":" + clientSecret).getBytes());
		String bodyPost = "grant_type=client_credentials";
		
		HttpURLConnection conn = this.getConnection(OverDriveAPIWrapper.tokenUrl);
		
		conn.setRequestProperty("Authorization", "Basic " + base64AuthString);
		conn.setRequestProperty("Content-Length", "" + Integer.toString(bodyPost.getBytes().length));
		conn.setRequestMethod("POST");
		
		DataOutputStream wr = new DataOutputStream(conn.getOutputStream ());
		wr.writeBytes(bodyPost);
		wr.flush();
		wr.close();
		
		return this.getResults(conn);
	}
	
	public JSONObject getInfoDCLLibrary(String accessToken, long libraryid) throws Exception
	{
		String url = OverDriveAPIWrapper.baseAPIUrl + "/v1/libraries/" + libraryid;
		return this.execCallToAPIAndReturnResults(accessToken, url);
	}
	
	public JSONObject getDigitalCollection(String accessToken, String productsUrl, Integer limit) throws Exception
	{
		return this.getDigitalCollection(accessToken, productsUrl, limit, 0);
	}
	
	public JSONObject getDigitalCollection(String accessToken, String productsUrl, int limit, int offset) throws Exception
	{
		productsUrl += "?limit=" + limit + "&offset=" + offset;
		return this.execCallToAPIAndReturnResults(accessToken, productsUrl);
	}
	
	public JSONObject getItemMetadata(String accessToken, String productsUrl, String overDriveId) throws Exception {
		String urlItemMetadata = productsUrl + "/" + overDriveId+"/metadata";
		return this.execCallToAPIAndReturnResults(accessToken, urlItemMetadata);
	}
	
	/** PRIVATE METHODS 
	 * @throws Exception **/
	private JSONObject execCallToAPIAndReturnResults(String accessToken, String Url) throws Exception
	{
		HttpURLConnection conn = this.getConnection(Url);
		//System.out.println(Url);
		conn.setRequestProperty("Authorization", "Bearer " + accessToken);
		return this.getResults(conn);
	}
	
	private JSONObject getResults(HttpURLConnection conn) throws Exception
	{
		String line;
		response = "";
		BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		while ((line = reader.readLine()) != null) {
			response += line;
		}
		conn.disconnect();
		//System.out.println(response);
		JSONObject obj= (JSONObject) JSONValue.parse(response);
		return obj;
	}
	
	private HttpURLConnection getConnection(String url) throws Exception
	{
		URL urlObject = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) urlObject.openConnection();
		conn.setRequestProperty("User-Agent", OverDriveAPIWrapper.userAgent);
		conn.setDoOutput(true);
		conn.setUseCaches(false);
		return conn;
	}
	
}