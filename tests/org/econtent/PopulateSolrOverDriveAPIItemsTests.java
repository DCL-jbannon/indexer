package org.vufind.econtent;

import static org.junit.Assert.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import mother.OverDriveAPIResultsMother;
import mother.SolrInputDocumentMother;

import org.API.OverDrive.IOverDriveAPIServices;
import org.API.OverDrive.IOverDriveAPIUtils;
import org.API.OverDrive.IOverDriveCollectionIterator;
import org.apache.solr.common.SolrInputDocument;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.solr.ISolrUtils;
import org.solr.ISolrWrapper;
import org.vufind.econtent.db.IDBeContentRecordServices;
import dbTests.DBeContentRecordServicesTests;

public class PopulateSolrOverDriveAPIItemsTests {

	private static final String clientSecret = "aDummyClientSecret";
	private static final String clientKey = "aDummyClientSecret";
	private static final int libraryId = 0; //aDummyValue
	
	private IOverDriveCollectionIterator overDriveCollectionIteratorMock;
	private IDBeContentRecordServices daoEContentMock;
	private ISolrWrapper solrWrapperMock;
	private IOverDriveAPIUtils overDriveAPIUtilsMock;
	private Connection connMock;
	private ISolrUtils solrUtilsMock;
	private IOverDriveAPIServices overDriveAPIServices;
	
	private OverDriveAPIResultsMother overDriveAPIResultsMother;
	private SolrInputDocumentMother solrDocumentMother;
	private PopulateSolrOverDriveAPIItems service;

	@Before
	public void setUp() throws Exception
	{
		this.overDriveAPIResultsMother = new OverDriveAPIResultsMother();
		this.solrDocumentMother = new SolrInputDocumentMother();
	
		this.overDriveCollectionIteratorMock = Mockito.mock(IOverDriveCollectionIterator.class);
		this.daoEContentMock = Mockito.mock(IDBeContentRecordServices.class);
		this.solrWrapperMock = Mockito.mock(ISolrWrapper.class);
		this.overDriveAPIUtilsMock = Mockito.mock(IOverDriveAPIUtils.class);
		this.connMock = Mockito.mock(Connection.class);
		this.solrUtilsMock = Mockito.mock(ISolrUtils.class);
		this.overDriveAPIServices = Mockito.mock(IOverDriveAPIServices.class);
		
		this.service = new PopulateSolrOverDriveAPIItems(this.overDriveCollectionIteratorMock,
														 this.daoEContentMock,
														 this.overDriveAPIUtilsMock,
														 this.solrWrapperMock,
														 this.solrUtilsMock,
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
		
		JSONObject itemDigitalCollection1 = this.overDriveAPIResultsMother.getItemFromDigitalCollection(overDriveId1);
		JSONObject itemDigitalCollection2 = this.overDriveAPIResultsMother.getItemFromDigitalCollection(overDriveId2);
		
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
		
		Mockito.when(this.overDriveAPIUtilsMock.getSolrInputDocumentFromDigitalCollectionItem(dbId1, itemMetadata1)).thenReturn(document1);
		Mockito.when(this.overDriveAPIUtilsMock.getSolrInputDocumentFromDigitalCollectionItem(dbId2, itemMetadata2)).thenReturn(document2);
		
		Mockito.when(this.solrWrapperMock.addCollectionDocuments(collection)).thenReturn(null);
		
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
		
		Mockito.verify(this.overDriveAPIUtilsMock, Mockito.times(1)).getSolrInputDocumentFromDigitalCollectionItem(dbId1, itemMetadata1);
		Mockito.verify(this.overDriveAPIUtilsMock, Mockito.times(1)).getSolrInputDocumentFromDigitalCollectionItem(dbId2, itemMetadata2);
		
		Mockito.verify(this.daoEContentMock, Mockito.times(1)).selectRecordIdByOverDriveIdBySource(overDriveId1, "OverDriveAPI");
		Mockito.verify(this.daoEContentMock, Mockito.times(2)).selectRecordIdByOverDriveIdBySource(overDriveId2, "OverDriveAPI");
		
		Mockito.verify(this.overDriveAPIServices, Mockito.times(1)).getItemMetadata(overDriveId1);
		Mockito.verify(this.overDriveAPIServices, Mockito.times(1)).getItemMetadata(overDriveId2);
		
		Mockito.verify(this.daoEContentMock, Mockito.times(1)).updateOverDriveAPIItem(dbId1, itemMetadata1);
		Mockito.verify(this.daoEContentMock, Mockito.times(0)).updateOverDriveAPIItem(dbId2, itemMetadata2);
		
		Mockito.verify(this.solrWrapperMock, Mockito.times(1)).addCollectionDocuments(collection);
		
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
		
		JSONObject itemDigitalCollection1 = this.overDriveAPIResultsMother.getItemFromDigitalCollection(overDriveId1);
		JSONObject itemDigitalCollection2 = this.overDriveAPIResultsMother.getItemFromDigitalCollection(overDriveId2);

		SolrInputDocument document1 = this.solrDocumentMother.getEContentOPverDriveAPIItemSolrInputDocument("aDummIdBase64_1");
		SolrInputDocument document2 = this.solrDocumentMother.getEContentOPverDriveAPIItemSolrInputDocument("aDummIdBase64_2");
		
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
		
		Mockito.when(this.solrWrapperMock.deleteDocumentById("econtentRecord" + dbId1)).thenReturn(null);
		
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
		
		Mockito.verify(this.solrWrapperMock, Mockito.times(1)).deleteDocumentById("econtentRecord" + dbId1);
		Mockito.verify(this.solrWrapperMock, Mockito.times(0)).deleteDocumentById("econtentRecord" + dbId2);
		
		Mockito.verify(this.daoEContentMock, Mockito.times(1)).deleteRecordById(dbId1);
		Mockito.verify(this.daoEContentMock, Mockito.times(0)).deleteRecordById(dbId2);
		
	}
}