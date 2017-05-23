/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.elasticsearch.index.similarity;

import org.apache.lucene.search.similarities.Similarity;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;

/**
 *
 * @author marijom
 */
public class ClosedSimilarityProvider extends AbstractSimilarityProvider{
    
    private final ClosedSimilarity similarity;
    
    @Inject
    public ClosedSimilarityProvider(@Assisted String name, @Assisted Settings settings){
        super(name);
        
        //set similarity parameters from settings
        similarity = new ClosedSimilarity();
    }

    @Override
    public Similarity get() {
        return similarity;
    }
}
