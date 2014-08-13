package org.vufind.econtent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.API.OverDrive.IOverDriveAPIServices;
import org.API.OverDrive.IOverDriveCollectionIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.OverDriveConfigOptions;
import org.vufind.econtent.db.DBeContentRecordServices;
import org.vufind.econtent.db.IDBeContentRecordServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PopulateOverDriveAPIItems
{
    final static Logger logger = LoggerFactory.getLogger(PopulateOverDriveAPIItems.class);

	private IOverDriveCollectionIterator overDriveAPICollectionIterator;
	private IDBeContentRecordServices eContentRecordDAO;
	private IOverDriveAPIServices overDriveApiServices;
    private Connection econtentConnection;
    private DynamicConfig config;
	
	public PopulateOverDriveAPIItems(IOverDriveCollectionIterator overDriveCollectionIterator,
										 Connection econtentConnection,
										 IOverDriveAPIServices overDriveApiServices,
                                         DynamicConfig config)
	{
		this(
                overDriveCollectionIterator,
                new DBeContentRecordServices(econtentConnection),
                overDriveApiServices,
                config
        );

        this.econtentConnection = econtentConnection;
	}

	private PopulateOverDriveAPIItems(IOverDriveCollectionIterator overDriveAPICollectionIterator,
										 IDBeContentRecordServices eContentRecordDAO,
										 IOverDriveAPIServices overDriveApiServices,
                                         DynamicConfig config)
	{
		this.overDriveAPICollectionIterator = overDriveAPICollectionIterator;
		this.eContentRecordDAO = eContentRecordDAO;
		this.overDriveApiServices = overDriveApiServices;
        this.config = config;
	}

    private class OverDriveTouple {
        public final String id;
        public final String source;
        public final String overDriveId;

        public OverDriveTouple(String id, String source, String overDriveId) {
            this.id = id;
            this.source = source;
            this.overDriveId = overDriveId;
        }

        public String getRecordKey() {
            return overDriveId.toUpperCase();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OverDriveTouple that = (OverDriveTouple) o;

            if (!id.equals(that.id)) return false;
            if (!overDriveId.equals(that.overDriveId)) return false;
            if (!source.equals(that.source)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = source.hashCode();
            result = 31 * result + overDriveId.hashCode();
            return result;
        }
    }

    private String guidRegex = "([0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12})";
    private Pattern guidPattern = Pattern.compile(guidRegex);

    private Map<String, OverDriveTouple> getOverDriveRecords() throws SQLException{
        HashMap<String, OverDriveTouple> records = new HashMap();
        PreparedStatement getOverDriveRecords_PS = this.econtentConnection.prepareStatement(
                "SELECT id, sourceURL, source " +
                "FROM econtent_record " +
                "WHERE " +
                    "(source = 'OverDrive' OR source = 'OverDriveAPI')");
        ResultSet rs = getOverDriveRecords_PS.executeQuery();
        //TODO - This is so stupid. We need to add a "SourceID" column to econtent_record
        while(rs.next()) {
            String id = rs.getString(1);
            String sourceURL = rs.getString(2);
            String source = rs.getString(3);
            Matcher m = guidPattern.matcher(sourceURL);
            if(m.find()) {
                String guid = m.group();
                OverDriveTouple odTouple = new OverDriveTouple(id, source, guid);
                checkAndStoreRecord(records, odTouple);
            }
        }

        return records;
    }

    private void checkAndStoreRecord(HashMap<String, OverDriveTouple> records, OverDriveTouple newRecord) {
        if(records.containsKey(newRecord.getRecordKey())) {
            OverDriveTouple oldRecord = records.get(newRecord.getRecordKey());
            if(newRecord.source.equals(oldRecord.source)) {
                logger.error("Duplicated OverDrive records ["+oldRecord.id+", "+ newRecord.id+"]");
                //Delete the newest duplicate record
                try {
                    this.eContentRecordDAO.deleteRecordById(newRecord.id);
                } catch (SQLException e) {
                    logger.error("Error deleting OverDrive record", e);
                    return;
                }
                return;
            }
            logger.info("Deleting OverDrive API Item because it is now on Marc File. ID: " + newRecord.overDriveId);
            if(newRecord.source.equals("OverDrive")) {
                //Keep new record
                try {
                    this.eContentRecordDAO.deleteRecordById(oldRecord.id);
                } catch (SQLException e) {
                    logger.error("Error deleting OverDrive record", e);
                    return;
                }
                records.replace(newRecord.getRecordKey(), oldRecord, newRecord);
            } else {
                //Keep old record
                try {
                    this.eContentRecordDAO.deleteRecordById(newRecord.id);
                } catch (SQLException e) {
                    logger.error("Error deleting OverDrive record", e);
                    return;
                }
            }

        } else {
            records.put(newRecord.getRecordKey(), newRecord);
        }
    }

	public void execute() throws SQLException 
	{
		logger.info("Started getting OverDrive API Collection");
		int j = 0;
		long totalItems = new Long("0");
		JSONArray items = new JSONArray();

        PreparedStatement deleteDuplicatedOverDriveRecords_PS = this.econtentConnection.prepareStatement(
                "DELETE e1 FROM econtent_record e1, econtent_record e2 " +
                "WHERE " +
                    "e1.source = 'OverDriveAPI' " +
                    "AND e2.source = 'OverDrive' " +
                    "AND e1.sourceUrl = e2.sourceUrl"
        );
        deleteDuplicatedOverDriveRecords_PS.executeUpdate();

        Map<String, OverDriveTouple> recordsMap = getOverDriveRecords();

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
				logger.error("Error getting Items. OFFSET:" +  j*300, e );
			}

			for (int i = 0; i < items.size(); i++) 
			{
				j++;
				logger.debug("Processing OverDrive API Item: " + j + "/" + totalItems, true);
                if(j%1000 == 0) {
                    logger.info("Processing OverDrive API Item: " + j + "/" + totalItems, true);
                }
				
				JSONObject item = (JSONObject) items.get(i);
				String overDriveId = (String) item.get("id");

                OverDriveTouple fromAPITouple = new OverDriveTouple(null, "OverDriveAPI", overDriveId);
                OverDriveTouple existingRecord = recordsMap.get(fromAPITouple.getRecordKey());
                if(existingRecord != null && existingRecord.source.equals("OverDrive")) {
                    //Don't do anything. The Marc version takes precedence.
                } else {

                    if(existingRecord == null) {
                        try
                        {
                            JSONObject itemMetadata = this.overDriveApiServices.getItemMetadata(overDriveId);
                            logger.debug("New OverDrive API Item" + overDriveId);
                            this.eContentRecordDAO.addOverDriveAPIItem(itemMetadata);
                        }
                        catch (Exception e)
                        {
                            logger.error("Could not add OverDrive API Item: ", e);
                            e.printStackTrace();
                        }
                    } else if(config.getBool(OverDriveConfigOptions.UPDATE_ALL)) {
                        try
                        {
                            JSONObject itemMetadata = this.overDriveApiServices.getItemMetadata(overDriveId);
                            logger.debug("Update OverDrive API Item" + overDriveId);
                            this.eContentRecordDAO.updateOverDriveAPIItem(existingRecord.id, itemMetadata);
                        }
                        catch (Exception e)
                        {
                            logger.error("Error Updating  " + overDriveId + "API Item to the Database: ", e);
                        }
                    }  else {
                        logger.debug("Skip record[" + overDriveId+"] nothing to do.");
                    }
                }
			}
		}
		logger.info("Processed " + j + " items from OverDrive API");
		logger.info("Finished getting OverDrive API Collection");
	}
}