/**
 * Copyright (c) 2014 Oculus Info Inc. 
 * http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.binning.io.serialization.impl;

import com.oculusinfo.binning.io.serialization.GenericJSONSerializer;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StringIntPairArrayJsonSerializer extends GenericJSONSerializer<List<Pair<String, Integer>>> {
	private static final long serialVersionUID = -7445619308538292627L;
	private static final TypeDescriptor TYPE_DESCRIPTOR = new TypeDescriptor(List.class,
		   new TypeDescriptor(Pair.class,
		                      new TypeDescriptor(String.class),
		                      new TypeDescriptor(Integer.class)));



	public StringIntPairArrayJsonSerializer() {
		super();
	}

	@Override
	public TypeDescriptor getBinTypeDescription () {
		return TYPE_DESCRIPTOR;
	}

	
	@Override
	public Object translateToJSON (List<Pair<String, Integer>> value) {
		JSONArray outputMap = new JSONArray();

		try {
			for (Pair<String, Integer> pair: value) {
				JSONObject entryObj = new JSONObject();
				entryObj.put(pair.getFirst(), pair.getSecond());
				outputMap.put(entryObj);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return outputMap;
	}

	@Override
	protected List<Pair<String, Integer>> getValue(Object obj) throws JSONException {
		JSONArray bin = (JSONArray) obj;
	
		List<Pair<String, Integer>> result = new ArrayList<Pair<String,Integer>>(); 

		for (int j=0; j < bin.length(); j++){
			JSONObject entry = bin.getJSONObject(j);
			@SuppressWarnings("unchecked")
			Iterator<String> keys = entry.keys();
			while (keys.hasNext()){
				String key = keys.next();
				result.add(new Pair<String, Integer>(key, entry.getInt(key)));
			}
		}
		
		return result;
	}
}
