/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.elasticsearch.index.analysis.test;

import java.util.Map;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;
import org.elasticsearch.common.settings.Settings;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

/**
 *
 * @author marijom
 */
public class GeneralTest {
    
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
