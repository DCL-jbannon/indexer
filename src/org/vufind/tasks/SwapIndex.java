package org.vufind.tasks;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.URLPostResponse;
import org.vufind.Util;
import org.vufind.config.Config;

import java.io.File;

/**
 * Created by jbannon on 7/1/14.
 */
public class SwapIndex {
    final static Logger logger = LoggerFactory.getLogger(SwapIndex.class);
    final Config config;

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out
                    .println("Please enter params: 1 - configFile, 2 - oldCore, 3 - newCore");
            System.exit(-1);
        }
        String confileFileLoc = args[0];
        String oldCore = args[1];
        String newCore = args[2];

        SwapIndex si = new SwapIndex(new Config(new File(confileFileLoc)));
        si.run(oldCore, newCore);
    }

    public SwapIndex(Config config) {
        this.config = config;
    }

    public boolean run(String oldCore, String newCore) {

        logger.info("Swapping cores old["+oldCore+"] new["+newCore+"]");

        logger.info("Optimizing index ["+newCore+"]");
        try {
            ConcurrentUpdateSolrServer updateServer = config.getSolrUpdateServer(newCore);
            updateServer.optimize();
        } catch (Exception e) {
            logger.error("Error optimizing index ["+newCore+"]", e);
        }

        URLPostResponse response = Util.getURL(config.getBaseSolrURL() + "/admin/cores?action=SWAP&core="+newCore+"&other="+oldCore, logger);
        if (!response.isSuccess()){
            logger.error("Error swapping cores", response.getMessage());
            return false;
        }else{
            logger.info("Swapped cores old[" + oldCore + "] new[" + newCore + "]");
        }
        return true;
    }
}
