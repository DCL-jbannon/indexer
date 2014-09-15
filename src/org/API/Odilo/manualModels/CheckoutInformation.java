package org.API.Odilo.manualModels;

import org.joda.time.DateTime;

import java.net.URL;
import java.util.List;

/**
 * Created by jbannon on 8/26/2014.
 */
public class CheckoutInformation {
    protected String recordId;
    protected String loanId;
    protected DateTime startDate;
    protected DateTime endDate;


    public CheckoutInformation(String recordId, String loanId, DateTime startDate, DateTime endDate) {
        this.recordId = recordId;
        this.loanId = loanId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getRecordId() {
        return recordId;
    }

    public String getLoanId() {
        return loanId;
    }

    public DateTime getStartDate() {
        return startDate;
    }

    public DateTime getEndDate() {
        return endDate;
    }
}
