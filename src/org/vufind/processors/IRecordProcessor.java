package org.vufind.processors;

import org.vufind.config.DynamicConfig;

import java.util.function.Consumer;

public interface IRecordProcessor extends Consumer {
	public boolean init(DynamicConfig config);
	public void finish();
}
