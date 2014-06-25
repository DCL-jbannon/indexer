package org.econtent;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Data access object for the EContentRecord.
 */
public class EContentRecordDAO {
    private static EContentRecordDAO instance = null;

    private Logger logger = null;
	private Connection connection = null;
    private PreparedStatement insertEcontentItemPS = null;

	private final Map<Long, Double> ratingsCache;
	private final Map<Long, List<String>> itemTypesCache;

	private EContentRecordDAO() {
		ratingsCache = new HashMap<Long, Double>();
		itemTypesCache = new HashMap<Long, List<String>>();
        logger = Logger.getLogger(EContentRecordDAO.class);
	}

	/**
	 * Setup this class before it can be used. Should be called only once.
	 *
	 * @param connection
	 */
	public static void initialize(Connection connection) throws SQLException {
        instance = new EContentRecordDAO();
        instance.connection = connection;
	}

	/**
	 * Get an instance of this class.
	 * 
	 * @return
	 */
	public static EContentRecordDAO getInstance() {
		return instance;
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

	/**
	 * Delete an eContent record and associated items.
	 * 
	 * @param id
	 * @throws SQLException
	 */
	public void delete(long id) throws SQLException {
		deleteItems(id);
		PreparedStatement stmt = connection
				.prepareStatement("DELETE FROM econtent_record WHERE id = ?");
		stmt.setLong(1, id);
		stmt.executeUpdate();
		stmt.close();
	}

	/**
	 * Delete items associated with the given eContent record.
	 * 
	 * @param id
	 * @throws SQLException
	 */
	public void deleteItems(long id) throws SQLException {
		PreparedStatement stmt = connection
				.prepareStatement("DELETE FROM econtent_item WHERE recordId = ?");
		stmt.setLong(1, id);
		stmt.executeUpdate();
		stmt.close();
	}

    /**
     * DELETEs all Freegal econtent_item since we get everything new with each Freegal update
     * @throws SQLException
     */
    public void deleteFreegalItems() throws SQLException {
        PreparedStatement stmt = connection
                .prepareStatement(
                        "DELETE ei\n" +
                        "    FROM dclecontent_prod.econtent_item ei, dclecontent_prod.econtent_record er\n" +
                        "    WHERE\n" +
                        "    er.id = ei.recordId\n" +
                        "    AND er.source = 'Freegal'");
        stmt.executeUpdate();
        stmt.close();
    }

	/**
	 * Find a record given its ID.
	 * 
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public EContentRecord find(long id) throws SQLException {
		EContentRecord record = null;
		PreparedStatement stmt = connection
				.prepareStatement("select * from econtent_record where id=?");
		stmt.setLong(1, id);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			record = new EContentRecord(rs);
		}
		rs.close();
		stmt.close();
		return record;
	}

    private HashMap<String, EContentRecord> allRecords = null;

    private HashMap<String, EContentRecord> selectAllFreegalRecordsInDB() throws SQLException {
        if(allRecords==null) {
            allRecords = new HashMap<String, EContentRecord>();
            PreparedStatement stmt = connection
                    .prepareStatement("SELECT * FROM econtent_record WHERE source = 'Freegal'");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                allRecords.put( rs.getString("title")+rs.getString("author"), new EContentRecord(rs));
            }
            rs.close();
            stmt.close();
        }

        return allRecords;
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

		return allRecords.get(title+author);
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
		Set<String> columns = record.getSetProperties();
		for (String column : columns) {
			columnList += separator + column;
			paramList += separator + "?";
			separator = ",";
		}
		String query = "INSERT INTO econtent_record " + "(" + columnList + ") "
				+ "VALUES " + "(" + paramList + ")";
		logger.debug(query);
		PreparedStatement stmt = connection.prepareStatement(query,
				PreparedStatement.RETURN_GENERATED_KEYS);
		int index = 1;
		for (String column : columns) {
			setEContentRecordParameter(stmt, index++, column, record);
		}
		stmt.executeUpdate();
		ResultSet generatedKeys = stmt.getGeneratedKeys();
		long id = -1;
		if (generatedKeys.next()) {
			id = generatedKeys.getLong(1);
		}
		stmt.close();
		record.set("id", id);
		return id;
	}

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
		Set<String> columns = record.getSetProperties();
		for (String column : columns) {
			columnList += separator + column + "=?";
			separator = ",";
		}
		String query = "UPDATE econtent_record SET " + columnList
				+ " WHERE id=?";

		logger.debug(query);
		PreparedStatement stmt = connection.prepareStatement(query);
		int index = 1;
		for (String column : columns) {
			setEContentRecordParameter(stmt, index++, column, record);
		}
		stmt.setLong(index, record.getInteger("id"));
		int rowsUpdated = stmt.executeUpdate();
		stmt.close();
		return rowsUpdated;
	}

	/**
	 * Add an item record to an existing eContent record.
	 * 
	 * @param item
	 * @throws SQLException
	 */
	public void addEContentItem(EContentItem item) throws SQLException {
        if(this.insertEcontentItemPS == null || this.insertEcontentItemPS.isClosed()) {
            String query = "INSERT INTO econtent_item " +
                    "(recordId, link, item_type, notes, addedBy, date_added, date_updated) " +
                    "VALUES (?,?,?,?,?,?,?)";
            this.insertEcontentItemPS = connection.prepareStatement(query, PreparedStatement.NO_GENERATED_KEYS);
        }
        this.insertEcontentItemPS.setString(1, item.getRecordId());
        this.insertEcontentItemPS.setString(2, item.getLink());
        this.insertEcontentItemPS.setString(3, item.getType());
        if(item.getNotes()==null) {
            this.insertEcontentItemPS.setNull(4, Types.VARCHAR);
        } else {

            this.insertEcontentItemPS.setString(4, item.getNotes());
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
        this.insertEcontentItemPS.executeBatch();
        if(clean) {
            this.insertEcontentItemPS.close();
            this.insertEcontentItemPS = null;
        }

    }

	/**
	 * Set all Freegal econtent record status to "deleted".
	 * 
	 * @return
	 * @throws SQLException
	 */
	public int flagAllFreegalRecordsAsDeleted() throws SQLException {
		PreparedStatement stmt = connection
				.prepareStatement("update econtent_record set status='deleted' where lower(source)='freegal'");
		int count = stmt.executeUpdate();
		stmt.close();
		return count;
	}

	public int deleteFreegalRecordsFlaggedAsDeleted() throws SQLException {
		PreparedStatement stmt = connection
				.prepareStatement("select id from econtent_record where status='deleted' and lower(source)='freegal'");
		ResultSet rs = stmt.executeQuery();
		int count = 0;
		while (rs.next()) {
			delete(rs.getLong(1));
		}
		rs.close();
		return count;
	}

	private void setEContentRecordParameter(PreparedStatement stmt, int index,
			String column, EContentRecord record) throws SQLException {
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
