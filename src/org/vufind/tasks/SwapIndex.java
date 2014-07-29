package org.vufind.tasks;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.URLPostResponse;
import org.vufind.Util;
import org.vufind.config.*;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.CoreSwapConfigOptions;
import org.vufind.solr.SolrUpdateServerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Created by jbannon on 7/1/14.
 */
public class SwapIndex {
    public static void main(String[] args) {
        StatusPrinter.print((LoggerContext) LoggerFactory.getILoggerFactory());
        if (args.length < 1) {
            System.out
                    .println("Please enter params: 1 - configFile");
            System.exit(-1);
        }
        String configFolder = args[0];

        DynamicConfig config = new DynamicConfig();
        ConfigFiller.fill(config, Arrays.asList(BasicConfigOptions.values()), new File(configFolder));
        ConfigFiller.fill(config, Arrays.asList(CoreSwapConfigOptions.values()), new File(configFolder));

        SwapIndex si = new SwapIndex(config);

        String newCore = config.getString(CoreSwapConfigOptions.PRINT_NEW_CORE);
        String oldCore = config.getString(CoreSwapConfigOptions.PRINT_OLD_CORE);
        si.run(oldCore, newCore);

        newCore = config.getString(CoreSwapConfigOptions.ECONTENT_NEW_CORE);
        oldCore = config.getString(CoreSwapConfigOptions.ECONTENT_OLD_CORE);
        si.run(oldCore, newCore);
    }

    final static Logger logger = LoggerFactory.getLogger(SwapIndex.class);
    final DynamicConfig config;

    public SwapIndex(DynamicConfig config) {
        this.config = config;
    }

    public boolean run(String oldCore, String newCore) {
        logger.info("Swapping cores old["+oldCore+"] new["+newCore+"]");
        logger.info("Optimizing index ["+newCore+"]");
        try {
            ConcurrentUpdateSolrServer updateServer =  SolrUpdateServerFactory.getSolrUpdateServer(config.get(BasicConfigOptions.BASE_SOLR_URL) + newCore);
            updateServer.optimize();
        } catch (Exception e) {
            logger.error("Error optimizing index ["+newCore+"]", e);
        }

        URLPostResponse response = Util.getURL(config.get(BasicConfigOptions.BASE_SOLR_URL)  + "admin/cores?action=SWAP&core="+newCore+"&other="+oldCore, logger);
        if (!response.isSuccess()){
            logger.error("Error swapping cores", response.getMessage());
            return false;
        }else{
            logger.info("Swapped cores old[" + oldCore + "] new[" + newCore + "]");
        }
        return true;
    }
}
