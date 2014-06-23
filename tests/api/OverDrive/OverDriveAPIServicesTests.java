package api.OverDrive;

import static org.junit.Assert.*;

import org.API.OverDrive.IOverDriveAPI;
import org.API.OverDrive.OverDriveAPIServices;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

public class OverDriveAPIServicesTests {

	private OverDriveAPIServices service;
	private IOverDriveAPI overDriveAPIMock;
	
	
	private String clientKey = "aDummyClientKey";
	private String clientSecret = "aDummyClientSecret";
	private long libraryId = 0;
	
	@Before
	public void setUp() throws Exception 
	{
		this.overDriveAPIMock = Mockito.mock(IOverDriveAPI.class);
		this.service = new OverDriveAPIServices(this.clientKey, this.clientSecret, this.libraryId, this.overDriveAPIMock);		
	}
	
	
	/**
	 * method getDigitalCollection
	 * when called
	 * should executesCorrectly
	 */
	@Test
	public void test_getDigitalCollection_called_executesCorrectly()
	{
		int limit = 25;
		int offset = 0;
		
		JSONObject expected = new JSONObject(); // Dummy Object
		
		Mockito.when(this.overDriveAPIMock.login()).thenReturn(null);//It does not matter
		Mockito.when(this.overDriveAPIMock.getLibraryInfo()).thenReturn(null);//It does not matter
		Mockito.when(this.overDriveAPIMock.getDigitalCollection(anyInt(), anyInt())).thenReturn(expected);
		
		JSONObject actual = this.service.getDigitalCollection(limit, offset);
		
		Mockito.verify(this.overDriveAPIMock, times(1)).login();
		Mockito.verify(this.overDriveAPIMock, times(1)).getLibraryInfo();
		Mockito.verify(this.overDriveAPIMock, times(1)).getDigitalCollection(limit, offset);
		
		assertEquals(expected, actual);
	}
	
	/**
	 * method getItemMetadata
	 * when called
	 * should executesCorrectly
	 */
	@Test
	public void test_getItemMetadata_called_executesCorrectly()
	{
		String overDriveId = "aDummyOverDriveId";
		JSONObject expected = new JSONObject(); // Dummy Object
		
		Mockito.when(this.overDriveAPIMock.login()).thenReturn(null);//It does not matter
		Mockito.when(this.overDriveAPIMock.getLibraryInfo()).thenReturn(null);//It does not matter
		Mockito.when(this.overDriveAPIMock.getItemMetadata(overDriveId)).thenReturn(expected);
		
		
		JSONObject actual = this.service.getItemMetadata(overDriveId);
		
		Mockito.verify(this.overDriveAPIMock, times(1)).login();
		Mockito.verify(this.overDriveAPIMock, times(1)).getLibraryInfo();
		Mockito.verify(this.overDriveAPIMock, times(1)).getItemMetadata(overDriveId);
		
		assertEquals(expected, actual);
	}
}