package org.vufind.econtent;

import org.API.OverDrive.OverDriveAPIServices;
import org.API.OverDrive.OverDriveCollectionIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.ConnectionProvider;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.OverDriveConfigOptions;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by jbannon on 7/16/2014.
 */
public class OverDriveImporter {
    final static Logger logger = LoggerFactory.getLogger(OverDriveImporter.class);
    private DynamicConfig config;

    public void importRecords(){
        logger.info("Importing OverDrive API Items");

        String clientKey = config.getString(OverDriveConfigOptions.CLIENT_KEY);
        String clientSecret = config.getString(OverDriveConfigOptions.CLIENT_SECRET);
        int libraryId = config.getInteger(OverDriveConfigOptions.LIBRARY_ID);
        //ProcessorResults pr = new ProcessorResults("OverDrive API Item", reindexLogId, vufindConn, logger);
        //pr.addNote("The eContent Solr url is: " + "http://" + configIni.get("IndexShards", "eContent"));
        OverDriveCollectionIterator odci = new OverDriveCollectionIterator(clientKey, clientSecret, libraryId);
        OverDriveAPIServices overDriveAPIServices = new OverDriveAPIServices(clientKey, clientSecret, libraryId);

        Connection econtentConn = ConnectionProvider.getConnection(config, ConnectionProvider.PrintOrEContent.E_CONTENT);

        PopulateOverDriveAPIItems service = new PopulateOverDriveAPIItems(odci, econtentConn, overDriveAPIServices);

        try {
            service.execute();
            //pr.saveResults();
        } catch (SQLException e) {
            logger.error("Error importing OverDrive API Items.", e);
            //pr.addNote("Error importing OverDrive API Items. " + e.getMessage());
        }
    }

    public void init(DynamicConfig config){
        this.config = config;
    }
}
