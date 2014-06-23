package org.solr;

import org.apache.solr.client.solrj.response.QueryResponse;

public class SolrUtils implements ISolrUtils
{

	public int getSelectNumDocumentsFound(QueryResponse solrQueryResponse) 
	{
		return (int) solrQueryResponse.getResults().getNumFound();
	}

	
		
}
