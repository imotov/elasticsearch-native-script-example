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

import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.SearchHit;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_REPLICAS;
import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_SHARDS;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.*;
import static org.hamcrest.Matchers.closeTo;

/**
 */
public class RelevantFieldScriptTests extends AbstractSearchScriptTests {

    @Override
    public Settings indexSettings() {
        ImmutableSettings.Builder builder = ImmutableSettings.builder();
        builder.put(SETTING_NUMBER_OF_SHARDS, randomIntBetween(1,3));
        builder.put(SETTING_NUMBER_OF_REPLICAS, 0);
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRelevantFields() throws Exception {
        // Create a new quotes index
        String mapping = XContentFactory.jsonBuilder().startObject().startObject("quote")
                .startObject("properties")
                .startObject("text").field("type", "string").endObject()
                .endObject().endObject().endObject()
                .string();

        assertAcked(prepareCreate("test").addMapping("quote", mapping));

        List<IndexRequestBuilder> indexBuilders = new ArrayList<>();
        // Index some quotes:
        indexBuilders.add(client().prepareIndex("test", "quote").setSource("text", "For instance, on the planet Earth, man had always assumed that he was more intelligent than dolphins because he had achieved so much—the wheel, New York, wars and so on—whilst all the dolphins had ever done was muck about in the water having a good time. But conversely, the dolphins had always believed that they were far more intelligent than man—for precisely the same reasons."));
        indexBuilders.add(client().prepareIndex("test", "quote").setSource("text", "Would it save you a lot of time if I just gave up and went mad now?"));
        indexBuilders.add(client().prepareIndex("test", "quote").setSource("text", "Space is big. You just won't believe how vastly, hugely, mind-bogglingly big it is. I mean, you may think it's a long way down the road to the chemist's, but that's just peanuts to space."));
        indexBuilders.add(client().prepareIndex("test", "quote").setSource("text", "You know,\" said Arthur, \"it's at times like this, when I'm trapped in a Vogon airlock with a man from Betelgeuse, and about to die of asphyxiation in deep space that I really wish I'd listened to what my mother told me when I was young.\"\n\"Why, what did she tell you?\"\n\"I don't know, I didn't listen."));
        indexBuilders.add(client().prepareIndex("test", "quote").setSource("text", "Time is an illusion. Lunchtime doubly so."));
        indexRandom(true, indexBuilders);

        // Script parameters
        Map<String, Object> params = MapBuilder.<String, Object>newMapBuilder()
                .put("factor", 4.0)
                .map();

        // Find all quotes about time
        SearchResponse searchResponse = client().prepareSearch("test")
                .setTypes("quote")
                .setQuery(matchQuery("text", "time"))
                .addField("text")
                .addScriptField("scoreX4", "native", "score_factor", params)
                .setSize(10)
                .execute().actionGet();

        assertNoFailures(searchResponse);

        // There should be 3 quotes
        assertHitCount(searchResponse, 3);

        for (SearchHit hit : searchResponse.getHits().hits()) {
            assertThat((Double) hit.field("scoreX4").value(), closeTo(hit.score() * 4.0, 0.01));
        }
    }


}
