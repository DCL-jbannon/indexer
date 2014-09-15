package org.vufind.processors;

import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.dcl.utils.ActiveEcontentUtils;
import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.*;

import au.com.bytecode.opencsv.CSVReader;
import org.vufind.config.ConfigFiller;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.*;
import org.vufind.econtent.DetectionSettings;
import org.vufind.econtent.GutenbergItemInfo;
import org.vufind.econtent.LibrarySpecificLink;
import org.vufind.econtent.LinkInfo;

/**
 * Run this export to build the file to import into VuFind
 * SELECT econtent_record.id, sourceUrl, item_type, filename, folder INTO OUTFILE 'd:/gutenberg_files.csv' FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' FROM econtent_record INNER JOIN econtent_item on econtent_record.id = econtent_item.recordId  WHERE source = 'Gutenberg';

 * @author Mark Noble
 *
 */

public class ExtractEContentFromMarc implements IMarcRecordProcessor, IRecordProcessor {
    final static Logger logger = LoggerFactory.getLogger(ExtractEContentFromMarc.class);

    private DynamicConfig config = null;

	private ArrayList<GutenbergItemInfo> gutenbergItemInfo = null;
	
	private PreparedStatement doesIlsIdExist;
	private PreparedStatement createEContentRecord;
	private PreparedStatement updateEContentRecord;
    private PreparedStatement deleteEContentRecord;
	private PreparedStatement deleteEContentItem;
    private PreparedStatement deleteEContentItemForRecord;
	private PreparedStatement doesGutenbergItemExist;
	private PreparedStatement addGutenbergItem;
	private PreparedStatement updateGutenbergItem;
	
	private PreparedStatement existingEContentRecordLinks;
	private PreparedStatement addSourceUrl;
	private PreparedStatement updateSourceUrl;
	
	private PreparedStatement doesOverDriveIdExist;
	private PreparedStatement addOverDriveId;
	private PreparedStatement updateOverDriveId;
	private PreparedStatement createActiveEContentRecord;
	;
	
	public boolean init(DynamicConfig config) {
		this.config = config;
        ConfigFiller.fill(config, OverDriveConfigOptions.values(), new File(config.getString(BasicConfigOptions.CONFIG_FOLDER)));
        ConfigFiller.fill(config, ExtractEContentConfigOptions.values(), new File(config.getString(BasicConfigOptions.CONFIG_FOLDER)));

        //Get a list of information about Gutenberg items
        String gutenbergItemFile = config.getString(ExtractEContentConfigOptions.GUTENBERG_ITEM_FILE);
        if (gutenbergItemFile == null || gutenbergItemFile.length() == 0){
            logger.warn("Unable to get Gutenberg Item File in Process settings.  Please add a gutenbergItemFile key.");
        }else{
            HashSet<String> validFormats = new HashSet<String>();
            validFormats.add("epub");
            validFormats.add("pdf");
            validFormats.add("jpg");
            validFormats.add("gif");
            validFormats.add("mp3");
            validFormats.add("plucker");
            validFormats.add("kindle");
            validFormats.add("externalLink");
            validFormats.add("externalMP3");
            validFormats.add("interactiveBook");
            validFormats.add("overdrive");

            //Load the items
            gutenbergItemInfo = new ArrayList<GutenbergItemInfo>();
            try {
                CSVReader gutenbergReader = new CSVReader(new FileReader(gutenbergItemFile));
                //Read headers
                gutenbergReader.readNext();
                String[] curItemInfo = gutenbergReader.readNext();
                while (curItemInfo != null){
                    GutenbergItemInfo itemInfo = new GutenbergItemInfo(curItemInfo[1], curItemInfo[2], curItemInfo[3], curItemInfo[4], curItemInfo[5]);

                    gutenbergItemInfo.add(itemInfo);
                    curItemInfo = gutenbergReader.readNext();
                }
            } catch (Exception e) {
                logger.error("Could not read Gutenberg Item file");
            }

        }

		return resetPreparedStatements();
	}

    private boolean resetPreparedStatements() {
        Connection econtentConn = ConnectionProvider.getConnection(config, ConnectionProvider.PrintOrEContent.E_CONTENT);

        try {
            //Connect to the vufind database
            doesIlsIdExist = econtentConn.prepareStatement("SELECT id from econtent_record WHERE ilsId = ?");
            createEContentRecord = econtentConn.prepareStatement(
                "INSERT INTO econtent_record " +
                "(ilsId, cover, source, title, subTitle, author, author2, description, contents, subject, language, " +
                    "publisher, edition, isbn, issn, upc, lccn, topic, genre, region, era, target_audience, sourceUrl, " +
                    "purchaseUrl, publishDate, marcControlField, accessType, date_added, marcRecord, external_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
            updateEContentRecord = econtentConn.prepareStatement(
                "UPDATE econtent_record " +
                "SET ilsId = ?, source = ?, title = ?, subTitle = ?, author = ?, author2 = ?, description = ?, " +
                    "contents = ?, subject = ?, language = ?, publisher = ?, edition = ?, isbn = ?, issn = ?, upc = ?, " +
                    "lccn = ?, topic = ?, genre = ?, region = ?, era = ?, target_audience = ?, sourceUrl = ?, " +
                    "purchaseUrl = ?, publishDate = ?, marcControlField = ?, accessType = ?, date_updated = ?, " +
                    "marcRecord = ?, external_id = ? " +
                    "WHERE id = ?");
            deleteEContentRecord = econtentConn.prepareStatement("DELETE FROM econtent_record WHERE ilsId = ?");
            deleteEContentItem = econtentConn.prepareStatement("DELETE FROM econtent_item where id = ?");
            deleteEContentItemForRecord = econtentConn.prepareStatement("DELETE ei.* FROM dclecontent_prod.econtent_item ei, dclecontent_prod.econtent_record er WHERE er.ilsId = ? AND er.id = ei.recordId");

            doesGutenbergItemExist = econtentConn.prepareStatement("SELECT id from econtent_item WHERE recordId = ? AND item_type = ? and notes = ?");
            addGutenbergItem = econtentConn.prepareStatement("INSERT INTO econtent_item (recordId, item_type, filename, folder, link, notes, date_added, addedBy, date_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            updateGutenbergItem = econtentConn.prepareStatement("UPDATE econtent_item SET filename = ?, folder = ?, link = ?, date_updated =? WHERE recordId = ? AND item_type = ? AND notes = ?");

            existingEContentRecordLinks = econtentConn.prepareStatement("SELECT id, link, libraryId from econtent_item WHERE recordId = ?");
            addSourceUrl = econtentConn.prepareStatement("INSERT INTO econtent_item (recordId, item_type, link, date_added, addedBy, date_updated, libraryId) VALUES (?, ?, ?, ?, ?, ?, ?)");
            updateSourceUrl = econtentConn.prepareStatement("UPDATE econtent_item SET link = ?, date_updated =? WHERE id = ?");

            doesOverDriveIdExist =  econtentConn.prepareStatement("SELECT id, overDriveId, link from econtent_item WHERE recordId = ? AND item_type = ? AND libraryId = ?");
            addOverDriveId = econtentConn.prepareStatement("INSERT INTO econtent_item (recordId, item_type, overDriveId, link, date_added, addedBy, date_updated, libraryId) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            updateOverDriveId = econtentConn.prepareStatement("UPDATE econtent_item SET overDriveId = ?, link = ?, date_updated =? WHERE id = ?");

            createActiveEContentRecord = econtentConn.prepareStatement("INSERT INTO active_econtent_records (ilsId) VALUES (?)");
        } catch (Exception ex) {
            // handle any errors
            logger.error("Error initializing econtent extraction ", ex);
            return false;
        }
        return true;
    }

	public boolean processMarcRecord(MarcRecordDetails recordInfo) {
		try {
            logger.debug("Processing record: "+recordInfo.getId());

            String ilsId = recordInfo.getId();

            if (ilsId.length() == 0){
                //Get the ils id
                ilsId = recordInfo.getId();
            }

            MarcProcessor.RecordStatus recordStatus = recordInfo.getRecordStatus();
			if(recordStatus == MarcProcessor.RecordStatus.RECORD_UNCHANGED && !
                    (config.getBool(BasicConfigOptions.DO_FULL_REINDEX) && config.getBool(ExtractEContentConfigOptions.UPDATE_UNCHANGED_MARC)))
			{
				logger.debug("Skipping eContent extraction because record has not changed. Active Records: " + ActiveEcontentUtils.getList().size());
				return false;
			}

            //Check the 856 tag to see if this is a source that we can handle.
            if (!recordInfo.isEContent()){
                logger.debug("Skipping record, it is not eContent");
                return false;
            }

            if (recordStatus == MarcProcessor.RecordStatus.RECORD_DELETED) {
                deleteEContentItemForRecord.setString(1, ilsId);
                deleteEContentItemForRecord.executeUpdate();

                deleteEContentRecord.setString(1, ilsId);
                deleteEContentRecord.executeUpdate();

                return true;
            }

            ActiveEcontentUtils.addActiveEcontent(ilsId);

            logger.debug("Record is eContent, processing");
			//Record is eContent, get additional details about how to process it.
			HashMap<String, DetectionSettings> detectionSettingsBySource = recordInfo.getEContentDetectionSettings();
			if (detectionSettingsBySource == null || detectionSettingsBySource.size() == 0){
				logger.error("Record " + recordInfo.getId() + " was tagged as eContent, but we did not get detection settings for it.");
				return false;
			}
			
			for (String source : detectionSettingsBySource.keySet()){
				logger.debug("Record " + recordInfo.getId() + " is eContent, source is " + source);
				DetectionSettings detectionSettings = detectionSettingsBySource.get(source);
				//Generally should only have one source, but in theory there could be multiple sources for a single record
				String accessType = detectionSettings.getAccessType();
				//Make sure that overdrive titles are updated if we need to check availability
				if (source.equalsIgnoreCase("overdrive") && config.getBool(OverDriveConfigOptions.CHECK_AVAILABILITY)){
					//Overdrive record, force processing to make sure we get updated availability
					logger.debug("Record is overdrive, forcing reindex to check overdrive availability");
				}else if (recordStatus == MarcProcessor.RecordStatus.RECORD_UNCHANGED){
					if (config.getBool(BasicConfigOptions.DO_FULL_REINDEX)){
						logger.debug("Record is unchanged, but reindex unchanged records is on");
					}else{
						logger.debug("Skipping because the record is not changed");
						return false;
					}
				}else{
					logger.debug("Record has changed or is new");
				}
				
				
				//Check to see if the record already exists

				boolean importRecordIntoDatabase = true;
				long eContentRecordId = -1;
				if (ilsId.length() == 0){
					logger.error("ILS Id could not be found in the marc record, skipping.");
                    return false;
				}else{
					doesIlsIdExist.setString(1, ilsId);
					ResultSet ilsIdExists = doesIlsIdExist.executeQuery();
					if (ilsIdExists.next()){
						//The record already exists, check if it needs to be updated?
						importRecordIntoDatabase = false;
						eContentRecordId = ilsIdExists.getLong("id");
					}else{
						//Add to database
						importRecordIntoDatabase = true;
					}
				}		

				boolean recordAdded = false;
				
				//logger.info("ECONTENT: " + recordStatus + " " +ilsId);
				
				logger.debug("ADDING/UPDATING ECONTENT: " + recordStatus + " " + ilsId);
                if (importRecordIntoDatabase){
					//Add to database
					//logger.info("Adding ils id " + ilsId + " to the database.");
					createEContentRecord.setString(1, recordInfo.getId());
					createEContentRecord.setString(2, "");
					createEContentRecord.setString(3, source);
					createEContentRecord.setString(4, Util.trimTo(255, recordInfo.getFirstFieldValueInSet("title_short")));
					createEContentRecord.setString(5, Util.trimTo(255, recordInfo.getFirstFieldValueInSet("title_sub")));
					createEContentRecord.setString(6, recordInfo.getFirstFieldValueInSet("author"));
					createEContentRecord.setString(7, Util.getCRSeparatedString(recordInfo.getMappedField("author2")));
					createEContentRecord.setString(8, recordInfo.getDescription());
					createEContentRecord.setString(9, Util.getCRSeparatedString(recordInfo.getMappedField("contents")));
					createEContentRecord.setString(10, Util.getCRSeparatedString(recordInfo.getMappedField("topic_facet")));
					createEContentRecord.setString(11, recordInfo.getFirstFieldValueInSet("language"));
					createEContentRecord.setString(12, recordInfo.getFirstFieldValueInSet("publisher"));
					createEContentRecord.setString(13, recordInfo.getFirstFieldValueInSet("edition"));
					createEContentRecord.setString(14, Util.trimTo(500, Util.getCRSeparatedString(recordInfo.getMappedField("isbn"))));
					createEContentRecord.setString(15, Util.getCRSeparatedString(recordInfo.getMappedField("issn")));
					createEContentRecord.setString(16, recordInfo.getFirstFieldValueInSet("language"));
					createEContentRecord.setString(17, recordInfo.getFirstFieldValueInSet("lccn"));
					createEContentRecord.setString(18, Util.getCRSeparatedString(recordInfo.getMappedField("topic")));
					createEContentRecord.setString(19, Util.getCRSeparatedString(recordInfo.getMappedField("genre")));
					createEContentRecord.setString(20, Util.getCRSeparatedString(recordInfo.getMappedField("geographic")));
					createEContentRecord.setString(21, Util.getCRSeparatedString(recordInfo.getMappedField("era")));
					createEContentRecord.setString(22, Util.getCRSeparatedString(recordInfo.getMappedField("target_audience")));
					String sourceUrl = "";
                    String externalId = "";
					if (recordInfo.getSourceUrls().size() == 1){
						sourceUrl = recordInfo.getSourceUrls().get(0).getUrl();
                        if(source.equalsIgnoreCase("overdrive")) {
                            List<NameValuePair> parameters = URLEncodedUtils.parse(
                                    new URI(sourceUrl),
                                    java.nio.charset.Charset.defaultCharset().toString());
                            for(NameValuePair pair : parameters) {
                                if(pair.getName().equalsIgnoreCase("ID")) {
                                    externalId = pair.getValue();
                                }
                            }
                        }
					}
					createEContentRecord.setString(23, sourceUrl);
					createEContentRecord.setString(24, recordInfo.getPurchaseUrl());
					createEContentRecord.setString(25, recordInfo.getFirstFieldValueInSet("publishDate"));
					createEContentRecord.setString(26, recordInfo.getFirstFieldValueInSet("ctrlnum"));
					createEContentRecord.setString(27, accessType);
					createEContentRecord.setLong(28, new Date().getTime() / 1000);
					createEContentRecord.setString(29, recordInfo.toString());
                    createEContentRecord.setString(30, externalId);
					int rowsInserted = createEContentRecord.executeUpdate();
					if (rowsInserted != 1){
						logger.error("Could not insert row into the database");
					}else{
						ResultSet generatedKeys = createEContentRecord.getGeneratedKeys();
						while (generatedKeys.next()){
							eContentRecordId = generatedKeys.getLong(1);
							recordAdded = true;
						}
					}
				}else{
					//Update the record
					//logger.info("Updating ilsId " + ilsId + " recordId " + eContentRecordId);
					updateEContentRecord.setString(1, recordInfo.getId());
					updateEContentRecord.setString(2, source);
					updateEContentRecord.setString(3, Util.trimTo(255, recordInfo.getFirstFieldValueInSet("title_short")));
					updateEContentRecord.setString(4, Util.trimTo(255, recordInfo.getFirstFieldValueInSet("title_sub")));
					updateEContentRecord.setString(5, recordInfo.getFirstFieldValueInSet("author"));
					updateEContentRecord.setString(6, Util.getCRSeparatedString(recordInfo.getMappedField("author2")));
					updateEContentRecord.setString(7, recordInfo.getDescription());
					updateEContentRecord.setString(8, Util.getCRSeparatedString(recordInfo.getMappedField("contents")));
					updateEContentRecord.setString(9, Util.getCRSeparatedString(recordInfo.getMappedField("topic_facet")));
					updateEContentRecord.setString(10, recordInfo.getFirstFieldValueInSet("language"));
					updateEContentRecord.setString(11, recordInfo.getFirstFieldValueInSet("publisher"));
					updateEContentRecord.setString(12, recordInfo.getFirstFieldValueInSet("edition"));
					updateEContentRecord.setString(13, Util.trimTo(500, Util.getCRSeparatedString(recordInfo.getMappedField("isbn"))));
					updateEContentRecord.setString(14, Util.getCRSeparatedString(recordInfo.getMappedField("issn")));
					updateEContentRecord.setString(15, recordInfo.getFirstFieldValueInSet("upc"));
					updateEContentRecord.setString(16, recordInfo.getFirstFieldValueInSet("lccn"));
					updateEContentRecord.setString(17, Util.getCRSeparatedString(recordInfo.getMappedField("topic")));
					updateEContentRecord.setString(18, Util.getCRSeparatedString(recordInfo.getMappedField("genre")));
					updateEContentRecord.setString(19, Util.getCRSeparatedString(recordInfo.getMappedField("geographic")));
					updateEContentRecord.setString(20, Util.getCRSeparatedString(recordInfo.getMappedField("era")));
					updateEContentRecord.setString(21, Util.getCRSeparatedString(recordInfo.getMappedField("target_audience")));
                    String sourceUrl = "";
                    String externalId = "";
                    if (recordInfo.getSourceUrls().size() == 1){
                        sourceUrl = recordInfo.getSourceUrls().get(0).getUrl();
                        if(source.equalsIgnoreCase("overdrive")) {
                            List<NameValuePair> parameters = URLEncodedUtils.parse(
                                    new URI(sourceUrl),
                                    java.nio.charset.Charset.defaultCharset().toString());
                            for(NameValuePair pair : parameters) {
                                if(pair.getName().equalsIgnoreCase("ID")) {
                                    externalId = pair.getValue();
                                }
                            }
                        }
                    }
					updateEContentRecord.setString(22, sourceUrl);
					updateEContentRecord.setString(23, recordInfo.getPurchaseUrl());
					updateEContentRecord.setString(24, recordInfo.getFirstFieldValueInSet("publishDate"));
					updateEContentRecord.setString(25, recordInfo.getFirstFieldValueInSet("ctrlnum"));
					updateEContentRecord.setString(26, accessType);
					updateEContentRecord.setLong(27, new Date().getTime() / 1000);
					updateEContentRecord.setString(28, recordInfo.toString());
                    updateEContentRecord.setString(29, externalId);
					updateEContentRecord.setLong(30, eContentRecordId);
					int rowsInserted = updateEContentRecord.executeUpdate();
					if (rowsInserted != 1){
						logger.error("Could not insert row into the database");
					}else{
						recordAdded = true;
					}
				}
				
				logger.debug("Finished initial insertion/update recordAdded = " + recordAdded);
				
				if (recordAdded){
					if (source.equalsIgnoreCase("gutenberg")){
						attachGutenbergItems(recordInfo, eContentRecordId, logger);
					}else if (detectionSettings.getSource().equalsIgnoreCase("overdrive")){
						setupOverDriveItems(recordInfo, eContentRecordId, detectionSettings, logger);
					}else if (detectionSettings.isAdd856FieldsAsExternalLinks()){
						//Automatically setup 856 links as external links
						setupExternalLinks(recordInfo, eContentRecordId, detectionSettings, logger);
					}
					logger.debug("Record processed successfully.");
				}else{
					logger.debug("Record NOT processed successfully.");
				}
			}
			
			/*updateRecordsProcessed.setLong(1, this.recordsProcessed + 1);
			updateRecordsProcessed.setLong(2, logEntryId);
			updateRecordsProcessed.executeUpdate();*/
			logger.debug("Finished processing record");
			return true;
		} catch (Exception e) {
            if(e instanceof SQLException) {
                resetPreparedStatements();
            }
			logger.error("Error importing marc record ", e);
			return false;
		}finally{

		}
	}

	private void setupExternalLinks(MarcRecordDetails recordInfo, long eContentRecordId, DetectionSettings detectionSettings, Logger logger) {
		//Get existing links from the record
		ArrayList<LinkInfo> allLinks = new ArrayList<LinkInfo>();
		try {
			existingEContentRecordLinks.setLong(1, eContentRecordId);
			ResultSet allExistingUrls = existingEContentRecordLinks.executeQuery();
			while (allExistingUrls.next()){
				LinkInfo curLinkInfo = new LinkInfo();
				curLinkInfo.setItemId(allExistingUrls.getLong("id"));
				curLinkInfo.setLink(allExistingUrls.getString("link"));
				curLinkInfo.setLibraryId(allExistingUrls.getLong("libraryId"));
				allLinks.add(curLinkInfo);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug("Found " + allLinks.size() + " existing links");
		
		//Add the links that are currently available for the record
		ArrayList<LibrarySpecificLink> sourceUrls = recordInfo.getSourceUrls();
		logger.debug("Found " + sourceUrls.size() + " urls for " + recordInfo.getId());
		for (LibrarySpecificLink curLink : sourceUrls){
			//Look for an existing link
			LinkInfo linkForSourceUrl = null;
			for (LinkInfo tmpLinkInfo : allLinks){
				if (tmpLinkInfo.getLibraryId() == curLink.getLibrarySystemId()){
					linkForSourceUrl = tmpLinkInfo;
				}
			}
			addExternalLink(linkForSourceUrl, curLink.getUrl(), curLink.getLibrarySystemId(), eContentRecordId, detectionSettings, logger);
			if (linkForSourceUrl != null){
				allLinks.remove(linkForSourceUrl);
			}
		}
		
		//Remove any links that no longer exist
		logger.debug("There are " + allLinks.size() + " links that need to be deleted");
		for (LinkInfo tmpLinkInfo : allLinks){
			try {
				deleteEContentItem.setLong(1, tmpLinkInfo.getItemId());
				deleteEContentItem.executeUpdate();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void addExternalLink(LinkInfo existingLinkInfo, String sourceUrl, long libraryId, long eContentRecordId, DetectionSettings detectionSettings, Logger logger) {
		//Check to see if the link already exists
		try {
			if (existingLinkInfo != null){
				logger.debug("Updating link " + sourceUrl + " libraryId = " + libraryId);
				String existingUrlValue = existingLinkInfo.getLink();
				Long existingItemId = existingLinkInfo.getItemId();
				if (!existingUrlValue.equals(sourceUrl)){
					//Url does not match, add it to the record. 
					updateSourceUrl.setString(1, sourceUrl);
					updateSourceUrl.setLong(2, new Date().getTime());
					updateSourceUrl.setLong(3, existingItemId);
					updateSourceUrl.executeUpdate();
				}
			}else{
				logger.debug("Adding link " + sourceUrl + " libraryId = " + libraryId);
				//the url does not exist, insert it
				addSourceUrl.setLong(1, eContentRecordId);
				addSourceUrl.setString(2, detectionSettings.getItem_type());
				addSourceUrl.setString(3, sourceUrl);
				addSourceUrl.setLong(4, new Date().getTime());
				addSourceUrl.setLong(5, -1);
				addSourceUrl.setLong(6, new Date().getTime());
				addSourceUrl.setLong(7, libraryId);
				addSourceUrl.executeUpdate();
			}
		} catch (SQLException e) {
			logger.error("Error adding link to record " + eContentRecordId + " " + sourceUrl, e);
		}
		
	}
	
	private void setupOverDriveItems(MarcRecordDetails recordInfo, long eContentRecordId, DetectionSettings detectionSettings, Logger logger){
		ArrayList<LibrarySpecificLink> sourceUrls = recordInfo.getSourceUrls();
		logger.debug("Found " + sourceUrls.size() + " urls for overdrive id " + recordInfo.getId());
		//Check the items within the record to see if there are any location specific links
		for(LibrarySpecificLink link : recordInfo.getSourceUrls()){
			addOverdriveItem(link.getUrl(), link.getLibrarySystemId(), eContentRecordId, detectionSettings, logger);
		}
	}
	
	private void addOverdriveItem(String sourceUrl, long libraryId, long eContentRecordId, DetectionSettings detectionSettings, Logger logger) {
		//Check to see if the link already exists
		Pattern Regex = Pattern.compile("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}", Pattern.CANON_EQ);
		Matcher RegexMatcher = Regex.matcher(sourceUrl);
		String overDriveId = null;
		if (RegexMatcher.find()) {
			overDriveId = RegexMatcher.group();
		}
		if (overDriveId == null){
			logger.debug(sourceUrl + " was not a link to overdrive content");
			return;
		}
		logger.debug("Found overDrive url\r\n" + sourceUrl + "\r\nlibraryId: " + libraryId + "  overDriveId: " + overDriveId);

		try {
			doesOverDriveIdExist.setLong(1, eContentRecordId);
			doesOverDriveIdExist.setString(2, detectionSettings.getItem_type());
			doesOverDriveIdExist.setLong(3, libraryId);
			ResultSet existingOverDriveId = doesOverDriveIdExist.executeQuery();
			if (existingOverDriveId.next()){
				String existingOverDriveIdValue = existingOverDriveId.getString("overDriveId");
				Long existingItemId = existingOverDriveId.getLong("id");
				String existingLink = existingOverDriveId.getString("link");
				if (existingLink == null || existingOverDriveIdValue == null || !overDriveId.equals(existingOverDriveIdValue) || !sourceUrl.equals(existingLink)){
					//Url does not match, add it to the record. 
					updateOverDriveId.setString(1, overDriveId);
					updateOverDriveId.setString(2, sourceUrl);
					updateOverDriveId.setLong(3, new Date().getTime());
					updateOverDriveId.setLong(4, existingItemId);
					updateOverDriveId.executeUpdate();
				}
			}else{
				//the url does not exist, insert it
				addOverDriveId.setLong(1, eContentRecordId);
				addOverDriveId.setString(2, detectionSettings.getItem_type());
				addOverDriveId.setString(3, overDriveId);
				addOverDriveId.setString(4, sourceUrl);
				addOverDriveId.setLong(5, new Date().getTime());
				addOverDriveId.setLong(6, -1);
				addOverDriveId.setLong(7, new Date().getTime());
				addOverDriveId.setLong(8, libraryId);
				addOverDriveId.executeUpdate();
			}
		} catch (SQLException e) {
			logger.error("Error adding overdrive id to record " + eContentRecordId + " " + overDriveId, e);
		}
		
	}

	private void attachGutenbergItems(MarcRecordDetails recordInfo, long eContentRecordId, Logger logger) {
		//If no, load the source url
		for (LibrarySpecificLink curLink : recordInfo.getSourceUrls()){
			String sourceUrl = curLink.getUrl();
			logger.info("Loading gutenberg items " + sourceUrl);
			try {
				//Get the source URL from the export of all items. 
				for (GutenbergItemInfo curItem : gutenbergItemInfo){
					if (curItem.getSourceUrl().equalsIgnoreCase(sourceUrl)){
						//Check to see if the item is already attached to the record.  
						doesGutenbergItemExist.setLong(1, eContentRecordId);
						doesGutenbergItemExist.setString(2, curItem.getFormat());
						doesGutenbergItemExist.setString(3, curItem.getNotes());
						ResultSet itemExistRs = doesGutenbergItemExist.executeQuery();
						if (itemExistRs.next()){
							//Check to see if the item needs to be updated (different folder or filename)
							updateGutenbergItem.setString(1, curItem.getFilename());
							updateGutenbergItem.setString(2, curItem.getFolder());
							updateGutenbergItem.setString(3, curItem.getLink());
							updateGutenbergItem.setLong(4, new Date().getTime());
							updateGutenbergItem.setLong(5, eContentRecordId);
							updateGutenbergItem.setString(6, curItem.getFormat());
							updateGutenbergItem.setString(7, curItem.getNotes());
							updateGutenbergItem.executeUpdate();
						}else{
							//Item does not exist, need to add it to the record.
							addGutenbergItem.setLong(1, eContentRecordId);
							addGutenbergItem.setString(2, curItem.getFormat());
							addGutenbergItem.setString(3, curItem.getFilename());
							addGutenbergItem.setString(4, curItem.getFolder());
							addGutenbergItem.setString(5, curItem.getLink());
							addGutenbergItem.setString(6, curItem.getNotes());
							addGutenbergItem.setLong(7, new Date().getTime());
							addGutenbergItem.setInt(8, -1);
							addGutenbergItem.setLong(9, new Date().getTime());
							addGutenbergItem.executeUpdate();
						}
					}
				}
				//Attach items based on the source URL
			} catch (Exception e) {
				logger.info("Unable to add items for " + eContentRecordId, e);
			}
		}
	}

    @Override
	public void finish() {

	}
}
