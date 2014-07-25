package org.elasticsearch.examples.nativescript.script;


import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.script.AbstractExecutableScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.script.ScriptException;

import java.util.Map;

/**
 * Script that takes an input field and splits it's value
 * by the configured delimiter.  "a,b,c" -> ["a","b","c"]
 */
public class SplitTransformScript extends AbstractExecutableScript {

    private final String field;
    private final String delimiter;

    // expect size 1 because we should only have a "ctx" var
    private Map<String, Object> vars = Maps.newHashMapWithExpectedSize(1);

    final static public String SCRIPT_NAME = "split_transform_script";

    /**
     * Native scripts are build using factories that are registered in the
     * {@link org.elasticsearch.examples.nativescript.plugin.NativeScriptExamplesPlugin#onModule(org.elasticsearch.script.ScriptModule)}
     * method when plugin is loaded.
     */
    public static class Factory implements NativeScriptFactory {

        /**
         * This method is called for every document indexed on all shards
         *
         * @param params list of script parameters configured in the mapping
         * @return new native script
         */
        @Override
        public ExecutableScript newScript(@Nullable Map<String, Object> params) {
            // Example of a mandatory string parameter
            // The XContentMapValues helper class can be used to simplify parameter parsing
            String field = params == null ? null : XContentMapValues.nodeStringValue(params.get("field"), null);
            if (field == null) {
                throw new ScriptException("[" + SCRIPT_NAME + "]: Missing the field parameter");
            }

            // Example of an optional string parameter
            String delimiter = params == null ? "," : XContentMapValues.nodeStringValue(params.get("delimiter"), ",");
            return new SplitTransformScript(field, delimiter);
        }
    }

    /**
     * @param field     the field within _source to split
     * @param delimiter the value to split on
     */
    private SplitTransformScript(String field, String delimiter) {
        this.field = field;
        this.delimiter = delimiter;
    }

    // this loads the ctx
    @Override
    public void setNextVar(String name, Object value) {
        vars.put(name, value);
    }

    @Override
    public Object run() {
        // extract the source from the ctx
        if (vars.containsKey("ctx") && vars.get("ctx") instanceof Map) {
            Map<String, Object> ctx = (Map<String, Object>) vars.get("ctx");
            if (ctx.containsKey("_source") && ctx.get("_source") instanceof Map) {
                Map<String, Object> source = (Map<String, Object>) ctx.get("_source");

                // only split if the field is a string
                String fieldVal = XContentMapValues.nodeStringValue(source.get(field), null);
                if (field != null) {
                    // split the value and only overwrite existing value if the split was successful
                    String[] splitVals = Strings.delimitedListToStringArray(fieldVal, delimiter);
                    if (splitVals.length > 1) {
                        source.put(field, splitVals);
                    }
                }
            }

            // return the context
            return ctx;
        }

        // we should always have a ctx above, but if not, just return a null value
        return null;
    }
}