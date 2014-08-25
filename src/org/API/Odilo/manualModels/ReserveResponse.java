package org.API.Odilo.manualModels;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlElement;

/**
 * Created by jbannon on 8/25/2014.
 */
public class ReserveResponse {
    protected String recordId;
    protected String reserveId;
    protected String status;

    public ReserveResponse(String recordId, String reserveId, String status) {
        this.recordId = recordId;
        this.reserveId = reserveId;
        this.status = status;
    }

    public String getRecordId() {
        return recordId;
    }

    public String getReserveId() {
        return reserveId;
    }

    public String getStatus() {
        return status;
    }
}
