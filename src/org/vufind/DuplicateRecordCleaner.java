package org.vufind;

import org.vufind.processors.UpdateResourceInformation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by jbannon on 5/1/2015.
 */
public class DuplicateRecordCleaner {

    public static void Clean(Connection vufindConn)
    {
        PreparedStatement getDuplicateRecordIdsStmt;
        PreparedStatement getBestResourceVersionStmt;

        try {
            getDuplicateRecordIdsStmt = vufindConn.prepareStatement(
                    "SELECT DISTINCT r2.id, r2.record_ID, r2.deleted\n" +
                            "FROM resource r1 LEFT JOIN resource r2 ON(r1.record_id = r2.record_id AND r1.id < r2.id AND r2.source='VuFind')\n" +
                            "WHERE " +
                            "\tr1.source='VuFind'\n" +
                            "\tAND r2.id IS NOT NULL", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

            getBestResourceVersionStmt = vufindConn.prepareStatement(
                    "SELECT DISTINCT id FROM (" +
                            "SELECT id " +
                            "FROM resource " +
                            "WHERE record_id = ? " +
                            "ORDER BY id ASC) AS r1 " +
                            "LIMIT 1", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

            PreparedStatement transferCommentsStmt = vufindConn.prepareStatement("UPDATE comments SET resource_id = ? WHERE resource_id = ?");
            PreparedStatement transferTagsStmt = vufindConn.prepareStatement("UPDATE resource_tags SET resource_id = ? WHERE resource_id = ?");
            PreparedStatement transferRatingsStmt = vufindConn.prepareStatement("UPDATE user_rating SET resourceid = ? WHERE resourceid = ?");
            PreparedStatement transferReadingHistoryStmt = vufindConn.prepareStatement("UPDATE user_reading_history SET resourceId = ? WHERE resourceId = ?");
            PreparedStatement transferUserResourceStmt = vufindConn.prepareStatement("UPDATE user_resource SET resource_id = ? WHERE resource_id = ?");

            PreparedStatement deleteResourcePermanentStmt = vufindConn.prepareStatement("DELETE FROM resource WHERE id = ?");
            PreparedStatement deleteResourceCallNumberPermanentStmt = vufindConn.prepareStatement("DELETE FROM resource_callnumber WHERE resourceId = ?");
            PreparedStatement deleteResourceSubjectPermanentStmt = vufindConn.prepareStatement("DELETE FROM resource_subject WHERE resourceId = ?");

            UpdateResourceInformation.logger.info("Cleaning up resources table");

            //Get a list of the total number of resources
            //Can return multiple copies and also return the "correct" record in edge cases, so be careful
            ResultSet duplicateIdRS = getDuplicateRecordIdsStmt.executeQuery();
            int resourcesProcessed = 0;
            while (duplicateIdRS.next()) {
                //SELECT DISTINCT r2.id, r2.record_ID, r2.deleted
                String duplicateResourceId = duplicateIdRS.getString(1);
                String recordId = duplicateIdRS.getString(2);


                boolean duplicateWasDeleted = duplicateIdRS.getInt(3) > 0 ? true : false;

                String bestResourceId = getBestResourceVersion(recordId, getBestResourceVersionStmt);
                if (bestResourceId.equals(duplicateResourceId) || bestResourceId.equals("")) {
                    continue;
                }

                UpdateResourceInformation.logger.error("Whoa! We found a duplicate recordId["+recordId+"] resourceId["+duplicateResourceId+"]");


                UpdateResourceInformation.logger.info("Transferring resource[" + duplicateResourceId + "] for record id " + recordId);
                transferUserInfo(duplicateResourceId, bestResourceId,
                        transferCommentsStmt,
                        transferTagsStmt,
                        transferRatingsStmt,
                        transferReadingHistoryStmt,
                        transferUserResourceStmt);


                deleteResourcePermanently(duplicateResourceId,
                        deleteResourcePermanentStmt,
                        deleteResourceCallNumberPermanentStmt,
                        deleteResourceSubjectPermanentStmt);

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param resourceId
     */
    private static  void deleteResourcePermanently(String resourceId,
                                                   PreparedStatement deleteResourcePermanentStmt,
                                                   PreparedStatement deleteResourceCallNumberPermanentStmt,
                                                   PreparedStatement deleteResourceSubjectPermanentStmt) {
        try {
            deleteResourcePermanentStmt.setString(1, resourceId);
            deleteResourcePermanentStmt.executeUpdate();
            deleteResourceCallNumberPermanentStmt.setString(1, resourceId);
            deleteResourceCallNumberPermanentStmt.executeUpdate();
            deleteResourceSubjectPermanentStmt.setString(1, resourceId);
            deleteResourceSubjectPermanentStmt.executeUpdate();
        } catch (SQLException e) {
            UpdateResourceInformation.logger.error("Error deleting resource permanently " + resourceId, e);
        }
    }

    /**
     * This returns the "best" resource in the database for a recordId. It sorts by resource_id with the smallest id
     * winning.
     *
     * @param recordId
     * @return
     */
    private static String getBestResourceVersion(String recordId, PreparedStatement getBestResourceVersionStmt) {
        if(recordId.equals("1118588"))            {
            int ii = 0;
            ii++;
        }
        try {
            getBestResourceVersionStmt.clearParameters();
            getBestResourceVersionStmt.setString(1, recordId);
            ResultSet bestRS = getBestResourceVersionStmt.executeQuery();
            bestRS.first();
            return bestRS.getString(1);
        } catch (SQLException e) {

        }
        return "";
    }

    private static void transferUserInfo(String idToTransferFrom, String idToTransferTo,
                                  PreparedStatement transferCommentsStmt,
                                  PreparedStatement transferTagsStmt,
                                  PreparedStatement transferRatingsStmt,
                                  PreparedStatement transferReadingHistoryStmt,
                                  PreparedStatement transferUserResourceStmt) {
        try {
            //Transfer comments
            transferCommentsStmt.setString(1, idToTransferTo);
            transferCommentsStmt.setString(2, idToTransferFrom);
            int numCommentsMoved = transferCommentsStmt.executeUpdate();
            if (numCommentsMoved > 0) UpdateResourceInformation.logger.info("Moved " + numCommentsMoved + " comments");
            //Transfer tags
            transferTagsStmt.setString(1, idToTransferTo);
            transferTagsStmt.setString(2, idToTransferFrom);
            int numTagsMoved = transferTagsStmt.executeUpdate();
            if (numTagsMoved > 0) UpdateResourceInformation.logger.info("Moved " + numTagsMoved + " tags");
            //Transfer ratings
            transferRatingsStmt.setString(1, idToTransferTo);
            transferRatingsStmt.setString(2, idToTransferFrom);
            int numRatingsMoved = transferRatingsStmt.executeUpdate();
            if (numRatingsMoved > 0) UpdateResourceInformation.logger.info("Moved " + numRatingsMoved + " ratings");
            //Transfer reading history
            transferReadingHistoryStmt.setString(1, idToTransferTo);
            transferReadingHistoryStmt.setString(2, idToTransferFrom);
            int numReadingHistoryMoved = transferReadingHistoryStmt.executeUpdate();
            if (numReadingHistoryMoved > 0) UpdateResourceInformation.logger.info("Moved " + numReadingHistoryMoved + " reading history entries");
            //Transfer User Resource Information
            transferUserResourceStmt.setString(1, idToTransferTo);
            transferUserResourceStmt.setString(2, idToTransferFrom);
            int numUserResourceMoved = transferUserResourceStmt.executeUpdate();
            if (numUserResourceMoved > 0) UpdateResourceInformation.logger.info("Moved " + numUserResourceMoved + " user resource (list) entries");

        } catch (SQLException e) {
            UpdateResourceInformation.logger.error("Error transferring resource info for user from " + idToTransferFrom + " to " + idToTransferTo, e);
        }
    }
}
