/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.annotation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.oculusinfo.annotation.index.*;
import com.oculusinfo.annotation.index.impl.*;
import com.oculusinfo.annotation.io.*;
import com.oculusinfo.annotation.io.serialization.*;
import com.oculusinfo.annotation.io.serialization.impl.*;
import com.oculusinfo.annotation.query.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class AnnotationSerializationTests extends AnnotationTestsBase {
	
	static final boolean VERBOSE = true;
	
	private AnnotationIndexer<JSONObject>    _indexer;
	private AnnotationSerializer<JSONObject> _serializer;
	
   @Before
    public void setup () {
    	_indexer = new TiledAnnotationIndexer();
    	_serializer = new TestJSONSerializer();
    }

    @After
    public void teardown () {
    	_indexer = null;
    	_serializer = null;
    }

    @Test
    public void testJSONSerialization () throws Exception {
    	
		List<AnnotationBin<JSONObject>> before = generateJSONAnnotations( NUM_ENTRIES, _indexer );
		List<AnnotationBin<JSONObject>> after = new ArrayList<AnnotationBin<JSONObject>>();
			
		if (VERBOSE) {
			System.out.println( "*** Before ***");
			print( before );
		}
		
		for ( AnnotationBin<JSONObject> annotation : before ) {
			
			// serialize
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			_serializer.serialize( annotation, baos );
			baos.close();
            baos.flush();
            
            // deserialize
            byte[] data = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            AnnotationBin<JSONObject> bin = _serializer.deserialize( bais );
            after.add( bin );
            bais.close();
            
            Assert.assertEquals( annotation, bin );
		}
		
		
		if (VERBOSE) {
			System.out.println( "*** After ***");
			print( before );
		}
    }
	
	
	
}
