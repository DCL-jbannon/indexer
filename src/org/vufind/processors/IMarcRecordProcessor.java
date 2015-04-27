package org.vufind.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vufind.MarcRecordDetails;
import org.vufind.config.DynamicConfig;
import org.vufind.tasks.ProcessMarc;

import java.util.List;
import java.util.function.Consumer;

public interface IMarcRecordProcessor extends Consumer, IRecordProcessor {
    final static Logger logger = LoggerFactory.getLogger(ProcessMarc.class);

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
                    try {
                        processMarcRecord((MarcRecordDetails) oo);
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                        logger.error("Uncaught error processing Marc record ["+((MarcRecordDetails) oo).getId()+"]");
                    }
                    if(cl!=null)cl.increment();
                }
            }
        }
    }

}
