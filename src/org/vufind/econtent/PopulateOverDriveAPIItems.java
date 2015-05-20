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

    private class OverDriveTuple {
        public final String id;
        public final String source;
        public final String overDriveId;

        private boolean needsUpdate; // We need to force an update because the external_id is not set

        public OverDriveTuple(String id, String source, String overDriveId, boolean needsUpdate) {
            this.id = id;
            this.source = source;
            this.overDriveId = overDriveId;
            this.needsUpdate = needsUpdate;
        }

        public boolean needsUpdate() {
            return needsUpdate;
        }

        public String getRecordKey() {
            return overDriveId.toUpperCase();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OverDriveTuple that = (OverDriveTuple) o;

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

    private Map<String, OverDriveTuple> getOverDriveRecords() throws SQLException{
        HashMap<String, OverDriveTuple> records = new HashMap();
        PreparedStatement getOverDriveRecords_PS = this.econtentConnection.prepareStatement(
                "SELECT id, sourceURL, source, external_id " +
                "FROM econtent_record " +
                "WHERE " +
                    "(source = 'OverDrive' OR source = 'OverDriveAPI')");
        ResultSet rs = getOverDriveRecords_PS.executeQuery();

        while(rs.next()) {
            String id = rs.getString(1);
            String sourceURL = rs.getString(2);
            String source = rs.getString(3);
            String externalId = rs.getString(4);

            if(externalId != null && externalId.equals("")) {
                OverDriveTuple odTuple = new OverDriveTuple(id, source, externalId.toUpperCase(), false);
                checkAndStoreRecord(records, odTuple);
            } else {
                Matcher m = guidPattern.matcher(sourceURL);
                if (m.find()) {
                    String guid = m.group();
                    OverDriveTuple odTouple = new OverDriveTuple(id, source, guid.toUpperCase(), true);
                    checkAndStoreRecord(records, odTouple);
                }
            }
        }

        return records;
    }

    private void checkAndStoreRecord(HashMap<String, OverDriveTuple> records, OverDriveTuple newRecord) {
        if(records.containsKey(newRecord.getRecordKey())) {
            OverDriveTuple oldRecord = records.get(newRecord.getRecordKey());
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

        Map<String, OverDriveTuple> recordsMap = getOverDriveRecords();

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

                OverDriveTuple fromAPITouple = new OverDriveTuple(null, "OverDriveAPI", overDriveId.toUpperCase(), true);
                OverDriveTuple existingRecord = recordsMap.get(fromAPITouple.getRecordKey());
                if(existingRecord != null && existingRecord.source.equals("OverDrive")) {
                    //Don't do anything except for updating "last_touched". The Marc version takes precedence.
                    this.eContentRecordDAO.touch(existingRecord.id, true);
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
                    } else if(config.getBool(OverDriveConfigOptions.UPDATE_ALL) || existingRecord.needsUpdate()) {
                        try
                        {
                            JSONObject itemMetadata = this.overDriveApiServices.getItemMetadata(overDriveId);
                            itemMetadata.put("id", itemMetadata.get("id").toString().toUpperCase());
                            logger.debug("Update OverDrive API Item" + overDriveId);
                            this.eContentRecordDAO.updateOverDriveAPIItem(existingRecord.id, itemMetadata);
                        }
                        catch (Exception e)
                        {
                            logger.error("Error Updating  " + overDriveId + "API Item to the Database: ", e);
                        }
                    }  else {
                        this.eContentRecordDAO.touch(existingRecord.id, true);
                        logger.debug("Skip record[" + overDriveId+"] nothing to do.");
                    }
                }
			}
		}

        PreparedStatement selectUntouchedOverDrive = this.econtentConnection.prepareStatement(
                "SELECT er.id, er.external_id " +
                "FROM  econtent_record er " +
                "WHERE last_touched < DATE_SUB(NOW(), INTERVAL 4 HOUR) AND (source = 'OverDrive' OR source = 'OverDrive API') AND status = 'active'"
        );
        ResultSet rs = selectUntouchedOverDrive.executeQuery();
        while(rs.next()) {
            try {
                //Double check because sometimes the OverDrive API just fails
                int eId = rs.getInt(1);
                String overDriveId = rs.getString(2);
                boolean isOwned = false;
                if(overDriveId != null && !overDriveId.equals(""))      {
                    JSONObject itemMetadata = this.overDriveApiServices.getItemMetadata(overDriveId);
                    isOwned = Boolean.parseBoolean(itemMetadata.get("isOwnedByCollections").toString());
                }

                if(!isOwned)
                {
                    // Delete any that didn't get touched
                    PreparedStatement markUntouchedOverDriveAsDeleted = this.econtentConnection.prepareStatement(
                            "UPDATE econtent_record er " +
                                    "SET `status` = 'deleted' " +
                                    "WHERE last_touched < DATE_SUB(NOW(), INTERVAL 4 HOUR) AND (source = 'OverDrive' OR source = 'OverDrive API') AND er.Id = ?"
                    );
                    markUntouchedOverDriveAsDeleted.setInt(1, eId);
                    markUntouchedOverDriveAsDeleted.execute();
                } else {
                    int ii = 0;
                }

            } catch(Exception e){}

        }


		logger.info("Processed " + j + " items from OverDrive API");
		logger.info("Finished getting OverDrive API Collection");
	}
}