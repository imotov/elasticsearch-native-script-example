package org.elasticsearch.examples.nativescript.script;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.ScoreDoc;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.script.AbstractDoubleSearchScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.search.internal.SearchContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates how to access score values from script field script
 */
public class RelevantFieldScriptFactory implements NativeScriptFactory {

    public final static String SCRIPT_NAME = "score_factor";

    public final static double DEFAULT_FACTOR = 2.0;

    @Override
    public ExecutableScript newScript(@Nullable Map<String, Object> params) {
        double factor = params == null ? DEFAULT_FACTOR : XContentMapValues.nodeDoubleValue(params.get("factor"), DEFAULT_FACTOR);
        return new RelevantFieldScript(factor);
    }


    /**
     * This script looks up the score of the current document, multiplies it by a factor (defaults to 2.0) and returns
     * it as a script field.
     */
    private static class RelevantFieldScript extends AbstractDoubleSearchScript {

        private final double factor;

        private Map<Integer, Float> scoreMap;

        private int doc;

        private int docBase;


        public RelevantFieldScript(double factor) {
            this.factor = factor;
        }

        @Override
        public void setNextReader(AtomicReaderContext context) {
            docBase = context.docBase;
        }

        @Override
        public void setNextDocId(int doc) {
            this.doc = doc;
        }

        @Override
        public double runAsDouble() {
            if (scoreMap == null) {
                // We are first time here - need to build a map of hits first
                ScoreDoc[] scoreDocs = SearchContext.current().queryResult().topDocs().scoreDocs;
                if (scoreDocs == null) {
                    throw new RuntimeException("Enable scoring");
                }
                scoreMap = new HashMap<>(scoreDocs.length);
                for (ScoreDoc scoreDoc : scoreDocs) {
                    scoreMap.put(scoreDoc.doc, scoreDoc.score);
                }
            }
            int docId = doc + docBase;
            Float score = scoreMap.get(docId);
            if (score == null) {
                // We really shouldn't be here. That means that coordinating node has requests a record that we didn't return to it
                throw new RuntimeException("Score not found for the doc = " + doc + " docBase = " + docBase);
            }
            return score * factor;
        }
    }
}
