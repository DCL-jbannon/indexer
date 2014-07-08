package org.vufind;

import org.slf4j.Logger;

import java.util.List;
import java.util.function.Consumer;

public interface IMarcRecordProcessor extends Consumer {
	public boolean processMarcRecord(MarcRecordDetails recordInfo);

    default void accept(Object o)
    {
        if(o instanceof List) {
            List l = (List)o;
            for(Object oo: l) {
                if(oo instanceof MarcRecordDetails) {
                    processMarcRecord((MarcRecordDetails)oo);
                }
            }
        }
    }

}
