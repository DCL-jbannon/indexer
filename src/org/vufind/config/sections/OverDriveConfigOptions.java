package org.vufind.config.sections;

import org.vufind.config.ConfigMethods;
import org.vufind.config.I_ConfigOption;

import java.util.function.Function;

/**
 * Created by jbannon on 7/7/14.
 */
public enum OverDriveConfigOptions implements I_ConfigOption {
    CLIENT_KEY(ConfigMethods::fillSimpleString, false),
    CLIENT_SECRET(ConfigMethods::fillSimpleString, false),
    LIBRARY_ID(ConfigMethods::fillInteger, false),
    ;

    final private Function fillFunction;
    final private boolean isList;

    OverDriveConfigOptions(Function fillFunction, boolean isList) {
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
