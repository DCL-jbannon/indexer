package org.API.OverDrive;

import org.apache.solr.common.SolrInputDocument;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.solr.SolrConstantsEContentFields;

public class OverDriveAPIUtils implements IOverDriveAPIUtils
{

	public SolrInputDocument getSolrInputDocumentFromDigitalCollectionItem(String eContentRecordId, JSONObject item) 
	{
		String solrId = "econtentRecord" + eContentRecordId;
		String author = this.getAuthorFromAPIItem(item);
		String title = (String)item.get("title");
		String availableAt = "OverDrive";
		
		
		SolrInputDocument document = new SolrInputDocument();
		document.addField(SolrConstantsEContentFields.id, solrId);
		document.addField(SolrConstantsEContentFields.recordType, "econtentRecord");
		document.addField(SolrConstantsEContentFields.title, title);
		document.addField(SolrConstantsEContentFields.titleSort, title);
		document.addField(SolrConstantsEContentFields.available, availableAt);
		document.addField(SolrConstantsEContentFields.author, author);
		document.addField(SolrConstantsEContentFields.keywords, title + "\n" + author + "\n" + availableAt);
		document.addField(SolrConstantsEContentFields.language, "English");
		document.addField(SolrConstantsEContentFields.format_category, "EMedia");
		document.addField(SolrConstantsEContentFields.bibsupresion, "notsuppressed");
		
		return document;
	}

	public String getAuthorFromAPIItem(JSONObject item)
	{
		try
		{
			JSONArray pc = (JSONArray) item.get("creators");
			JSONObject authors = (JSONObject) pc.get(0);
			return (String) authors.get("name");
		}
		catch (NullPointerException e)
		{
			return "";
		}
		
	}

	public String getISBNFromAPIItem(JSONObject item) {
		try
		{
			String isbn = null;
			JSONArray formats = (JSONArray) item.get("formats");

			for (int i = 0; i < formats.size(); i++) 
			{
				JSONObject format = (JSONObject) formats.get(i);
				JSONArray identifiers = (JSONArray) format.get("identifiers");
				
				for (int j = 0; j < identifiers.size(); j++) 
				{
					JSONObject identifier = (JSONObject) identifiers.get(j);
					String type = (String)identifier.get("type");
					
					if (type.equals("ISBN"))
					{
						isbn = (String)identifier.get("value");
					}
				}
			}
			return isbn;
		}
		catch (NullPointerException e)
		{
			//System.out.println(e.getMessage());
			return null;
		}
	}
}