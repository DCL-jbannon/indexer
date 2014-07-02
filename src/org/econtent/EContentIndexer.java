package org.econtent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.API.OverDrive.OverDriveAPIServices;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.LukeResponse.FieldInfo;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.*;
import org.vufind.config.Config;

public class EContentIndexer implements IRecordProcessor, IEContentProcessor {
    final static Logger logger = LoggerFactory.getLogger(EContentIndexer.class);

    private Config config;


	private Properties collectionGroupMap;
	private Properties itemTypeFormatMap;
	private Properties deviceCompatibilityMap;
	private OverDriveAPIServices overDriveAPIServices;
	private String fullTextPath;
	private boolean finished = false;

    private List<String> solrFields = null;

	@Override
	public boolean processEContentRecord(String indexName, ResultSet rs) {

		EContentRecord record;
		try {
			record = new EContentRecord(config.getEContentRecordDAO(), rs);
		} catch (SQLException e) {
			logger.error("Error instantiating EContentRecord from ResultSet.", e);
			return false;
		}

		Object id = record.get("id");
        logger.info("Indexing econtent record: " + id);

        ConcurrentUpdateSolrServer solrServer = config.getSolrUpdateServer(indexName);
        if(solrFields==null){
            try {
                solrFields = readFieldNames(solrServer);
            } catch (SolrServerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
		// get record as a solr document
		SolrInputDocument doc = record.getSolrInputDocument(solrFields,
				collectionGroupMap, itemTypeFormatMap, deviceCompatibilityMap,
				overDriveAPIServices, fullTextPath);

		// Add document to index
		try {
			UpdateResponse response = solrServer.add(doc);
			if (response.getStatus() != 0) {
				logger.error("Failed to add econtent record to solr: " + id);
				return false;
			}
		} catch (Exception e) {
			logger.error("Failed to add econtent record to solr: " + id, e);
			return false;
		}

		return true;
	}

	@Override
	public boolean init(Config config) {

		fullTextPath = config.getEcontentFileFolder();

		// Load Collection Group map
		collectionGroupMap = new Properties();

		File file = new File(config.getEcontentCollectionGroupMapPath());
		logger.info("Trying to load collection group map from "+ file.getAbsolutePath());
		try {
			collectionGroupMap.load(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			logger.error("File not found: " + file.getAbsolutePath(), e);
			return false;
		} catch (IOException e) {
			logger.error("Error loading file: " + file.getAbsolutePath(), e);
			return false;
		}

		// Load item type to format map
		itemTypeFormatMap = new Properties();

		file = new File(config.getEcontentItemTypeFormatMapPath());
		try {
			loadIniFileAsProperties(file, itemTypeFormatMap);
		} catch (FileNotFoundException e) {
			logger.error("File not found: " + file.getAbsolutePath(), e);
			return false;
		} catch (IOException e) {
			logger.error("Error loading file: " + file.getAbsolutePath(), e);
			return false;
		}

		// Load device compatibility map

		deviceCompatibilityMap = new Properties();
		file = new File(config.getEcontentDeviceCompatibilityMapPath());
		logger.info("Trying to load device compatibility map from "+ file.getAbsolutePath());
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
			return false;
		} catch (IOException e) {
			logger.error("Error loading file: " + file.getAbsolutePath(), e);
			return false;
		};

        //TODO why the hell is this here?
		// Initialize OverDrive API service
		/*String clientKey = configIni.get("OverDriveAPI", "clientKey");
		String clientSecret = configIni.get("OverDriveAPI", "clientSecret");
		int libraryId = new Integer(configIni.get("OverDriveAPI", "libraryId"));
		overDriveAPIServices = new OverDriveAPIServices(clientKey,
				clientSecret, libraryId);*/

		return true;
	}

	@Override
	public void finish() {

	}

	private List<String> readFieldNames(ConcurrentUpdateSolrServer solrServer) throws SolrServerException, IOException {
		final LukeRequest request = new LukeRequest();
		request.setShowSchema(true);
		request.setNumTerms(0);
		final LukeResponse response = request.process(solrServer);
		final Map<String, FieldInfo> fields = response.getFieldInfo();
		return new ArrayList<String>(fields.keySet());
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
