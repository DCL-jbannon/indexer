package org.API.OverDrive;

import org.apache.solr.common.SolrInputDocument;
import org.json.simple.JSONObject;

public interface IOverDriveAPIUtils 
{

	public SolrInputDocument getSolrInputDocumentFromDigitalCollectionItem(String eContentRecordId,JSONObject dcItem);
	public String getAuthorFromAPIItem(JSONObject item);
	public String getISBNFromAPIItem(JSONObject item);
	
}
