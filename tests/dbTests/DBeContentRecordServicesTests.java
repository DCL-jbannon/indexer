/***
 * http://www.dbunit.org/howto.html
 */
package dbTests;

import static org.junit.Assert.*;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import mother.OverDriveAPIResultsMother;

import org.API.OverDrive.IOverDriveAPIUtils;
import org.dbunit.DataSourceBasedDBTestCase;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import org.vufind.econtent.db.DBeContentRecordServices;

@RunWith(Parameterized.class)
public class DBeContentRecordServicesTests extends BaseDBEcontentTests
{
	private String overDriveID = "C1081EDF-3356-4D46-8EDE-3E977EBB6FF2";
	
	private DBeContentRecordServices service;
	private OverDriveAPIResultsMother overDriveAPIResultsMother;	
	private IOverDriveAPIUtils overDriveApiUtilsMock;
	
	private String source;
	private String expectedId;
	private String deleteId;
	

	@Before
	public void setUp() throws Exception 
	{
		this.overDriveApiUtilsMock = Mockito.mock(IOverDriveAPIUtils.class);
		this.overDriveAPIResultsMother = new OverDriveAPIResultsMother();
		this.service = new DBeContentRecordServices(this.conn, this.overDriveApiUtilsMock);
	}
	
	
	public DBeContentRecordServicesTests(String source, String Id)
	{
		this.source = source;
		this.expectedId = Id;
		this.deleteId = Id;
	}
	
	/**
	 * method existOverDriveRecord
	 * when DoNotExists
	 * should returnFalse
	 * @throws SQLException 
	 */
	@Test
	public void test_existOverDriveRecord_DoNotExists_returnTrue() throws SQLException 
	{
		this.insertNoValidOverDriveRecord();
		Boolean actual = this.service.existOverDriveRecord(this.overDriveID, this.source);
		assertFalse(actual);
	}
	
	@Parameterized.Parameters
    public static Collection primeNumbers()
	{
		return Arrays.asList(new Object[][] {
                {"OverDrive", "1"},
                { "OverDriveAPI", "2"}
        });
    }
	
	/**
	 * method existOverDriveRecord
	 * when existsAndSourceisOverDrive
	 * should returnTrue
	 * @throws SQLException 
	 */
	@Test
	public void test_existOverDriveRecord_existsAndSourceisOverDrive_returnTrue() throws SQLException
	{
		this.insertValidOverDriveRecords();
		Boolean actual = this.service.existOverDriveRecord(this.overDriveID, this.source);
		assertTrue(actual);
	}
	
	/**
	 * method selectRecordIdByOverDriveIdBySource
	 * when called
	 * should executesCorrectly
	 * @throws SQLException 
	 */
	@Test
	public void test_selectRecordIdByOverDriveIdBySource_called_executesCorrectly() throws SQLException 
	{
		this.insertValidOverDriveRecords();
		String actual = this.service.selectRecordIdByOverDriveIdBySource(this.overDriveID, this.source);
		assertEquals(this.expectedId, actual);
	}
	
	/**
	 * method deleteRecordById
	 * when called
	 * should executesCorrectly
	 * @throws SQLException 
	 */
	@Test
	public void test_deleteRecordById_called_executesCorrectly() throws SQLException 
	{
		this.insertValidOverDriveRecords();
		this.service.deleteRecordById(this.deleteId);
		Boolean actual = this.service.existOverDriveRecord(this.overDriveID, this.source);
		assertFalse(actual);
	}
	
	/**
	 * method addOverDriveAPIItem
	 * when called
	 * should executesCorrectly
	 * @throws SQLException 
	 */
	@Test
	public void test_addOverDriveAPIItem_called_executesCorrectly() throws SQLException 
	{
		String author = "aDummyAuthor";
		JSONObject item = this.overDriveAPIResultsMother.getItemMetadata(this.overDriveID);
		
		Mockito.when(this.overDriveApiUtilsMock.getAuthorFromAPIItem(item)).thenReturn(author);
		
		this.service.addOverDriveAPIItem(item);
		Boolean actual = this.service.existOverDriveRecord(this.overDriveID, "OverDriveAPI");
		assertTrue(actual);
		
		Mockito.verify(this.overDriveApiUtilsMock, Mockito.times(1)).getAuthorFromAPIItem(item);
		
	}
	
	/**
	 * method addOverDriveAPIItem
	 * when titleHasSingleQuote
	 * should executesCorrectly
	 * @throws SQLException 
	 */
	@Test
	public void test_addOverDriveAPIItem_titleHasSingleQuote_executesCorrectly() throws SQLException 
	{
		JSONObject item = this.overDriveAPIResultsMother.getItemMetadata(this.overDriveID);
		item.put("title", "The Girl Who Kicked the Hornet's Nest");
		this.service.addOverDriveAPIItem(item);
		Boolean actual = this.service.existOverDriveRecord(this.overDriveID, "OverDriveAPI");
		assertTrue(actual);
	}
	
	/**
	 * method addOverDriveAPIItem
	 * when subtitleIsNull
	 * should executesCorrectly
	 * @throws SQLException 
	 */
	@Test
	public void test_addOverDriveAPIItem_subtitleIsNull_executesCorrectly() throws SQLException 
	{
		JSONObject item = this.overDriveAPIResultsMother.getItemMetadata(this.overDriveID);
		item.put("subtitle", null);
		this.service.addOverDriveAPIItem(item);
		Boolean actual = this.service.existOverDriveRecord(this.overDriveID, "OverDriveAPI");
		assertTrue(actual);
	}
	
	/**
	 * method updateOverDriveAPIItem
	 * when called
	 * should executesCorrectly
	 * @throws SQLException 
	 */
	@Test
	public void test_updateOverDriveAPIItem_called_executesCorrectly() throws SQLException 
	{
		String author = "aDummyAuthor";
		String isbn = "aDummyISBN";
		JSONObject item = this.overDriveAPIResultsMother.getItemMetadata(this.overDriveID);
		
		Mockito.when(this.overDriveApiUtilsMock.getAuthorFromAPIItem(item)).thenReturn(author);
		Mockito.when(this.overDriveApiUtilsMock.getISBNFromAPIItem(item)).thenReturn(isbn);
		
		this.service.addOverDriveAPIItem(item);
		String recordId = this.service.selectRecordIdByOverDriveIdBySource(this.overDriveID, "OverDriveAPI");
		this.service.updateOverDriveAPIItem(recordId, item);
		
		Boolean actual = this.service.existOverDriveRecord(this.overDriveID, "OverDriveAPI");
		assertTrue(actual);
		
		Mockito.verify(this.overDriveApiUtilsMock, Mockito.times(2)).getAuthorFromAPIItem(item);
		Mockito.verify(this.overDriveApiUtilsMock, Mockito.times(2)).getISBNFromAPIItem(item);
		
	}
	
	/*@Override
	protected IDataSet getDataSet() throws Exception 
	{
		return new FlatXmlDataSetBuilder().build(new FileInputStream("C:/projects/VuFind-Plus/vufind/import/tests/datasets/existsOverDriveItemByID.xml"));
	}*/
	
	private void insertValidOverDriveRecord(String source) throws SQLException
	{
		PreparedStatement stmt = this.conn.prepareStatement("INSERT INTO `econtent_record` (`id`, `cover`, `title`, `subTitle`, `accessType`, `availableCopies`, `onOrderCopies`, `author`, `author2`, `description`, `contents`, `subject`, `language`, `publisher`, `edition`, `isbn`, `issn`, `upc`, `lccn`, `series`, `topic`, `genre`, `region`, `era`, `target_audience`, `notes`, `ilsId`, `source`, `sourceUrl`, `purchaseUrl`, `publishDate`, `addedBy`, `date_added`, `date_updated`, `reviewedBy`, `reviewStatus`, `reviewDate`, `reviewNotes`, `trialTitle`, `marcControlField`, `collection`, `marcRecord`, `literary_form_full`, `status`) VALUES (NULL, NULL, 'aDummyTile', 'aDummySubTitle', 'free', '1', '0', NULL, NULL, NULL, NULL, NULL, 'aDummyLanguage', 'aDummyPublisher', 'aDummyEdition', NULL, NULL, NULL, 'aDummyLCCN', 'aDummySeries', NULL, NULL, NULL, 'aDummyEra', 'aDummyTargetAudience', NULL, NULL, '" + source + "', 'http://www.emedia2go.org/ContentDetails.htm?ID=C1081EDF-3356-4D46-8EDE-3E977EBB6FF2', NULL, NULL, '-1', '1212123123', NULL, '-1', 'Not Reviewed', NULL, NULL, '0', NULL, NULL, NULL, NULL, 'active');");
		stmt.execute();
	}
	
	private void insertValidOverDriveRecords() throws SQLException
	{
		this.insertValidOverDriveRecord("OverDrive");
		this.insertValidOverDriveRecord("OverDriveAPI");
	}
	
	private void insertNoValidOverDriveRecord() throws SQLException
	{
		PreparedStatement stmt = this.conn.prepareStatement("INSERT INTO `econtent_record` (`id`, `cover`, `title`, `subTitle`, `accessType`, `availableCopies`, `onOrderCopies`, `author`, `author2`, `description`, `contents`, `subject`, `language`, `publisher`, `edition`, `isbn`, `issn`, `upc`, `lccn`, `series`, `topic`, `genre`, `region`, `era`, `target_audience`, `notes`, `ilsId`, `source`, `sourceUrl`, `purchaseUrl`, `publishDate`, `addedBy`, `date_added`, `date_updated`, `reviewedBy`, `reviewStatus`, `reviewDate`, `reviewNotes`, `trialTitle`, `marcControlField`, `collection`, `marcRecord`, `literary_form_full`, `status`) VALUES (NULL, NULL, 'aDummyTile', 'aDummySubTitle', 'free', '1', '0', NULL, NULL, NULL, NULL, NULL, 'aDummyLanguage', 'aDummyPublisher', 'aDummyEdition', NULL, NULL, NULL, 'aDummyLCCN', 'aDummySeries', NULL, NULL, NULL, 'aDummyEra', 'aDummyTargetAudience', NULL, NULL, 'aDummySource', 'aDummySourceUrl', NULL, NULL, '-1', '1212123123', NULL, '-1', 'Not Reviewed', NULL, NULL, '0', NULL, NULL, NULL, NULL, 'active');");
		stmt.execute();
	}
	
	@After
    public void tearDown() throws SQLException
	{
		PreparedStatement stmt = this.conn.prepareStatement("TRUNCATE TABLE econtent_record");
		stmt.execute();
    }
}