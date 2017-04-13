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

package org.elasticsearch.examples.nativescript.plugin;

import org.elasticsearch.examples.nativescript.script.IsPrimeSearchScriptFactory;
import org.elasticsearch.examples.nativescript.script.stockaggs.CombineScriptFactory;
import org.elasticsearch.examples.nativescript.script.stockaggs.InitScriptFactory;
import org.elasticsearch.examples.nativescript.script.stockaggs.MapScriptFactory;
import org.elasticsearch.examples.nativescript.script.stockaggs.ReduceScriptFactory;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.NativeScriptFactory;

import java.util.Arrays;
import java.util.List;

/**
 * This class is instantiated when Elasticsearch loads the plugin for the
 * first time. If you change the name of this plugin, make sure to update
 * src/main/resources/es-plugin.properties file that points to this class.
 */
public class NativeScriptExamplesPlugin extends Plugin implements ScriptPlugin {

//    public void onModule(ScriptModule module) {
//        // Register each script that we defined in this plugin
//        module.registerScript("is_prime", IsPrimeSearchScript.Factory.class);
//        module.registerScript("lookup", LookupScript.Factory.class);
//        module.registerScript("random", RandomSortScriptFactory.class);
//        module.registerScript("popularity", PopularityScoreScriptFactory.class);
//        module.registerScript(TFIDFScoreScript.SCRIPT_NAME, TFIDFScoreScript.Factory.class);
//        module.registerScript(CosineSimilarityScoreScript.SCRIPT_NAME, CosineSimilarityScoreScript.Factory.class);
//        module.registerScript(PhraseScoreScript.SCRIPT_NAME, PhraseScoreScript.Factory.class);
//        module.registerScript(LanguageModelScoreScript.SCRIPT_NAME, LanguageModelScoreScript.Factory.class);
//        // Scripted Metric Aggregation Scripts
//        module.registerScript("stockaggs_init", InitScriptFactory.class);
//        module.registerScript("stockaggs_map", MapScriptFactory.class);
//        module.registerScript("stockaggs_combine", CombineScriptFactory.class);
//        module.registerScript("stockaggs_reduce", ReduceScriptFactory.class);
//    }

    @Override
    public List<NativeScriptFactory> getNativeScripts() {
        return Arrays.asList(
            new IsPrimeSearchScriptFactory(),
            new InitScriptFactory(),
            new MapScriptFactory(),
            new CombineScriptFactory(),
            new ReduceScriptFactory()
        );
    }
}
