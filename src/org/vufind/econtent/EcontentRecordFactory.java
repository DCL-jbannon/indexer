package org.vufind.econtent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.EContentConfigOptions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by jbannon on 7/21/2014.
 */
public class EcontentRecordFactory {
    private static Logger logger = LoggerFactory.getLogger(EContentRecord.class);

    private final DynamicConfig config;

    private EcontentRecordFactory(DynamicConfig config) {
        this.config = config;
    }

    private static EcontentRecordFactory single;
    public static EcontentRecordFactory getRecordFactory(DynamicConfig config) {
        if(single==null) {
            single = new EcontentRecordFactory(config);
        }

        return single;
    }


    private HashMap<String,Class> sourceToRecordTypeMap = null;

    private HashMap<String,Class> getSourceToRecordType() {
        if(sourceToRecordTypeMap == null) {
            List cl = config.getList(EContentConfigOptions.ECONTENT_RECORD_CLASSES);
            sourceToRecordTypeMap = new HashMap<>();

            for(Object o:cl) {
                if(o instanceof String) {
                    String configValue = (String)o;

                    String[] configV_A = configValue.split(",");
                    if(configV_A.length==2) {
                        try {
                            sourceToRecordTypeMap.put(configV_A[0], Class.forName(configV_A[1]));
                        } catch (ClassNotFoundException e) {
                            logger.error("Could not load configured class: "+configV_A[1], e);
                        }
                    }
                }
            }
        }
        return sourceToRecordTypeMap;
    }

    //TODO for God's sake get Hybernate or something in here.
    /**
     * This is an atrocity, but we're stuck with a bunch of shit that was done previously and I don't have time to
     * re-do everything right now.
     * @param eContentRecordDAO
     * @param rs
     * @return
     * @throws SQLException
     */
    public EContentRecord get(EContentRecordDAO eContentRecordDAO, ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String name = metaData.getColumnName(i);
            String value = rs.getString(i);
            if (value != null && value.trim().length() > 0) {
                if(name.equals("source")) {
                    if(this.getSourceToRecordType().containsKey(value)) {
                        //IF we have a record type configured for this source then use it
                        Class recordClass =  this.getSourceToRecordType().get(value);
                        try {
                            Constructor cons = recordClass.getDeclaredConstructor(EContentRecordDAO.class, ResultSet.class);
                            return (EContentRecord)cons.newInstance(eContentRecordDAO, rs);
                        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                            logger.error("Could not instantiate specified EContentRecord class", e);
                        }
                    }
                }
            }
        }

        //Default
        return new EContentRecord(eContentRecordDAO, rs);
    }
}
