package org.econtent;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jbannon on 6/24/14.
 */
public class EContentItem {
    private String itemId = null;
    private String recordId = null;
    private String link = null;
    private String type = null;
    private String notes = null;
    private String addedBy = null;
    private int dateAdded = -1;
    private int dateUpdated = -1;

    public EContentItem(String itemId, String recordId, String link, String type, String notes, String addedBy, int dateAdded, int dateUpdated) {
        this.itemId = itemId;
        this.recordId = recordId;
        this.link = link;
        this.type = type;
        this.notes = notes;
        this.addedBy = addedBy == null ? "-1":addedBy;
        this.dateAdded = dateAdded;
        this.dateUpdated = dateUpdated;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public int getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(int dateAdded) {
        this.dateAdded = dateAdded;
    }

    public int getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(int dateUpdated) {
        this.dateUpdated = dateUpdated;
    }
}
