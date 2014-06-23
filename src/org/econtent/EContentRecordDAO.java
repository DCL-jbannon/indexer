package org.econtent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
	private static Logger logger = Logger.getLogger(EContentRecordDAO.class);
	private static Connection connection = null;
	private static EContentRecordDAO instance = null;

	private final Map<Long, Double> ratingsCache;
	private final Map<Long, List<String>> itemTypesCache;

	protected EContentRecordDAO() {
		ratingsCache = new HashMap<Long, Double>();
		itemTypesCache = new HashMap<Long, List<String>>();
	}

	/**
	 * Setup this class before it can be used. Should be called only once.
	 * 
	 * @param connection
	 */
	public static void initialize(Connection connection) {
		EContentRecordDAO.connection = connection;
	}

	/**
	 * Get an instance of this class.
	 * 
	 * @return
	 */
	public static EContentRecordDAO getInstance() {
		if (connection == null) {
			throw new IllegalStateException(EContentRecordDAO.class.getName()
					+ " initialize class method has not been called.");
		}
		if (instance == null) {
			instance = new EContentRecordDAO();
		}
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
		EContentRecord record = null;
		PreparedStatement stmt = connection
				.prepareStatement("select * from econtent_record where title=? and author=?");
		stmt.setString(1, title);
		stmt.setString(2, author);
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			record = new EContentRecord(rs);
		}
		rs.close();
		stmt.close();
		return record;
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
	 * @param properties
	 * @return
	 * @throws SQLException
	 */
	public long addEContentItem(Map<String, Object> item) throws SQLException {
		String columnList = "";
		String paramList = "";
		String separator = "";
		Set<String> columns = item.keySet();
		for (String column : columns) {
			columnList += separator + column;
			paramList += separator + "?";
			separator = ",";
		}
		String query = "INSERT INTO econtent_item " + "(" + columnList + ") "
				+ "VALUES " + "(" + paramList + ")";
		logger.debug(query);
		PreparedStatement stmt = connection.prepareStatement(query,
				PreparedStatement.RETURN_GENERATED_KEYS);
		int index = 1;
		for (String column : item.keySet()) {
			setEContentItemParameter(stmt, index++, column, item);
		}
		stmt.executeUpdate();
		ResultSet generatedKeys = stmt.getGeneratedKeys();
		long id = -1;
		if (generatedKeys.next()) {
			id = generatedKeys.getLong(1);
		}
		stmt.close();
		item.put("id", id);
		return id;
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
