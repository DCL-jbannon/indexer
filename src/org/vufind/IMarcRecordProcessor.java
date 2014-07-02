package org.vufind;

import org.slf4j.Logger;

public interface IMarcRecordProcessor {
	public boolean processMarcRecord(MarcProcessor processor, MarcRecordDetails recordInfo, MarcProcessor.RecordStatus recordStatus, Logger logger) ;
}
