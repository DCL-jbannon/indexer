package api.OverDrive;

import org.API.OverDrive.OverDriveAPIWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;


public class OverDriveAPIWrapperTests {

	public static final String clientKey = "";
	public static final String clientSecret = "";
	public static final Long libraryId = 1344L;
	
	
	private OverDriveAPIWrapper service;
	private static String accessToken = new String();
	private static String productURL = new String();
	
	@Before
	public void setUp() throws Exception 
	{
		this.service = new OverDriveAPIWrapper();		
	}

	/**
	 * method login
	 * when called
	 * should returnCorrectLoginInfo
	 * @throws Exception 
	 */
	@Test
	public void test_login_called_returnCorrectLoginInfo() throws Exception
	{
		String expected = "LIB META AVAIL SRCH";
		JSONObject actual = this.service.login(OverDriveAPIWrapperTests.clientKey, OverDriveAPIWrapperTests.clientSecret);
		org.junit.Assert.assertEquals(expected, actual.get("scope"));
		OverDriveAPIWrapperTests.accessToken = (String) actual.get("access_token");
	}
	
	/**
	 * method login
	 * when calledTwice
	 * should returnCorrectLoginInfo
	 * @throws Exception 
	 * The problem was that the response var was appending the results if you called more than one time a method of this class.
	 */
	@Test
	public void test_login_calledTwice_returnCorrectLoginInfo() throws Exception
	{
		String expected = "LIB META AVAIL SRCH";
		JSONObject actual = this.service.login(OverDriveAPIWrapperTests.clientKey, OverDriveAPIWrapperTests.clientSecret);
		actual = this.service.login(OverDriveAPIWrapperTests.clientKey, OverDriveAPIWrapperTests.clientSecret);
		org.junit.Assert.assertEquals(expected, actual.get("scope"));
		OverDriveAPIWrapperTests.accessToken = (String) actual.get("access_token");
	}
	
	/**
	 * method getInfoDCLLibrary
	 * when called
	 * should returnCorrectCollectionInfo
	 */
	@Test
	public void test_getInfoDCLLibrary_called_returnCorrectCollectionInfo() 
	{
		Long expected = OverDriveAPIWrapperTests.libraryId;
		
		JSONObject actual;
		try 
		{		
			actual = this.service.getInfoDCLLibrary(OverDriveAPIWrapperTests.accessToken, OverDriveAPIWrapperTests.libraryId);
			org.junit.Assert.assertEquals(expected, actual.get("id"));
			
			JSONObject links = (JSONObject) actual.get("links");
			JSONObject products = (JSONObject) links.get("products");
			OverDriveAPIWrapperTests.productURL = (String) products.get("href");
			
		} catch (Exception e) {
			org.junit.Assert.assertTrue("Exception not expected: " + e.getMessage(), false);
		}
	}
	
	/**
	 * method getDigitalCollection
	 * when called
	 * should returnCorrectDigitalCollection
	 */
	@Test
	public void test_getDigitalCollection_called_returnCorrectDigitalCollection()
	{
		Integer expected = 3;
		JSONObject actual;
		try
		{
			actual = this.service.getDigitalCollection(OverDriveAPIWrapperTests.accessToken, OverDriveAPIWrapperTests.productURL, 3, 300);
			JSONArray items = (JSONArray) actual.get("products");
			org.junit.Assert.assertEquals(expected, (Integer)items.size());
			
		} catch (Exception e) {
			org.junit.Assert.assertTrue("Exception not expected: " + e.getMessage(), false);
		}
	}
	
	/**
	 * method getItemMetadata
	 * when called
	 * should returnItem
	 */
	@Test
	public void test_getItemMetadata_called_returnItem()
	{
		String expected,overDriveId;
		expected = overDriveId = "76c1b7d0-17f4-4c05-8397-c66c17411584";
		try
		{
			JSONObject results = this.service.getItemMetadata(OverDriveAPIWrapperTests.accessToken, OverDriveAPIWrapperTests.productURL, overDriveId);
			String actual = (String) results.get("id");
			org.junit.Assert.assertEquals(expected, actual);
		} catch (Exception e) {
			org.junit.Assert.assertTrue("Exception not expected: " + e.getMessage(), false);
		}
	}
	
}