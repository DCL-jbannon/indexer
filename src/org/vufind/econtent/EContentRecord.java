package org.vufind.econtent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.config.DynamicConfig;

public class EContentRecord {
	protected static Logger logger = LoggerFactory.getLogger(EContentRecord.class);
	private static final String RECORD_TYPE = "econtentRecord";
	private Map<String, Object> properties;
	private EContentRecordDAO dao = null;
	protected Set<String> formats = null;

    protected DynamicConfig config = null;

	/**
	 * Instantiate a new empty EContentRecord.
	 */
	public EContentRecord(EContentRecordDAO eContentRecordDAO, DynamicConfig config) {
        this.properties = new HashMap<String, Object>();
		this.dao = eContentRecordDAO;
        this.config = config;
	}

	/**
	 * Instantiate an EContentRecord with the data in the current row of the
	 * given ResultSet.
	 * 
	 * @param rs
	 * @throws SQLException
	 */
	public EContentRecord(EContentRecordDAO eContentRecordDAO, ResultSet rs, DynamicConfig config) throws SQLException {
		this(eContentRecordDAO, config);
		// fetch all the columns from current row
		ResultSetMetaData metaData = rs.getMetaData();
		int columnCount = metaData.getColumnCount();
		for (int i = 1; i <= columnCount; i++) {
			String name = metaData.getColumnName(i);
			String value = rs.getString(i);
			if (value != null && value.trim().length() > 0) {
				set(name, value);
			}
		}
	}

    public void init() {

    }

	/**
	 * Get property.
	 * 
	 * @param name
	 * @return
	 */
	public Object get(String name) {
		return properties.get(name);
	}

	/**
	 * Get property.
	 * 
	 * @param name
	 * @return
	 */
	public String getString(String name) {
		Object val = get(name);
		return (val == null) ? null : String.valueOf(val);
	}

	/**
	 * Get property.
	 * 
	 * @param name
	 * @return
	 */
	public Integer getInteger(String name) {
		String val = getString(name);
		if (val == null) {
			return null;
		}
		try {
			return Integer.valueOf(val);
		} catch (NumberFormatException e) {
			return null;
		}
	}

    /**
     * Get property.
     *
     * @param name
     * @return
     */
    public Long getLong(String name) {
        String val = getString(name);
        if (val == null) {
            return null;
        }
        try {
            return Long.valueOf(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

	/**
	 * Set property.
	 * 
	 * @param name
	 * @param value
	 */
	public void set(String name, Object value) {
		properties.put(name, value);
	}

	/**
	 * Get names of properties that have been set.
	 * 
	 * @return
	 */
	public List<String> getSetProperties() {
        String[] setKeys = new String[]{};
        setKeys  = properties.keySet().toArray(setKeys);
        Arrays.sort(setKeys);

		return new ArrayList<String>(Arrays.asList(setKeys));
	}

	/**
	 * Create a SolrInputDocument based on the given schema fields and
	 * translation maps.
	 * 
	 * @param solrFields
	 * @param collectionGroupMap
	 * @param itemTypeFormatMap
	 * @return
	 */
	public SolrInputDocument getSolrInputDocument(List<String> solrFields,
                                                  Properties collectionGroupMap, Properties itemTypeFormatMap,
                                                  Properties deviceCompatibilityMap,
                                                  String fullTextPath) {
		SolrInputDocument doc = new SolrInputDocument();
		for (String name : solrFields) {
			Object value = getSolrField(name, collectionGroupMap,
					itemTypeFormatMap, deviceCompatibilityMap,
                    fullTextPath);

			if (value != null) {
				doc.addField(name, value);
			}
		}
		return doc;
	}

	protected Object getSolrField(String name, Properties collectionGroupMap,
                                  Properties itemTypeFormatMap, Properties deviceCompatibilityMap,
                                  String fullTextPath) {
		if (name.equals("id")) {
			return RECORD_TYPE + getString("id");
		}
		if (name.equals("recordtype")) {
			return RECORD_TYPE;
		}
		if (name.equals("institution") || name.equals("building")) {
			return "Digital Collection";
		}
		if (name.equals("collection_group")) {
			String collection = getString("collection");
			if (collection == null || collection.trim().length() == 0) {
				return null;
			}
			return translateValue(collectionGroupMap, collection.trim());
		}
		if (name.equals("econtentText")) {
			return null;
		}
		if (name.equals("subject_facet")) {
			String value = getString("subject");
			return (value == null) ? null : value.split("\r\n");
		}
		if (name.equals("topic_facet")) {
			String value = getString("topic");
			return (value == null) ? null : value.split("\r\n");
		}
		if (name.equals("format_category")) {
			return "EMedia";
		}
		if (name.equals("format")) {
			Set<String> formats = getFormats(itemTypeFormatMap);
			logger.debug("formats => " + formats);
			return formats;
		}
		if (name.equals("econtent_device")) {
			Set<String> allDevices = new HashSet<String>();
			Set<String> formats = getFormats(itemTypeFormatMap);
			for (String format : formats) {
				List devices = (List) deviceCompatibilityMap.get(format);
				if (devices != null) {
					allDevices.addAll(devices);
				}
			}
			logger.debug("formats => " + formats);
			logger.debug("econtent_device => " + allDevices);
			return allDevices;
		}
		if (name.equals("genre_facet")) {
			return getString("genre");
		}
		if (name.equals("geographic")) {
			return getString("region");
		}
		if (name.equals("geographic_facet")) {
			String region = getString("region");
			return (region == null) ? null : region.split("\r\n");
		}
		if (name.equals("target_audience_full")) {
			String targetAudience = getString("target_audience");
			return (targetAudience == null || targetAudience.trim().length() == 0) ? null
					: targetAudience;
		}
		if (name.equals("keywords")) {
			return getStringOrEmptyString("title") + "\r\n"
					+ getStringOrEmptyString("subTitle") + "\r\n"
					+ getStringOrEmptyString("author") + "\r\n"
					+ getStringOrEmptyString("author2") + "\r\n"
					+ getStringOrEmptyString("description") + "\r\n"
					+ getStringOrEmptyString("subject") + "\r\n"
					+ getStringOrEmptyString("language") + "\r\n"
					+ getStringOrEmptyString("publisher") + "\r\n"
					+ getStringOrEmptyString("publishDate") + "\r\n"
					+ getStringOrEmptyString("edition") + "\r\n"
					+ getStringOrEmptyString("isbn") + "\r\n"
					+ getStringOrEmptyString("issn") + "\r\n"
					+ getStringOrEmptyString("upc") + "\r\n"
					+ getStringOrEmptyString("lccn") + "\r\n"
					+ getStringOrEmptyString("series") + "\r\n"
					+ getStringOrEmptyString("topic") + "\r\n"
					+ getStringOrEmptyString("genre") + "\r\n"
					+ getStringOrEmptyString("region") + "\r\n"
					+ getStringOrEmptyString("era") + "\r\n"
					+ getStringOrEmptyString("target_audience") + "\r\n"
					+ getStringOrEmptyString("notes") + "\r\n"
					+ getStringOrEmptyString("source") + "\r\n";
		}
		if (name.equals("format_boost")) {
			if ("active".equals(getString("status"))) {
				return 575;
			}
			return 0;
		}
		if (name.equals("language_boost")) {
			if ("active".equals(getString("status"))
					&& "English".equalsIgnoreCase(getString("language"))) {
				return 300;
			}
			return 0;
		}
		if (name.equals("num_holdings")) {
			if (!"active".equals(getString("status"))) {
				return null;
			}

			if ("free".equalsIgnoreCase(getString("accessType"))) {
				return 25;
			}
			return getString("availableCopies");
		}
		if (name.equals("available_at")) {
			if (!"active".equals(getString("status"))) {
				return null;
			}
			String source = getString("source");
			if ("Freegal".equalsIgnoreCase(source)) {
				return "Freegal";
			}
			try {
				int availableCopies = Integer
						.parseInt(getString("availableCopies"));
				if (availableCopies > 0) {
					return "Online";
				}
			} catch (NumberFormatException e) {
			}
			return null;
		}
		if (name.equals("date_added")) {
			String dateStr = getString("date_added");
			if (dateStr != null) {
				try {
					Date date = new Date(Long.parseLong(dateStr) * 1000);
					DateFormat df = new SimpleDateFormat(
							"yyyy-MM-dd'T'HH:mm:ss'Z'");
					df.setTimeZone(TimeZone.getTimeZone("UTC"));
					dateStr = df.format(date);
				} catch (NumberFormatException e) {
				}
			}
			return dateStr;
		}
		if (name.equals("bib_suppression")) {
			String status = getString("status");
			if ("deleted".equalsIgnoreCase(status)) {
				return "suppressed";
			}
			return "notsuppressed";
		}
		if (name.equals("rating")) {
			return String.valueOf(getNormalizedRating());
		}
		if (name.equals("rating_facet")) {
			double rating = getNormalizedRating();
			if (rating > 4.5) {
				return "fiveStar";
			} else if (rating > 3.5) {
				return "fourStar";
			} else if (rating > 2.5) {
				return "threeStar";
			} else if (rating > 1.5) {
				return "twoStar";
			} else if (rating > 0.0001) {
				return "oneStar";
			} else {
				return "Unrated";
			}
		}
		if (name.equals("allfields")) {
			StringBuilder buf = new StringBuilder();
			for (Object value : properties.values()) {
				if (value != null) {
					buf.append(" " + value.toString());
				}
			}
			return buf.toString();
		}
		if (name.equals("title_sort")) {
			String title = getString("title");
			if (title == null || title.length() == 0) {
				return null;
			}
			return title.toLowerCase().replaceAll("^(the|an|a|el|la)\\s", "");
		}
		if (name.equals("time_since_added")) {
			return null;
		}
		String value = getString(name);
		if (name.equals("author2") || name.equals("edition")
				|| name.equals("isbn") || name.equals("issn")
				|| name.equals("upc") || name.equals("lccn")
				|| name.equals("series") || name.equals("topic")
				|| name.equals("genre") || name.equals("era")) {
			return (value == null) ? null : value.split("\r\n");
		}
		return value;
	}

	private String getStringOrEmptyString(String name) {
		String val = getString(name);
		return (val == null) ? "" : val;
	}

	private Set<String> getFormats(Properties itemTypeFormatMap) {
		// if formats have been determined, then simply return them
		if (formats != null) {
			return formats;
		}
		// determine formats
		formats = new HashSet<String>();
		String source = getString("source");
		if ("3M".equalsIgnoreCase(source)) {
			formats.add("EPUB");
			return formats;
		}

		// get formats from item types
		try {
			List<String> itemTypes = dao.getItemTypes(Long
					.parseLong(getString("id")));
			for (String type : itemTypes) {
				String format = translateValue(itemTypeFormatMap, type);
				if (format != null) {
					formats.add(format);
				} else {
                    formats.add(type);//is this ok?
                }
			}

		} catch (Exception e) {
		}
		return formats;
	}

	private double getNormalizedRating() {
		Double rating = -2.5;
		long id = Long.parseLong(getString("id"));
		try {
			rating = dao.getRating(id);
		} catch (SQLException e) {
		}
		return rating == null ? -2.5 : rating;
	}

	private String translateValue(Properties map, String inVal) {
		String outVal = translateCaseSensitive(map, inVal);
        if(outVal!=null) {
            return outVal;
        } else {
            outVal = translateCaseSensitive(map, inVal.toLowerCase());
            if(outVal!=null) {
                return outVal;
            } else {
                outVal = translateCaseSensitive(map, inVal.toUpperCase());
            }
        }
		return outVal;
	}

    private String translateCaseSensitive(Properties map, String inVal) {
        String outVal = map.getProperty(inVal);
        if (outVal != null) {
            outVal = outVal.replaceAll("^\"|\"$", "");
        }
        return outVal;
    }

	protected String readFile(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		StringBuilder buf = new StringBuilder();
		while ((line = reader.readLine()) != null) {
			buf.append(line);
			buf.append("\r\n");
		}
		reader.close();
		return buf.toString();
	}
}
