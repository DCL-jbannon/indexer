package org.vufind.econtent;

import org.API.Odilo.OdiloAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.ConnectionProvider;
import org.vufind.config.ConfigFiller;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.EvokeConfigOptions;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by jbannon on 7/16/2014.
 */
public class EvokeImporter implements I_ExternalImporter {
    final static Logger logger = LoggerFactory.getLogger(EvokeImporter.class);
    private DynamicConfig config;

    public void importRecords(){
        logger.info("Importing Evoke API Items");
        OdiloAPI api = new OdiloAPI(config.getString(EvokeConfigOptions.URL),
                config.getString(EvokeConfigOptions.USER),
                config.getString(EvokeConfigOptions.PASS));

        Connection econtentConn = ConnectionProvider.getConnection(config, ConnectionProvider.PrintOrEContent.E_CONTENT);

        PopulateOdiloItems service = new PopulateOdiloItems(api, config, "Evoke", config.getInteger(EvokeConfigOptions.UPDATE_FROM_DAYS_AGO));

        try {
            service.execute();
            //pr.saveResults();
        } catch (SQLException e) {
            logger.error("Error importing Evoke API Items.", e);
            //pr.addNote("Error importing OverDrive API Items. " + e.getMessage());
        }
    }

    public boolean init(DynamicConfig config){
        ConfigFiller.fill(config, EvokeConfigOptions.values(),  new File(config.getString(BasicConfigOptions.CONFIG_FOLDER)));
        this.config = config;
        return true;
    }

    @Override
    public void finish() {

    }
}
