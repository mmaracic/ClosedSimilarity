package org.elasticsearch.index.analysis.test;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import static com.google.common.io.Files.createTempDir;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.elasticsearch.common.settings.Settings;
import static org.elasticsearch.common.settings.Settings.settingsBuilder;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.EnvironmentModule;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexNameModule;
import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.index.mapper.MapperServiceModule;
import org.elasticsearch.index.settings.IndexSettingsModule;
import org.elasticsearch.index.similarity.CustomSimilarityModule;
import org.elasticsearch.index.similarity.SimilarityService;
import org.elasticsearch.indices.analysis.IndicesAnalysisService;
import org.elasticsearch.indices.mapper.MapperRegistry;
import org.elasticsearch.script.ScriptModule;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.threadpool.ThreadPoolModule;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Marijo
 */
public class ClosedSimilarityTest {
    
    public ClosedSimilarityTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

/*     @Test
     public void testSimilarity() throws IOException
     {
        Settings settings = Settings.settingsBuilder()
                .put("path.home", createTempDir())
                .build();
        SimilarityService simService = createSimilarityService(settings);
        Similarity sim = simService.similarityLookupService().similarity("ClosedSimilarity").get();
     }
    
    public static SimilarityService createSimilarityService(Settings settings) {
        Index index = new Index("test");
        Settings indexSettings = settingsBuilder().put(settings)
                .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                .build();
        Injector parentInjector = new ModulesBuilder().add(new SettingsModule(settings), new EnvironmentModule(new Environment(settings))).createInjector();
        Injector injector = new ModulesBuilder().add(
                new IndexSettingsModule(index, indexSettings),
                new IndexNameModule(index),
                new AnalysisModule(settings, parentInjector.getInstance(IndicesAnalysisService.class)),
                new ScriptModule(settings),
                new MapperServiceModule(),
                new ThreadPoolModule(new ThreadPool(settings)),
                new CustomSimilarityModule(settings))
                .createChildInjector(parentInjector);

        return injector.getInstance(SimilarityService.class);
    }    */
    
    @Test
    public void testDecodeInteger(){
        int testNumber = 52341;
        byte[] testBytes = {0x60, 0x8, 0x0, 0x3, 0x18, 0x75};
        BytesRef br = new BytesRef(testBytes);
        int resultNumber = NumericUtils.prefixCodedToInt(br);
        assertEquals("Numbers are not the same", testNumber, resultNumber);
    }
    
    @Test
    public void testCodeInteger() {
        int shift = 0;
        int testNumber = 52341;
        byte[] testBytes = {0x60, 0x8, 0x0, 0x3, 0x18, 0x75};
        
        BytesRefBuilder brb = new BytesRefBuilder();
        NumericUtils.intToPrefixCoded(testNumber, shift, brb);
        BytesRef br = brb.toBytesRef();
        byte[] b = br.bytes;
        
        assertEquals("Byte array is not of the same length", testBytes.length, b.length);
        boolean different = false;
        for(int i=0; i<b.length; i++){
            if (b[i] != testBytes[i]){
                different = true;
            }
        }
        assertFalse("Array bytes are not the same!", different);       
    }
    
    @Test
    public void testSettings(){
        Settings settings = Settings.settingsBuilder()
                .put("attributeWeights.streetNumberAlfa", 1)
                .put("attributeWeights.streetNumber", 2)
                .put("attributeWeights.streetName", 4)
                .put("attributeWeights.postalCode", 8)
                .put("attributeWeights.settlementName", 16)
                .put("attributeWeights.countyName", 32)
                .put("attributeTypes.streetNumberAlfa", "String")
                .put("attributeTypes.streetNumber", "String")
                .put("attributeTypes.streetName", "String")
                .put("attributeTypes.postalCode", "Integer")
                .put("attributeTypes.settlementName", "String")
                .put("attributeTypes.countyName", "String")
                .build();
        
        System.out.println("Settings: "+settings.toDelimitedString('#'));
        Map<String, Object> attributeWeights = settings.getByPrefix("attributeWeights.").getAsStructuredMap();
        System.out.println("Provider raw parameters: Weights: "+attributeWeights.toString());
        Map<String, String> attributeTypes = settings.getByPrefix("attributeTypes.").getAsMap();
        System.out.println("Provider raw parameters: Weights: "+attributeTypes.toString());
        
    }
}
