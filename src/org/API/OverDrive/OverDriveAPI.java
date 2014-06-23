package org.API.OverDrive;

import org.json.simple.JSONObject;

public class OverDriveAPI implements IOverDriveAPI
{
	private String clientKey;
	private String clientSecret;
	private long libraryId;
	private IOverDriveAPIWrapper odw;
	
	private String acccessToken = new String();
	private String productsUrl;
	private Integer tokenTS;
	private JSONObject resultLogin;
	private JSONObject resultLibraryInfo = new JSONObject();
	private static final Integer tokenValidFor = 3400000; //Milliseconds 3400 seconds
	

	public OverDriveAPI(String clientKey, String clientSecret, long libraryId)
	{
		this(clientKey, clientSecret, libraryId, new OverDriveAPIWrapper());
	}
	
	public OverDriveAPI(String clientKey, String clientSecret, long libraryId, IOverDriveAPIWrapper overDriveAPIWrapper)
	{
		this.clientKey = clientKey;
		this.clientSecret = clientSecret;
		this.libraryId = libraryId;
		this.odw = overDriveAPIWrapper;
	}
	
	public JSONObject login()
	{
		if(this.isTokenValid())
		{
			return this.resultLogin;
		}
		try
		{
			this.resultLogin = this.odw.login(this.clientKey, this.clientSecret);
			this.acccessToken = (String) this.resultLogin.get("access_token");
			this.tokenTS = this.getTimeStamp();
			return this.resultLogin;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public JSONObject getLibraryInfo()
	{
		if (this.resultLibraryInfo.size() == 0)
		{
			try
			{
				this.resultLibraryInfo = this.odw.getInfoDCLLibrary(this.acccessToken, this.libraryId);
			} 
			catch (Exception e) 
			{
				return null;
			}
			JSONObject links = (JSONObject) this.resultLibraryInfo.get("links");
			JSONObject products = (JSONObject) links.get("products");
			this.productsUrl = (String) products.get("href");
		}
		return this.resultLibraryInfo;
	}

	public JSONObject getDigitalCollection(int limit, int offset)
	{
		try
		{
			return this.odw.getDigitalCollection(this.acccessToken, this.productsUrl, limit, offset);
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	
	public JSONObject getItemMetadata(String overDriveId) 
	{
		try
		{
			return this.odw.getItemMetadata(this.acccessToken, this.productsUrl, overDriveId);
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	
	//Private methods & utils for test
	public String getAccessToken()
	{
		return this.acccessToken;
	}
	
	public Integer getTokenTimeStamp()
	{
		return this.tokenTS;
	}
	
	
	private Boolean isTokenValid()
	{
		if (this.acccessToken.isEmpty())
		{
			return false;
		}
		Integer maxValidTimeStamp = this.tokenTS + OverDriveAPI.tokenValidFor;
		if ( (maxValidTimeStamp) >= this.getTimeStamp())
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Test Purpouse
	 * @param String accessToken
	 */
	public void setAccessToken(String accessToken)
	{
		this.acccessToken = accessToken;
	}

	public Object getProductsUsl() {
		return this.productsUrl;
	}
	
	public void setProductsUsl(String productsUrl)
	{
		this.productsUrl = productsUrl;
	}
	
	public void setTokenTimeStamp(Integer timestamp)
	{
		this.tokenTS = timestamp;
	}
	
	private Integer getTimeStamp()
	{
		return  (int)System.currentTimeMillis();
	}

	public Integer getTokenValidFor()
	{
		return OverDriveAPI.tokenValidFor;
	}

	
	
}
