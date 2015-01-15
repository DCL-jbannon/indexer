package org.API.OverDrive;

import org.json.simple.JSONObject;
import org.vufind.config.ConfigFiller;
import org.vufind.config.DynamicConfig;
import org.vufind.config.sections.BasicConfigOptions;
import org.vufind.config.sections.OdiloConfigOptions;
import org.vufind.config.sections.OverDriveConfigOptions;
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
    public OverDriveEContentRecord(EContentRecordDAO eContentRecordDAO, ResultSet rs, DynamicConfig config) throws SQLException {
        super(eContentRecordDAO, rs, config);
    }

    public OverDriveEContentRecord(EContentRecordDAO eContentRecordDAO, DynamicConfig config) throws SQLException {
        super(eContentRecordDAO, config);
    }

    static private OverDriveAPIServices api = null;
    public void init() {
        if(api==null) {
            ConfigFiller.fill(config,
                    OverDriveConfigOptions.values(),
                    new File(config.getString(BasicConfigOptions.CONFIG_FOLDER)));
            String clientKey = config.getString(OverDriveConfigOptions.CLIENT_KEY);
            String clientSecret = config.getString(OverDriveConfigOptions.CLIENT_SECRET);
            int libraryId = config.getInteger(OverDriveConfigOptions.LIBRARY_ID);
            api = new OverDriveAPIServices(clientKey, clientSecret, libraryId);
        }
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

        if (name.equals("format")) {
            Set<String> formats = getFormats(itemTypeFormatMap, api);
            logger.debug("formats => " + formats);
            return formats;
        }

        return super.getSolrField(name, collectionGroupMap, itemTypeFormatMap, deviceCompatibilityMap, fullTextPath);
    }

    private Set<String> getFormats(Properties itemTypeFormatMap,
                                   OverDriveAPIServices overDriveAPIServices) {
        if (formats != null) {
            return formats;
        }
        // determine formats
        formats = new HashSet<String>();

        String overDriveId = getOverDriveId();
        if (overDriveId != null) {
            Set<String> overDriveFormats = getOverDriveFormats(
                    overDriveAPIServices, overDriveId);

            formats.addAll(overDriveFormats);
        }
        return formats;
    }

    private String getOverDriveId() {
        String overDriveId = getString("external_id");
        if(overDriveId != null && overDriveId.length()>0) {
            return overDriveId;
        }
        String sourceUrl = getString("sourceUrl");
        if (sourceUrl == null || sourceUrl.length() == 0) {
            return null;
        }
        Pattern Regex = Pattern.compile(
                "[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}",
                Pattern.CANON_EQ);
        Matcher RegexMatcher = Regex.matcher(sourceUrl);
        overDriveId = null;
        if (RegexMatcher.find()) {
            overDriveId = RegexMatcher.group();
        }
        return overDriveId;
    }

    private Set<String> getOverDriveFormats(OverDriveAPIServices overDriveAPIServices, String overDriveId) {
        Set<String> formats = new HashSet<String>();
        try{
            JSONObject meta = overDriveAPIServices.getItemMetadata(overDriveId);
            if (meta != null) {
                List jsonFormats = (List) meta.get("formats");
                for (Object o : jsonFormats) {
                    Map m = (Map) o;
                    formats.add((String) m.get("name"));
                }
            }
        } catch(Exception e)
        {
            //OverDrive has started returning items with no "formats" which was blowing up here, so we're swallowing errors.
            e.printStackTrace();
        }

        return formats;
    }

}
