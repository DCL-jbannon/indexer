package org.API.Odilo.manualModels;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;

/**
 * Created by jbannon on 8/25/2014.
 */
public class GetLoanablesResponse {
    protected String recordId;
    protected List<String> types;
    protected Integer available;

    public GetLoanablesResponse(String recordId, List<String> types, int available) {
        this.recordId = recordId;
        this.types = types;
        this.available = available;
    }

    public String getRecordId() {
        return recordId;
    }

    public List<String> getTypes() {
        return types;
    }

    public Integer getAvailable() {
        return available;
    }
}
