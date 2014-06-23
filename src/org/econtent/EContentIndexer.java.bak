package org.econtent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.API.OverDrive.OverDriveAPIServices;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.LukeResponse.FieldInfo;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.ini4j.Ini;
import org.vufind.IEContentProcessor;
import org.vufind.IRecordProcessor;
import org.vufind.ProcessorResults;
import org.vufind.URLPostResponse;
import org.vufind.Util;

public class EContentIndexer implements IRecordProcessor, IEContentProcessor {
	private String solrPort;
	private String solrHost;
	private Logger logger;
	private ProcessorResults results;
	private ConcurrentUpdateSolrServer solrServer;
	private List<String> solrFields;
	private Properties collectionGroupMap;
	private Properties itemTypeFormatMap;
	private Properties deviceCompatibilityMap;
	private OverDriveAPIServices overDriveAPIServices;
	private String fullTextPath;
	private boolean finished = false;

	@Override
	public boolean processEContentRecord(ResultSet rs) {
		// increment number of records processed.
		results.incEContentRecordsProcessed();

		EContentRecord record;
		try {
			record = new EContentRecord(rs);
		} catch (SQLException e) {
			logger.error("Error instantiating EContentRecord from ResultSet.",
					e);
			results.addNote("Error instantiating EContentRecord from ResultSet.");
			results.incErrors();
			return false;
		}

		Object id = record.get("id");
		logger.info("Indexing econtent record: " + id);

		// get record as a solr document
		SolrInputDocument doc = record.getSolrInputDocument(solrFields,
				collectionGroupMap, itemTypeFormatMap, deviceCompatibilityMap,
				overDriveAPIServices, fullTextPath);

		// Add document to index
		try {
			UpdateResponse response = solrServer.add(doc);
			if (response.getStatus() != 0) {
				logger.error("Failed to add econtent record to solr: " + id);
				results.addNote("Failed to add econtent record to solr: " + id);
				results.incErrors();
				return false;
			}
		} catch (Exception e) {
			logger.error("Failed to add econtent record to solr: " + id, e);
			results.addNote("Failed to add econtent record to solr: " + id
					+ ". " + e.getMessage());
			results.incErrors();
			return false;
		}

		// If we got here, the record has been added to solr index
		results.incAdded();
		return true;
	}

	@Override
	public boolean init(Ini configIni, String serverName, long reindexLogId,
			Connection vufindConn, Connection econtentConn, Logger logger) {

		this.logger = logger;
		results = new ProcessorResults("Update Solr eContent", reindexLogId,
				vufindConn, logger);

		fullTextPath = configIni.get("EContent", "fullTextPath");
		if (fullTextPath == null || fullTextPath.length() == 0) {
			logger.error("fullTextPath not set in config.ini");
			results.addNote("fullTextPath not set in config.ini");
			return false;
		}

		// Load Collection Group map
		collectionGroupMap = new Properties();
		File file = new File(configIni.get("Site", "local") + "/../../sites/"
				+ serverName
				+ "/translation_maps/collection_group_map.properties");
		logger.info("Trying to load collection group map from "
				+ file.getAbsolutePath());
		try {
			collectionGroupMap.load(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			logger.error("File not found: " + file.getAbsolutePath(), e);
			results.addNote("File not found: " + file.getAbsolutePath());
			return false;
		} catch (IOException e) {
			logger.error("Error loading file: " + file.getAbsolutePath(), e);
			results.addNote("Error loading file: " + file.getAbsolutePath());
			return false;
		}

		// Load item type to format map
		itemTypeFormatMap = new Properties();
		file = new File(configIni.get("Site", "local") + "/lang/en.ini");
		logger.info("Trying to load item type to format map from "
				+ file.getAbsolutePath());
		try {
			loadIniFileAsProperties(file, itemTypeFormatMap);
		} catch (FileNotFoundException e) {
			logger.error("File not found: " + file.getAbsolutePath(), e);
			results.addNote("File not found: " + file.getAbsolutePath());
			return false;
		} catch (IOException e) {
			logger.error("Error loading file: " + file.getAbsolutePath(), e);
			results.addNote("Error loading file: " + file.getAbsolutePath());
			return false;
		}

		// Load device compatibility map
		deviceCompatibilityMap = new Properties();
		file = new File(configIni.get("Site", "local") + "/../../sites/"
				+ serverName + "/conf/device_compatibility_map.ini");
		logger.info("Trying to load device compatibility map from "
				+ file.getAbsolutePath());
		try {
			loadIniFileAsProperties(file, deviceCompatibilityMap);
			for (Object format : deviceCompatibilityMap.keySet()) {
				String cvsValues = (String) deviceCompatibilityMap.get(format);
				List<String> devices = Arrays.asList(cvsValues.split(","));
				logger.debug("Device compatibility map: " + format + " => "
						+ devices);
				deviceCompatibilityMap.put(format, devices);
			}
		} catch (FileNotFoundException e) {
			logger.error("File not found: " + file.getAbsolutePath(), e);
			results.addNote("File not found: " + file.getAbsolutePath());
			return false;
		} catch (IOException e) {
			logger.error("Error loading file: " + file.getAbsolutePath(), e);
			results.addNote("Error loading file: " + file.getAbsolutePath());
			return false;
		}

		// get SOLR host/port configuration
		solrPort = configIni.get("Reindex", "solrPort");
		solrHost = configIni.get("Reindex", "solrHost");
		if (solrHost == null || solrHost.length() == 0) {
			solrHost = "localhost";
		}

		// Initialize the solrServer
		try {
			solrServer = new ConcurrentUpdateSolrServer("http://" + solrHost
					+ ":" + solrPort + "/solr/" + "econtent2", 1024, 10);
		} catch (MalformedURLException e) {
			logger.error("Unable to instantiate ConcurrentUpdateSolrServer.", e);
			results.addNote("Unable to instantiate ConcurrentUpdateSolrServer.");
			return false;
		}

		// Get all solr field names
		try {
			solrFields = readFieldNames();
		} catch (Exception e) {
			logger.error("Unable to read field names from SOLR server.", e);
			results.addNote("Unable to read field names from SOLR server.");
			return false;
		}

		// Delete existing records from Solr index.
		try {
			solrServer.deleteByQuery("*:*");
			solrServer.commit();
		} catch (Exception e) {
			logger.error("Unable to delete existing records from SOLR index.",
					e);
			results.addNote("Unable to delete existing records from SOLR index.");
			return false;
		}

		// Initialize OverDrive API service
		String clientKey = configIni.get("OverDriveAPI", "clientKey");
		String clientSecret = configIni.get("OverDriveAPI", "clientSecret");
		int libraryId = new Integer(configIni.get("OverDriveAPI", "libraryId"));
		overDriveAPIServices = new OverDriveAPIServices(clientKey,
				clientSecret, libraryId);

		return true;
	}

	@Override
	public void finish() {
		if (!finished) {
			boolean okToSwapCores = true;
			try {
				results.addNote("calling final commit on index econtent2");
				solrServer.commit();
			} catch (Exception e) {
				logger.error("Error committing changes to index econtent2", e);
				results.addNote("Error committing changes to index econtent2. "
						+ e.getMessage());
				okToSwapCores = false;
			}
			try {
				results.addNote("optimizing index econtent2");
				solrServer.optimize();
			} catch (Exception e) {
				logger.error("Error optimizing index econtent2.", e);
				results.addNote("Error optimizing index econtent2. "
						+ e.getMessage());
				okToSwapCores = false;
			}

			if (okToSwapCores && indexIsOk()) {
				results.addNote("index passed checks, swapping cores econtent, and econtent2 so new index is active.");
				logger.info("index passed checks, swapping cores econtent, and econtent2 so new index is active.");
				swapCores("econtent", "econtent2");
			} else {
				results.addNote("index econtent2 did not pass check, not swapping");
				logger.info("index econtent2 did not pass check, not swapping");
			}
			results.saveResults();
			finished = true;
		}
	}

	@Override
	public ProcessorResults getResults() {
		return results;
	}

	private List<String> readFieldNames() throws SolrServerException,
			IOException {
		final LukeRequest request = new LukeRequest();
		request.setShowSchema(true);
		request.setNumTerms(0);
		final LukeResponse response = request.process(solrServer);
		final Map<String, FieldInfo> fields = response.getFieldInfo();
		return new ArrayList<String>(fields.keySet());
	}

	private boolean swapCores(String core1, String core2) {
		logger.info("Swapping cores: " + core1 + ", " + core2);
		URLPostResponse response = Util.getURL("http://" + solrHost + ":"
				+ solrPort + "/solr/admin/cores?action=SWAP&core=" + core1
				+ "&other=" + core2, logger);
		if (!response.isSuccess()) {
			logger.error("Error while attempting to SWAP cores: " + core1
					+ ", and " + core2 + ". Solr response:"
					+ response.getMessage());
			results.addNote("Error while attempting to SWAP cores: " + core1
					+ ", and " + core2 + ". Solr response:"
					+ response.getMessage());
		} else {
			results.addNote("Cores " + core1 + ", and " + core2
					+ " are swapped successfully.");
		}
		return response.isSuccess();
	}

	private boolean indexIsOk() {
		// Do not pass the import if more than 1% of the records have errors
		if (results.getNumErrors() > results.getRecordsProcessed() * .01) {
			return false;
		} else {
			return true;
		}
	}

	private void loadIniFileAsProperties(File file, Properties properties)
			throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		while ((line = reader.readLine()) != null) {
			// skip comment lines
			if (line.trim().charAt(0) == ';') {
				continue;
			}
			String[] parts = line.split("=");
			if (parts.length == 2) {
				properties.put(parts[0].trim(), parts[1].trim());
			} else if (parts.length > 2) {
				reader.close();
				throw new IOException(
						"Error parsing ini file. There appears to be more than one equal signs.");
			}
		}
		reader.close();
	}
}
