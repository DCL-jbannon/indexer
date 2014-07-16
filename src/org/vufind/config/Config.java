package org.vufind.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.vufind.econtent.EContentRecordDAO;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by jbannon on 6/27/14.
 */
public class Config {
    final static Logger logger = LoggerFactory.getLogger(Config.class);

    public Config(File configIniFile) {
        init(configIniFile);
    }

    // Database connections and prepared statements
    private BasicDataSource vufindDatasource = null;

    //Basic
    private String vufindUrl = null;

    //Reindex
    private boolean shouldRemoveRecordsNotInMarc;
    //Information about how to process call numbers for local browse
    private String itemTag;
    private String callNumberSubfield;
    private String locationSubfield;

    //Freegal
    private String freegalUrl = null;
    private String freegalUser = null;
    private String freegalPIN = null;
    private String freegalAPIkey = null;
    private String freegalLibraryId = null;

    //Marc
    private boolean shouldClearMarcRecordsAtStartOfIndex;
    private boolean shouldReindexUnchangedRecords;


    //Solr
    private String baseSolrURL;
    private String realSolrMarcCore;
    private String tempSolrMarcCore;
    private String realSolrEContentCore;
    private String tempSolrEContentCore;

    //EContent
    private String econtentFileFolder = null;
    private String econtentCollectionGroupMapPath = null;
    private String econtentItemTypeFormatMapPath = null;
    private String econtentDeviceCompatibilityMapPath = null;
    private BasicDataSource econtentDatasource = null;
    private EContentRecordDAO eContentRecordDAO = null;
    private boolean econtentShouldCheckOverDriveAvailability = false;

    //Strands
    private String strandsBookcoverUrl = null;
    private String strandsCatalogFile = null;

    private Ini loadIni(File file) {
        Ini ini = new Ini();
        try {
            ini.load(new FileReader(file));
        } catch (InvalidFileFormatException e) {
            System.exit(-1);
        } catch (FileNotFoundException e) {
            System.exit(-1);
        } catch (IOException e) {
            System.exit(-1);
        }

        return ini;
    }

    public void init(File configFile) {
        // Parse the configuration file
        Ini configIni = loadIni(configFile);

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


        logger.info("Running index using config: " + configFile.getAbsolutePath());


        vufindUrl = configIni.get("Basic", "vufindURL"); assert(vufindUrl != null);

        baseSolrURL = configIni.get("Solr", "baseURL"); assert(baseSolrURL != null);

        //Reindex
        String removeTitlesNotInMarcExportVal = configIni.get("Reindex", "removeTitlesNotInMarcExport");  assert(removeTitlesNotInMarcExportVal != null);
        shouldRemoveRecordsNotInMarc = Boolean.parseBoolean(removeTitlesNotInMarcExportVal);

        itemTag = configIni.get("Reindex", "itemTag"); assert(itemTag != null);
        callNumberSubfield = configIni.get("Reindex", "callNumberSubfield"); assert(callNumberSubfield != null);
        locationSubfield = configIni.get("Reindex", "locationSubfield"); assert(locationSubfield != null);

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
        this.vufindDatasource = vufindDataSource;

        driverClassName = Util.cleanIniValue(configIni.get("Database", "econtentDBDriver"));
        username = configIni.get("Database", "econtentDBusername");
        password = configIni.get("Database", "econtentDBpassword");
        connectionURL = Util.cleanIniValue(configIni.get("Database", "econtentDBURL"));

        BasicDataSource econtentDataSource = new BasicDataSource();
        econtentDataSource.setDriverClassName(driverClassName);
        econtentDataSource.setUsername(username);
        econtentDataSource.setPassword(password);
        econtentDataSource.setUrl(connectionURL);
        this.econtentDatasource = econtentDataSource;
        this.eContentRecordDAO = new EContentRecordDAO(econtentDataSource);

        //Freegal

        freegalUrl = configIni.get("Freegal", "freegalUrl"); assert(freegalUrl != null);
        freegalUser = configIni.get("Freegal", "freegalUser"); assert(freegalUser != null);
        freegalPIN = configIni.get("Freegal", "freegalPIN"); assert(freegalPIN != null);
        freegalAPIkey = configIni.get("Freegal", "freegalAPIkey"); assert(freegalAPIkey != null);
        freegalLibraryId = configIni.get("Freegal", "libraryId"); assert(freegalLibraryId != null);

        //EContent
        econtentFileFolder = configIni.get("EContent", "fullTextPath");
        econtentCollectionGroupMapPath = configIni.get("EContent", "collectionGroupMapPath");
        econtentItemTypeFormatMapPath = configIni.get("EContent", "itemTypeFormatMapPath");
        econtentDeviceCompatibilityMapPath = configIni.get("EContent", "deviceCompatibilityMapPath");

        String econtentCheckOverDriveAvailabilityVal = configIni.get("EContent", "econtentCheckOverDriveAvailabilityVal"); assert(econtentCheckOverDriveAvailabilityVal != null);
        econtentShouldCheckOverDriveAvailability = Boolean.parseBoolean(econtentCheckOverDriveAvailabilityVal);

        //Strands
        strandsBookcoverUrl = configIni.get("Strands", "url"); assert(strandsBookcoverUrl != null);
        vufindUrl = configIni.get("Strands", "url"); assert(vufindUrl != null);
    }

    public BasicDataSource getVufindDatasource() {
        return vufindDatasource;
    }

    public String getFreegalUrl() {
        return freegalUrl;
    }

    public String getFreegalUser() {
        return freegalUser;
    }

    public String getFreegalPIN() {
        return freegalPIN;
    }

    public String getFreegalAPIkey() {
        return freegalAPIkey;
    }

    public String getFreegalLibraryId() {
        return freegalLibraryId;
    }

    public boolean shouldClearMarcRecordsAtStartOfIndex() {
        return shouldClearMarcRecordsAtStartOfIndex;
    }

    public boolean shouldReindexUnchangedRecords() {
        return shouldReindexUnchangedRecords;
    }

    public String getBaseSolrURL() {
        return baseSolrURL;
    }

    public String getRealSolrMarcCore() {
        return realSolrMarcCore;
    }

    public String getTempSolrMarcCore() {
        return tempSolrMarcCore;
    }

    public String getRealSolrEContentCore() {
        return realSolrEContentCore;
    }

    public String getTempSolrEContentCore() {
        return tempSolrEContentCore;
    }

    public String getEcontentFileFolder() {
        return econtentFileFolder;
    }

    public String getEcontentCollectionGroupMapPath() {
        return econtentCollectionGroupMapPath;
    }

    public String getEcontentItemTypeFormatMapPath() {
        return econtentItemTypeFormatMapPath;
    }

    public String getEcontentDeviceCompatibilityMapPath() {
        return econtentDeviceCompatibilityMapPath;
    }

    public BasicDataSource getEcontentDatasource() {
        return econtentDatasource;
    }

    public EContentRecordDAO getEContentRecordDAO() {
        return eContentRecordDAO;
    }

    public boolean econtentShouldCheckOverDriveAvailability() {
        return econtentShouldCheckOverDriveAvailability;
    }

    public String getVufindUrl() {
        return vufindUrl;
    }

    public String getStrandsBookcoverUrl() {
        return strandsBookcoverUrl;
    }

    public String getStrandsCatalogFile() {
        return strandsCatalogFile;
    }

    public boolean shouldRemoveRecordsNotInMarc() {
        return shouldRemoveRecordsNotInMarc;
    }

    public String getItemTag() {
        return itemTag;
    }

    public String getCallNumberSubfield() {
        return callNumberSubfield;
    }

    public String getLocationSubfield() {
        return locationSubfield;
    }

    private HashMap<String,ConcurrentUpdateSolrServer> solrServers = new HashMap<String,ConcurrentUpdateSolrServer> ();
    public ConcurrentUpdateSolrServer getSolrUpdateServer(String indexName) {
        ConcurrentUpdateSolrServer ret = solrServers.get(indexName);
        if(ret == null) {
            ret = new ConcurrentUpdateSolrServer(this.getBaseSolrURL() + indexName, 1024, 5);
            solrServers.put(indexName, ret);
        }
        return ret;
    }
}
