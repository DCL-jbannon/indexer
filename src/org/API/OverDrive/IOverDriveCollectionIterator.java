package org.API.OverDrive;

import org.json.simple.JSONObject;

public interface IOverDriveCollectionIterator {

	boolean hasNext();

	JSONObject next();

}
