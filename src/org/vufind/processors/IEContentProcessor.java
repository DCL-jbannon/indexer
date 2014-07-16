package org.vufind.processors;

import java.sql.ResultSet;

public interface IEContentProcessor extends IRecordProcessor {
	public boolean processEContentRecord(String indexName, ResultSet resource);
}
