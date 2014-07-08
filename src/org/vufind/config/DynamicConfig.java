package org.vufind.config;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

/**
 * Created by jbannon on 7/3/14.
 */
public class DynamicConfig {
    HashMap<I_ConfigOption, Object> values = new HashMap<I_ConfigOption, Object>();

    public Object get(I_ConfigOption option) {
        return values.get(option);
    }

    public <T> T get(I_ConfigOption option, ParameterizedType type) {
        return (T)values.get(option);
    }

    public void put(I_ConfigOption option, Object value) {
        values.put(option, value);
    }
}
