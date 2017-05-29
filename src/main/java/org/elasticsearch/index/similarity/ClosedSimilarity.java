/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.elasticsearch.index.similarity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;

/**
 *
 * @author marijom
 */
public class ClosedSimilarity extends Similarity{
    
    private static Logger log = Logger.getLogger(ClosedSimilarity.class);
    
    public ClosedSimilarity(){}
    
    private enum AttributeType {
        String,
        Integer,
        Float,
        Geometry
    }
    
    //multiplicity of attributes in a query, needed when calculating score
    private static final Map<String, Long> attributeMultiplicity = new HashMap<>();

    /** list of fields/attributes and their weights */
    private static final Map<String, Long> attribWeights = new HashMap<>();
    
    /** list of fields/attributes and their types */
    private static final Map<String, AttributeType> attribTypes = new HashMap<>();

    static {
        attribWeights.put("streetNumberAlfa", 1l);
        attribWeights.put("streetNumber", 2l);
        attribWeights.put("streetName", 4l);
        attribWeights.put("postalCode", 8l);
        attribWeights.put("settlementName", 16l);
        attribWeights.put("countyName", 32l);
        
        attribTypes.put("streetNumberAlfa", AttributeType.String);
        attribTypes.put("streetNumber", AttributeType.String);
        attribTypes.put("streetName", AttributeType.String);
        attribTypes.put("postalCode", AttributeType.Integer);
        attribTypes.put("settlementName", AttributeType.String);
        attribTypes.put("countyName", AttributeType.String);
    }
    
    /**
     * Query term matching
     * @param overlap the number of query terms matched in the document
     * @param maxOverlap the total number of terms in the query
     * @return 
     */
    @Override
    public float coord(int overlap, int maxOverlap) {
        float coord = maxOverlap;
        log.info("Calculating query coordination. Overlap: "+overlap+" Max overlap: "+maxOverlap+" Value: "+coord);
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
        attributeMultiplicity.clear();
        log.info("Calculating query norm: "+1f+" valueForNormalization: "+valueForNormalization);
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

    /** Implemented as <code>log(1 + (numDocs - docFreq + 0.5)/(docFreq + 0.5))</code>.
     * @param docFreq
     * @param numDocs
     * @return  
     */
    protected float idf(long docFreq, long numDocs) {
      return (float) Math.log(1 + (numDocs - docFreq + 0.5D)/(docFreq + 0.5D));
    }
    
    @Override
    public SimWeight computeWeight(CollectionStatistics collectionStats, TermStatistics... termStats) {
        Map<String, QueryTokenInfo> termIdfs = new HashMap<>();
        String desc="Query field: "+collectionStats.field()+" Terms: ";
        final long max = collectionStats.maxDoc();
        for (final TermStatistics stat : termStats) {
            final long df = stat.docFreq();
            final float termIdf = idf(df, max);
            QueryTokenInfo qti = new QueryTokenInfo(stat.term(), collectionStats.field(),df,termIdf);
            termIdfs.put(stat.term().utf8ToString(), qti);
//            log.info("Query term: "+stat.term().utf8ToString()+" Binary: "+stat.term().toString()+" frequency: "+df+" Appearance: "+df);
            desc += stat.term().utf8ToString() + " # ";
        }
        log.info("Creating sim weight for field: "+desc);
        ClosedSimWeight csw = new ClosedSimWeight(collectionStats.field(), desc, termIdfs);
        return csw;
    }

    @Override
    public SimScorer simScorer(SimWeight weight, LeafReaderContext context) throws IOException {
        ClosedSimWeight csWeight = (ClosedSimWeight) weight;
        ClosedSimScorer css = new ClosedSimScorer(csWeight, context);
        log.info("Returning sim scorer for: "+csWeight.field);
        return css;
    }
    
    private class QueryTokenInfo{
        private final BytesRef rawToken;
        private String attribute;
        private final long count;
        private final float idf;
        
        public QueryTokenInfo(BytesRef rawToken, String attribute, long count, float idf){
            this.rawToken = rawToken;
            this.attribute = attribute;
            this.count = count;
            this.idf = idf;
        }

        public BytesRef getRawToken() {
            return rawToken;
        }
        public String getAttribute() {
            return attribute;
        }
        public void setAttribute(String attribute) {
            this.attribute = attribute;
        }
        public long getCount() {
            return count;
        }
        public float getIdf() {
            return idf;
        }
    }
    
    private static class ClosedSimWeight extends SimWeight{
        
        private String field;
        
        private Map<String, QueryTokenInfo> termInfos;
        
        private String desc;
        
        ClosedSimWeight(String field, String description, Map<String, QueryTokenInfo> termInfos){
            this.termInfos = termInfos;
            this.field = field;
            this.desc = description;
        }

        @Override
        public float getValueForNormalization() {
            //float wNorm = weight * weight;
            log.info("Returning weight normalization. Query: "+desc+" Value: "+1f);
            return 1f;
        }

        @Override
        public void normalize(float queryNorm, float boost) {
            log.info("Calculating weight normalization. Query: "+desc+" qNorm: "+queryNorm+" boost: "+boost);
       }    
    }
    
    private class ClosedSimScorer extends SimScorer {
        
        private final ClosedSimWeight csw;
        
        private final LeafReaderContext context;

        ClosedSimScorer(ClosedSimWeight weights, LeafReaderContext context){
            log.info("Creating scorer: "+weights.desc);
            this.csw = weights;
            this.context = context;
            
            //calculating attribute multiplicity
            for(String term: csw.termInfos.keySet()){
                QueryTokenInfo qti = csw.termInfos.get(term);
                if (qti.getAttribute().compareTo("_all")==0){
                    estimateTermWeight(context.reader(), qti);
                }
                String queryAttribute = qti.getAttribute();
                if (!attributeMultiplicity.containsKey(queryAttribute)){
                    attributeMultiplicity.put(queryAttribute, 1l);
                } else {
                    Long multiplicity = attributeMultiplicity.get(queryAttribute);
                    attributeMultiplicity.put(queryAttribute, multiplicity+1l);
                }
            }
        }
        
        /**
         * Estimate term weight according to term field frequency
         * @param docFields
         * @param termIdfs
         * @return 
         */
        private float estimateTermWeight(LeafReader indexReader, QueryTokenInfo rawTermInfo){
            log.info("Estimating term weight: "+rawTermInfo.getRawToken().utf8ToString());
            try{
                int shift = 0;
                float weight = 0;
                float maxTermFrequency = 0;
                Map<String, Long> fieldFrequency = new HashMap<>();
                long totalTermFreq = 0;
                for(String field: attribWeights.keySet()){
                    try{
                        long fieldWeight = attribWeights.get(field);
                        AttributeType attributeType = attribTypes.get(field);
                        Term term = null;
                        long termFreq = 0;
                        if (attributeType == AttributeType.Integer){
                            int tokenValue = Integer.parseInt(rawTermInfo.getRawToken().utf8ToString());
                            BytesRefBuilder brb = new BytesRefBuilder();
                            NumericUtils.intToPrefixCoded(tokenValue, shift, brb);
                            BytesRef br = brb.toBytesRef();
//                            log.info("Converting integer term: "+tokenValue+" to internal encoding "+br.toString());
                            term = new Term(field, br);
                            termFreq = indexReader.docFreq(term);
                        } else {
                            term = new Term(field, rawTermInfo.getRawToken());
                            termFreq = indexReader.totalTermFreq(term);
                        }
                        if (termFreq>0){
                            fieldFrequency.put(field, termFreq);
                            //log.info("Term: "+rawTermInfo.getRawToken().utf8ToString()+" frequency: "+termFreq+" in field: "+field);
                            totalTermFreq+=termFreq;
                        } else {
//                            log.info("Term: "+term.bytes().utf8ToString()+" frequency is "+termFreq+" in field: "+field);
//                            StringBuilder info = new StringBuilder("Field: "+field+" Terms: ");
//                            Terms ts = indexReader.terms(field);
//                            if (ts!=null){
//                                TermsEnum tsIt = ts.iterator();
//                                BytesRef  tsItem = tsIt.next();
//                                while(tsItem != null){
//                                    if (attributeType == AttributeType.Integer){
//                                        int resultNumber = NumericUtils.prefixCodedToInt(tsItem);
//                                        info.append(resultNumber+" | ");
//                                        info.append(tsItem.toString() +" # ");
//                                    } else {
//                                        info.append(tsItem+" # ");
//                                    }
//                                    tsItem = tsIt.next();
//                                }
//                            }
//                            log.info(info);
                        }
                        //assigning weight by maximum frequency
                        if (termFreq > maxTermFrequency){
                            maxTermFrequency = termFreq;
                            weight = fieldWeight;
                            rawTermInfo.setAttribute(field);
                        }
                    } catch(NumberFormatException ex){
                        
                    }
                }
                log.info("Term: "+rawTermInfo.getRawToken().utf8ToString()+" total frequency: "+totalTermFreq);
                
                //assigning weight by proportional frequencies
//                for(String field: attribWeights.keySet()){
//                    long fieldWeight = attribWeights.get(field);
//                    if (fieldFrequency.containsKey(field)){
//                        weight += (fieldFrequency.get(field)/(float)totalTermFreq)*fieldWeight;
//                    }
//                }
                log.info("Term: "+rawTermInfo.getRawToken().utf8ToString()+" WEIGHT: "+weight);
                return weight;
            } catch(IOException ex){
                
            }
            return 1f;
        }
        
        /**
         * Estimates norm for an index based on attributes/fields it contains
         * 
         * @param indexReader index reader
         * @return norm
         */
        private float estimateIndexNorm(LeafReader indexReader){
            try {
                log.info("Estimating index norm.");
                float norm = 0f;
                Fields fs = indexReader.fields();
                Iterator<String> it = fs.iterator();
                while(it.hasNext()){
                    String fName = it.next();
                    if (attribWeights.containsKey(fName)){
                        long attWeight = attribWeights.get(fName);
                        norm += attWeight;
                    }
                }
                log.info("Index norm: "+norm);
                return norm;
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(ClosedSimilarity.class.getName()).log(Level.SEVERE, null, ex);
            }
            return 0f;
        }
        
        /**
         * Estimate doc coordination toward query
         * @param docFields
         * @param termIdfs
         * @return 
         */
        private float docCoord(int docId, LeafReader indexReader, Map<String, QueryTokenInfo> termInfos){
            log.info("Estimating doc coordination");
            try{
                Map<String, Long> docTokenFreq = new HashMap<>();
                long docTokenCount = 0;
                boolean vectors = indexReader.getFieldInfos().hasVectors();
                if (!vectors){
                    log.info("No term vectors on any field!");
                }
                for(String field: attribWeights.keySet()){
                    AttributeType attributeType = attribTypes.get(field);
                    if (attributeType == AttributeType.Integer){
                        String fValue = indexReader.document(docId).get(field);
                        if (fValue != null){
                            docTokenCount++;
                            log.info("Counting integer field: "+field+" term: "+fValue);
                            if (docTokenFreq.containsKey(fValue)){
                                long freq = docTokenFreq.get(fValue);
                                docTokenFreq.put(fValue, freq+1l);
                            } else {
                                docTokenFreq.put(fValue, 1l);
                            }
                        }
                    } else {
                        Terms terms = indexReader.getTermVector(docId, field);
                        if (terms != null){
                            TermsEnum it = terms.iterator();
                            BytesRef term = it.next();
                            while(term != null){
                                String token = term.utf8ToString();
                                log.info("Counting field: "+field+" term: "+token);
                                docTokenCount++;
                                if (docTokenFreq.containsKey(token)){
                                    long freq = docTokenFreq.get(token);
                                    docTokenFreq.put(token, freq+1l);
                                } else {
                                    docTokenFreq.put(token, 1l);
                                }
                                term = it.next();
                            }
                        } else {
                            log.info("Term vector for field: "+field+" is null!");
                        }
                    }
                }
                
                //listing query terms
//                for(String queryTerm: termInfos.keySet()){
//                    log.info("Query term: "+queryTerm);
//                }
                
                long missing = 0;
                for (String docToken: docTokenFreq.keySet()){
                    if (!termInfos.containsKey(docToken)){
                        long diff = docTokenFreq.get(docToken);
                        log.info("Document's token missing: "+docToken+" multiplicity: "+diff);
                        missing+= diff;
                    } else {
                        long docCount = docTokenFreq.get(docToken);
                        long queryCount = termInfos.get(docToken).getCount();
                        long diff = (docCount > queryCount)?docCount - queryCount:0;
                        missing += diff;
                        if (diff>0){
                            log.info("Document's token missing: "+docToken+" multiplicity: "+diff);
                        }
                    }
                }
                log.info("Document's tokens missing in query: "+missing+" Total tokens: "+docTokenCount);
                float docCoord = (float)(docTokenCount - missing)/docTokenCount;
                log.info("Document's coordination: "+docCoord);
                return docCoord;
            } catch(IOException ex){
                
            }
            return 1f;
        }
        
        
                
        @Override
        public float score(int doc, float freq) {
            try {
                log.info("Scoring: ID of the document: "+doc+" Sloppy frequency: "+freq);
                float docCoord = docCoord(doc, context.reader(), csw.termInfos);

                float score = 0;
                float indexNorm = estimateIndexNorm(context.reader());
                float termNorm = context.reader().getNormValues(csw.field).get(doc);
                
                for(String term: csw.termInfos.keySet()){
                    QueryTokenInfo qti = csw.termInfos.get(term);
                    String queryAttribute = qti.getAttribute();
                    Long localNorm = attribWeights.get(queryAttribute);
                    Long localMultiplicity = attributeMultiplicity.get(queryAttribute);

                    log.info("Term: "+term+" norm is: "+localNorm+" and multiplicity: "+localMultiplicity);
                    float idfGain =(9f + (2f * qti.getIdf()))/10f;
                    log.info("Idf gain for term "+term+" is: "+idfGain);
                    float sumPart = (localNorm/(indexNorm*localMultiplicity)) * idfGain;
                    log.info("Sum part for term (multiplicity included) "+term+" is: "+sumPart);
                    score+= sumPart;
                }
                log.info("Scorer: "+csw.desc+" Score after summation: "+score);
                //score /= csw.termInfos.keySet().size();
                score *= docCoord;
                log.info("Scorer: "+csw.desc+" Score without query: "+score);
                return score;
            } catch (IOException ex) {
                log.error(ex);
            }
            return 0f;
        }

        @Override
        public float computeSlopFactor(int distance) {
            float slop = 1.0f / (distance + 1);
            log.info("Computing slope factor. Scorer: "+csw.desc+" Distance: "+distance+" Slop: "+slop);
            return slop;
        }

        @Override
        public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
            log.info(" Scorer: "+csw.desc+" Calculating payload: Doc: "+doc+" Start: "+start+" End: "+end+" Payload: "+payload.utf8ToString());
            return 1f;
        }
        
    }
}
