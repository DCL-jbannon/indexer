package solr;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import mother.SolrInputDocumentMother;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.junit.Before;
import org.junit.Test;
import org.solr.SolrWrapper;

/**
 * http://wiki.apache.org/solr/Solrj
 * http://lucidworks.lucidimagination.com/display/solr/Using+SolrJ
 * @author jgimenez@dclibraries.org
 *
 */

public class SolrWrapperTests 
{
	SolrInputDocumentMother solrInputDocumentMother;
	SolrWrapper service;

	private static String urlServer = "http://vufind-dev:8080";
	
	@Before
	public void setUp() throws Exception 
	{
		this.solrInputDocumentMother = new SolrInputDocumentMother();
		this.service = new SolrWrapper(SolrWrapperTests.urlServer + "/solr/testEcontent");		
	}
	
	
	/**
	 * method addDocument
	 * when called
	 * should executesCorrectly
	 */
	@Test
	public void test_addDocument_called_executesCorrectly() 
	{
		int expected = 0;
		SolrInputDocument document = this.solrInputDocumentMother.getEContentOPverDriveAPIItemSolrInputDocument();
		
		NamedList<Object> actual = this.service.addDocument(document);
		org.junit.Assert.assertEquals(expected, actual.get("status"));
	}
	
	/**
	 * method select
	 * when selectById
	 * should returnOneDocument
	 */
	@Test
	public void test_select_selectById_returnOneDocument()
	{
		int expected = 1; 
		String query = "id:" + SolrInputDocumentMother.id;
		QueryResponse actual = this.service.select(query);
		
		assertEquals(expected, actual.getResults().getNumFound());
	}
	
	/**
	 * method deleteDocumentById
	 * when called
	 * should executesCorrectly
	 */
	@Test
	public void test_deleteDocumentById_called_executesCorrectly() 
	{
		int expected = 0;
		String id = SolrInputDocumentMother.id;
		NamedList<Object>  actual = this.service.deleteDocumentById(id);
		org.junit.Assert.assertEquals(expected, actual.get("status"));
	}
	
	/**
	 * method select
	 * when selectById
	 * should returnNoDocument
	 */
	@Test
	public void test_select_selectById_returnNoDocument()
	{
		int expected = 0; 
		String query = "id:" + SolrInputDocumentMother.id;
		QueryResponse actual = this.service.select(query);
		
		assertEquals(expected, actual.getResults().getNumFound());
	}
	
	/**
	 * method addColletionDocuments
	 * when called
	 * should executesCorrectly
	 */
	@Test
	public void test_addColletionDocuments_called_executesCorrectly()
	{
		int expected = 2;
		SolrInputDocument document1 = this.solrInputDocumentMother.getEContentOPverDriveAPIItemSolrInputDocument("aDummyId");
		SolrInputDocument document2 = this.solrInputDocumentMother.getEContentOPverDriveAPIItemSolrInputDocument("anotherDummyId");
		
		ArrayList<SolrInputDocument> collection = new ArrayList<SolrInputDocument>();
		collection.add(document1);
		collection.add(document2);
		
		this.service.addCollectionDocuments(collection);
		QueryResponse actual = this.service.select("*:*");
		int numFound = actual.getResponse().size();
		this.service.deleteDocumentById("aDummyId");
		this.service.deleteDocumentById("anotherDummyId");
		assertEquals(expected, numFound);
	}
}