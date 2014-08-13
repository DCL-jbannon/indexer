package org.vufind.econtent;

import org.API.Odilo.OdiloAPI;
import org.API.OverDrive.OverDriveAPI;
import org.API.OverDrive.OverDriveAPIServices;
import org.API.OverDrive.OverDriveCollectionIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.ConnectionProvider;
import org.vufind.config.ConfigFiller;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.OdiloConfigOptions;
import org.vufind.config.sections.OverDriveConfigOptions;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by jbannon on 7/16/2014.
 */
public class OdiloImporter implements I_ExternalImporter {
    final static Logger logger = LoggerFactory.getLogger(OdiloImporter.class);
    private DynamicConfig config;

    public void importRecords(){
        logger.info("Importing Odilo API Items");
        OdiloAPI api = new OdiloAPI(config.getString(OdiloConfigOptions.URL),
                config.getString(OdiloConfigOptions.USER),
                config.getString(OdiloConfigOptions.PASS));

        Connection econtentConn = ConnectionProvider.getConnection(config, ConnectionProvider.PrintOrEContent.E_CONTENT);

        PopulateOdiloItems service = new PopulateOdiloItems(api, config);

        try {
            service.execute();
            //pr.saveResults();
        } catch (SQLException e) {
            logger.error("Error importing Odilo API Items.", e);
            //pr.addNote("Error importing OverDrive API Items. " + e.getMessage());
        }
    }

    public boolean init(DynamicConfig config){
        ConfigFiller.fill(config, OdiloConfigOptions.values(),  new File(config.getString(BasicConfigOptions.CONFIG_FOLDER)));
        this.config = config;
        return true;
    }

    @Override
    public void finish() {

    }
}
