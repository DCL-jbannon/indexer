package org.vufind.config.sections;

import org.vufind.config.ConfigMethods;
import org.vufind.config.I_ConfigOption;

import java.util.function.Function;

/**
 * Created by jbannon on 7/7/14.
 */
public enum FreegalConfigOptions implements I_ConfigOption {
    URL(ConfigMethods::fillSimpleString, false),
    USER(ConfigMethods::fillSimpleString, false),
    PIN(ConfigMethods::fillSimpleString, false),
    API_KEY(ConfigMethods::fillSimpleString, false),
    LIBRARY_ID(ConfigMethods::fillSimpleString, false),
    ;

    final private Function fillFunction;
    final private boolean isList;

    FreegalConfigOptions(Function fillFunction, boolean isList) {
        this.isList = isList;
        this.fillFunction = fillFunction;
    }

    public Function getFillFunction() {
        return fillFunction;
    }

    @Override
    public boolean isList() {
        return this.isList;
    }
}
