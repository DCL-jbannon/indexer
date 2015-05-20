package org.vufind.tasks;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.config.ConfigFiller;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.MarcConfigOptions;
import org.vufind.processors.AlphaBrowseProcessor;

import java.io.File;
import java.util.Arrays;

/**
 * Created by jbannon on 7/25/2014.
 */
public class UpdateAlphaBrowse {
    final static Logger logger = LoggerFactory.getLogger(ProcessMarc.class);

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please enter the config file loc as the first param");
            System.exit(-1);
        }
        String configFolder = args[0];

        if(args.length > 1) {
            ((LoggerContext) LoggerFactory.getILoggerFactory()).putProperty("guid", args[1]);
        }
        StatusPrinter.print((LoggerContext) LoggerFactory.getILoggerFactory());

        DynamicConfig config = new DynamicConfig();
        ConfigFiller.fill(config, Arrays.asList(BasicConfigOptions.values()), new File(configFolder));

        AlphaBrowseProcessor alpha = new AlphaBrowseProcessor();
        alpha.process(config);
    }
}
