package org.API.OverDrive;

import org.json.simple.JSONObject;
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
public class OverDriveEContentRecord extends EContentRecord {
    public OverDriveEContentRecord(EContentRecordDAO eContentRecordDAO, ResultSet rs) throws SQLException {
        super(eContentRecordDAO, rs);
    }

    public OverDriveEContentRecord(EContentRecordDAO eContentRecordDAO) throws SQLException {
        super(eContentRecordDAO);
    }

    @Override
    protected Object getSolrField(String name, Properties collectionGroupMap,
                                  Properties itemTypeFormatMap, Properties deviceCompatibilityMap,
                                  String fullTextPath) {
        if (name.equals("econtentText")) {
            File file = new File(fullTextPath + "/" + get("id") + ".txt");
            logger.debug("Looking for OverDrive epub text in: "
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
            return "OverDrive";
        }

        return super.getSolrField(name, collectionGroupMap, itemTypeFormatMap, deviceCompatibilityMap, fullTextPath);
    }

    private Set<String> getFormats(Properties itemTypeFormatMap,
                                   OverDriveAPIServices overDriveAPIServices) {
        if (formats != null) {
            return formats;
        }

        String overDriveId = getOverDriveId();
        if (overDriveId != null) {
            Set<String> overDriveFormats = getOverDriveFormats(
                    overDriveAPIServices, overDriveId);
            StringBuilder buf = new StringBuilder();
            String separator = "";
            for (String format : overDriveFormats) {
                buf.append(separator + format);
                separator = ", ";
            }
            formats.add(buf.toString());
        }
        return formats;
    }

    private String getOverDriveId() {
        String sourceUrl = getString("sourceUrl");
        if (sourceUrl == null || sourceUrl.length() == 0) {
            return null;
        }
        Pattern Regex = Pattern.compile(
                "[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}",
                Pattern.CANON_EQ);
        Matcher RegexMatcher = Regex.matcher(sourceUrl);
        String overDriveId = null;
        if (RegexMatcher.find()) {
            overDriveId = RegexMatcher.group();
        }
        return overDriveId;
    }

    private Set<String> getOverDriveFormats(OverDriveAPIServices overDriveAPIServices, String overDriveId) {
        Set<String> formats = new HashSet<String>();
        JSONObject meta = overDriveAPIServices.getItemMetadata(overDriveId);
        if (meta != null) {
            List jsonFormats = (List) meta.get("formats");
            for (Object o : jsonFormats) {
                Map m = (Map) o;
                formats.add((String) m.get("name"));
            }
        }
        return formats;
    }

}
