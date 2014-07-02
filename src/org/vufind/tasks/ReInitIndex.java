package org.vufind.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.config.Config;
import org.vufind.URLPostResponse;
import org.vufind.Util;

import java.io.File;

/**
 * Created by jbannon on 7/1/14.
 */
public class ReInitIndex {
    final static Logger logger = LoggerFactory.getLogger(ReInitIndex.class);
    final Config config;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out
                    .println("Please enter the config file loc as the first param and the index to init as the second");
            System.exit(-1);
        }
        String confileFileLoc = args[0];
        String coreName = args[1];

        ReInitIndex reinit = new ReInitIndex(new Config(new File(confileFileLoc)));
        reinit.clearSolrIndex(coreName);
    }

    public ReInitIndex(Config config) {
        this.config = config;
    }

    public void clearSolrIndex(String indexName) {
        clearSolrIndex(indexName, "*:*");
    }

    public void clearSolrIndex(String indexName, String query) {
        logger.info("Clearing index["+indexName+"]");
        URLPostResponse response = Util.postToURL(config.getBaseSolrURL() + indexName + "/update/?commit=true", "<delete><query>" + query + "</query></delete>", logger);
        if (!response.isSuccess()){
            logger.info("Error clearing index["+indexName+"] -- "+response.getMessage());
        } else {
            logger.info("Cleared index");
        }
    }
}
