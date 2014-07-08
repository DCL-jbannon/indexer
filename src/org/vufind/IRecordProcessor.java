package org.vufind;

import org.vufind.config.Config;
import org.vufind.config.DynamicConfig;

import java.util.function.Consumer;

public interface IRecordProcessor extends Consumer {
	public boolean init(DynamicConfig config);
	public void finish();
}
