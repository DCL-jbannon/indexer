package api.OverDrive;

import static org.junit.Assert.*;

import mother.OverDriveAPIResultsMother;

import org.API.OverDrive.IOverDriveAPIWrapper;
import org.API.OverDrive.OverDriveAPI;
import org.API.OverDrive.OverDriveAPIWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;


public class OverDriveAPITests {

	private OverDriveAPI service;
	private OverDriveAPIResultsMother overDriveAPIResultsMother;
	private IOverDriveAPIWrapper overDriveWrapperMock;
	
	private String clientKey = "aDummyClientKey";
	private String clientSecret = "aDummyClientSecret";
	private Long libraryId = 5454L;
	
	private String accessToken = "aDummyAccessToken";
	private String productsUrl = "aDummyProductUrl";
	
	@Before
	public void setUp() throws Exception 
	{
		this.overDriveAPIResultsMother = new OverDriveAPIResultsMother();
		this.overDriveWrapperMock = mock(IOverDriveAPIWrapper.class);
		this.service = new OverDriveAPI(this.clientKey, this.clientSecret, this.libraryId, this.overDriveWrapperMock);		
	}
	
	
	/**
	 * method login
	 * when TokenStillValid
	 * should returnSameResult
	 * @throws Exception 
	 */
	@Test
	public void test_login_TokenStillValid_returnSameResult() throws Exception
	{
		JSONObject result = this.overDriveAPIResultsMother.getLoginResult();
		String expected = OverDriveAPIResultsMother.accessToken;
		Mockito.when(this.overDriveWrapperMock.login(anyString(),anyString())).thenReturn(result);
		
		this.service.login();
		
		JSONObject resultFutureWillNotBeRunIt = this.overDriveAPIResultsMother.getLoginResult("aFutureAccessToken");
		Mockito.when(this.overDriveWrapperMock.login(anyString(),anyString())).thenReturn(resultFutureWillNotBeRunIt);
		
		JSONObject actual = this.service.login();
		assertEquals(expected, actual.get("access_token"));
	}
	
	/**
	 * method login
	 * when tokenIsNotValid
	 * should returnDiferentAccessToken
	 * @throws Exception 
	 */
	@Test
	public void test_login_tokenIsNotValid_returnDiferentAccessToken() throws Exception
	{
		JSONObject result = this.overDriveAPIResultsMother.getLoginResult();
		String expected = "aFutureDummyAccessToken";
		Mockito.when(this.overDriveWrapperMock.login(anyString(),anyString())).thenReturn(result);
		
		this.service.login();
		Integer moveTimeStampForward = (int)System.currentTimeMillis() - this.service.getTokenValidFor() - 1;
		
		JSONObject resultFuture = this.overDriveAPIResultsMother.getLoginResult(expected);
		Mockito.when(this.overDriveWrapperMock.login(anyString(),anyString())).thenReturn(resultFuture);
		this.service.setTokenTimeStamp(moveTimeStampForward);
		
		JSONObject actual = this.service.login();
		assertEquals(expected, actual.get("access_token"));
	}
	
	/**
	 * method login
	 * when called
	 * should executesCorrectly
	 * @throws Exception 
	 */
	@Test
	public void test_login_called_executesCorrectly() throws Exception
	{
		JSONObject result = this.overDriveAPIResultsMother.getLoginResult();
		String expected = OverDriveAPIResultsMother.accessToken;
		
		Mockito.when(this.overDriveWrapperMock.login(anyString(),anyString())).thenReturn(result);
		
		JSONObject actual = this.service.login();
		
		Mockito.verify(this.overDriveWrapperMock).login(this.clientKey, clientSecret);
		assertEquals("Checking result returning", result, actual);
		assertEquals(expected, this.service.getAccessToken());
		assertNotNull(this.service.getTokenTimeStamp());
	}
	
	/**
	 * method getLibraryInfo
	 * when called
	 * should executesCorrectly
	 * @throws Exception 
	 */
	@Test
	public void test_getLibraryInfo_called_executesCorrectly() throws Exception 
	{
		prepareAccessToken();
		
		String expected = "https://api.overdrive.com/v1/collections/L1BGAEAAA2f/products";
		JSONObject result = this.overDriveAPIResultsMother.getInfoLibrary();	

		Mockito.when(this.overDriveWrapperMock.getInfoDCLLibrary(anyString(),anyLong())).thenReturn(result);

		JSONObject actual = this.service.getLibraryInfo();
		
		assertEquals(result, actual);
		assertEquals("Products URL", expected, this.service.getProductsUsl());
		Mockito.verify(this.overDriveWrapperMock).getInfoDCLLibrary(this.accessToken, this.libraryId);			
	}
	
	/**
	 * method getLibraryInfo
	 * when calledSecondTime
	 * should NotCallWrapper
	 * @throws Exception 
	 */
	@Test
	public void test_getLibraryInfo_calledSecondTime_NotCallWrapper() throws Exception
	{
		prepareAccessToken();
		String expected = "https://api.overdrive.com/v1/collections/L1BGAEAAA2f/products";
		JSONObject result = this.overDriveAPIResultsMother.getInfoLibrary();
		
		Mockito.when(this.overDriveWrapperMock.getInfoDCLLibrary(anyString(),anyLong())).thenReturn(result);
		
		this.service.getLibraryInfo();
		JSONObject actual = this.service.getLibraryInfo();
		
		Mockito.verify(this.overDriveWrapperMock, times(1)).getInfoDCLLibrary(this.accessToken, this.libraryId);		
		assertEquals(result, actual);
		assertEquals("Products URL", expected, this.service.getProductsUsl());
	}
	
	/**
	 * method getDigitalCollection
	 * when called
	 * should executesCorrectly
	 * @throws Exception 
	 */
	@Test
	public void test_getDigitalCollection_called_executesCorrectly() throws Exception {
		
		Integer expected = 2;
		Integer limit = 234234; //a Dummy Integer 
		Integer offset = 232342; //a Dummy Integer 
		
		this.prepareAccessTokenAndProducstUrl();
		JSONObject result = this.overDriveAPIResultsMother.getDigitalCollection();
		
		Mockito.when(this.overDriveWrapperMock.getDigitalCollection(anyString(), anyString(), anyInt(), anyInt())).thenReturn(result);
		
		JSONObject actual = this.service.getDigitalCollection(limit, offset);
		
		assertEquals(result, actual);
		assertEquals("Three items", expected, (Integer) ((JSONArray)actual.get("products")).size());
		Mockito.verify(this.overDriveWrapperMock).getDigitalCollection(this.accessToken, this.productsUrl, limit, offset);
	}
	
	/**
	 * method getItemMetadata
	 * when called
	 * should executesCorrectly
	 * @throws Exception 
	 */
	@Test
	public void test_getItemMetadata_called_executesCorrectly() throws Exception
	{
		String overDriveId = "aDummyOverDriveId";
		JSONObject result = this.overDriveAPIResultsMother.getItemMetadata(overDriveId);
		this.prepareAccessTokenAndProducstUrl();
		Mockito.when(this.overDriveWrapperMock.getItemMetadata(anyString(), anyString(), anyString())).thenReturn(result);
		
		JSONObject actual = this.service.getItemMetadata(overDriveId);
		assertEquals(result, actual);
		Mockito.verify(this.overDriveWrapperMock).getItemMetadata(this.accessToken, this.productsUrl, overDriveId);
	}
		
	/**EXERCISES**/
	private void prepareAccessToken()
	{
		this.service.setAccessToken(this.accessToken);
	}
	
	private void prepareProductsUrl()
	{
		this.service.setProductsUsl(this.productsUrl);
	}
	
	private void prepareAccessTokenAndProducstUrl()
	{
		this.prepareAccessToken();
		this.prepareProductsUrl();
	}
	
}
