package org.vufind.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solrmarc.tools.Utils;
import org.vufind.*;
import org.vufind.config.ConfigFiller;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.MarcConfigOptions;
import org.vufind.config.sections.OverDriveConfigOptions;
import org.vufind.config.sections.UpdateResourcesConfigOptions;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class UpdateResourceInformation implements IMarcRecordProcessor, IEContentProcessor {
    public final static Logger logger = LoggerFactory.getLogger(UpdateResourceInformation.class);

    private DynamicConfig config = null;
	
	private PreparedStatement resourceUpdateStmt = null;
	private PreparedStatement resourceUpdateStmtNoMarc = null;
	private PreparedStatement resourceInsertStmt = null;
	private PreparedStatement deleteResourceStmt = null;
	
	//Code related to subjects of resources
	private HashMap<String, Long> existingSubjects;
	private PreparedStatement getExistingSubjectsStmt = null;
	private PreparedStatement insertSubjectStmt = null;
	private PreparedStatement clearResourceSubjectsStmt = null;
	private PreparedStatement linkResourceToSubjectStmt = null;
	
	//Setup prepared statements that we will use
	private PreparedStatement existingResourceStmt;
	private PreparedStatement addResourceStmt;
	private PreparedStatement updateResourceStmt;
	
	
	//Code related to call numbers
	private HashMap<String, Long> locations;
	private PreparedStatement getLocationsStmt = null;
	private PreparedStatement clearResourceCallnumbersStmt = null;
	private PreparedStatement addCallnumberToResourceStmt = null;
	
	//A list of existing resources so we can mark records as deleted if they no longer exist
	private HashMap<String, BasicResourceInfo> existingResources = new HashMap<String, BasicResourceInfo>();

	
	public boolean init(DynamicConfig config) {
		// Load configuration
        this.config = config;
        ConfigFiller.fill(config, UpdateResourcesConfigOptions.values(), new File(config.getString(BasicConfigOptions.CONFIG_FOLDER)));
        ConfigFiller.fill(config, MarcConfigOptions.values(), new File(config.getString(BasicConfigOptions.CONFIG_FOLDER)));

        Connection vufindConn = ConnectionProvider.getConnection(config, ConnectionProvider.PrintOrEContent.PRINT);

        try {
			// Setup prepared statements
			resourceUpdateStmt = vufindConn.prepareStatement("UPDATE resource SET title = ?, title_sort = ?, author = ?, isbn = ?, upc = ?, format = ?, format_category = ?, marc_checksum=?, marc = ?, shortId = ?, date_updated=?, deleted=0 WHERE id = ?");
			resourceUpdateStmtNoMarc = vufindConn.prepareStatement("UPDATE resource SET title = ?, title_sort = ?, author = ?, isbn = ?, upc = ?, format = ?, format_category = ?, marc_checksum=?, shortId = ?, date_updated=?, deleted=0 WHERE id = ?");
			resourceInsertStmt = vufindConn.prepareStatement("REPLACE resource (title, title_sort, author, isbn, upc, format, format_category, record_id, shortId, marc_checksum, marc, source, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)", PreparedStatement.RETURN_GENERATED_KEYS);

            deleteResourceStmt = vufindConn.prepareStatement("UPDATE resource SET deleted = 1 WHERE record_id = ?", PreparedStatement.RETURN_GENERATED_KEYS);

			getExistingSubjectsStmt = vufindConn.prepareStatement("SELECT * FROM subject");
			ResultSet existingSubjectsRS = getExistingSubjectsStmt.executeQuery();
			existingSubjects = new HashMap<String, Long>();
			while (existingSubjectsRS.next()){
				existingSubjects.put(existingSubjectsRS.getString("subject"),existingSubjectsRS.getLong("id") );
			}
			existingSubjectsRS.close();
			insertSubjectStmt = vufindConn.prepareStatement("INSERT INTO subject (subject) VALUES (?)", PreparedStatement.RETURN_GENERATED_KEYS);
			clearResourceSubjectsStmt = vufindConn.prepareStatement("DELETE FROM resource_subject WHERE resourceId = ?");
			linkResourceToSubjectStmt = vufindConn.prepareStatement("INSERT INTO resource_subject (subjectId, resourceId) VALUES (?, ?)");
			
			getLocationsStmt = vufindConn.prepareStatement("SELECT locationId, code FROM location");
			ResultSet locationsRS = getLocationsStmt.executeQuery();
			locations = new HashMap<String, Long>();
			while (locationsRS.next()){
				locations.put(locationsRS.getString("code").toLowerCase(),locationsRS.getLong("locationId") );
			}
			locationsRS.close();
			
			clearResourceCallnumbersStmt = vufindConn.prepareStatement("DELETE FROM resource_callnumber WHERE resourceId = ?");
			addCallnumberToResourceStmt = vufindConn.prepareStatement("INSERT INTO resource_callnumber (resourceId, locationId, callnumber) VALUES (?, ?, ?)");
			
			//Setup prepared statements that we will use
            existingResourceStmt = vufindConn.prepareStatement("SELECT id, date_updated FROM resource WHERE record_id = ? AND source = 'eContent'");
			addResourceStmt = vufindConn.prepareStatement("INSERT INTO resource (record_id, title, source, author, title_sort, isbn, upc, format, format_category, marc_checksum, date_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			updateResourceStmt = vufindConn.prepareStatement("UPDATE resource SET record_id = ?, title = ?, source = ?, author = ?, title_sort = ?, isbn = ?, upc = ?, format = ?, format_category = ?, marc_checksum = ?, date_updated = ? WHERE id = ?");

            DuplicateRecordCleaner.Clean(vufindConn);
			
			//Get a list of resources that have already been installed. 
			logger.debug("Loading existing resources");
            PreparedStatement existingResourceStmt = vufindConn.prepareStatement("SELECT record_id, id, marc_checksum, deleted FROM resource WHERE source = 'VuFind'", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			ResultSet existingResourceRS = existingResourceStmt.executeQuery();
			while (existingResourceRS.next()){
				String ilsId = existingResourceRS.getString("record_id");
                //BasicResourceInfo resourceInfo = new BasicResourceInfo(ilsId, existingResourceRS.getLong("id"), existingResourceRS.getLong("marc_checksum"), existingResourceRS.getBoolean("deleted"));
                BasicResourceInfo resourceInfo = new BasicResourceInfo(ilsId, existingResourceRS.getLong(2), existingResourceRS.getLong(3), existingResourceRS.getBoolean(4));
				existingResources.put(ilsId, resourceInfo);
			}
			existingResourceRS.close();
			logger.debug("Finished loading existing resources");
			
		} catch (SQLException ex) {
			// handle any errors
			logger.error("Unable to setup prepared statements", ex);
			return false;
		}
		return true;
		
	}


	public boolean processMarcRecord(MarcRecordDetails recordInfo) {
        logger.debug("Updating database record: "+recordInfo.getId());
		Long resourceId = -1L;
		
		boolean updateSubjectAndCallNumber = true;

        if(recordInfo.getId().equals("1079986"))            {
            int ii = 0;
            ii++;
        }


		if (recordInfo.isEContent()){
			logger.debug("Skipping updating resource for record because it is eContent");
			BasicResourceInfo basicResourceInfo = existingResources.get(recordInfo.getId());
			if (basicResourceInfo != null && basicResourceInfo.getResourceId() != null ){
                if(basicResourceInfo.getResourceId().equals("1118588"))            {
                    int ii = 0;
                    ii++;
                }
				existingResources.remove(recordInfo.getId());
			}
			return true;
		}

        boolean updateUnchangedResources =
                config.getBool(BasicConfigOptions.DO_FULL_REINDEX)
                && config.getBool(UpdateResourcesConfigOptions.UPDATE_UNCHANGED_MARC);


		try {

            BasicResourceInfo basicResourceInfo = existingResources.get(recordInfo.getId());

            if (recordInfo.getRecordStatus() == MarcProcessor.RecordStatus.RECORD_UNCHANGED && !updateUnchangedResources){
                boolean updateResource = false;

                if (basicResourceInfo != null && basicResourceInfo.getResourceId() != null ){
                    if (basicResourceInfo.getMarcChecksum() == -1){
                        logger.debug("Forcing resource update because checksum is -1");
                        updateResource = true;
                    }else{
                        existingResources.remove(recordInfo.getId());
                    }
                }
                if (!updateResource){
                    logger.debug("Skipping record because it hasn't changed");
                    return true;
                }
            }

			if (basicResourceInfo != null && basicResourceInfo.getResourceId() != null ) {
                logger.debug("Updating the existing resource");
                resourceId = basicResourceInfo.getResourceId();
                //Remove the resource from the existingResourcesList so
                //We can determine which resources no longer exist
                existingResources.remove(recordInfo.getId());
            }

            if(recordInfo.getRecordStatus() != MarcProcessor.RecordStatus.RECORD_NEW
                    && (recordInfo == null || basicResourceInfo == null)
                    ) {
                int ii = 0;//This shouldn't happen
            }

            if(recordInfo.getRecordStatus() == MarcProcessor.RecordStatus.RECORD_NEW)     {
                logger.debug("This is a brand new record, adding to resources table");
                String author = recordInfo.getAuthor();
                // Update resource SQL
                resourceInsertStmt.setString(1, Util.trimTo(200, recordInfo.getTitle()));
                resourceInsertStmt.setString(2, Util.trimTo(200, recordInfo.getSortTitle()));
                resourceInsertStmt.setString(3, Util.trimTo(255, author));
                resourceInsertStmt.setString(4, Util.trimTo(13, recordInfo.getIsbn()));
                resourceInsertStmt.setString(5, Util.trimTo(13, recordInfo.getFirstFieldValueInSet("upc")));
                resourceInsertStmt.setString(6, Util.trimTo(50, recordInfo.getFirstFieldValueInSet("format")));
                resourceInsertStmt.setString(7, Util.trimTo(50, recordInfo.getFirstFieldValueInSet("format_category")));
                resourceInsertStmt.setString(8, recordInfo.getId());
                resourceInsertStmt.setString(9, recordInfo.getShortId());
                resourceInsertStmt.setLong(10, recordInfo.getChecksum());
                resourceInsertStmt.setString(11, recordInfo.getRawRecord());
                resourceInsertStmt.setString(12, "VuFind");

                int rowsUpdated = resourceInsertStmt.executeUpdate();
                if (rowsUpdated == 0) {
                    logger.debug("Unable to insert record " + recordInfo.getId());
                } else {
                    //Get the resourceId
                    ResultSet insertedResourceIds = resourceInsertStmt.getGeneratedKeys();
                    if (insertedResourceIds.next()){
                        resourceId = insertedResourceIds.getLong(1);
                    }
                }

            } else if (updateUnchangedResources
                    || (basicResourceInfo == null || recordInfo == null)
                    || basicResourceInfo.getMarcChecksum() == null
                    || (basicResourceInfo.getMarcChecksum() != recordInfo.getChecksum())) {
                // Update the existing record
                String title = recordInfo.getTitle();
                String author = recordInfo.getAuthor();

                // Update resource SQL
                String marcString = recordInfo.getRawRecord();
                int rowsUpdated = 0;
                if (marcString == null || marcString.length() == 0) {
                    logger.error("MarcRecordDetails.getRawRecord() returned NULL or empty for record " + recordInfo.getId());
                    resourceUpdateStmtNoMarc.setString(1, Util.trimTo(200, title));
                    resourceUpdateStmtNoMarc.setString(2, Util.trimTo(200, recordInfo.getSortTitle()));
                    resourceUpdateStmtNoMarc.setString(3, Util.trimTo(255, author));
                    resourceUpdateStmtNoMarc.setString(4, Util.trimTo(13, recordInfo.getIsbn()));
                    resourceUpdateStmtNoMarc.setString(5, Util.trimTo(13, recordInfo.getFirstFieldValueInSet("upc")));
                    resourceUpdateStmtNoMarc.setString(6, Util.trimTo(50, recordInfo.getFirstFieldValueInSet("format")));
                    resourceUpdateStmtNoMarc.setString(7, Util.trimTo(50, recordInfo.getFirstFieldValueInSet("format_category")));
                    resourceUpdateStmtNoMarc.setLong(8, recordInfo.getChecksum());
                    resourceUpdateStmtNoMarc.setString(9, recordInfo.getShortId());
                    resourceUpdateStmtNoMarc.setLong(10, new Date().getTime() / 1000);
                    resourceUpdateStmtNoMarc.setLong(11, resourceId);
                    rowsUpdated = resourceUpdateStmtNoMarc.executeUpdate();
                } else {
                    resourceUpdateStmt.setString(1, Util.trimTo(200, title));
                    resourceUpdateStmt.setString(2, Util.trimTo(200, recordInfo.getSortTitle()));
                    resourceUpdateStmt.setString(3, Util.trimTo(255, author));
                    resourceUpdateStmt.setString(4, Util.trimTo(13, recordInfo.getIsbn()));
                    resourceUpdateStmt.setString(5, Util.trimTo(13, recordInfo.getFirstFieldValueInSet("upc")));
                    resourceUpdateStmt.setString(6, Util.trimTo(50, recordInfo.getFirstFieldValueInSet("format")));
                    resourceUpdateStmt.setString(7, Util.trimTo(50, recordInfo.getFirstFieldValueInSet("format_category")));
                    resourceUpdateStmt.setLong(8, recordInfo.getChecksum());
                    resourceUpdateStmt.setString(9, marcString);
                    resourceUpdateStmt.setString(10, recordInfo.getShortId());
                    resourceUpdateStmt.setLong(11, new Date().getTime() / 1000);
                    resourceUpdateStmt.setLong(12, resourceId);
                    rowsUpdated = resourceUpdateStmt.executeUpdate();
                }
                if (rowsUpdated == 0) {
                    logger.debug("Unable to update resource for record " + recordInfo.getId() + " " + resourceId);
                }else{
                }
            }else{
                updateSubjectAndCallNumber = false;
            }
				


			
			if (resourceId != -1 && updateSubjectAndCallNumber){
				logger.debug("Updating subject and call number");
				clearResourceSubjectsStmt.setLong(1, resourceId);
				clearResourceSubjectsStmt.executeUpdate();
				clearResourceCallnumbersStmt.setLong(1, resourceId);
				clearResourceCallnumbersStmt.executeUpdate();
				//Add subjects 
				Object subjects = recordInfo.getMappedField("topic_facet");
				Set<String> subjectsToProcess = new HashSet<String>();
				if (subjects != null){
					if (subjects instanceof String){
						subjectsToProcess.add((String)subjects); 
					}else{
						subjectsToProcess.addAll((Set<String>)subjects);
					}
					Iterator<String> subjectIterator = subjectsToProcess.iterator();
					while (subjectIterator.hasNext()){
						String curSubject = subjectIterator.next();
						//Trim trailing punctuation from the subject
						curSubject = Utils.cleanData(curSubject);
						//Check to see if the subject exists already
						Long subjectId = existingSubjects.get(curSubject);
						if (subjectId == null){
							//Insert the subject into the subject table
							insertSubjectStmt.setString(1, Util.trimTo(512, curSubject));
							insertSubjectStmt.executeUpdate();
							ResultSet generatedKeys = insertSubjectStmt.getGeneratedKeys();
							if (generatedKeys.next()){
								subjectId = generatedKeys.getLong(1);
								existingSubjects.put(curSubject, subjectId);
							}
						}
						if (subjectId != null){
							linkResourceToSubjectStmt.setLong(1, subjectId);
							linkResourceToSubjectStmt.setLong(2, resourceId);
							linkResourceToSubjectStmt.executeUpdate();
						}
					}
				}

                String callNumberSubfield = config.getString(MarcConfigOptions.CALL_NUMBER_SUBFIELD);
                String locationSubfield = config.getString(MarcConfigOptions.LOCATION_SUBFIELD);
                String itemTag = config.getString(MarcConfigOptions.ITEM_TAG);

                if (callNumberSubfield!= null && callNumberSubfield.length() > 0 && locationSubfield != null && locationSubfield.length() > 0){
					//Add call numbers based on the location
					Set<LocalCallNumber> localCallNumbers = recordInfo.getLocalCallNumbers(itemTag, callNumberSubfield, locationSubfield);
					for (LocalCallNumber curCallNumber : localCallNumbers){
						Long locationId = locations.get(curCallNumber.getLocationCode());
						if (locationId != null){
							addCallnumberToResourceStmt.setLong(1, resourceId);
							addCallnumberToResourceStmt.setLong(2, locationId);
							addCallnumberToResourceStmt.setString(3, curCallNumber.getCallNumber());
							addCallnumberToResourceStmt.executeUpdate();
						}
					}
				}
			}
		} catch (SQLException ex) {
			// handle any errors
			logger.error("Error updating resource for record " + recordInfo.getId() + " " + recordInfo.getTitle(), ex);
		}finally{

		}
		logger.debug("Finished updating resource");
		return true;
	}

    @Override
	public void finish() {
        try {


            if (config.getBool(MarcConfigOptions.REMOVE_RECORDS_NOT_IN_MARC_EXPORT)) {          //....

                //Mark any resources that no longer exist as deleted.
                int numResourcesToDelete = 0;
                for (BasicResourceInfo resourceInfo : existingResources.values()) {
                    if (resourceInfo.getDeleted() == false) {
                        numResourcesToDelete++;
                    }
                }

                logger.info("Deleting resources that no longer from resources table, there are " + numResourcesToDelete + " of " + existingResources.size() + " resources to be deleted.");

                for (BasicResourceInfo resourceInfo : existingResources.values()) {
                    if (resourceInfo.getDeleted() == false) {
                        try {
                            deleteResourceStmt.setString(1, resourceInfo.getIlsId());
                            deleteResourceStmt.executeUpdate();

                        } catch (SQLException e) {
                            logger.error("Unable to delete resources", e);
                            break;
                        }
                    }
                }

            }
        }catch(Exception e) {
            e.printStackTrace();
        }
	}

	@Override
	public boolean processEContentRecord(ResultSet allEContent) {
		try {
			String econtentId = allEContent.getString("id");
			String ilsId = allEContent.getString("ilsId");
            if("1079986".equals(econtentId) ||"1079986".equals(ilsId)) {
                int ii = 0;
                ii++;
            }
			//Load title information so we have access regardless of 
			String title = allEContent.getString("title");
			String subTitle = allEContent.getString("subTitle");
			if (subTitle.length() > 0){
				title += ": " + subTitle;
			}
			String sortTitle = title.toLowerCase().replaceAll("^(the|an|a|el|la)\\s", "");
			String isbn = allEContent.getString("isbn");
			if (isbn != null){
				if (isbn.indexOf(' ') > 0){
					isbn = isbn.substring(0, isbn.indexOf(' '));
				}
				if (isbn.indexOf("\r") > 0){
					isbn = isbn.substring(0,isbn.indexOf("\r"));
				}
				if (isbn.indexOf("\n") > 0){
					isbn = isbn.substring(0,isbn.indexOf("\n"));
				}
			}
			String upc = allEContent.getString("upc");
			if (upc != null){
				if (upc.indexOf(' ') > 0){
					upc = upc.substring(0, upc.indexOf(' '));
				}
				if (upc.indexOf("\r") > 0){
					upc = upc.substring(0,upc.indexOf("\r"));
				}
				if (upc.indexOf("\n") > 0){
					upc = upc.substring(0,upc.indexOf("\n"));
				}
			}
			//System.out.println("UPC: " + upc);
			
			//Check to see if we have an existing resource
			existingResourceStmt.setString(1, econtentId);
			ResultSet existingResource = existingResourceStmt.executeQuery();
			if (existingResource.next()){
				//Check the date resource was updated and update if it was updated before the record was changed last
				boolean updateResource = false;
				long resourceUpdateTime = existingResource.getLong("date_updated");
				long econtentUpdateTime = allEContent.getLong("date_updated");
				if (econtentUpdateTime > resourceUpdateTime){
					updateResource = true;
				}
				if (updateResource){
					logger.debug("Updating Resource for eContentRecord " + econtentId);
					updateResourceStmt.setString(1, econtentId);
					updateResourceStmt.setString(2, Util.trimTo(255, title));
					updateResourceStmt.setString(3, "eContent");
					updateResourceStmt.setString(4, Util.trimTo(255, allEContent.getString("author")));
					updateResourceStmt.setString(5, Util.trimTo(255, sortTitle));
					updateResourceStmt.setString(6, Util.trimTo(13, isbn));
					updateResourceStmt.setString(7, Util.trimTo(13, upc));
					updateResourceStmt.setString(8, "");
					updateResourceStmt.setString(9, "emedia");
					updateResourceStmt.setLong(10, 0);
					updateResourceStmt.setLong(11, new Date().getTime() / 1000);
					updateResourceStmt.setLong(12, existingResource.getLong("id"));
					
					int numUpdated = updateResourceStmt.executeUpdate();
					if (numUpdated != 1){
						logger.error("Resource not updated for econtent record " + econtentId);
					}else{
					}
				}else{
					logger.debug("Not updating resource for eContentRecord " + econtentId + ", it is already up to date");
				}
			}else{
				//Insert a new resource
				logger.debug("Adding resource for eContentRecord " + econtentId);
				addResourceStmt.setString(1, econtentId);
				addResourceStmt.setString(2, Util.trimTo(255, title));
				addResourceStmt.setString(3, "eContent");
				addResourceStmt.setString(4, Util.trimTo(255, allEContent.getString("author")));
				addResourceStmt.setString(5, Util.trimTo(255, sortTitle));
				addResourceStmt.setString(6, Util.trimTo(13, isbn));
				addResourceStmt.setString(7, Util.trimTo(13, upc));
				addResourceStmt.setString(8, "");
				addResourceStmt.setString(9, "emedia");
				addResourceStmt.setLong(10, 0);
				addResourceStmt.setLong(11, new Date().getTime() / 1000);
				int numAdded = addResourceStmt.executeUpdate();
				if (numAdded != 1){
					logger.error("Resource not added for econtent record " + econtentId);
				}else{
				}
			}
			return true;
		} catch (SQLException e) {
			logger.error("Error updating resources for eContent", e);
			return false;
		}finally{

		}
	}
}
