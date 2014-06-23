package org.solr;

import org.apache.solr.client.solrj.response.QueryResponse;

public interface ISolrUtils 
{
	public int getSelectNumDocumentsFound(QueryResponse solrQueryResponse);
}
