package solr;

import static org.junit.Assert.*;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.verification.Times;
import org.solr.ISolrWrapper;
import org.solr.SolrUtils;

public class SolrUtilsTests
{

	private SolrUtils service;
	private ISolrWrapper solrWrapperMock;
	private QueryResponse solrQueryResponseMock;
	private SolrDocumentList solrDocumentListMock;
	
	@Before
	public void setUp() throws Exception 
	{
		this.solrQueryResponseMock = Mockito.mock(QueryResponse.class);
		this.solrDocumentListMock = Mockito.mock(SolrDocumentList.class);
		this.service = new SolrUtils();
	}
	
	/**
	 * method getSelectNumDocumentsFound
	 * when called
	 * should returnNumberOfDocumentsFound
	 */
	@Test
	public void test_getNumDocumentsFound_called_returnNumberOfDocumentsFound() 
	{
		long expected = 123; //a Dummy Value 
		Mockito.when(this.solrQueryResponseMock.getResults()).thenReturn(this.solrDocumentListMock);
		Mockito.when(this.solrDocumentListMock.getNumFound()).thenReturn(expected);
		
		int actual = this.service.getSelectNumDocumentsFound(this.solrQueryResponseMock);
		
		Mockito.verify(this.solrQueryResponseMock, org.mockito.Mockito.times(1)).getResults();
		Mockito.verify(this.solrDocumentListMock, org.mockito.Mockito.times(1)).getNumFound();
		
		assertEquals(expected, actual);
	}

}
