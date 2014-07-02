package org.vufind;

import org.vufind.config.Config;

public interface IRecordProcessor {
	public boolean init(Config config);
	public void finish();
}
