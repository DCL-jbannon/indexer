package dcl.utils;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.dcl.utils.ActiveEcontentUtils;
import org.junit.Test;

public class ActiveEcontentUtilsTests
{


	/**
	 * method add
	 * when calledFirstTime
	 * should executesCorrectly
	 */
	@Test
	public void test_add_calledFirstTime_executesCorrectly()
	{
		int expected = 1;
		ActiveEcontentUtils.addActiveEcontent("112");
		ArrayList<String> list = ActiveEcontentUtils.getList();
		
		assertEquals(expected, list.size());
	}
	
	/**
	 * method add
	 * when calledSecondTime
	 * should executesCorrectly
	 */
	@Test
	public void test_add_calledSecondTime_executesCorrectly()
	{
		int expected = 2;
		ActiveEcontentUtils.addActiveEcontent("12311");
		ArrayList<String> list = ActiveEcontentUtils.getList();
		
		assertEquals(expected, list.size());
	}
	
	/**
	 * method getCommaSeparatedString
	 * when called
	 * should executesCorrectly
	 */
	@Test
	public void test_getCommaSeparatedString_called_executesCorrectly()
	{
		String expected = "112,12311,981";
		ActiveEcontentUtils.addActiveEcontent("981");
		String actual = ActiveEcontentUtils.getCommaSeparatedString();
		assertEquals(expected, actual);
	}
	

}
