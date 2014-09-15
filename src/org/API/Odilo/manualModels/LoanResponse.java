package org.API.Odilo.manualModels;

import java.net.URL;
import java.util.List;

/**
 * Created by jbannon on 8/26/2014.
 */
public class LoanResponse {
    protected String recordId;
    protected List<URL> urls;
    protected String status;

    public LoanResponse(String recordId, List<URL> urls) {
        this.recordId = recordId;
        this.urls = urls;
    }

    public String getRecordId() {
        return recordId;
    }

    public List<URL> getUrls() {
        return urls;
    }
}
