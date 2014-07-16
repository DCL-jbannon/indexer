package org.vufind.tasks;

import org.marc4j.MarcPermissiveStreamReader;
import org.marc4j.MarcReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.processors.IMarcRecordProcessor;
import org.vufind.MarcProcessor;
import org.vufind.MarcRecordDetails;
import org.vufind.config.ConfigFiller;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.MarcConfigOptions;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

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
        config.put(BasicConfigOptions.CONFIG_FOLDER, configFolder);

        ProcessMarc processMarcTask = new ProcessMarc(config);
        processMarcTask.run(coreName);
    }

    final static Logger logger = LoggerFactory.getLogger(ProcessMarc.class);
    final DynamicConfig config;
    final private MarcProcessor marcProcessor;

    public ProcessMarc(DynamicConfig config) {
        this.config = config;
        this.marcProcessor = new MarcProcessor();
        this.marcProcessor.init(this.config);
    }

    public static class CountingList<E> extends ArrayList<E> {
        private int count = 0;

        public void increment() {
            count++;
        }

        public int getCount() {
            return count;
        }

        public CountingList(Collection c) {
            super(c);
            this.count = 0;
        }
    }

    public void run(String indexName) {
        List<IMarcRecordProcessor> recordProcessors = loadRecordProcessors();

        //We use our own ForkJoinPool rather than allowing parallelStream() to select the default, largely this
        //is to see greater dependability in testing as the default pool will only have one thread on some machines.
        ForkJoinPool forkJoinPool = new ForkJoinPool(3);

        List<File> marcFiles = getMarcFiles();
        for(File marcFile : marcFiles) {
            InputStream input = null;
            try {input = new FileInputStream(marcFile);} catch (FileNotFoundException e) {e.printStackTrace();}
            MarcReader reader = new MarcPermissiveStreamReader(input, true, true, "UTF8");

            List<MarcRecordDetails> records = null;
            while((records = getNextRecords(reader, 1000)).size()>0) {
                final CountingList<MarcRecordDetails> closedOverRecords = new CountingList(records);

                System.out.println("------------------------\nAfter getting records");
                /* Total amount of free memory available to the JVM */
                System.out.println("Free memory (bytes): " +
                        Runtime.getRuntime().freeMemory());
                /* This will return Long.MAX_VALUE if there is no preset limit */
                long maxMemory = Runtime.getRuntime().maxMemory();
                /* Maximum amount of memory the JVM will attempt to use */
                System.out.println("Maximum memory (bytes): " +
                        (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));

                long start = System.nanoTime();

                ForkJoinTask task = forkJoinPool.submit(()->
                    recordProcessors.parallelStream().forEach((processor) -> processor.accept(closedOverRecords)));
                task.join();

                System.out.println("------------------------\nAfter processing");
                System.out.println("Count: "+closedOverRecords.getCount());
                long elapsedTime = System.nanoTime() - start;
                System.out.println("Time: "+(elapsedTime/1000.0/1000.0/1000.0));
                /* Total amount of free memory available to the JVM */
                System.out.println("Free memory (bytes): " +
                        Runtime.getRuntime().freeMemory());
                /* This will return Long.MAX_VALUE if there is no preset limit */
                maxMemory = Runtime.getRuntime().maxMemory();
                /* Maximum amount of memory the JVM will attempt to use */
                System.out.println("Maximum memory (bytes): " +
                        (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));
                break;//Just temporary for profiling purposes
            }
        }

        for (IMarcRecordProcessor processor : recordProcessors) {
            processor.finish();
        }
    }

    private List<File> getMarcFiles() {
        File marcRecordDirectory = new File(config.get(MarcConfigOptions.MARC_FOLDER).toString());
        List<File> marcFiles = null;
        if (marcRecordDirectory.isDirectory()) {
            marcFiles = Arrays.asList(marcRecordDirectory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (name.matches("(?i).*?\\.(marc|mrc)")) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }));
        } else {
            marcFiles = Arrays.asList(new File[] { marcRecordDirectory });
        }

        return marcFiles;
    }

    private List<MarcRecordDetails> getNextRecords(MarcReader reader, int limit) {
        List<MarcRecordDetails> records = new ArrayList();
        while(reader.hasNext() && records.size()<limit) {
            try{
                records.add(new MarcRecordDetails(this.marcProcessor, reader.next()));
            } catch(Exception e) {
                e.printStackTrace();
            }

        }

        return records;
    }

    private List<IMarcRecordProcessor> loadRecordProcessors() {
        List<Class> processorClasses = (List<Class>)config.get(MarcConfigOptions.MARC_RECORD_PROCESSOR);
        List<IMarcRecordProcessor> processors = new ArrayList();

        for(Class processorClass: processorClasses) {
            Object instance = null;
            try {
                instance = processorClass.newInstance();
                if(instance instanceof IMarcRecordProcessor) {
                    IMarcRecordProcessor processor = (IMarcRecordProcessor)instance;
                    processor.init(config);
                    processors.add(processor);
                }
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }

        }

        return processors;
    }
}
