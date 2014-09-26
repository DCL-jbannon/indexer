package org.API.Odilo.manualModels;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlElement;

/**
 * Created by jbannon on 8/25/2014.
 */
public class HoldResponse {
    protected String recordId;
    protected String reserveId;
    protected String status;

    public HoldResponse(String recordId, String reserveId, String status) {
        this.recordId = recordId;
        this.reserveId = reserveId;
        this.status = status;
    }

    public String getRecordId() {
        return recordId;
    }

    public String getHoldId() {
        return reserveId;
    }

    public String getStatus() {
        return status;
    }
}
