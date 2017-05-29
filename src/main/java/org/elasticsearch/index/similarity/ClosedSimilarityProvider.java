/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.elasticsearch.index.similarity;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import org.apache.lucene.search.similarities.Similarity;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.similarity.ClosedSimilarity.AttributeType;

/**
 *
 * @author marijom
 */
public class ClosedSimilarityProvider extends AbstractSimilarityProvider{
    
    private static Logger log = Logger.getLogger(ClosedSimilarityProvider.class);
    
    private final ClosedSimilarity similarity;
    
    @Inject
    public ClosedSimilarityProvider(@Assisted String name, @Assisted Settings settings){
        super(name);
        
        //set similarity parameters from settings
        Map<String, Long> attribWeights = new HashMap<>();
        Map<String, ClosedSimilarity.AttributeType> attribTypes = new HashMap<>();
        
        Map<String, Object> attributeWeights = settings.getByPrefix("attributeWeights.").getAsStructuredMap();
        for(Entry<String, Object> entry: attributeWeights.entrySet()){
            long weight = 1l;
            try{
                weight = Long.parseLong((String) entry.getValue());
            } catch(NumberFormatException ex) {}
            attribWeights.put(entry.getKey(), weight);
        }

        Map<String, String> attributeTypes = settings.getByPrefix("attributeTypes.").getAsMap();
        for(Entry<String, String> entry: attributeTypes.entrySet()){
            String type = entry.getValue();
            switch(type){
                case "String": attribTypes.put(entry.getKey(), AttributeType.String); break;
                case "Integer": attribTypes.put(entry.getKey(), AttributeType.Integer); break;
                case "Float": attribTypes.put(entry.getKey(), AttributeType.Float); break;
                default: attribTypes.put(entry.getKey(), AttributeType.String); break;
            }
        }
        
        log.info("Provider parameters: Weights"+attribWeights.toString()+" Types: "+attribTypes.toString());
        
        similarity = new ClosedSimilarity(attribWeights, attribTypes);
    }

    @Override
    public Similarity get() {
        return similarity;
    }
}
