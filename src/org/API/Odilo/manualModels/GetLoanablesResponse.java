package org.API.Odilo.manualModels;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;

/**
 * Created by jbannon on 8/25/2014.
 */
public class GetLoanablesResponse {
    protected String recordId;
    protected List<String> types;

    public GetLoanablesResponse(String recordId, List<String> types) {
        this.recordId = recordId;
        this.types = types;
    }

    public String getRecordId() {
        return recordId;
    }

    public List<String> getTypes() {
        return types;
    }
}
