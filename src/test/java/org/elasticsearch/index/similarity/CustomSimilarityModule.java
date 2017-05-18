/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.elasticsearch.index.similarity;

import com.google.common.collect.Maps;
import java.util.Map;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Scopes;
import org.elasticsearch.common.inject.assistedinject.FactoryProvider;
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.common.settings.Settings;

/**
 *
 * @author marijom
 */
public class CustomSimilarityModule extends AbstractModule{

    public static final String SIMILARITY_SETTINGS_PREFIX = "index.similarity";

    private final Settings settings;
    private final Map<String, Class<? extends SimilarityProvider>> similarities = Maps.newHashMap();

    public CustomSimilarityModule(Settings settings) {
        this.settings = settings;
        addSimilarity("default", DefaultSimilarityProvider.class);
        addSimilarity("BM25", BM25SimilarityProvider.class);
        addSimilarity("DFR", DFRSimilarityProvider.class);
        addSimilarity("IB", IBSimilarityProvider.class);
        addSimilarity("LMDirichlet", LMDirichletSimilarityProvider.class);
        addSimilarity("LMJelinekMercer", LMJelinekMercerSimilarityProvider.class);
        addSimilarity("DFI", DFISimilarityProvider.class);
        addSimilarity("ClosedSimilarity", ClosedSimilarityProvider.class);
    }

    /**
     * Registers the given {@link SimilarityProvider} with the given name
     *
     * @param name Name of the SimilarityProvider
     * @param similarity SimilarityProvider to register
     */
    public void addSimilarity(String name, Class<? extends SimilarityProvider> similarity) {
        similarities.put(name, similarity);
    }

    @Override
    protected void configure() {
        MapBinder<String, SimilarityProvider.Factory> similarityBinder =
            MapBinder.newMapBinder(binder(), String.class, SimilarityProvider.Factory.class);

        Map<String, Settings> similaritySettings = settings.getGroups(SIMILARITY_SETTINGS_PREFIX);
        for (Map.Entry<String, Settings> entry : similaritySettings.entrySet()) {
            String name = entry.getKey();
            Settings settings = entry.getValue();

            String typeName = settings.get("type");
            if (typeName == null) {
                throw new IllegalArgumentException("Similarity [" + name + "] must have an associated type");
            } else if (similarities.containsKey(typeName) == false) {
                throw new IllegalArgumentException("Unknown Similarity type [" + typeName + "] for [" + name + "]");
            }
            similarityBinder.addBinding(entry.getKey()).toProvider(FactoryProvider.newFactory(SimilarityProvider.Factory.class, similarities.get(typeName))).in(Scopes.SINGLETON);
        }

        for (PreBuiltSimilarityProvider.Factory factory : Similarities.listFactories()) {
            if (!similarities.containsKey(factory.name())) {
                similarityBinder.addBinding(factory.name()).toInstance(factory);
            }
        }

        bind(SimilarityLookupService.class).asEagerSingleton();
        bind(SimilarityService.class).asEagerSingleton();
    }    
}
