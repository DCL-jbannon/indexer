package org.vufind;

import java.sql.ResultSet;

public interface IEContentProcessor extends IRecordProcessor {
	public boolean processEContentRecord(String indexName, ResultSet resource);
}
