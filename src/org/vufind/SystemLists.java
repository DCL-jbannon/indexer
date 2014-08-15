package org.vufind;

import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by jbannon on 8/15/2014.
 */
public class SystemLists {
    protected static Logger logger = LoggerFactory.getLogger(SystemLists.class);

    //Hashtable<RecordId, Set<Titles>>
    static Hashtable<String, Set<String>> listTitlesByRecord = null;

    /**
     * Determine the system lists to add the record to based on lists exported.
     * @param record
     * @param configFile
     * @return
     */
    static public Set<String> getSystemLists(Record record, String configFile) {
        logger.debug("Get SystemLists by Record");
        if (listTitlesByRecord == null){
            loadTitlesByRecord(configFile);
        }
        DataField recordIdField = (DataField) record.getVariableField("950");
        String recordId = recordIdField.getSubfield('a').getData();
        if (listTitlesByRecord.containsKey(recordId)){
            return listTitlesByRecord.get(recordId);
        }else{
            return null;
        }
    }

    static private void loadTitlesByRecord(String configFilename){
        logger.debug("Starting loadTitlesByRecord");
        listTitlesByRecord = new Hashtable();
        //Load all of the lists in the directory.
        Properties props = new Properties();
        File configFile = new File(configFilename);
        logger.debug("configFile = " + configFile.getAbsolutePath());
        FileReader configFileReader = null;
        try {
            configFileReader = new FileReader(configFile);
            props.load(configFileReader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String configDirectory = props.getProperty("listDirectory");

        File listDirectory = new File(configDirectory);
        File[] filesToImport = listDirectory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith("csv");
            }
        });

        if (filesToImport == null){
            logger.error("Warning, no system lists found");
        }else{
            for (File fileToImport : filesToImport){
                loadTitlesFromFile(props, fileToImport);
            }
        }
    }

    static private void loadTitlesFromFile(Properties props, File fileToImport){
        logger.debug("Loading titles from file " + fileToImport.getName());
        String shortName = fileToImport.getName().substring(0, fileToImport.getName().lastIndexOf('.'));
        String listName = props.getProperty(shortName);
        if (listName == null || listName.length() == 0){
            listName = shortName;
        }
        logger.debug("  List Name - " + listName);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileToImport));
            String line = reader.readLine();
            int lineNumber = 0;
            while (line != null){
                if (lineNumber >= 2 && line.trim().length() > 0){
                    //The line is not a header line
                    //line format is Bib#|Collection|Call|Title|ISN
                    String[] fields = line.split("\\|");
                    if (fields.length > 0 && fields[0].length() > 0){
                        String recordNumber = fields[0];
                        Set<String> titles;
                        if (listTitlesByRecord.containsKey(recordNumber)){
                            titles = listTitlesByRecord.get(recordNumber);
                        }else{
                            titles = new LinkedHashSet();
                            listTitlesByRecord.put(recordNumber, titles);
                        }
                        titles.add(listName);
                    }
                }
                lineNumber++;
                line = reader.readLine();
            }

        } catch (IOException e) {
            logger.error("Couldn't read csv in SystemLists", e);
        }
    }
}
