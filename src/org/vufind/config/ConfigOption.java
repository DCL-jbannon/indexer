package org.vufind.config;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Created by jbannon on 7/3/14.
 */
//public enum ConfigOption {
public enum ConfigOption implements I_ConfigOption {
    ONE(ConfigMethods::fillSimpleString, false),
    TWO(ConfigMethods::fillSimpleString, true);

    final private Function fillFunction;
    final private boolean isList;

    ConfigOption(Function fillFunction, boolean isList) {
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
