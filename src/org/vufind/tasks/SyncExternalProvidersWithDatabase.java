package org.vufind.tasks;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.config.ConfigFiller;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.EContentConfigOptions;
import org.vufind.config.sections.ExternalSourcesConfigOptions;
import org.vufind.config.sections.MarcConfigOptions;
import org.vufind.econtent.FreegalImporter;
import org.vufind.econtent.I_ExternalImporter;
import org.vufind.processors.IEContentProcessor;
import org.vufind.processors.IMarcRecordProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by jbannon on 7/3/14.
 */
public class SyncExternalProvidersWithDatabase {

    public static void main(String[] args) {
        StatusPrinter.print((LoggerContext) LoggerFactory.getILoggerFactory());

        if (args.length < 1) {
            System.out
                    .println("Please enter the config file loc as the first param");
            System.exit(-1);
        }
        String configFolder = args[0];


        DynamicConfig config = new DynamicConfig();
        ConfigFiller.fill(config, Arrays.asList(BasicConfigOptions.values()), new File(configFolder));
        ConfigFiller.fill(config, Arrays.asList(ExternalSourcesConfigOptions.values()), new File(configFolder));
        config.put(BasicConfigOptions.CONFIG_FOLDER, configFolder);

        SyncExternalProvidersWithDatabase task = new SyncExternalProvidersWithDatabase(config);
        task.run();
    }

    final static Logger logger = LoggerFactory.getLogger(ProcessMarc.class);
    final DynamicConfig config;

    public SyncExternalProvidersWithDatabase(DynamicConfig config) {
        this.config = config;
    }

    private void run() {
        StatusPrinter.print((LoggerContext) LoggerFactory.getILoggerFactory());

        List<I_ExternalImporter> importers = loadImporters();

        for(I_ExternalImporter importer: importers) {
            importer.init(config);
            importer.importRecords();
        }
    }

    private List<I_ExternalImporter> loadImporters() {
        List<Class> importerClasses = (List<Class>)config.get(ExternalSourcesConfigOptions.EXTERNAL_IMPORTER);
        List<I_ExternalImporter> importers = new ArrayList();

        for(Class importerClass: importerClasses) {
            Object instance = null;
            try {
                instance = importerClass.newInstance();
                if(instance instanceof I_ExternalImporter) {
                    I_ExternalImporter importer = (I_ExternalImporter)instance;
                    importer.init(config);
                    importers.add(importer);
                }
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return importers;
    }
}
