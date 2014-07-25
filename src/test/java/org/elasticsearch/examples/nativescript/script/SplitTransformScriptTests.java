package org.elasticsearch.examples.nativescript.script;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.index.query.FilterBuilders.termFilter;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.*;
import static org.hamcrest.Matchers.equalTo;

public class SplitTransformScriptTests extends AbstractSearchScriptTests {

    @Test
    public void testSplitTransformScript() throws Exception {

        // Create a new index
        // strcomma = test default delimiter
        // strdash = test configured delimiter
        String mapping = XContentFactory.jsonBuilder().startObject().startObject("type")
                .startArray("transform")
                .startObject()
                .field("script", SplitTransformScript.SCRIPT_NAME)
                .startObject("params")
                .field("field", "strcomma")
                .endObject()
                .field("lang", "native")
                .endObject()
                .startObject()
                .field("script", SplitTransformScript.SCRIPT_NAME)
                .startObject("params")
                .field("field", "strdash")
                .field("delimiter", "-")
                .endObject()
                .field("lang", "native")
                .endObject()
                .endArray()
                .startObject("properties")
                .startObject("strcomma").field("type", "string").field("index", "not_analyzed").field("store", true).endObject()
                .startObject("strdash").field("type", "string").field("index", "not_analyzed").field("store", true).endObject()
                .startObject("num").field("type", "integer").endObject()
                .endObject().endObject().endObject()
                .string();

        assertAcked(prepareCreate("test")
                .addMapping("type", mapping));

        List<IndexRequestBuilder> indexBuilders = new ArrayList<IndexRequestBuilder>();
        // Index 100 records (0..99)
        // str = 0,1,...,i
        // num = i
        StringBuilder strComma = new StringBuilder();
        StringBuilder strDash = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            if (i != 0) {
                strComma.append(",");
                strDash.append("-");
            }
            strComma.append(i);
            strDash.append(i);
            indexBuilders.add(
                    client().prepareIndex("test", "type", Integer.toString(i))
                            .setSource(XContentFactory.jsonBuilder().startObject()
                                    .field("strcomma", strComma.toString())
                                    .field("strdash", strDash.toString())
                                    .field("num", i)
                                    .endObject()));
        }

        // Index a few records with empty str
        for (int i = 100; i < 105; i++) {
            indexBuilders.add(
                    client().prepareIndex("test", "type", Integer.toString(i))
                            .setSource(XContentFactory.jsonBuilder().startObject()
                                    .field("num", i)
                                    .endObject()));
        }

        indexRandom(true, indexBuilders);

        // test comma (default) delimiter
        for (int i = 0; i < 105; i++) {
            ListenableActionFuture<SearchResponse> commaFuture = client().prepareSearch("test")
                    .setQuery(filteredQuery(matchAllQuery(), termFilter("strcomma", Integer.toString(i))))
                    .addFields("strcomma", "num")
                    .setSize(105)
                    .execute();

            ListenableActionFuture<SearchResponse> dashFuture = client().prepareSearch("test")
                    .setQuery(filteredQuery(matchAllQuery(), termFilter("strdash", Integer.toString(i))))
                    .addFields("strdash", "num")
                    .setSize(105)
                    .execute();

            SearchResponse commaResp = commaFuture.actionGet();
            SearchResponse dashResp = dashFuture.actionGet();

            assertNoFailures(commaResp);
            assertNoFailures(dashResp);

            if (i >= 100) {
                assertHitCount(commaResp, 0);
                assertHitCount(dashResp, 0);
            } else {
                assertHitCount(commaResp, 100 - i);
                assertHitCount(dashResp, 100 - i);
            }

            // Verify that they were actually split
            for (int j = 0; j < 100 - i; j++) {
                int commaNum = commaResp.getHits().getAt(j).field("num").value();
                int dashNum = dashResp.getHits().getAt(j).field("num").value();
                assertThat(commaResp.getHits().getAt(j).field("strcomma").values().size(), equalTo(commaNum + 1));
                assertThat(dashResp.getHits().getAt(j).field("strdash").values().size(), equalTo(dashNum + 1));
            }
        }
    }
}