package org.econtent;

import java.sql.SQLException;
import java.util.ArrayList;
import mother.OverDriveAPIResultsMother;
import mother.SolrInputDocumentMother;
import org.API.OverDrive.IOverDriveAPIServices;
import org.API.OverDrive.IOverDriveCollectionIterator;
import org.apache.solr.common.SolrInputDocument;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import db.IDBeContentRecordServices;

public class PopulateOverDriveAPIItemsTests {
	
	private IOverDriveCollectionIterator overDriveCollectionIteratorMock;
	private IDBeContentRecordServices daoEContentMock;
	private IOverDriveAPIServices overDriveAPIServices;
	
	private OverDriveAPIResultsMother overDriveAPIResultsMother;
	private SolrInputDocumentMother solrDocumentMother;
	private PopulateOverDriveAPIItems service;

	@Before
	public void setUp() throws Exception
	{
		this.overDriveAPIResultsMother = new OverDriveAPIResultsMother();
		this.solrDocumentMother = new SolrInputDocumentMother();
	
		this.overDriveCollectionIteratorMock = Mockito.mock(IOverDriveCollectionIterator.class);
		this.daoEContentMock = Mockito.mock(IDBeContentRecordServices.class);
		this.overDriveAPIServices = Mockito.mock(IOverDriveAPIServices.class);
		
		this.service = new PopulateOverDriveAPIItems(this.overDriveCollectionIteratorMock,
													 this.daoEContentMock,
													 this.overDriveAPIServices);
	}
	
	/**
	 * method execute
	 * when DigitalCollectionIsEMpty
	 * should executesCorrectly
	 * @throws SQLException 
	 */
	@Test
	public void test_execute_DigitalCollectionIsEMpty_executesCorrectly() throws SQLException
	{
		Mockito.when(this.overDriveCollectionIteratorMock.hasNext())
			   .thenReturn(false);
		
		this.service.execute();
		Mockito.verify(this.overDriveCollectionIteratorMock, Mockito.times(1)).hasNext();
	}
	
	/**
	 * method execute
	 * when OverDriveItemDoesNotExistsInDB
	 * should executesCorrectly
	 * @throws SQLException 
	 */
	@Test
	public void test_execute_OverDriveItemDoesNotExistsInDB_executesCorrectly() throws Exception
	{
		String dbId1 = "123";
		String dbId2 = "321";
		
		JSONObject resultsDigitalCollection = this.overDriveAPIResultsMother.getDigitalCollection();
		String overDriveId1 = OverDriveAPIResultsMother.overDriveID_one;
		String overDriveId2 = OverDriveAPIResultsMother.overDriveID_two;

		JSONObject itemMetadata1 = this.overDriveAPIResultsMother.getItemMetadata(overDriveId1);
		JSONObject itemMetadata2 = this.overDriveAPIResultsMother.getItemMetadata(overDriveId2);
		
		SolrInputDocument document1 = this.solrDocumentMother.getEContentOPverDriveAPIItemSolrInputDocument("aDummIdBase64_1");
		SolrInputDocument document2 = this.solrDocumentMother.getEContentOPverDriveAPIItemSolrInputDocument("aDummIdBase64_2");
		
		ArrayList<SolrInputDocument> collection = new ArrayList<SolrInputDocument>();
		collection.add(document1);
		collection.add(document2);
		
		/**
		 * MOCKITOS WHEN
		 **/
		Mockito.when(this.overDriveCollectionIteratorMock.hasNext())
			   .thenReturn(true)
			   .thenReturn(false);

		Mockito.when(this.overDriveCollectionIteratorMock.next()).thenReturn(resultsDigitalCollection);

		Mockito.when(this.daoEContentMock.existOverDriveRecord(overDriveId1, "OverDrive")).thenReturn(false);
		Mockito.when(this.daoEContentMock.existOverDriveRecord(overDriveId2, "OverDrive")).thenReturn(false);
		
		Mockito.when(this.daoEContentMock.addOverDriveAPIItem(itemMetadata1)).thenReturn(true);
		Mockito.when(this.daoEContentMock.addOverDriveAPIItem(itemMetadata2)).thenReturn(true);
		
		Mockito.when(this.daoEContentMock.selectRecordIdByOverDriveIdBySource(overDriveId1, "OverDriveAPI")).thenReturn(dbId1);
		Mockito.when(this.daoEContentMock.selectRecordIdByOverDriveIdBySource(overDriveId2, "OverDriveAPI")).thenReturn(null).thenReturn(dbId2);
		
		Mockito.when(this.overDriveAPIServices.getItemMetadata(overDriveId1)).thenReturn(itemMetadata1);
		Mockito.when(this.overDriveAPIServices.getItemMetadata(overDriveId2)).thenReturn(itemMetadata2);
		
		Mockito.when(this.daoEContentMock.updateOverDriveAPIItem(dbId1, itemMetadata1)).thenReturn(true);
		
		/**
		 * Execute the method
		 */
		this.service.execute();
		
		/**
		 * MOCKITOS VERIFY
		 **/
		
		Mockito.verify(this.overDriveCollectionIteratorMock, Mockito.times(2)).hasNext();
		Mockito.verify(this.overDriveCollectionIteratorMock, Mockito.times(1)).next();
		
		Mockito.verify(this.daoEContentMock, Mockito.times(0)).addOverDriveAPIItem(itemMetadata1);
		Mockito.verify(this.daoEContentMock, Mockito.times(1)).addOverDriveAPIItem(itemMetadata2);
		
		Mockito.verify(this.daoEContentMock, Mockito.times(1)).selectRecordIdByOverDriveIdBySource(overDriveId1, "OverDriveAPI");
		Mockito.verify(this.daoEContentMock, Mockito.times(2)).selectRecordIdByOverDriveIdBySource(overDriveId2, "OverDriveAPI");
		
		Mockito.verify(this.overDriveAPIServices, Mockito.times(1)).getItemMetadata(overDriveId1);
		Mockito.verify(this.overDriveAPIServices, Mockito.times(1)).getItemMetadata(overDriveId2);
		
		Mockito.verify(this.daoEContentMock, Mockito.times(1)).updateOverDriveAPIItem(dbId1, itemMetadata1);
		Mockito.verify(this.daoEContentMock, Mockito.times(0)).updateOverDriveAPIItem(dbId2, itemMetadata2);	
	}
	
	/**
	 * method execute
	 * when DoNotExistsDB
	 * should executesCorrectly
	 * @throws SQLException 
	 */
	@Test
	public void test_execute_DoNotExistsDB_executesCorrectly() throws SQLException
	{
		String dbId1 = "123";
		String dbId2 = "321";
		
		JSONObject resultsDigitalCollection = this.overDriveAPIResultsMother.getDigitalCollection();
		String overDriveId1 = OverDriveAPIResultsMother.overDriveID_one;
		String overDriveId2 = OverDriveAPIResultsMother.overDriveID_two;
		
		/**
		 * MOCKITOS WHEN
		 **/
		Mockito.when(this.overDriveCollectionIteratorMock.hasNext())
			   .thenReturn(true)
			   .thenReturn(false);
		Mockito.when(this.overDriveCollectionIteratorMock.next()).thenReturn(resultsDigitalCollection);
		
		Mockito.when(this.daoEContentMock.existOverDriveRecord(overDriveId1, "OverDrive")).thenReturn(true);
		Mockito.when(this.daoEContentMock.existOverDriveRecord(overDriveId2, "OverDrive")).thenReturn(true);
		
		Mockito.when(this.daoEContentMock.selectRecordIdByOverDriveIdBySource(overDriveId1, "OverDriveAPI")).thenReturn(dbId1);
		Mockito.when(this.daoEContentMock.selectRecordIdByOverDriveIdBySource(overDriveId2, "OverDriveAPI")).thenReturn(null);
		
		Mockito.when(this.daoEContentMock.deleteRecordById(dbId1)).thenReturn(true);
		
		/**
		 * Execute the method
		 */
		this.service.execute();
		
		/**
		 * MOCKITOS VERIFY
		 **/
	
		Mockito.verify(this.overDriveCollectionIteratorMock, Mockito.times(2)).hasNext();
		Mockito.verify(this.overDriveCollectionIteratorMock, Mockito.times(1)).next();
		
		Mockito.verify(this.daoEContentMock, Mockito.times(1)).existOverDriveRecord(overDriveId1, "OverDrive");
		Mockito.verify(this.daoEContentMock, Mockito.times(1)).existOverDriveRecord(overDriveId1, "OverDrive");
		
		Mockito.verify(this.daoEContentMock, Mockito.times(1)).selectRecordIdByOverDriveIdBySource(overDriveId1, "OverDriveAPI");
		Mockito.verify(this.daoEContentMock, Mockito.times(1)).selectRecordIdByOverDriveIdBySource(overDriveId1, "OverDriveAPI");
		
		Mockito.verify(this.daoEContentMock, Mockito.times(1)).deleteRecordById(dbId1);
		Mockito.verify(this.daoEContentMock, Mockito.times(0)).deleteRecordById(dbId2);
		
	}
}