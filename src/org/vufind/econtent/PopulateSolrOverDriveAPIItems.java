package org.vufind.econtent;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import org.API.OverDrive.IOverDriveAPIServices;
import org.API.OverDrive.IOverDriveAPIUtils;
import org.API.OverDrive.IOverDriveCollectionIterator;
import org.API.OverDrive.OverDriveAPIUtils;
import org.apache.solr.common.SolrInputDocument;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solr.ISolrUtils;
import org.solr.ISolrWrapper;
import org.solr.SolrUtils;

import org.vufind.econtent.db.DBeContentRecordServices;
import org.vufind.econtent.db.IDBeContentRecordServices;

public class PopulateSolrOverDriveAPIItems
{
    final static Logger logger = LoggerFactory.getLogger(PopulateOverDriveAPIItems.class);

	private IOverDriveCollectionIterator overDriveAPICollectionIterator;
	private IDBeContentRecordServices eContentRecordDAO;
	private IOverDriveAPIUtils overDriveAPIUtils;
	private ISolrWrapper solrWrapper;
	private IOverDriveAPIServices overDriveApiServices;
	@SuppressWarnings("unused")
	private ISolrUtils solrUtils;

	public PopulateSolrOverDriveAPIItems(IOverDriveCollectionIterator overDriveCollectionIterator,
										 Connection conn, 
										 ISolrWrapper solrWrapper,
										 IOverDriveAPIServices overDriveApiServices)
	{
		this(
				overDriveCollectionIterator,
				new DBeContentRecordServices(conn),
				new OverDriveAPIUtils(),
				solrWrapper,
				new SolrUtils(),
				overDriveApiServices
			);
	}

	public PopulateSolrOverDriveAPIItems(IOverDriveCollectionIterator overDriveAPICollectionIterator,
										 IDBeContentRecordServices eContentRecordDAO,
										 IOverDriveAPIUtils overDriveAPIUtilsMock,
										 ISolrWrapper solrWrapper,
										 ISolrUtils solrUtils,
										 IOverDriveAPIServices overDriveApiServices)
	{
		this.overDriveAPICollectionIterator = overDriveAPICollectionIterator;
		this.eContentRecordDAO = eContentRecordDAO;
		this.overDriveAPIUtils = overDriveAPIUtilsMock;
		this.solrWrapper = solrWrapper;
		this.solrUtils = solrUtils;
		this.overDriveApiServices = overDriveApiServices;
	}
	
	public void addNote(String note)
	{
		this.addNote(note, false);
	}
	
	public void addNote(String note, Boolean onlySystemOut)
	{
        if(onlySystemOut) {
            System.out.println(note);
        } else {
            logger.debug(note);
        }
	}

	public void execute() throws SQLException 
	{
		this.addNote("Started getting OverDrive API Collection");
		int j = 0;
		long totalItems = new Long("0");
		ArrayList<SolrInputDocument> collection = new ArrayList<SolrInputDocument>();
		JSONArray items = new JSONArray();
		
		while (this.overDriveAPICollectionIterator.hasNext())
		{
			
			try
			{
				JSONObject resultsDC = this.overDriveAPICollectionIterator.next();
				items = (JSONArray) resultsDC.get("products");
				totalItems = (Long)resultsDC.get("totalItems");
			}
			catch (Exception e)
			{
				this.addNote("\rError getting Items. OFFSSET:" +  j*300 );
			}


			for (int i = 0; i < items.size(); i++) 
			{
				j++;
				this.addNote("\rProcessing OverDrive API Item: " + j + "/" + totalItems, true);
				
				JSONObject item = (JSONObject) items.get(i);
				
				String overDriveId = (String) item.get("id");
				Boolean dbExists = this.eContentRecordDAO.existOverDriveRecord((String) item.get("id"),"OverDrive");
				if(dbExists)
				{
					String recordId = this.eContentRecordDAO.selectRecordIdByOverDriveIdBySource(overDriveId, "OverDriveAPI");
					if(recordId != null)
					{
						try
						{
							this.addNote("Deleting OverDrive API Item because it is now on Marc File. ID: " + overDriveId);
							String solrDocumentId = "econtentRecord" + recordId;
							this.solrWrapper.deleteDocumentById(solrDocumentId);
							this.eContentRecordDAO.deleteRecordById(recordId);
						}
						catch (Exception e)
						{
							this.addNote("Could not delete old OverDrive API Item: " + e.getMessage());
							e.printStackTrace();
						}
					}
				}
				else
				{
					String recordId = this.eContentRecordDAO.selectRecordIdByOverDriveIdBySource(overDriveId, "OverDriveAPI");
					JSONObject itemMetadata = this.overDriveApiServices.getItemMetadata(overDriveId);
					if(recordId == null)
					{
						try
						{
							this.addNote("New OverDrive API Item" + overDriveId);
							this.eContentRecordDAO.addOverDriveAPIItem(itemMetadata);
							recordId = this.eContentRecordDAO.selectRecordIdByOverDriveIdBySource(overDriveId, "OverDriveAPI");
						}
						catch (Exception e)
						{
							this.addNote("Could not delete old OverDrive API Item: " + e.getMessage());
							e.printStackTrace();
						}
					}
					else
					{
						try
						{
							this.eContentRecordDAO.updateOverDriveAPIItem(recordId, itemMetadata);	
						}
						catch (Exception e) 
						{
							this.addNote("Error Updating  " + overDriveId + "API Item to the Database: " + e.getMessage());
						}
					}
					
					try
					{
						SolrInputDocument document = this.overDriveAPIUtils.getSolrInputDocumentFromDigitalCollectionItem(recordId, itemMetadata);
						collection.add(document);
					}
					catch(Exception e)
					{
						this.addNote("Error creating SolrInputDocument for recordId: " + recordId + ". Exception Message: " + e.getMessage());
					}
			 	}
			}
		}
		
		this.addNote("Updating Solr with " + collection.size() + " documents");
		this.solrWrapper.addCollectionDocuments(collection);
		this.addNote("Added / Updated " + collection.size() + " new items from OverDrive API");
		this.addNote("Processed " + j + " items from OverDrive API");
		this.addNote("Finished getting OverDrive API Collection");
	}
}