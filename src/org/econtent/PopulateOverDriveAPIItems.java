package org.econtent;

import java.sql.Connection;
import java.sql.SQLException;
import org.API.OverDrive.IOverDriveAPIServices;
import org.API.OverDrive.IOverDriveCollectionIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.vufind.ProcessorResults;
import db.DBeContentRecordServices;
import db.IDBeContentRecordServices;

public class PopulateOverDriveAPIItems
{

	private IOverDriveCollectionIterator overDriveAPICollectionIterator;
	private IDBeContentRecordServices eContentRecordDAO;
	private IOverDriveAPIServices overDriveApiServices;
	ProcessorResults processorResults = null;
	
	public PopulateOverDriveAPIItems(IOverDriveCollectionIterator overDriveCollectionIterator,
			 Connection conn,
			 IOverDriveAPIServices overDriveApiServices,
			 ProcessorResults processorResults)
	{
		this(
				overDriveCollectionIterator,
				new DBeContentRecordServices(conn),
				overDriveApiServices
			);
		this.processorResults = processorResults;
	}
	
	public PopulateOverDriveAPIItems(IOverDriveCollectionIterator overDriveCollectionIterator,
										 Connection conn, 
										 IOverDriveAPIServices overDriveApiServices)
	{
		this(
				overDriveCollectionIterator,
				new DBeContentRecordServices(conn),
				overDriveApiServices
			);
	}

	public PopulateOverDriveAPIItems(IOverDriveCollectionIterator overDriveAPICollectionIterator,
										 IDBeContentRecordServices eContentRecordDAO,
										 IOverDriveAPIServices overDriveApiServices)
	{
		this.overDriveAPICollectionIterator = overDriveAPICollectionIterator;
		this.eContentRecordDAO = eContentRecordDAO;
		this.overDriveApiServices = overDriveApiServices;
	}
	
	public void addNote(String note)
	{
		this.addNote(note, false);
	}
	
	public void addNote(String note, Boolean onlySystemOut)
	{
		if ( (this.processorResults!=null) && !onlySystemOut)
		{
			this.processorResults.addNote(note);
		}
		if(onlySystemOut && this.processorResults!= null)
		{
			System.out.println(note);
		}
	}

	public void execute() throws SQLException 
	{
		this.addNote("Started getting OverDrive API Collection");
		int j = 0;
		long totalItems = new Long("0");
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
			 	}
			}
		}
		this.addNote("Processed " + j + " items from OverDrive API");
		this.addNote("Finished getting OverDrive API Collection");
	}
}