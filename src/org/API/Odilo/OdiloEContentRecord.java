package org.API.Odilo;

import org.API.OverDrive.OverDriveAPIServices;
import org.json.simple.JSONObject;
import org.vufind.config.DynamicConfig;
import org.vufind.econtent.EContentRecord;
import org.vufind.econtent.EContentRecordDAO;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jbannon on 7/21/2014.
 */
public class OdiloEContentRecord extends EContentRecord {
    public OdiloEContentRecord(EContentRecordDAO eContentRecordDAO, ResultSet rs, DynamicConfig config) throws SQLException {
        super(eContentRecordDAO, rs, config);
    }

    public OdiloEContentRecord(EContentRecordDAO eContentRecordDAO, DynamicConfig config) throws SQLException {
        super(eContentRecordDAO, config);
    }

    @Override
    protected Object getSolrField(String name, Properties collectionGroupMap,
                                  Properties itemTypeFormatMap, Properties deviceCompatibilityMap,
                                  String fullTextPath) {
        if (name.equals("econtentText")) {
            File file = new File(fullTextPath + "/" + get("id") + ".txt");
            logger.debug("Looking for Odilo epub text in: "
                    + file.getAbsolutePath());
            if (file.exists()) {
                try {
                    return readFile(file);
                } catch (IOException e) {
                    logger.error("Error reading epub text file.", e);
                }
            }
            return null;
        }

        if (name.equals("num_holdings")) {
            if (!"active".equals(getString("status"))) {
                return null;
            }
            return 1;
        }

        if (name.equals("available_at")) {
            if (!"active".equals(getString("status"))) {
                return null;
            }
            return "Odilo";
        }

        return super.getSolrField(name, collectionGroupMap, itemTypeFormatMap, deviceCompatibilityMap, fullTextPath);
    }

    private String getOdiloId() {
        String sourceUrl = getString("sourceUrl");
        if (sourceUrl == null || sourceUrl.length() == 0) {
            return null;
        }
        Pattern Regex = Pattern.compile(
                "[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}",
                Pattern.CANON_EQ);
        Matcher RegexMatcher = Regex.matcher(sourceUrl);
        String OdiloId = null;
        if (RegexMatcher.find()) {
            OdiloId = RegexMatcher.group();
        }
        return OdiloId;
    }

}
