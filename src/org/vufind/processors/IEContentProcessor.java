package org.vufind.processors;

import java.sql.ResultSet;
import java.util.HashMap;

public interface IEContentProcessor extends IRecordProcessor {
	public boolean processEContentRecord(ResultSet resource);
}
