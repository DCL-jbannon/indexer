package api.OverDrive;

import static org.junit.Assert.*;
import mother.OverDriveAPIResultsMother;
import org.API.OverDrive.OverDriveAPIUtils;
import org.apache.solr.common.SolrInputDocument;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.solr.SolrConstantsEContentFields;

public class OverDriveAPIUtilsTests {

	private OverDriveAPIResultsMother overDriveApiResultMother;
	private OverDriveAPIUtils service;
	
	@Before
	public void setUp() throws Exception 
	{
		this.overDriveApiResultMother = new OverDriveAPIResultsMother();
		this.service = new OverDriveAPIUtils();
	}
	
	/**
	 * method getSolrInputDocumentFromDigitalCollectionItem
	 * when called
	 * should executesCorrectly
	 */
	@Test
	public void test_getSolrInputDocumentFromDigitalCollectionItem_called_executesCorrectly() 
	{
		String eContentRecordId = "12345";
		String idExpected = "econtentRecord" + eContentRecordId;
		String recordTypeExpected = "econtentRecord";
		String titleExpected = "The Adventures of Sherlock Holmes";
		String titleSortExpected = "The Adventures of Sherlock Holmes";
		String availableExpected = "OverDrive";
		String authorExpected = "Sir Arthur Conan Doyle";
		String keywordsExpected = titleExpected + "\n" + authorExpected + "\n" + availableExpected;
		String languageExpected = "English";
		String formatCategoryExpected = "EMedia";
		String bibSuppressionExpected = "notsuppressed";
		
		JSONObject itemFromItemMetadata = this.overDriveApiResultMother.getItemMetadata();
		
		SolrInputDocument actual = this.service.getSolrInputDocumentFromDigitalCollectionItem(eContentRecordId, itemFromItemMetadata);
		
		assertEquals(idExpected, actual.get(SolrConstantsEContentFields.id).getValue());
		assertEquals(recordTypeExpected, actual.get(SolrConstantsEContentFields.recordType).getValue());
		assertEquals(titleExpected, actual.get(SolrConstantsEContentFields.title).getValue());
		assertEquals(titleSortExpected, actual.get(SolrConstantsEContentFields.titleSort).getValue());
		assertEquals(availableExpected, actual.get(SolrConstantsEContentFields.available).getValue());
		assertEquals(authorExpected, actual.get(SolrConstantsEContentFields.author).getValue());
		assertEquals(keywordsExpected, actual.get(SolrConstantsEContentFields.keywords).getValue());
		assertEquals(languageExpected, actual.get(SolrConstantsEContentFields.language).getValue());
		assertEquals(formatCategoryExpected, actual.get(SolrConstantsEContentFields.format_category).getValue());
		assertEquals(bibSuppressionExpected, actual.get(SolrConstantsEContentFields.bibsupresion).getValue());
	}
	
	/**
	 * method getAuthorFromAPIItem
	 * when called
	 * should executesCorrectly
	 */
	@Test
	public void test_getAuthorFromAPIItem_called_executesCorrectly()
	{
		String expected = "Sir Arthur Conan Doyle";
		JSONObject item = this.overDriveApiResultMother.getItemMetadata();
		String actual = this.service.getAuthorFromAPIItem(item);
		assertEquals(expected, actual);
	}
	
	/**
	 * method getAuthorFromAPIItem
	 * when noAuthor
	 * should returnEmptyString
	 */
	@Test
	public void test_getAuthorFromAPIItem_noAuthor_returnEmptyString()
	{
		String expected = "";
		JSONObject item = this.overDriveApiResultMother.getItemMetadataNoAuthor();
		String actual = this.service.getAuthorFromAPIItem(item);
		assertEquals(expected, actual);
	}
	
	/**
	 * method getISBNFromAPIItem
	 * when called
	 * should returnCorrectly
	 */
	@Test
	public void test_getISBN_called_returnCorrectly()
	{
		String expected = "9781620115091";
		JSONObject item = this.overDriveApiResultMother.getItemMetadata();
		String actual = this.service.getISBNFromAPIItem(item);
		assertEquals(expected, actual);
	}

}