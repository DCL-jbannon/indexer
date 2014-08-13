package org.vufind.tasks;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.config.*;
import org.vufind.URLPostResponse;
import org.vufind.Util;
import org.vufind.config.sections.BasicConfigOptions;

import java.io.File;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Created by jbannon on 7/1/14.
 */
public class ReInitIndex {
    public static void main(String[] args) {
        StatusPrinter.print((LoggerContext) LoggerFactory.getILoggerFactory());

        logger.info("Starting ReInit");
        if (args.length < 1) {
            System.out
                    .println("Please enter the config file loc");
            System.exit(-1);
        }
        String configFolder = args[0];


        System.out.println("configFolder: "+configFolder);

        System.out.println("About to load config");

        DynamicConfig config = new DynamicConfig();
        ConfigFiller.fill(config, Arrays.asList(BasicConfigOptions.values()), new File(configFolder));

        System.out.println("Loaded Config");

        ReInitIndex reinit = new ReInitIndex(config);

        String printCore = config.getString(BasicConfigOptions.PRINT_CORE);
        String econtentCore = config.getString(BasicConfigOptions.ECONTENT_CORE);

        System.out.println("PrintCore: "+printCore);
        System.out.println("econtentCore: "+econtentCore);
        reinit.clearSolrIndex(printCore);
        reinit.clearSolrIndex(econtentCore);
    }

    final static Logger logger = LoggerFactory.getLogger(ReInitIndex.class);
    final DynamicConfig config;

    public ReInitIndex(DynamicConfig config) {
        this.config = config;
    }

    public void clearSolrIndex(String indexName) {
        clearSolrIndex(indexName, "*:*");
    }

    public void clearSolrIndex(String indexName, String query) {
        logger.info("Clearing index["+indexName+"]");
        logger.info("Calling["+config.get(BasicConfigOptions.BASE_SOLR_URL) + indexName + "/update/?commit=true");
        URLPostResponse response = Util.postToURL(config.get(BasicConfigOptions.BASE_SOLR_URL) + indexName + "/update/?commit=true", "<delete><query>" + query + "</query></delete>", logger);
        if (!response.isSuccess()){
            logger.info("Error clearing index["+indexName+"] -- "+response.getMessage());
        } else {
            logger.info("Cleared index");
        }
    }
}
