package org.vufind.econtent;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data access object for the EContentRecord.
 */
public class EContentRecordDAO {
    final static Logger logger = LoggerFactory.getLogger(EContentRecordDAO.class);
    private static EContentRecordDAO instance = null;

	private BasicDataSource dataSource = null;
    private Connection connection = null;

	private final Map<Long, Double> ratingsCache;
	private final Map<Long, List<String>> itemTypesCache;

	public EContentRecordDAO(BasicDataSource dataSource) {
        this.ratingsCache = new HashMap<Long, Double>();
        this.itemTypesCache = new HashMap<Long, List<String>>();
        this.dataSource = dataSource;
	}

	/**
	 * Get the average rating of a given eContent record.
	 * 
	 * @param recordId
	 * @return
	 * @throws SQLException
	 */
	public Double getRating(long recordId) throws SQLException {
		loadRatingsCache();
		return ratingsCache.get(Long.valueOf(recordId));
	}

	/**
	 * Get the item types of a given eContent record.
	 * 
	 * @param recordId
	 * @return
	 * @throws SQLException
	 */
	public List<String> getItemTypes(long recordId) throws SQLException {
		loadItemTypesCache();
		return itemTypesCache.get(Long.valueOf(recordId));
	}

    private PreparedStatement deleteEcontentRecord = null;
	/**
	 * Delete an eContent record and associated items.
	 * 
	 * @param id
	 * @throws SQLException
	 */
	public void delete(long id) throws SQLException {
		deleteItems(id);
        String query = "DELETE FROM econtent_record WHERE id = ?";
        this.deleteEcontentRecord = getActivePreparedStatement(this.deleteEcontentRecord, query);
        this.deleteEcontentRecord.setLong(1, id);
        this.deleteEcontentRecord.executeUpdate();
	}

    private PreparedStatement deleteEcontentItems = null;
	/**
	 * Delete items associated with the given eContent record.
	 * 
	 * @param id
	 * @throws SQLException
	 */
	public void deleteItems(long id) throws SQLException {
        String query = "DELETE FROM econtent_item WHERE recordId = ?";
        this.deleteEcontentItems = getActivePreparedStatement(this.deleteEcontentRecord, query);
        this.deleteEcontentItems.setLong(1, id);
        this.deleteEcontentItems.executeUpdate();
	}

    private PreparedStatement deleteFreegalEcontentItems = null;
    /**
     * DELETEs all Freegal econtent_item since we get everything new with each Freegal update
     * @throws SQLException
     */
    public void deleteFreegalItems() throws SQLException {
        String query =
                "DELETE ei\n" +
                "    FROM dclecontent_prod.econtent_item ei, dclecontent_prod.econtent_record er\n" +
                "    WHERE\n" +
                "    er.id = ei.recordId\n" +
                "    AND er.source = 'Freegal'";
        this.deleteFreegalEcontentItems = getActivePreparedStatement(this.deleteFreegalEcontentItems, query);
        this.deleteFreegalEcontentItems.executeUpdate();
    }

    private PreparedStatement getRecordPS = null;
	/**
	 * Find a record given its ID.
	 * 
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public EContentRecord find(long id) throws SQLException {
        EContentRecord record = null;
        this.getRecordPS = getActivePreparedStatement(this.getRecordPS, "SELECT * FROM econtent_record WHERE id=?");
        this.getRecordPS.setLong(1, id);
		ResultSet rs = this.getRecordPS.executeQuery();
		if (rs.first()) {
			record = new EContentRecord(this, rs);
		}
		rs.close();
		return record;
	}

    private HashMap<String, EContentRecord> allRecords = null;

    private PreparedStatement getAllFreegalRecordPS = null;
    private HashMap<String, EContentRecord> selectAllFreegalRecordsInDB() throws SQLException {
        if(allRecords==null) {
            allRecords = new HashMap<String, EContentRecord>();
            this.getAllFreegalRecordPS = getActivePreparedStatement(this.getAllFreegalRecordPS,
                    "SELECT * FROM econtent_record WHERE lower(source)='freegal'");
            ResultSet rs = this.getAllFreegalRecordPS.executeQuery();
            while (rs.next()) {
                allRecords.put( getAllRecordsKey(rs.getString("title"),rs.getString("author")), new EContentRecord(this, rs));
            }
            rs.close();
        }

        return allRecords;
    }

    private String getAllRecordsKey(String title, String author) {
        return title+author;
    }

	/**
	 * Find a record by title and author.
	 * 
	 * @param title
	 * @param author
	 * @return
	 * @throws SQLException
	 */
	public EContentRecord findByTitleAndAuthor(String title, String author)
			throws SQLException {
        HashMap<String, EContentRecord> allRecords = selectAllFreegalRecordsInDB();

		return allRecords.get(getAllRecordsKey(title,author));
	}

	/**
	 * Save the given record.
	 * 
	 * @param record
	 * @return
	 * @throws SQLException
	 */
	public long save(EContentRecord record) throws SQLException {
		return (record.get("id") == null) ? insert(record) : update(record);
	}

    private PreparedStatement insertRecordPS = null;
	/**
	 * Insert the given record.
	 * 
	 * @param record
	 * @return
	 * @throws SQLException
	 */
	public long insert(EContentRecord record) throws SQLException {
		String columnList = "";
		String paramList = "";
		String separator = "";
		List<String> columns = record.getSetProperties();
		for (String column : columns) {
			columnList += separator + column;
			paramList += separator + "?";
			separator = ",";
		}
		String query = "INSERT INTO econtent_record " + "(" + columnList + ") "
				+ "VALUES " + "(" + paramList + ")";
		logger.debug(query);
        this.insertRecordPS = getActivePreparedStatement(this.insertRecordPS, query);
		int index = 1;
		for (String column : columns) {
			setEContentRecordParameter(this.insertRecordPS, index++, column, record);
		}
        this.insertRecordPS.executeUpdate();
		ResultSet generatedKeys = this.insertRecordPS.getGeneratedKeys();
		long id = -1;
		if (generatedKeys.next()) {
			id = generatedKeys.getLong(1);
		}
		record.set("id", id);
        allRecords.put(getAllRecordsKey(record.getString("title"), record.getString("author")), record);
		return id;
	}

    private String lastUpdateRecordsQuery = null;
    private PreparedStatement updateRecordPS = null;
	/**
	 * Update the given record.
	 * 
	 * @param record
	 * @return
	 * @throws SQLException
	 */
	public long update(EContentRecord record) throws SQLException {
		String columnList = "";
		String separator = "";
        if(record.getString("title") != null && record.getString("title").equals("Gluck: Trio Sonatas")) {
            int ii = 0;
            ii++;
        }

		List<String> columns = record.getSetProperties();
        columns.remove("id");
		for (String column : columns) {
			columnList += separator + column + "=?";
			separator = ",";
		}

        String query = "UPDATE econtent_record SET " + columnList
                + " WHERE id=?";

        if(!query.equals(lastUpdateRecordsQuery)) {
            lastUpdateRecordsQuery = query;
            if(updateRecordPS!=null) {
                this.updateRecordPS.close();
                this.updateRecordPS = null;
            }
        }

        this.updateRecordPS = getActivePreparedStatement(this.updateRecordPS, query);
        this.updateRecordPS.clearParameters();

		logger.debug(query);


		int index = 1;
		for (String column : columns) {
			setEContentRecordParameter(this.updateRecordPS, index, column, record);
            index++;
		}
        this.updateRecordPS.setLong(index, record.getInteger("id"));
		int rowsUpdated = 0;
        try {
            rowsUpdated = this.updateRecordPS.executeUpdate();
        } catch(Exception e) {
            e.printStackTrace();
        }

        allRecords.put(getAllRecordsKey(record.getString("title"), record.getString("author")), record);
		return rowsUpdated;
	}

    private Connection getActiveConnection(Connection connection) throws SQLException {
        try {
            if(connection == null || connection.isClosed()) {
                connection = this.dataSource.getConnection();
            }
        } catch (SQLException e) {
            connection = this.dataSource.getConnection();
        }
        return connection;
    }

    private PreparedStatement getActivePreparedStatement(PreparedStatement checkMe, String sqlStr) throws SQLException {
        connection = getActiveConnection(connection);
        PreparedStatement ret = null;
        try {
            if (checkMe == null || checkMe.isClosed() || connection == null || connection.isClosed()) {
                ret = connection.prepareStatement(sqlStr, PreparedStatement.RETURN_GENERATED_KEYS);
            } else {
                ret = checkMe;
            }
        } catch(SQLException e) {
            //Try again, it's possible for isClosed() to throw a SQLException, and we should try to make sure we can't
            //continue anyway
            ret = connection.prepareStatement(sqlStr, PreparedStatement.RETURN_GENERATED_KEYS);
        }
        return ret;
    }

    private PreparedStatement insertEcontentItemPS = null;
	/**
	 * Add an item record to an existing eContent record.
	 * 
	 * @param item
	 * @throws SQLException
	 */
	public void addEContentItem(EContentItem item) throws SQLException {
        String query = "INSERT INTO econtent_item " +
                "(recordId, link, item_type, notes, addedBy, date_added, date_updated) " +
                "VALUES (?,?,?,?,?,?,?)";
        this.insertEcontentItemPS = getActivePreparedStatement(this.insertEcontentItemPS, query);

        this.insertEcontentItemPS.setString(1, item.getRecordId());
        this.insertEcontentItemPS.setString(2, item.getLink());
        this.insertEcontentItemPS.setString(3, item.getType());
        if(item.getNotes()==null) {
            //this.insertEcontentItemPS.setNull(4, Types.VARCHAR); //Causes an error with our table definition
            this.insertEcontentItemPS.setString(4, ""); //This doesn't seem good
        } else {
            String notes = item.getNotes();
            if(notes.length() > 250) {
                System.out.println("Long notes: "+item.getNotes());
                notes = notes.substring(0, 240);
            }
            this.insertEcontentItemPS.setString(4, notes);
        }
        if(item.getAddedBy()==null) {
            this.insertEcontentItemPS.setNull(5, Types.VARCHAR);
        } else {
            this.insertEcontentItemPS.setString(5, item.getAddedBy());
        }
        //TODO should we really be handling dates as ints?
        if(item.getDateAdded() > -1) {
            this.insertEcontentItemPS.setInt(6, item.getDateAdded());
        } else {
            this.insertEcontentItemPS.setNull(6, Types.BIGINT);
        }
        if(item.getDateUpdated() > -1) {
            this.insertEcontentItemPS.setInt(7, item.getDateUpdated());
        } else {
            this.insertEcontentItemPS.setNull(7, Types.BIGINT);
        }

        this.insertEcontentItemPS.addBatch();
	}

    public void flushEContentItems(boolean clean) throws SQLException {
        try{
            this.insertEcontentItemPS.executeBatch();
        } catch(SQLException e) {
            logger.error("Error flushing EContentRecords", e);
        }
        if(clean) {
            this.insertEcontentItemPS.close();
            this.insertEcontentItemPS = null;
        }

    }

    private PreparedStatement flagFreegalAsDelPS = null;
	/**
	 * Set all Freegal econtent record status to "deleted".
	 * 
	 * @return
	 * @throws SQLException
	 */
	public int flagAllFreegalRecordsAsDeleted() throws SQLException {
        String query = "update econtent_record set status='deleted' where lower(source)='freegal'";
        this.flagFreegalAsDelPS = getActivePreparedStatement(this.flagFreegalAsDelPS, query);

		return this.flagFreegalAsDelPS.executeUpdate();
	}

    private PreparedStatement selectFlaggedFreegalPS = null;
	public int deleteFreegalRecordsFlaggedAsDeleted() throws SQLException {
		String query = "select id from econtent_record where status='deleted' and lower(source)='freegal'";
        this.selectFlaggedFreegalPS = getActivePreparedStatement(this.selectFlaggedFreegalPS, query);
		ResultSet rs = this.selectFlaggedFreegalPS.executeQuery();
		int count = 0;
		while (rs.next()) {
			delete(rs.getLong(1));
		}
		rs.close();
		return count;
	}

	private void setEContentRecordParameter(PreparedStatement stmt, int index,
			String column, EContentRecord record) throws SQLException {
        if( column.equals("addedBy")) {
            int ii = 0;
            ii++;
        }
		if (column.equals("id") || column.equals("availableCopies")
				|| column.equals("onOrderCopies") || column.equals("addedBy")
				|| column.equals("date_added") || column.equals("date_updated")
				|| column.equals("reviewedBy") || column.equals("reviewDate")
				|| column.equals("trialTitle")) {
			if (record.get(column) == null) {
				// pay special attention to NULL values in integer columns
				stmt.setNull(index, java.sql.Types.BIGINT);
			} else {
				stmt.setLong(index, record.getInteger(column));
			}
		} else {
			stmt.setString(index, record.getString(column));
		}
	}

	private void setEContentItemParameter(PreparedStatement stmt, int index,
			String column, Map<String, Object> item) throws SQLException {
		if (column.equals("id") || column.equals("addedBy")
				|| column.equals("date_added") || column.equals("date_updated")
				|| column.equals("reviewedBy") || column.equals("libraryId")
				|| column.equals("recordId")) {
			if (item.get(column) == null) {
				// pay special attention to NULL values in integer columns
				stmt.setNull(index, java.sql.Types.BIGINT);
			} else {
				stmt.setLong(index,
						Integer.valueOf(item.get(column).toString()));
			}
		} else {
			stmt.setString(index, (String) item.get(column));
		}
	}

	private void loadRatingsCache() throws SQLException {
		if (ratingsCache.isEmpty()) {
            connection = getActiveConnection(connection);
			// load average rating for all records into a map for quick lookup
			PreparedStatement stmt = connection
					.prepareStatement("select recordId,avg(rating) as avgRating from econtent_rating group by recordId");
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				ratingsCache.put(rs.getLong("recordId"),
						rs.getDouble("avgRating"));
			}
			rs.close();
			stmt.close();
		}
	}

	private void loadItemTypesCache() throws SQLException {
		if (itemTypesCache.isEmpty()) {
            connection = getActiveConnection(connection);
			// load item types for all records into a map for quick lookup
			PreparedStatement stmt = connection
					.prepareStatement("select recordId,item_type from econtent_item order by recordId,id");
			ResultSet rs = stmt.executeQuery();
			Long currentId = null;
			while (rs.next()) {
				Long id = rs.getLong("recordId");
				if (!id.equals(currentId)) {
					currentId = id;
					itemTypesCache.put(id, new ArrayList<String>());
				}
				List<String> types = itemTypesCache.get(id);
				types.add(rs.getString("item_type"));
				itemTypesCache.put(id, types);
			}
			rs.close();
			stmt.close();
		}
	}
}
