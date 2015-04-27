package org.vufind;

import bsh.EvalError;
import bsh.Interpreter;
import org.dcl.utils.ActiveEcontentUtils;
import org.vufind.econtent.DetectionSettings;
import org.marc4j.marc.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solrmarc.tools.Utils;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.MarcConfigOptions;
import org.vufind.processors.IMarcRecordProcessor;

import java.io.*;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Reads Marc Records from a marc file or files Loads the data into fields based
 * on marc.properties and marc_local.properties, Determines if the record is
 * new/updated/unchanged Applies processors to the record Determines which
 * records no longer exist in the marc record Applies processors to the deleted
 * records
 * 
 * Draws heavily from SolrMarc functionality with some simplifications
 * 
 * @author Mark Noble
 * 
 */
public class MarcProcessor {
    final static Logger logger = LoggerFactory.getLogger(MarcProcessor.class);

    protected String translationMapsFolder;
    protected String scriptFilesFolder;
    protected String marcRecordPath;
    protected int recordsProcessed = 0;
    protected int maxRecordsToProcess = -1;
    /**
     * map: keys are solr field names, values inform how to get solr field values
     */
    HashMap<String, String[]> marcFieldProps = new HashMap<String, String[]>();
    /**
     * map of translation maps. keys are names of translation maps; values are the
     * translation maps (hence, it's a map of maps)
     */
    HashMap<String, Map<String, String>> translationMaps = new HashMap<String, Map<String, String>>();
    private String marcEncoding = "UTF8";
    private HashMap<String, MarcIndexInfo> marcIndexInfo = new HashMap<String, MarcIndexInfo>();
    /**
     * map of custom methods. keys are names of custom methods; values are the
     * methods to call for that custom method
     */
    private Map<String, Method> customMethodMap = new HashMap<String, Method>();
    /**
     * map of script interpreters. keys are names of scripts; values are the
     * Interpterers
     */
    private Map<String, Interpreter> scriptMap = new HashMap<String, Interpreter>();
    private PreparedStatement insertMarcInfoStmt;
    private PreparedStatement updateMarcInfoStmt;
    private Set<String> existingEContentIds = Collections.synchronizedSet(new HashSet<String>());
    private Map<String, Float> printRatings = Collections.synchronizedMap(new HashMap<String, Float>());
    private Map<String, Float> econtentRatings = Collections.synchronizedMap(new HashMap<String, Float>());
    private Map<String, Long> librarySystemFacets = Collections.synchronizedMap(new HashMap<String, Long>());
    private Map<String, Long> eContentLinkRules = Collections.synchronizedMap(new HashMap<String, Long>());
    private ArrayList<DetectionSettings> detectionSettings = new ArrayList<DetectionSettings>();
    private String itemTag;
    private String locationSubfield;
    private String urlSubfield;
    private String sharedEContentLocation;
    private boolean scrapeItemsForLinks;

    private Connection econtentConn = null;
    private Connection vufindConn = null;
    public HashMap<String, String[]> getMarcFieldProps() {
        return marcFieldProps;
    }

    public static enum RecordStatus {
        RECORD_CHANGED, RECORD_UNCHANGED, RECORD_NEW, RECORD_DELETED
    }
	
	//public boolean init(String serverName, Ini configIni, Connection vufindConn, Connection econtentConn, long reindexLogId) {
    public boolean init(DynamicConfig config) {
		marcRecordPath = config.getString(MarcConfigOptions.MARC_FOLDER);
		// Get the directory where the marc records are stored.vufindConn
		if (marcRecordPath == null || marcRecordPath.length() == 0) {
			logger.error("Marc Record Path not found in Reindex Settings.  Please specify the path as the marcPath key.");
			return false;
		}

		// Setup where to look for translation maps
		translationMapsFolder = config.getString(BasicConfigOptions.TRANSLATION_MAPS_FOLDER);
		scriptFilesFolder = config.getString(BasicConfigOptions.SCRIPTS_FOLDER);

		logger.info("Loading marc properties");
		// Load default marc.properties and marc properties for site into Properties
		// Object
		Properties marcProperties = new Properties();
        List marcPropertiesFiles = config.getList(MarcConfigOptions.MARC_PROPERTIES);
        for(Object propFileO : marcPropertiesFiles) {
            File propFile = new File(propFileO.toString());
            try {
                marcProperties.load(new FileReader(propFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


		// Do additional processing of map properties to determine how each should
		// be processed.
		processMarcFieldProperties(marcProperties);

		/*String maxRecordsToProcessValue = configIni.get("Reindex", "maxRecordsToProcess");
		if (maxRecordsToProcessValue != null) {
			maxRecordsToProcess = Integer.parseInt(maxRecordsToProcessValue);
		}*/

		// Load field information for local call numbers
		/*itemTag = configIni.get("Reindex", "itemTag");
		urlSubfield = configIni.get("Reindex", "itemUrlSubfield");
		locationSubfield = configIni.get("Reindex", "locationSubfield");
		sharedEContentLocation = configIni.get("Reindex", "sharedEContentLocation");
		String scrapeItemsForLinksStr = configIni.get("Reindex", "scrapeItemsForLinks");
		if (scrapeItemsForLinksStr != null) {
			scrapeItemsForLinks = Boolean.parseBoolean(scrapeItemsForLinksStr);
		}*/

		// Load the checksums of any marc records that have been loaded already
		// This allows us to detect whether or not the record is new, has changed,
		// or is deleted
		logger.info("Loading existing checksums for records");
        vufindConn = ConnectionProvider.getConnection(config, ConnectionProvider.PrintOrEContent.PRINT);
        econtentConn = ConnectionProvider.getConnection(config, ConnectionProvider.PrintOrEContent.E_CONTENT);

		try {
			//PreparedStatement existingRecordChecksumsStmt = vufindConn.prepareStatement("SELECT * FROM marc_import");
            PreparedStatement existingRecordChecksumsStmt = vufindConn.prepareStatement("SELECT record_id as id, marc_checksum as checksum, source  FROM resource");
			ResultSet existingRecordChecksumsRS = existingRecordChecksumsStmt.executeQuery();
			while (existingRecordChecksumsRS.next()) {
				MarcIndexInfo marcInfo = new MarcIndexInfo();
				marcInfo.setChecksum(existingRecordChecksumsRS.getLong("checksum"));
                String source = existingRecordChecksumsRS.getString("source");
				marcInfo.setEContent(source!=null && source.equals("eContent"));
				marcIndexInfo.put(existingRecordChecksumsRS.getString("id"), marcInfo);
			}
		} catch (SQLException e) {
			logger.error("Unable to load checksums for existing records", e);
			return false;
		}

		// Load the ILS ids of any eContent records that have been loaded so we can
		// suppress the record in the regular content
		logger.info("Loading ils ids for econtent records for suppression");
		try {
			PreparedStatement existingEContentRecordStmt = econtentConn.prepareStatement("SELECT ilsId FROM econtent_record");
			ResultSet existingEContentRecordRS = existingEContentRecordStmt.executeQuery();
			while (existingEContentRecordRS.next()) {
				existingEContentIds.add(existingEContentRecordRS.getString("ilsId"));
			}
		} catch (SQLException e) {
			logger.error("Unable to load checksums for existing records", e);
			return false;
		}

		// Load detection settings to determine if a record is eContent.
		logger.info("Loading record detection settings");
		try {
			PreparedStatement eContentDetectionSettingsStmt = econtentConn.prepareStatement("SELECT * FROM econtent_record_detection_settings");
			ResultSet eContentDetectionSettingsRS = eContentDetectionSettingsStmt.executeQuery();
			while (eContentDetectionSettingsRS.next()) {
				DetectionSettings settings = new DetectionSettings();
				settings.setFieldSpec(eContentDetectionSettingsRS.getString("fieldSpec"));
                String matchValue = eContentDetectionSettingsRS.getString("valueToMatch");
                matchValue = matchValue.trim().toLowerCase();
                if(matchValue.endsWith(".")) {
                    matchValue = matchValue.substring(0, matchValue.length()-1);
                }
				settings.setValueToMatch(matchValue);
				settings.setSource(eContentDetectionSettingsRS.getString("source"));
				settings.setAccessType(eContentDetectionSettingsRS.getString("accessType"));
				settings.setItem_type(eContentDetectionSettingsRS.getString("item_type"));
				settings.setAdd856FieldsAsExternalLinks(eContentDetectionSettingsRS.getBoolean("add856FieldsAsExternalLinks"));
				detectionSettings.add(settings);
			}
		} catch (SQLException e) {
			logger.error("Unable to load detection settings for eContent.", e);
			return false;
		}

		// Load ratings for print and eContent titles
		logger.info("Loading ratings");
		try {
			PreparedStatement printRatingsStmt = vufindConn
					.prepareStatement(
							"SELECT record_id, avg(rating) as rating from resource inner join user_rating on user_rating.resourceid = resource.id where source = 'VuFind' GROUP BY record_id",
							ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			ResultSet printRatingsRS = printRatingsStmt.executeQuery();
			while (printRatingsRS.next()) {
				printRatings.put(printRatingsRS.getString("record_id"), printRatingsRS.getFloat("rating"));
			}
			printRatingsRS.close();
			PreparedStatement econtentRatingsStmt = econtentConn
					.prepareStatement(
							"SELECT ilsId, avg(rating) as rating from econtent_record inner join econtent_rating on econtent_rating.recordId = econtent_record.id WHERE ilsId <> '' GROUP BY ilsId",
							ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			ResultSet econtentRatingsRS = econtentRatingsStmt.executeQuery();
			while (econtentRatingsRS.next()) {
				econtentRatings.put(econtentRatingsRS.getString("ilsId"), econtentRatingsRS.getFloat("rating"));
			}
			econtentRatingsRS.close();
		} catch (SQLException e) {
			logger.error("Unable to load ratings for resource", e);
			return false;
		}

		// Load information from library table
		try {
			PreparedStatement librarySystemFacetStmt = vufindConn.prepareStatement("SELECT libraryId, facetLabel, eContentLinkRules from library");
			ResultSet librarySystemFacetRS = librarySystemFacetStmt.executeQuery();
			while (librarySystemFacetRS.next()) {
				librarySystemFacets.put(librarySystemFacetRS.getString("facetLabel"), librarySystemFacetRS.getLong("libraryId"));
				String eContentLinkRulesStr = librarySystemFacetRS.getString("eContentLinkRules");
				if (eContentLinkRulesStr != null && eContentLinkRulesStr.length() > 0) {
					eContentLinkRulesStr = ".*(" + eContentLinkRulesStr.toLowerCase() + ").*";
					eContentLinkRules.put(eContentLinkRulesStr, librarySystemFacetRS.getLong("libraryId"));
				}
			}
		} catch (SQLException e) {
			logger.error("Unable to load library System Facet information", e);
			return false;
		}

		// Setup additional statements
		try {
			insertMarcInfoStmt = vufindConn.prepareStatement("INSERT INTO marc_import (id, checksum, eContent) VALUES (?, ?, ?)");
			updateMarcInfoStmt = vufindConn
					.prepareStatement("UPDATE marc_import SET checksum = ?, backup_checksum = ?, eContent = ?, backup_eContent = ? WHERE id = ?");
		} catch (SQLException e) {
			logger.error("Unable to setup statements for updating marc_import table", e);
			return false;
		}
		return true;
	}

	public Set<String> getExistingEContentIds() {
		return existingEContentIds;
	}

	public Map<String, Float> getPrintRatings() {
		return printRatings;
	}

	public Map<String, Float> getEcontentRatings() {
		return econtentRatings;
	}

	public ArrayList<DetectionSettings> getDetectionSettings() {
		return detectionSettings;
	}

	/**
	 * Parse the properties
	 * 
	 * @param marcProperties
	 */
	private void processMarcFieldProperties(Properties marcProperties) {
		Enumeration<?> en = marcProperties.propertyNames();

		while (en.hasMoreElements()) {
			String propName = (String) en.nextElement();
			// ignore map, pattern_map; they are handled separately
			if (!propName.startsWith("map") && !propName.startsWith("pattern_map")) {
				String propValue = marcProperties.getProperty(propName);
				String fieldDef[] = new String[4];
				fieldDef[0] = propName;
				fieldDef[3] = null;
				if (propValue.startsWith("\"")) {
					// value is a constant if it starts with a quote
					fieldDef[1] = "constant";
					fieldDef[2] = propValue.trim().replaceAll("\"", "");
				} else
				// not a constant
				{
					// split it into two pieces at first comma or space
					String values[] = propValue.split("[, ]+", 2);
					if (values[0].startsWith("custom") || values[0].startsWith("script")) {
						fieldDef[1] = values[0];

						// parse sections of custom value assignment line in
						// _index.properties file
						String lastValues[];
						// get rid of empty parens
						if (values[1].indexOf("()") != -1) values[1] = values[1].replace("()", "");

						// index of first open paren after custom method name
						int parenIx = values[1].indexOf('(');

						// index of first unescaped comma after method name
						int commaIx = Utils.getIxUnescapedComma(values[1]);

						if (parenIx != -1 && commaIx != -1 && parenIx < commaIx) {
							// remainder should be split after close paren
							// followed by comma (optional spaces in between)
							lastValues = values[1].trim().split("\\) *,", 2);

							// Reattach the closing parenthesis:
							if (lastValues.length == 2) lastValues[0] += ")";
						} else
							// no parens - split comma preceded by optional spaces
							lastValues = values[1].trim().split(" *,", 2);

						fieldDef[2] = lastValues[0].trim();

						fieldDef[3] = lastValues.length > 1 ? lastValues[1].trim() : null;
						// is this a translation map?
						if (fieldDef[3] != null && fieldDef[3].contains("map")) {
							try {
								fieldDef[3] = loadTranslationMap(marcProperties, fieldDef[3]);
							} catch (IllegalArgumentException e) {
								logger.error("Unable to find file containing specified translation map (" + fieldDef[3] + ")");
								throw new IllegalArgumentException("Error: Problems reading specified translation map (" + fieldDef[3] + ")");
							}
						}
					} // end custom
					else if (values[0].equals("xml") || values[0].equals("raw") || values[0].equals("date") || values[0].equals("json") || values[0].equals("json2")
							|| values[0].equals("index_date") || values[0].equals("era")) {
						fieldDef[1] = "std";
						fieldDef[2] = values[0];
						fieldDef[3] = values.length > 1 ? values[1].trim() : null;
						// NOTE: assuming no translation map here
						if (fieldDef[2].equals("era") && fieldDef[3] != null) {
							try {
								fieldDef[3] = loadTranslationMap(marcProperties, fieldDef[3]);
							} catch (IllegalArgumentException e) {
								logger.error("Unable to find file containing specified translation map (" + fieldDef[3] + ")");
								throw new IllegalArgumentException("Error: Problems reading specified translation map (" + fieldDef[3] + ")");
							}
						}
					} else if (values[0].equalsIgnoreCase("FullRecordAsXML") || values[0].equalsIgnoreCase("FullRecordAsMARC")
							|| values[0].equalsIgnoreCase("FullRecordAsJson") || values[0].equalsIgnoreCase("FullRecordAsJson2")
							|| values[0].equalsIgnoreCase("FullRecordAsText") || values[0].equalsIgnoreCase("DateOfPublication")
							|| values[0].equalsIgnoreCase("DateRecordIndexed")) {
						fieldDef[1] = "std";
						fieldDef[2] = values[0];
						fieldDef[3] = values.length > 1 ? values[1].trim() : null;
						// NOTE: assuming no translation map here
					} else if (values.length == 1) {
						fieldDef[1] = "all";
						fieldDef[2] = values[0];
						fieldDef[3] = null;
					} else
					// other cases of field definitions
					{
						String values2[] = values[1].trim().split("[ ]*,[ ]*", 2);
						fieldDef[1] = "all";
						if (values2[0].equals("first") || (values2.length > 1 && values2[1].equals("first"))) fieldDef[1] = "first";

						if (values2[0].startsWith("join")) fieldDef[1] = values2[0];

						if ((values2.length > 1 && values2[1].startsWith("join"))) fieldDef[1] = values2[1];

						if (values2[0].equalsIgnoreCase("DeleteRecordIfFieldEmpty") || (values2.length > 1 && values2[1].equalsIgnoreCase("DeleteRecordIfFieldEmpty")))
							fieldDef[1] = "DeleteRecordIfFieldEmpty";

						fieldDef[2] = values[0];
						fieldDef[3] = null;

						// might we have a translation map?
						if (!values2[0].equals("all") && !values2[0].equals("first") && !values2[0].startsWith("join")
								&& !values2[0].equalsIgnoreCase("DeleteRecordIfFieldEmpty")) {
							fieldDef[3] = values2[0].trim();
							if (fieldDef[3] != null) {
								try {
									fieldDef[3] = loadTranslationMap(marcProperties, fieldDef[3]);
								} catch (IllegalArgumentException e) {
									logger.error("Unable to find file containing specified translation map (" + fieldDef[3] + ")");
									throw new IllegalArgumentException("Error: Problems reading specified translation map (" + fieldDef[3] + ")");
								}
							}
						}
					} // other cases of field definitions

				} // not a constant

				marcFieldProps.put(propName, fieldDef);

			} // if not map or pattern_map

		} // while enumerating through property names
	}

	/**
	 * load the translation map into transMapMap
	 * 
	 * @param indexProps
	 *          _index.properties as Properties object
	 * @param translationMapSpec
	 *          the specification of a translation map - could be name of a
	 *          _map.properties file, or something in _index properties ...
	 * @return the name of the translation map
	 */
	protected String loadTranslationMap(Properties indexProps, String translationMapSpec) {
		if (translationMapSpec.length() == 0) return null;

		String mapName = null;
		String mapKeyPrefix = null;
		if (translationMapSpec.startsWith("(") && translationMapSpec.endsWith(")")) {
			// translation map entries are in passed Properties object
			mapName = translationMapSpec.replaceAll("[\\(\\)]", "");
			mapKeyPrefix = mapName;
			loadTranslationMapValues(indexProps, mapName, mapKeyPrefix);
		} else {
			// translation map is a separate file
			String transMapFname = null;
			if (translationMapSpec.contains("(") && translationMapSpec.endsWith(")")) {
				String mapSpec[] = translationMapSpec.split("(//s|[()])+");
				transMapFname = mapSpec[0];
				mapName = mapSpec[1];
				mapKeyPrefix = mapName;
			} else {
				transMapFname = translationMapSpec;
				mapName = translationMapSpec.replaceAll(".properties", "");
				mapKeyPrefix = "";
			}

			if (findMap(mapName) == null) loadTranslationMapValues(transMapFname, mapName, mapKeyPrefix);
		}

		return mapName;
	}

	/**
	 * Get the appropriate Map object from populated transMapMap
	 * 
	 * @param mapName
	 *          the name of the translation map to find
	 * @return populated Map object
	 */
	public Map<String, String> findMap(String mapName) {
		if (mapName.startsWith("pattern_map:")) mapName = mapName.substring("pattern_map:".length());

		if (translationMaps.containsKey(mapName)) return (translationMaps.get(mapName));

		return null;
	}

	/**
	 * Load translation map into transMapMap. Look for translation map in site
	 * specific directory first; if not found, look in solrmarc top directory
	 * 
	 * @param transMapName
	 *          name of translation map file to load
	 * @param mapName
	 *          - the name of the Map to go in transMapMap (the key in
	 *          transMapMap)
	 * @param mapKeyPrefix
	 *          - any prefix on individual Map keys (entries in the value in
	 *          transMapMap)
	 */
	private void loadTranslationMapValues(String transMapName, String mapName, String mapKeyPrefix) {
		Properties props = null;
		props = Utils.loadProperties(new String[]{translationMapsFolder}, transMapName);
		logger.debug("Loading Custom Map: " + transMapName);
		loadTranslationMapValues(props, mapName, mapKeyPrefix);
	}

	/**
	 * populate transMapMap
	 * 
	 * @param transProps
	 *          - the translation map as a Properties object
	 * @param mapName
	 *          - the name of the Map to go in transMapMap (the key in
	 *          transMapMap)
	 * @param mapKeyPrefix
	 *          - any prefix on individual Map keys (entries in the value in
	 *          transMapMap)
	 */
	private void loadTranslationMapValues(Properties transProps, String mapName, String mapKeyPrefix) {
		Enumeration<?> en = transProps.propertyNames();
		while (en.hasMoreElements()) {
			String property = (String) en.nextElement();
			if (mapKeyPrefix.length() == 0 || property.startsWith(mapKeyPrefix)) {
				String mapKey = property.substring(mapKeyPrefix.length());
				if (mapKey.startsWith(".")) mapKey = mapKey.substring(1);
				String value = transProps.getProperty(property);
				value = value.trim();
				if (value.equals("null")) value = null;

				Map<String, String> valueMap;
				if (translationMaps.containsKey(mapName))
					valueMap = translationMaps.get(mapName);
				else {
					valueMap = new LinkedHashMap<String, String>();
					translationMaps.put(mapName, valueMap);
				}

				valueMap.put(mapKey, value);
			}
		}
	}

	/**
	 * Process the marc record and extract all fields from the raw marc record
	 * according to field rules.
	 * 
	 * @param marcRecord
	 * @param logger
	 * @return
	 */
	protected MarcRecordDetails mapMarcInfo(Record marcRecord, Logger logger) {
		MarcRecordDetails basicInfo = new MarcRecordDetails(this, marcRecord);
		return basicInfo;
	}

    public RecordStatus calculateRecordStatus(Record record, MarcRecordDetails marcInfo) {
        MarcIndexInfo marcFromIndex = null;
        if(record.getLeader().getRecordStatus()=='d') {
            //deleted record
            logger.debug("Record is deleted");
            return RecordStatus.RECORD_DELETED;

        } else if (marcIndexInfo.containsKey(marcInfo.getId())) {
            marcFromIndex = marcIndexInfo.get(marcInfo.getId());
            if (marcInfo.getChecksum() != marcFromIndex.getChecksum()){
                logger.debug("Record is changed - checksum");
                return RecordStatus.RECORD_CHANGED;
            } else if (marcInfo.isEContent() != marcFromIndex.isEContent()){
                logger.debug("Record is changed - econtent");
                return RecordStatus.RECORD_CHANGED;
            } else {
                logger.debug("Record is unchanged");
                return RecordStatus.RECORD_UNCHANGED;
            }
        } else {
            logger.debug("Record is new");
            return RecordStatus.RECORD_NEW;
        }
    }

	public Map<String, Method> getCustomMethodMap() {
		return customMethodMap;
	}

	/**
	 * First checks whether a given BeanShell script has been already loaded, and
	 * if so returns the BeanShell Interpreter created from that script. Is it
	 * hasn't been loaded this function will read in the named script file, create
	 * a new BeanShell Interpreter, and have that Interpreter process the named
	 * script.
	 * 
	 * @param scriptFileName
	 * @return
	 */
	public Interpreter getInterpreterForScript(String scriptFileName) {
		if (scriptMap.containsKey(scriptFileName)) {
			return (scriptMap.get(scriptFileName));
		}
		Interpreter bsh = new Interpreter();
		bsh.setClassLoader(this.getClass().getClassLoader());
		InputStream script = Utils.getPropertyFileInputStream(new String[]{scriptFilesFolder}, scriptFileName);
		String scriptContents;
		try {
			scriptContents = Utils.readStreamIntoString(script);
			bsh.eval(scriptContents);
		} catch (IOException e) {
			throw new IllegalArgumentException("Unable to read script: " + scriptFileName, e);
		} catch (EvalError e) {
			throw new IllegalArgumentException("Unable to evaluate script: " + scriptFileName, e);
		}
		scriptMap.put(scriptFileName, bsh);
		return (bsh);
	}

	public String getItemTag() {
		return itemTag;
	}

	public String getLocationSubfield() {
		return locationSubfield;
	}

	public String getUrlSubfield() {
		return urlSubfield;
	}

	public String getSharedEContentLocation() {
		return sharedEContentLocation;
	}

	public Long getLibrarySystemIdFromFacet(String librarySystemFacet) {
		return librarySystemFacets.get(librarySystemFacet);
	}
	
	public Long getLibraryIdForLink(String link){
		String lowerLink = link.toLowerCase();
		for (String curRule : eContentLinkRules.keySet()){
			if (lowerLink.matches(curRule)){
				return eContentLinkRules.get(curRule);
			}
		}
		return -1L;
	}

	public boolean isScrapeItemsForLinks() {
		return scrapeItemsForLinks;
	}
	
	private void suppressInActiveEContentRecords() {
		ArrayList<String> ids = new ArrayList<String>();
		try {
            //TODO this is shit. It can't possibly be working.
			PreparedStatement inActiveEContentRecordsStmt = econtentConn.prepareStatement(
				"SELECT id FROM econtent_record WHERE " +
				"status='active' AND source <> 'Freegal' AND source <> 'OverDriveAPI' AND ilsId IS NOT NULL AND ilsId <> ' ' AND ilsId <> '' " +
				"AND ilsId NOT IN(" + ActiveEcontentUtils.getCommaSeparatedString() + ")"
			);
			ResultSet inActiveEContentRecords = inActiveEContentRecordsStmt.executeQuery();
			while (inActiveEContentRecords.next()) {
				ids.add(inActiveEContentRecords.getString("id"));
			}
		} catch (SQLException e) {
			logger.error("Unable to get a list of inactive econtent records to suppresss.", e);
		}
		
		// get the number of econtent records in total 
		long eContentCount = 0;
		try {
			PreparedStatement countEContentRecordsStmt = econtentConn.prepareStatement("SELECT COUNT(*) FROM econtent_record WHERE status='active' AND source <> 'Freegal'");
			ResultSet countResult = countEContentRecordsStmt.executeQuery();
			if (countResult.next()) {
				eContentCount = countResult.getLong(1);
			}
		} catch (SQLException e) {
			logger.error("Unable get a total count of econtent records.", e);
		}
		
		// calculate the percentage of records to be suppressed, if it is lower
		// than the configured threshold, then we suppress them, otherwise don't do it
		long suppressCount = ids.size();
		double percentSuppressed = ((double) suppressCount / (double) eContentCount) * 100.00;

        //I have no idea what this crap is supposed to be doing
		/*String thresholdStr = configIni.get("Reindex", "eContentSuppressionThreshold");
		double threshold = (thresholdStr != null && thresholdStr.length() > 0) ? Double.parseDouble(thresholdStr) : 1.00;
		if (suppressCount > 0 && percentSuppressed < threshold) {
            logger.info("Number of eContent records to suppress is LOWER than threshold of " + threshold + "%. Proceeding with record suppression.");
			try {
				PreparedStatement suppressEContentRecordStmt = econtentConn.prepareStatement("UPDATE econtent_record SET status='deleted' WHERE id=?");
				for (String id : ids) {
					suppressEContentRecordStmt.setString(1, id);
                    suppressEContentRecordStmt.addBatch();
                    logger.debug("Setting status='deleted' for eContent record with id=" + id);
				}
                suppressEContentRecordStmt.executeBatch();
			} catch (SQLException e) {
				logger.error("Unable set status='deleted' for eContent record.", e);
			}
		} else {
            logger.info("There is no eContent record to suppress or the number of records to suppress is HIGHER than threshold of " + threshold + "%. SKIPPING record suppression.");
		}*/
	}
}
