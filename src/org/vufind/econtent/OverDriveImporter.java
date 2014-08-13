package org.vufind.econtent;

import org.API.OverDrive.OverDriveAPIServices;
import org.API.OverDrive.OverDriveCollectionIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.ConnectionProvider;
import org.vufind.config.ConfigFiller;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.OverDriveConfigOptions;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Created by jbannon on 7/16/2014.
 */
public class OverDriveImporter implements I_ExternalImporter {
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

        PopulateOverDriveAPIItems service = new PopulateOverDriveAPIItems(odci, econtentConn, overDriveAPIServices, config);

        try {
            service.execute();
            //pr.saveResults();
        } catch (SQLException e) {
            logger.error("Error importing OverDrive API Items.", e);
            //pr.addNote("Error importing OverDrive API Items. " + e.getMessage());
        }
    }

    public boolean init(DynamicConfig config){
        ConfigFiller.fill(config, OverDriveConfigOptions.values(),  new File(config.getString(BasicConfigOptions.CONFIG_FOLDER)));
        this.config = config;
        return true;
    }

    @Override
    public void finish() {

    }
}
