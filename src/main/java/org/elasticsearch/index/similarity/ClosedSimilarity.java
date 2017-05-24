/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.elasticsearch.index.similarity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author marijom
 */
public class ClosedSimilarity extends Similarity{
    
    private static Logger log = Logger.getLogger(ClosedSimilarity.class);
    
    public ClosedSimilarity(){}


    /** Cache of decoded bytes. */
    private static final Map<String, Long> attribWeights = new HashMap<>();

    static {
        attribWeights.put("streetNumberAlfa", 1l);
        attribWeights.put("streetNumber", 2l);
        attribWeights.put("streetName", 4l);
        attribWeights.put("postalCode", 8l);
        attribWeights.put("settlementName", 16l);
        attribWeights.put("countyName", 32l);
    }
    
    /**
     * Query term matching
     * @param overlap the number of query terms matched in the document
     * @param maxOverlap the total number of terms in the query
     * @return 
     */
    @Override
    public float coord(int overlap, int maxOverlap) {
        float coord = overlap/(float)maxOverlap;
        log.info("Calculating query coordination. Value: "+coord);
        return coord;
    }

    /** Computes the normalization value for a query given the sum of the
     * normalized weights {@link SimWeight#getValueForNormalization()} of 
     * each of the query terms.
     * @param valueForNormalization the sum of the term normalization values
     * @return a normalization factor for query weights
     */
    @Override
    public float queryNorm(float valueForNormalization) {
        //float qNorm = 1f/valueForNormalization;
        //log.info("Calculating query norm: "+qNorm);
        return 1f;
    }
    
    @Override
    public long computeNorm(FieldInvertState state) {
        long normValue = 1l;
        if (attribWeights.containsKey(state.getName())){
            normValue = attribWeights.get(state.getName());
        }
        log.info("Calculating term normalization. Term: "+state.getName()+" Value: "+normValue);
        return normValue;
    }

    /** Implemented as <code>log(1 + (numDocs - docFreq + 0.5)/(docFreq + 0.5))</code>. */
    protected float idf(long docFreq, long numDocs) {
      return (float) Math.log(1 + (numDocs - docFreq + 0.5D)/(docFreq + 0.5D));
    }
    
    @Override
    public SimWeight computeWeight(CollectionStatistics collectionStats, TermStatistics... termStats) {
        float idf = 0.0f;
        String desc="Field: "+collectionStats.field()+" Terms: ";
        final long max = collectionStats.maxDoc();
        for (final TermStatistics stat : termStats ) {
            final long df = stat.docFreq();
            final float termIdf = idf(df, max);
            idf += termIdf;
            log.info("Calculating term frequency: "+stat.term().utf8ToString()+" Value: "+df);
            desc += stat.term().utf8ToString() + " ";
        }
        log.info("Calculating term idf: "+desc+" Value: "+idf);
        ClosedSimWeight csw = new ClosedSimWeight(collectionStats.field(), idf, termStats);
        log.info("Calculating sim weight for field: "+csw.desc);
        return csw;
    }

    @Override
    public SimScorer simScorer(SimWeight weight, LeafReaderContext context) throws IOException {
        ClosedSimWeight csWeight = (ClosedSimWeight) weight;
        ClosedSimScorer css = new ClosedSimScorer(csWeight, context.reader().getNormValues(csWeight.field));
        log.info("Returning sim scorer for: "+csWeight.field);
        return css;
    }
    
    private static class ClosedSimWeight extends SimWeight{
        
        private String field;
        /** idf */
        private final float idf;
        /** query boost */
        private float boost;
        /** weight (idf * boost) */
        private float weight;
        
        private String desc="";
        
        ClosedSimWeight(String field, float idf, TermStatistics... termStats){
            this.idf = idf;
            this.field = field;
            
            desc="Field: "+field+" Terms: ";
            for (final TermStatistics stat : termStats ) {
                desc += stat.term().utf8ToString()+" ";
            }
            log.info("Creating weight for : "+desc);
        }

        @Override
        public float getValueForNormalization() {
            float wNorm = weight * weight;
            log.info("Returning weight normalization. Sim: "+desc+" Value: "+wNorm);
            return wNorm;
        }

        @Override
        public void normalize(float queryNorm, float boost) {
            this.boost = boost;
            this.weight = idf * boost;
            log.info("Calculating weight normalization. Sim: "+desc+" qNorm: "+queryNorm+" boost: "+boost+" weight: "+weight);
       }    
    }
    
    private class ClosedSimScorer extends SimScorer {
        
        private final float weightValue; // boost * idf * (k1 + 1)
        private final ClosedSimWeight csw;

        ClosedSimScorer(ClosedSimWeight weights, NumericDocValues norms){
            log.info("Creating scorer: "+weights.desc);
            this.csw = weights;
            this.weightValue = csw.weight;
        }
                
        @Override
        public float score(int doc, float freq) {
            log.info("Computing score. Scorer: "+csw.desc+" Score: "+weightValue);
            return weightValue;
        }

        @Override
        public float computeSlopFactor(int distance) {
            float slop = 1.0f / (distance + 1);
            log.info("Computing slope factor. Scorer: "+csw.desc+" Slop: "+slop);
            return slop;
        }

        @Override
        public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
            log.info(" Scorer: "+csw.desc+" Calculating payload: Doc: "+doc+" Start: "+start+" End: "+end+" Payload: "+payload.utf8ToString());
            return 1f;
        }
        
    }
}
