package org.vufind.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.config.ConfigFiller;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.MarcConfigOptions;
import org.vufind.econtent.FreegalImporter;

import java.io.File;
import java.util.Arrays;

/**
 * Created by jbannon on 7/3/14.
 */
public class SyncExternalProvidersWithDatabase {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out
                    .println("Please enter the config file loc as the first param and the index to init as the second");
            System.exit(-1);
        }
        String configFolder = args[0];
        String coreName = args[1];


        DynamicConfig config = new DynamicConfig();
        ConfigFiller.fill(config, Arrays.asList(BasicConfigOptions.values()), new File(configFolder));
        ConfigFiller.fill(config, Arrays.asList(MarcConfigOptions.values()), new File(configFolder));
        config.put(BasicConfigOptions.CONFIG_FOLDER, configFolder);

        SyncExternalProvidersWithDatabase task = new SyncExternalProvidersWithDatabase(config);
        task.run(coreName);
    }

    final static Logger logger = LoggerFactory.getLogger(ProcessMarc.class);
    final DynamicConfig config;

    public SyncExternalProvidersWithDatabase(DynamicConfig config) {
        this.config = config;
    }

    private void run(String coreName) {
        logger.info("START import Freegal");
        FreegalImporter freegalImporter = new FreegalImporter();
        if (freegalImporter.init(config)) {
            //recordProcessors.add(freegalImporter);
            try {
                freegalImporter.importRecords();
            } catch (RuntimeException e) {
                logger.error("Unknown error importing Freegal records.", e);
            }
        }
        logger.info("END import Freegal");

        // Import OverDrive records into econtent database
        logger.info("START import OverDrive");
        //harvestOverDrive();
        logger.info("END import OverDrive");

    }
}
