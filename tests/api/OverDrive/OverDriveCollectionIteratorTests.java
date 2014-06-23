package api.OverDrive;

import static org.junit.Assert.*;

import org.API.OverDrive.IOverDriveAPIServices;
import org.API.OverDrive.OverDriveAPIServices;
import org.API.OverDrive.OverDriveAPIWrapper;
import org.API.OverDrive.OverDriveCollectionIterator;
import org.json.simple.JSONObject;
import org.junit.*;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

public class OverDriveCollectionIteratorTests
{
	
	private OverDriveCollectionIterator service;
	private IOverDriveAPIServices overDriveAPIServicesMock; 
	
	@Before
	public void setUp() throws Exception 
	{
		this.overDriveAPIServicesMock = Mockito.mock(IOverDriveAPIServices.class);
		this.service = new OverDriveCollectionIterator(this.overDriveAPIServicesMock);		
	}
	
	
	/**
	 * method hasNext
	 * when calledFirstTime
	 * should returnTrue
	 */
	@Test
	public void test_hasNext_calledFirstTime_returnTrue()
	{
		
		JSONObject result = new JSONObject();
		result.put("totalItems", 9999L); //It is ok (http://code.google.com/p/json-simple/wiki/EncodingExamples)
		Mockito.when(this.overDriveAPIServicesMock.getDigitalCollection(anyInt(), anyInt()))
												  .thenReturn(result);
		
		Boolean actual = this.service.hasNext();
		assertTrue(actual);
	}
	
	/**
	 * method next
	 * when firstCalled
	 * should executesCorrectly
	 */
	@Test
	public void test_next_called_executesCorrectly()
	{
		JSONObject expected = new JSONObject();

		Mockito.when(this.overDriveAPIServicesMock.getDigitalCollection(anyInt(), anyInt()))
		  										  .thenReturn(expected);
		this.service.setTotalItems(302);
		
		JSONObject actual = this.service.next();
	
		Mockito.verify(this.overDriveAPIServicesMock).getDigitalCollection(eq(300), eq(0));
		assertEquals(expected, actual);
	}
	
	/**
	 * method next
	 * when secondCall
	 * should executesCorrectly
	 */
	@Test
	public void test_next_secondCall_executesCorrectly()
	{
		JSONObject resultFirstCall = new JSONObject();
		JSONObject expected = new JSONObject();

		Mockito.when(this.overDriveAPIServicesMock.getDigitalCollection(anyInt(), anyInt()))
		  										  .thenReturn(resultFirstCall);
		this.service.setTotalItems(302);
		
		this.service.next();
		JSONObject actual = this.service.next();
	
		Mockito.verify(this.overDriveAPIServicesMock, times(1)).getDigitalCollection(eq(300), eq(0));
		Mockito.verify(this.overDriveAPIServicesMock, times(1)).getDigitalCollection(eq(300), eq(300));
		assertEquals(expected, actual);
	}

}
