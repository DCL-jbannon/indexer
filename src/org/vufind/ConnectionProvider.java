package org.vufind;

import org.apache.commons.dbcp2.BasicDataSource;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.BasicConfigOptions;

import javax.sql.ConnectionPoolDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;

/**
 * Created by jbannon on 7/8/14.
 */
public class ConnectionProvider {
    public static enum PrintOrEContent {
        PRINT,
        E_CONTENT
    }

    static final Hashtable<PrintOrEContent, BasicDataSource> dataSourceHashtable = new Hashtable();

    public static BasicDataSource getDataSource(DynamicConfig config, PrintOrEContent printOrEContent) {
        if(dataSourceHashtable.isEmpty()) {
            loadDataSources(config);
        }

        return dataSourceHashtable.get(printOrEContent);
    }

    public static Connection getConnection(DynamicConfig config, PrintOrEContent printOrEContent) {
        if(dataSourceHashtable.isEmpty()) {
            loadDataSources(config);
        }

        try {
            return dataSourceHashtable.get(printOrEContent).getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void loadDataSources(DynamicConfig config) {
        // Setup connections to vufind and econtent databases

        BasicDataSource vufindDataSource = new BasicDataSource();
        vufindDataSource.setDriverClassName(config.getString(BasicConfigOptions.VUFINDDB_DRIVER));
        vufindDataSource.setUsername(config.getString(BasicConfigOptions.VUFINDDB_USER));
        vufindDataSource.setPassword(config.getString(BasicConfigOptions.VUFINDDB_PASS));
        vufindDataSource.setUrl(config.getString(BasicConfigOptions.VUFINDDB_URL));
        dataSourceHashtable.put(PrintOrEContent.PRINT, vufindDataSource);

        BasicDataSource econtentDataSource = new BasicDataSource();
        econtentDataSource.setDriverClassName(config.getString(BasicConfigOptions.ECONTENTDB_DRIVER));
        econtentDataSource.setUsername(config.getString(BasicConfigOptions.ECONTENTDB_USER));
        econtentDataSource.setPassword(config.getString(BasicConfigOptions.ECONTENTDB_PASS));
        econtentDataSource.setUrl(config.getString(BasicConfigOptions.ECONTENTDB_URL));
        dataSourceHashtable.put(PrintOrEContent.E_CONTENT, econtentDataSource);
    }
}
