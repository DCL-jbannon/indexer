package org.vufind.processors;

import java.sql.ResultSet;

public interface IResourceProcessor {
	public boolean processResource(ResultSet resource);	
}
