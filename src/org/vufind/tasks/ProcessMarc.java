package org.vufind.tasks;

import org.econtent.EContentIndexer;
import org.econtent.ExtractEContentFromMarc;
import org.marc4j.MarcStreamReader;
import org.marc4j.marc.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.strands.StrandsProcessor;
import org.vufind.*;
import org.vufind.config.ConfigFiller;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.MarcConfigOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by jbannon on 7/3/14.
 */
public class ProcessMarc {
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

        ProcessMarc processMarcTask = new ProcessMarc(config);
        processMarcTask.run(coreName);
    }

    final static Logger logger = LoggerFactory.getLogger(ReInitIndex.class);
    final DynamicConfig config;

    public ProcessMarc(DynamicConfig config) {
        this.config = config;
    }

    public void run(String indexName) {
        List<IRecordProcessor> recordProcessors = loadRecordProcesors();

        MarcStreamReader reader = null;
        List<Record> records = null;
        while((records = getNextRecords(reader, 1000)).size()>0) {
            final List<Record> closedOverRecords = records;
            recordProcessors.parallelStream().forEach((processor) -> processor.accept(closedOverRecords));
        }

        /*if (recordProcessors.size() > 0) {
            // Do processing of marc records with record processors loaded above.
            // Includes indexing records
            // Extracting eContent from records
            // Updating resource information
            // Saving records to strands - may need to move to resources if we are doing partial exports
            logger.info("START processMarcRecords");
            processMarcRecords(recordProcessors);
            logger.info("END processMarcRecords");
        }*/
    }

    private List<Record> getNextRecords(MarcStreamReader reader, int limit) {
        List<Record> records = new ArrayList<Record>();
        while(reader.hasNext() && records.size()<limit) {
            records.add(reader.next());
        }

        return records;
    }

    private List<IRecordProcessor> loadRecordProcesors() {
        List<Class> processorClasses = (List<Class>)config.get(MarcConfigOptions.MARC_RECORD_PROCESSOR);
        List<IRecordProcessor> processors = new ArrayList<IRecordProcessor>();

        for(Class processorClass: processorClasses) {
            Object instance = null;
            try {
                instance = processorClass.newInstance();
                if(instance instanceof IRecordProcessor) {
                    IRecordProcessor processor = (IRecordProcessor)instance;
                    processor.init(config);
                    processors.add(processor);
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        }

        return processors;
    }
}
