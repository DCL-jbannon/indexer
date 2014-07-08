package org.vufind.config;

import java.util.function.Function;

/**
 * Created by jbannon on 7/3/14.
 */
public interface I_ConfigOption {
    /**
     * Should return the relative path name of the config INI for this option set.
     * @return
     */
    //String getIniName();

    /**
     * Returns the type of value the config should hold for this option.
     * @return
     */
    //Class getOptionType();


    Function getFillFunction();
    boolean isList();
    String name();
}
