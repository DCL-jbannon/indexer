package org.vufind.config.sections;

import org.vufind.config.ConfigMethods;
import org.vufind.config.I_ConfigOption;

import java.util.function.Function;

/**
 * Created by jbannon on 7/7/14.
 */
public enum EvokeConfigOptions implements I_ConfigOption {
    USER(ConfigMethods::fillSimpleString, false),
    PASS(ConfigMethods::fillSimpleString, false),
    URL(ConfigMethods::fillSimpleString, false),
    UPDATE_FROM_DAYS_AGO(ConfigMethods::fillInteger, false),
    ;

    final private Function fillFunction;
    final private boolean isList;

    EvokeConfigOptions(Function fillFunction, boolean isList) {
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
