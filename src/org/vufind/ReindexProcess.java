package org.vufind;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

import org.API.OverDrive.OverDriveAPIServices;
import org.API.OverDrive.OverDriveCollectionIterator;
import org.apache.commons.dbcp2.BasicDataSource;
import org.econtent.EContentIndexer;
import org.econtent.EContentRecordDAO;
import org.econtent.ExtractEContentFromMarc;
import org.econtent.FreegalImporter;
import org.econtent.PopulateOverDriveAPIItems;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.strands.StrandsProcessor;
import org.vufind.config.Config;

/**
 * Runs the nightly reindex process to update solr index based on the latest
 * export from the ILS.
 * 
 * Reindex process does the following steps: 1) Runs export process to extract
 * marc records from the ILS (if applicable)
 * 
 * @author Mark Noble <mnoble@turningleaftech.com>
 * 
 */
public class ReindexProcess {
    final static Logger logger = LoggerFactory.getLogger(ReindexProcess.class);
	/**
	 * Starts the reindexing process
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		// Get the configuration filename
		if (args.length == 0) {
			System.out
					.println("Please enter the server to index as the first parameter");
			System.exit(-1);
		}
		String confileFileLoc = args[0];
        Config config = initializeReindex(new File(confileFileLoc));

		// Runs the export process to extract marc records from the ILS (if
		// applicable)
		runExportScript();

		// Reload schemas
		/*if (config.shouldReloadDefaultSchema()) {
			reloadDefaultSchemas();
		}*/

		// Process all records (marc records, econtent that has been added to the
		// database, and resources)
		ArrayList<IRecordProcessor> recordProcessors = loadRecordProcesors();
		if (recordProcessors.size() > 0) {
			// Do processing of marc records with record processors loaded above.
			// Includes indexing records
			// Extracting eContent from records
			// Updating resource information
			// Saving records to strands - may need to move to resources if we are doing partial exports
			logger.info("START processMarcRecords");
			processMarcRecords(recordProcessors);
			logger.info("END processMarcRecords");

			// Import Freegal records into econtent database
			logger.info("START import Freegal");
			FreegalImporter freegalImporter = new FreegalImporter();
			if (freegalImporter.init(config)) {
				recordProcessors.add(freegalImporter);
				try {
				    freegalImporter.importRecords();
				} catch (RuntimeException e) {
					logger.error("Unknown error importing Freegal records.", e);
				}
			}
			logger.info("END import Freegal");

			// Import OverDrive records into econtent database
			logger.info("START import OverDrive");
			harvestOverDrive();
			logger.info("END import OverDrive");

			// Process eContent records that have been saved to the database.
			logger.info("START processEContentRecords");
			processEContentRecords(recordProcessors);
			logger.info("END processEContentRecords");

			// Do processing of resources as needed (for extraction of
			// resources).
			logger.info("START processResources");
			processResources(recordProcessors);
			logger.info("END processResources");

			for (IRecordProcessor processor : recordProcessors) {
				processor.finish();
			}
		}

		// Send completion information
		sendCompletionMessage(recordProcessors);

		logger.info("Finished Reindex");
	}

	private static void harvestOverDrive() {
		// Let get the OverDrive API items
		logger.info("Importing OverDrive API Items");

		String clientKey = configIni.get("OverDriveAPI", "clientKey");
		String clientSecret = configIni.get("OverDriveAPI", "clientSecret");
		int libraryId = new Integer(configIni.get("OverDriveAPI", "libraryId"));
		ProcessorResults pr = new ProcessorResults("OverDrive API Item", reindexLogId, vufindConn, logger);
		pr.addNote("The eContent Solr url is: " + "http://" + configIni.get("IndexShards", "eContent"));
		OverDriveCollectionIterator odci = new OverDriveCollectionIterator(clientKey, clientSecret, libraryId);
		OverDriveAPIServices overDriveAPIServices = new OverDriveAPIServices(clientKey, clientSecret, libraryId);

		PopulateOverDriveAPIItems service = new PopulateOverDriveAPIItems(odci, econtentConn, overDriveAPIServices);

		try {
			service.execute();
			pr.saveResults();
		} catch (SQLException e) {
			logger.error("Error importing OverDrive API Items.", e);
			pr.addNote("Error importing OverDrive API Items. " + e.getMessage());
		}
	}

	private static void reloadDefaultSchemas() {
		logger.info("Reloading schemas from default");
		// biblio
		reloadSchema("biblio");
		// biblio2
		reloadSchema("biblio2");
		// econtent
		reloadSchema("econtent");

	}

	private static void reloadSchema(String schemaName) {
		boolean reloadIndex = true;
		try {
			logger.debug("Copying " + "../../sites/default/solr/" + schemaName
					+ "/conf/schema.xml" + " to " + "../../sites/" + serverName
					+ "/solr/" + schemaName + "/conf/schema.xml");
			if (!Util.copyFile(new File("../../sites/default/solr/"
					+ schemaName + "/conf/schema.xml"), new File("../../sites/"
					+ serverName + "/solr/" + schemaName + "/conf/schema.xml"))) {
				logger.info("Unable to copy schema for " + schemaName);
				reloadIndex = false;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("error reloading default schema for " + schemaName, e);
			reloadIndex = false;
		}
		if (reloadIndex) {
			URLPostResponse response = Util.getURL("http://"+solrHost+":"
                    + solrPort + "/solr/admin/cores?action=RELOAD&core="
					+ schemaName, logger);
			if (!response.isSuccess()) {
				logger.error("Error reloading default schema for " + schemaName
						+ " " + response.getMessage());
			}
		}
	}

	private static ArrayList<IRecordProcessor> loadRecordProcesors() {
		ArrayList<IRecordProcessor> supplementalProcessors = new ArrayList<IRecordProcessor>();
		if (updateSolr) {
			MarcIndexer marcIndexer = new MarcIndexer();
			if (marcIndexer.init(configIni, serverName, reindexLogId,
					vufindConn, econtentConn, logger)) {
				supplementalProcessors.add(marcIndexer);
			} else {
				logger.error("Could not initialize marcIndexer");
				System.exit(1);
			}
		}
		if (updateResources) {
			UpdateResourceInformation resourceUpdater = new UpdateResourceInformation();
			if (resourceUpdater.init(configIni, serverName, reindexLogId,
					vufindConn, econtentConn, logger)) {
				supplementalProcessors.add(resourceUpdater);
			} else {
				logger.error("Could not initialize resourceUpdater");
				System.exit(1);
			}
		}
		if (loadEContentFromMarc) {
			ExtractEContentFromMarc econtentExtractor = new ExtractEContentFromMarc();
			if (econtentExtractor.init(configIni, serverName, reindexLogId,
					vufindConn, econtentConn)) {
				supplementalProcessors.add(econtentExtractor);
			} else {
				logger.error("Could not initialize econtentExtractor");
				System.exit(1);
			}
		}
		if (exportStrandsCatalog) {
			StrandsProcessor strandsProcessor = new StrandsProcessor();
			if (strandsProcessor.init(configIni, serverName, reindexLogId,
					vufindConn, econtentConn, logger)) {
				supplementalProcessors.add(strandsProcessor);
			} else {
				logger.error("Could not initialize strandsProcessor");
				System.exit(1);
			}
		}
		if (updateAlphaBrowse) {
			AlphaBrowseProcessor alphaBrowseProcessor = new AlphaBrowseProcessor();
			if (alphaBrowseProcessor.init(configIni, serverName, reindexLogId,
					vufindConn, econtentConn, logger)) {
				supplementalProcessors.add(alphaBrowseProcessor);
			} else {
				logger.error("Could not initialize strandsProcessor");
				System.exit(1);
			}
		}
		if (exportOPDSCatalog) {
			// 14) Generate OPDS catalog
		}
		if (updateSolr) {
			EContentIndexer eContentIndexer = new EContentIndexer();
			if (eContentIndexer.init(configIni, serverName, reindexLogId,
					vufindConn, econtentConn, logger)) {
				supplementalProcessors.add(eContentIndexer);
			} else {
				logger.error("Could not initialize EContentIndexer");
				System.exit(1);
			}
		}
		return supplementalProcessors;
	}

	private static void processResources(
			ArrayList<IRecordProcessor> supplementalProcessors) {
		ArrayList<IResourceProcessor> resourceProcessors = new ArrayList<IResourceProcessor>();
		for (IRecordProcessor processor : supplementalProcessors) {
			if (processor instanceof IResourceProcessor) {
				resourceProcessors.add((IResourceProcessor) processor);
			}
		}
		if (resourceProcessors.size() == 0) {
			return;
		}

		logger.info("Processing resources");
		try {
			long batchCount = 0;
			PreparedStatement resourceCountStmt = vufindConn.prepareStatement(
					"SELECT count(id) FROM resource",
					ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			ResultSet resourceCountRs = resourceCountStmt.executeQuery();
			if (resourceCountRs.next()) {
				long numResources = resourceCountRs.getLong(1);
				logger.info("There are " + numResources
						+ " resources currently loaded");
				long firstResourceToProcess = 0;
				long batchSize = 100000;
				PreparedStatement allResourcesStmt = vufindConn
						.prepareStatement("SELECT * FROM resource LIMIT ?, ?",
								ResultSet.TYPE_FORWARD_ONLY,
								ResultSet.CONCUR_READ_ONLY);
				while (firstResourceToProcess <= numResources) {
					logger.debug("processing batch " + ++batchCount + " from "
							+ firstResourceToProcess + " to "
							+ (firstResourceToProcess + batchSize));
					allResourcesStmt.setLong(1, firstResourceToProcess);
					allResourcesStmt.setLong(2, batchSize);
					ResultSet allResources = allResourcesStmt.executeQuery();
					while (allResources.next()) {
						for (IResourceProcessor resourceProcessor : resourceProcessors) {
							resourceProcessor.processResource(allResources);
						}
					}
					allResources.close();
					firstResourceToProcess += batchSize;
				}
			}
		} catch (Exception e) {
			logger.error("Exception processing resources", e);
			System.out
					.println("Exception processing resources " + e.toString());
		} catch (Error e) {
			logger.error("Error processing resources", e);
			System.out.println("Error processing resources " + e.toString());
		}
	}

	private static void processEContentRecords(
			ArrayList<IRecordProcessor> supplementalProcessors) {
		logger.info("Processing econtent records");
		ArrayList<IEContentProcessor> econtentProcessors = new ArrayList<IEContentProcessor>();
		for (IRecordProcessor processor : supplementalProcessors) {
			if (processor instanceof IEContentProcessor) {
				econtentProcessors.add((IEContentProcessor) processor);
			}
		}
		if (econtentProcessors.size() == 0) {
			return;
		}
		// Get all "active" records in econtent_record table and process them
		// one-by-one
		try {
			PreparedStatement econtentRecordStatement = econtentConn
					.prepareStatement("SELECT * FROM econtent_record WHERE status = 'active'");
			ResultSet allEContent = econtentRecordStatement.executeQuery();
			while (allEContent.next()) {
				for (IEContentProcessor econtentProcessor : econtentProcessors) {
					econtentProcessor.processEContentRecord(allEContent);
				}
			}
		} catch (SQLException ex) {
			// handle any errors
			logger.error("Unable to load econtent records from database", ex);
		}
	}

	private static void processMarcRecords(
			ArrayList<IRecordProcessor> supplementalProcessors) {
		ArrayList<IMarcRecordProcessor> marcProcessors = new ArrayList<IMarcRecordProcessor>();
		for (IRecordProcessor processor : supplementalProcessors) {
			if (processor instanceof IMarcRecordProcessor) {
				marcProcessors.add((IMarcRecordProcessor) processor);
			}
		}
		if (marcProcessors.size() == 0) {
			return;
		}

		MarcProcessor marcProcessor = new MarcProcessor();
		marcProcessor.init(serverName, configIni, vufindConn, econtentConn,
                reindexLogId);

		if (supplementalProcessors.size() > 0) {
			logger.info("Processing exported marc records");
			marcProcessor.processMarcFiles(marcProcessors, logger);
		}
	}

	private static void runExportScript() {
		String extractScript = configIni.get("Reindex", "extractScript");
		if (extractScript.length() > 0) {
			logger.info("Running export script");
			try {
				String reindexResult = SystemUtil.executeCommand(extractScript,
						logger);
				logger.info("Result of extractScript (" + extractScript
						+ ") was " + reindexResult);
			} catch (IOException e) {
				logger.error(
						"Error running extract script, stopping reindex process",
						e);
				System.exit(1);
			}
		}
	}

	private static Config initializeReindex(File configFile) {

        Config config = new Config();
        config.setStartTime(new DateTime());
        // Parse the configuration file
        Ini configIni = loadConfigFile(configFile);

		// Delete the existing reindex.log file
        String solrmarcLogLoc = configIni.get("LOG", "solrmarcLog");
		File solrmarcLog = new File(solrmarcLogLoc);
		if (solrmarcLog.exists()) {
			solrmarcLog.delete();
		}
		for (int i = 1; i <= 10; i++) {
			File solrmarcLogExtended = new File(solrmarcLogLoc + "." + i);
			if (solrmarcLogExtended.exists()) {
                solrmarcLogExtended.delete();
			}
		}
		// Initialize the logger
		File log4jFile = new File(configIni.get("Log", "log4j.reindex.properties"));
		if (log4jFile.exists()) {
			PropertyConfigurator.configure(log4jFile.getAbsolutePath());
		} else {
			System.out.println("Could not find log4j configuration "
					+ log4jFile.getAbsolutePath());
			System.exit(-1);
		}

        logger.info("Running index using config: " + configFile.getAbsolutePath());

        String baseURL = configIni.get("Solr", "baseURL");
		if (baseURL == null || baseURL.length() == 0) {
            logger.error("Solr baseURL not found in configuration file");
			System.exit(-1);
		}

        boolean shouldReloadDefaultSchema = false;
		String reloadDefaultSchemaStr = configIni.get("Reindex", "shouldReloadDefaultSchema");
		if (reloadDefaultSchemaStr != null) {
            shouldReloadDefaultSchema = Boolean.parseBoolean(reloadDefaultSchemaStr);
		}
		String updateSolrStr = configIni.get("Reindex", "shouldUpdateSolr");
		if (updateSolrStr != null) {
            config.setShouldUpdateSolr(Boolean.parseBoolean(updateSolrStr));
		}
		String updateResourcesStr = configIni.get("Reindex", "shouldUpdateResources");
		if (updateResourcesStr != null) {
            config.setShouldUpdateResources(Boolean.parseBoolean(updateResourcesStr));
		}
		String exportStrandsCatalogStr = configIni.get("Reindex", "shouldExportStrandsCatalog");
		if (exportStrandsCatalogStr != null) {
            config.setShouldExportStrandsCatalog(Boolean.parseBoolean(exportStrandsCatalogStr));
		}
		String exportOPDSCatalogStr = configIni.get("Reindex", "shouldExportOPDSCatalog");
		if (exportOPDSCatalogStr != null) {
            config.setShouldExportOPDSCatalog(Boolean.parseBoolean(exportOPDSCatalogStr));
		}
		String loadEContentFromMarcStr = configIni.get("Reindex", "shouldLoadEContentFromMarc");
		if (loadEContentFromMarcStr != null) {
            config.setShouldLoadEContentFromMarc(Boolean.parseBoolean(loadEContentFromMarcStr));
		}
		String updateAlphaBrowseStr = configIni.get("Reindex", "shouldUpdateAlphaBrowse");
		if (updateAlphaBrowseStr != null) {
            config.setShouldUpdateAlphaBrowse(Boolean.parseBoolean(updateAlphaBrowseStr));
		}

        // Setup connections to vufind and econtent databases
        String driverClassName = Util.cleanIniValue(configIni.get("Database", "vufindDBDriver"));
        String username = configIni.get("Database", "vufindDBusername");
        String password = configIni.get("Database", "vufindDBpassword");
        String connectionURL = Util.cleanIniValue(configIni.get("Database", "vufindDBURL"));

        BasicDataSource vufindDataSource = new BasicDataSource();
        vufindDataSource.setDriverClassName(driverClassName);
        vufindDataSource.setUsername(username);
        vufindDataSource.setPassword(password);
        vufindDataSource.setUrl(connectionURL);
        config.setVufindDatasource(vufindDataSource);

        driverClassName = Util.cleanIniValue(configIni.get("Database", "econtentDBDriver"));
        username = configIni.get("Database", "econtentDBusername");
        password = configIni.get("Database", "econtentDBpassword");
        connectionURL = Util.cleanIniValue(configIni.get("Database", "econtentDBURL"));

        BasicDataSource econtentDataSource = new BasicDataSource();
        econtentDataSource.setDriverClassName(driverClassName);
        econtentDataSource.setUsername(username);
        econtentDataSource.setPassword(password);
        econtentDataSource.setUrl(connectionURL);
        config.setEcontentDatasource(econtentDataSource);

        config.setEContentRecordDAO(new EContentRecordDAO(econtentDataSource));

		// Start a reindex log entry
		try {
            Connection connection = vufindDataSource.getConnection();
			logger.info("Creating log entry for index");
            PreparedStatement createLogEntryStatement = connection.prepareStatement(
                    "INSERT INTO reindex_log (startTime) SET startTime = CURRENT_TIMESTAMP",
                    PreparedStatement.RETURN_GENERATED_KEYS);
			createLogEntryStatement.executeUpdate();
			ResultSet generatedKeys = createLogEntryStatement
					.getGeneratedKeys();
            long reindexLogId = -1;
			if (generatedKeys.next()) {
				config.setReindexLogId(generatedKeys.getLong(1));
			}
		} catch (SQLException e) {
			logger.error("Unable to create log entry for reindex process", e);
			System.exit(0);
		}

        //Freegal
        
        String freegalUrl = configIni.get("Freegal", "freegalUrl");
        if (!(freegalUrl == null || freegalUrl.length() == 0)) {
            config.setFreegalUrl(freegalUrl);

            String freegalUser = configIni.get("Freegal", "freegalUser");
            if (freegalUser == null || freegalUser.length() == 0) {
                logger.error("Freegal User not found.  Please specify the barcode of a patron to use while loading freegal information in the freegalUser key.");
                System.exit(0);
            }
            config.setFreegalUser(freegalUser);

            String freegalPIN = configIni.get("Freegal", "freegalPIN");
            if (freegalPIN == null || freegalPIN.length() == 0) {
                logger.error("Freegal PIN not found in.  Please specify the PIN of a patron to use while loading freegal information in the freegalPIN key.");
                System.exit(0);
            }
            config.setFreegalPIN(freegalPIN);

            String freegalAPIkey = configIni.get("Freegal", "freegalAPIkey");
            if (freegalAPIkey == null || freegalAPIkey.length() == 0) {
                logger.error("Freegal API Key not found.  Please specify the API Key for the Freegal webservices in the freegalAPIkey key.");
                System.exit(0);
            }
            config.setFreegalAPIkey(freegalAPIkey);

            String libraryId = configIni.get("Freegal", "libraryId");
            if (libraryId == null || libraryId.length() == 0) {
                logger.error("Freegal Library Id not found.  Please specify the Library for the Freegal webservices the libraryId key.");
                System.exit(0);
            }
            config.setFreegalLibraryId(libraryId);
        } else {

            logger.error("Freegal API URL not found.  Please specify url in freegalUrl key.");
            //System.exit(0); Don't quit without Freegal. We might not intend to index
        }

        //EContent
        String fullTextPath = configIni.get("EContent", "fullTextPath");
        if(fullTextPath != null) {
            config.setEcontentFullTextPath(fullTextPath);
        }

        return config;
	}

	private static void sendCompletionMessage(
			ArrayList<IRecordProcessor> recordProcessors) {
		logger.info("Reindex Results");
		logger.info("Processor, Records Processed, eContent Processed, Resources Processed, Errors, Added, Updated, Deleted, Skipped");
		for (IRecordProcessor curProcessor : recordProcessors) {
			ProcessorResults results = curProcessor.getResults();
			logger.info(results.toCsv());
		}
		long elapsedTime = endTime - startTime;
		float elapsedMinutes = (float) elapsedTime / (float) (60000);
		logger.info("Time elpased: " + elapsedMinutes + " minutes");

		try {
			PreparedStatement finishedStatement = vufindConn
					.prepareStatement("UPDATE reindex_log SET endTime = ? WHERE id = ?");
			finishedStatement.setLong(1, new Date().getTime() / 1000);
			finishedStatement.setLong(2, reindexLogId);
			finishedStatement.executeUpdate();
		} catch (SQLException e) {
			logger.error("Unable to update reindex log with completion time.",
					e);
		}
	}

	private static Ini loadConfigFile(File configFile) {
		// Parse the configuration file
		Ini ini = new Ini();
		try {
			ini.load(new FileReader(configFile));
		} catch (InvalidFileFormatException e) {
            System.exit(-1);
		} catch (FileNotFoundException e) {
            System.exit(-1);
		} catch (IOException e) {
            System.exit(-1);
		}

		return ini;
	}
}
