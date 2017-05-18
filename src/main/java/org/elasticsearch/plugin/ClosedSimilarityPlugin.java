/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.elasticsearch.plugin;

import org.elasticsearch.index.similarity.ClosedSimilarityProvider;
import org.elasticsearch.index.similarity.SimilarityModule;
import org.elasticsearch.plugins.Plugin;

/**
 *
 * @author marijom
 */
public class ClosedSimilarityPlugin extends Plugin{

    @Override
    public String name() {
        return "closed-similarity";
    }

    @Override
    public String description() {
        return "Closed Similarity measure, limited in range";
    }
    
    public void onModule(SimilarityModule module) {
        module.addSimilarity("closed-similarity", ClosedSimilarityProvider.class);
    }    
}
