package org.vufind.tasks;

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
        if (args.length < 2) {
            System.out
                    .println("Please enter the config file loc as the first param and the index to init as the second");
            System.exit(-1);
        }
        String confileFolder = args[0];
        String coreName = args[1];


        DynamicConfig config = new DynamicConfig();
        ConfigFiller.fill(config, Arrays.asList(BasicConfigOptions.values()), new File(confileFolder));

        ReInitIndex reinit = new ReInitIndex(config);
        reinit.clearSolrIndex(coreName);
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
        URLPostResponse response = Util.postToURL(config.get(BasicConfigOptions.BASE_SOLR_URL) + indexName + "/update/?commit=true", "<delete><query>" + query + "</query></delete>", logger);
        if (!response.isSuccess()){
            logger.info("Error clearing index["+indexName+"] -- "+response.getMessage());
        } else {
            logger.info("Cleared index");
        }
    }
}
