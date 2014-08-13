package org.vufind.processors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.ConnectionProvider;
import org.vufind.Util;
import org.vufind.config.DynamicConfig;

public class AlphaBrowseProcessor {
    final static Logger logger = LoggerFactory.getLogger(AlphaBrowseProcessor.class);

	/*private TreeMap<String, ArrayList<String>> titleBrowseInfo;
	private TreeMap<String, ArrayList<String>> authorBrowseInfo;
	private TreeMap<String, ArrayList<String>> callNumberBrowseInfo;
	private TreeMap<String, ArrayList<String>> subjectBrowseInfo;*/

	public void process(DynamicConfig config) {
        Connection vufindConn = null;
        try {
            vufindConn = ConnectionProvider.getDataSource(config, ConnectionProvider.PrintOrEContent.PRINT).getConnection();
        } catch (SQLException e) {
            logger.error("Couldn't get a database connection in AlphaBrowseProcessor");
            return;
        }
        logger.info("Building Alphabetic Browse tables");
		try {
			//Run queries to create alphabetic browse tables from resources table
			try {
				//Clear the current browse table
				logger.info("Truncating title table");
				PreparedStatement truncateTable = vufindConn.prepareStatement("TRUNCATE title_browse");
				truncateTable.executeUpdate();
				
				//Get all resources
				logger.info("Loading titles for browsing");
				PreparedStatement resourcesByTitleStmt = vufindConn.prepareStatement("SELECT count(id) as numResults, title, title_sort FROM `resource` WHERE (deleted = 0 OR deleted IS NULL) GROUP BY title_sort ORDER BY title_sort", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				ResultSet resourcesByTitleRS = resourcesByTitleStmt.executeQuery();

				logger.info("Saving titles to database");
				PreparedStatement insertBrowseRow = vufindConn.prepareStatement("INSERT INTO title_browse (id, numResults, value) VALUES (?, ?, ?)");
				int curRow = 1;
				while (resourcesByTitleRS.next()){
					String titleSort = resourcesByTitleRS.getString("title_sort");
					if (titleSort != null && titleSort.length() > 0){
						insertBrowseRow.setLong(1, curRow++);
						insertBrowseRow.setLong(2, resourcesByTitleRS.getLong("numResults"));
						insertBrowseRow.setString(3, Util.trimTo(255, resourcesByTitleRS.getString("title")));
						insertBrowseRow.executeUpdate();
						//System.out.print(".");
					}
				}
				resourcesByTitleRS.close();
				insertBrowseRow.close();
				
				logger.info("Added " + (curRow -1) + " rows to title browse table");
			} catch (SQLException e) {
				logger.error("Error creating title browse table", e);
			}
			
			try {
				//Clear the current browse table
				logger.info("Truncating author table");
				PreparedStatement truncateTable = vufindConn.prepareStatement("TRUNCATE author_browse");
				truncateTable.executeUpdate();
				
				//Get all resources
				logger.info("Loading authors for browsing");
				PreparedStatement resourcesByTitleStmt = vufindConn.prepareStatement("SELECT count(id) as numResults, author FROM `resource` WHERE (deleted = 0 OR deleted IS NULL) GROUP BY lower(author) ORDER BY lower(author)", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				ResultSet groupedSortedRS = resourcesByTitleStmt.executeQuery();

				logger.info("Saving authors to database");
				PreparedStatement insertBrowseRow = vufindConn.prepareStatement("INSERT INTO author_browse (id, numResults, value) VALUES (?, ?, ?)");
				int curRow = 1;
				while (groupedSortedRS.next()){
					String sortKey = groupedSortedRS.getString("author");
					if (sortKey != null && sortKey.length() > 0){
						insertBrowseRow.setLong(1, curRow++);
						insertBrowseRow.setLong(2, groupedSortedRS.getLong("numResults"));
						insertBrowseRow.setString(3, Util.trimTo(255, groupedSortedRS.getString("author")));
						insertBrowseRow.executeUpdate();
						//System.out.print(".");
					}
				}
				groupedSortedRS.close();
				
				logger.info("Added " + (curRow -1) + " rows to author browse table");
			} catch (SQLException e) {
				logger.error("Error creating author browse table", e);
			}

			//Setup subject browse
			try {
				//Clear the subject browse table
				logger.info("Truncating subject table");
				PreparedStatement truncateTable = vufindConn.prepareStatement("TRUNCATE subject_browse");
				truncateTable.executeUpdate();
				
				//Get all resources
				logger.info("Loading subjects for browsing");
				PreparedStatement resourcesByTitleStmt = vufindConn.prepareStatement("SELECT count(resource.id) as numResults, subject from resource inner join resource_subject on resource.id = resource_subject.resourceId inner join subject on subjectId = subject.id WHERE (deleted = 0 OR deleted is NULL) group by subjectId ORDER BY lower(subject)", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				ResultSet groupedSortedRS = resourcesByTitleStmt.executeQuery();

				logger.info("Saving subjects to database");
				PreparedStatement insertBrowseRow = vufindConn.prepareStatement("INSERT INTO subject_browse (id, numResults, value) VALUES (?, ?, ?)");
				int curRow = 1;
				while (groupedSortedRS.next()){
					String sortKey = groupedSortedRS.getString("subject");
					if (sortKey != null && sortKey.length() > 0){
						insertBrowseRow.setLong(1, curRow++);
						insertBrowseRow.setLong(2, groupedSortedRS.getLong("numResults"));
						insertBrowseRow.setString(3, Util.trimTo(255, groupedSortedRS.getString("subject")));
						insertBrowseRow.executeUpdate();
						//System.out.print(".");
					}
				}
				groupedSortedRS.close();
				logger.info("Added " + (curRow -1) + " rows to subject browse table");
			} catch (SQLException e) {
				logger.error("Error creating subject browse table", e);
			}
			
			//Setup call number browse
			try {
				//Clear the call number browse table
				logger.info("Truncating callnumber_browse table");
				PreparedStatement truncateTable = vufindConn.prepareStatement("TRUNCATE callnumber_browse");
				truncateTable.executeUpdate();
				
				//Get all resources
				logger.info("Loading call numbers for browsing");
				PreparedStatement resourcesByTitleStmt = vufindConn.prepareStatement("SELECT count(resource.id) as numResults, callnumber from resource inner join (select resourceId, callnumber FROM resource_callnumber group by resourceId, callnumber) titleCallNumber on resource.id = resourceId where (deleted = 0 OR deleted is NULL) group by callnumber ORDER BY lower(callnumber)", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				ResultSet groupedSortedRS = resourcesByTitleStmt.executeQuery();

				logger.info("Saving call numbers to database");
				PreparedStatement insertBrowseRow = vufindConn.prepareStatement("INSERT INTO callnumber_browse (id, numResults, value) VALUES (?, ?, ?)");
				int curRow = 1;
				while (groupedSortedRS.next()){
					String sortKey = groupedSortedRS.getString("callnumber");
					if (sortKey != null && sortKey.length() > 0){
						insertBrowseRow.setLong(1, curRow++);
						insertBrowseRow.setLong(2, groupedSortedRS.getLong("numResults"));
						insertBrowseRow.setString(3, groupedSortedRS.getString("callnumber"));
						insertBrowseRow.executeUpdate();
						//System.out.print(".");
					}
				}
				groupedSortedRS.close();
				logger.info("Added " + (curRow -1) + " rows to call number browse table");
			} catch (SQLException e) {
				logger.error("Error creating callnumber browse table", e);
			}
		} catch (Error e) {
			System.out.println("Error updating Alphabetic Browse");
			e.printStackTrace();
			logger.error("Error updating Alphabetic Browse", e);
		}
	}
}
