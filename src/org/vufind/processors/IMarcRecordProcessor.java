package org.vufind.processors;

import org.vufind.MarcRecordDetails;
import org.vufind.config.DynamicConfig;
import org.vufind.tasks.ProcessMarc;

import java.util.List;
import java.util.function.Consumer;

public interface IMarcRecordProcessor extends Consumer, IRecordProcessor {
	public boolean processMarcRecord(MarcRecordDetails recordInfo);
    public boolean init(DynamicConfig config);
    default void accept(Object o)
    {
        if(o instanceof List) {
            List l = (List)o;
            ProcessMarc.CountingList cl = null;
            if(l instanceof ProcessMarc.CountingList) {
                cl = (ProcessMarc.CountingList)l;
            }
            for(Object oo: l) {
                if(oo instanceof MarcRecordDetails) {
                    processMarcRecord((MarcRecordDetails)oo);
                    if(cl!=null)cl.increment();
                }
            }
        }
    }

}
