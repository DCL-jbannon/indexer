package org.solr;

import java.util.Collection;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;

public class SolrWrapper implements ISolrWrapper
{

	private String serverUrl;
	
	public SolrWrapper(String serverUrl)
	{
		this.serverUrl = serverUrl;
	}
	
	@SuppressWarnings("unchecked")
	public NamedList<Object> addDocument(SolrInputDocument document)
	{
		SolrServer server = new HttpSolrServer(this.serverUrl);
		try {
			UpdateResponse response = server.add(document);
			server.commit();
			return response.getResponseHeader();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public NamedList<Object> deleteDocumentById(String id)
	{
		SolrServer server = new HttpSolrServer(this.serverUrl);
		try
		{
			UpdateResponse response = server.deleteById(id);
			server.commit();
			return response.getResponseHeader();
		} catch (Exception e) 
		{
			e.printStackTrace();
			return null;
		}
	}

	public QueryResponse select(String queryString) 
	{
		SolrServer server = new HttpSolrServer(this.serverUrl);
		SolrQuery query = new SolrQuery();
		query.setQuery(queryString);
		try {
			QueryResponse queryResponse = server.query(query);
			return queryResponse;
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public UpdateResponse addCollectionDocuments(Collection<SolrInputDocument> collection)
	{
		SolrServer server = new HttpSolrServer(this.serverUrl);
		try
		{
			return server.add(collection);
		} catch (Exception e) 
		{
			e.printStackTrace();
			return null;
		}
	}
}
