/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticsearch.examples.nativescript.script;

import java.math.BigInteger;
import java.util.Map;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.index.fielddata.ScriptDocValues.Longs;
import org.elasticsearch.script.AbstractSearchScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;

/**
 * Implementation of the native script that checks that the field exists and contains a prime number.
 * <p>
 * Native scripts are built using factories that are returned by
 * {@link org.elasticsearch.examples.nativescript.plugin.NativeScriptExamplesPlugin#getNativeScripts()}
 * method when plugin is loaded.
 */
public class IsPrimeSearchScriptFactory implements NativeScriptFactory {
    // Plugins can use elasticsearch settings and even define their own settings
    // Here we are defining a settings from default value of the prime field
    public static final Setting<String> PRIME_SCRIPT_DEFAULT_FIELD_NAME =
        Setting.simpleString("my_scripts.prime.default_field_name", Setting.Property.NodeScope);

    public final String defaultFieldName;

    public IsPrimeSearchScriptFactory(Settings settings) {
        defaultFieldName = PRIME_SCRIPT_DEFAULT_FIELD_NAME.get(settings);
    }
    /**
     * This method is called for every search on every shard.
     *
     * @param params list of script parameters passed with the query
     * @return new native script
     */
    @Override
    public ExecutableScript newScript(@Nullable Map<String, Object> params) {
        // Example of a mandatory string parameter
        // The XContentMapValues helper class can be used to simplify parameter parsing
        String fieldName = params == null ? null : XContentMapValues.nodeStringValue(params.get("field"), defaultFieldName);
        if (!Strings.hasLength(fieldName)) {
            throw new IllegalArgumentException("Missing the field parameter");
        }

        // Example of an optional integer  parameter
        int certainty = params == null ? 10 : XContentMapValues.nodeIntegerValue(params.get("certainty"), 10);
        return new IsPrimeSearchScript(fieldName, certainty);
    }

    @Override
    public boolean needsScores() {
        return false;
    }

    @Override
    public String getName() {
        return "is_prime";
    }

    /**
     * The native script has to implement {@link org.elasticsearch.script.SearchScript} interface. But the
     * {@link org.elasticsearch.script.AbstractSearchScript} class can be used to simplify the implementation.
     */
    public static class IsPrimeSearchScript extends AbstractSearchScript {

        private final String fieldName;

        private final int certainty;

        /**
         * Factory creates this script on every
         *
         * @param fieldName the name of the field that should be checked
         * @param certainty the required certainty for the number to be prime
         */
        private IsPrimeSearchScript(String fieldName, int certainty) {
            this.fieldName = fieldName;
            this.certainty = certainty;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object run() {
            // First we get field using doc lookup
            ScriptDocValues<Long> docValue = (ScriptDocValues<Long>) doc().get(fieldName);
            // Check if field exists
            if (docValue != null && !docValue.isEmpty()) {
                try {
                    // Try to parse it as an integer
                    BigInteger bigInteger = BigInteger.valueOf(((Longs) docValue).getValue());
                    // Check if it's prime
                    return bigInteger.isProbablePrime(certainty);
                } catch (NumberFormatException ex) {
                    return false;
                }
            }
            return false;
        }

    }
}
