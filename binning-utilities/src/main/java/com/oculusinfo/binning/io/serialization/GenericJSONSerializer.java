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
package com.oculusinfo.binning.io.serialization;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.TileData;

public abstract class GenericJSONSerializer<T> implements TileSerializer<T> {
    private static final long serialVersionUID = 2617903534522413550L;



    abstract protected T getValue (Object bin) throws JSONException;
	abstract protected JSONArray translateToJSON (T value);

	protected GenericJSONSerializer () {
	}

	public String getFileExtension(){
		return "json";
	}

	@Override
	public TileData<T> deserialize (TileIndex index, InputStream rawData){

		String jsonString = convertStreamToString(rawData);
		try {
			JSONObject json = new JSONObject(jsonString);
			JSONObject metaData = json.getJSONObject("metadata");
			JSONObject tileSize = metaData.getJSONObject("tilesize");
			int xBins = tileSize.getInt("width");
			int yBins = tileSize.getInt("height");
			int level = json.getInt("z");
			int x = json.getInt("x");
			int y = json.getInt("y");
			JSONArray bins = json.getJSONArray("bins");

			List<T> values = new ArrayList<T>();
			for (int i = 0; i < bins.length(); i++) {
				values.add(getValue(bins.get(i)));
			}
			TileIndex defn = new TileIndex(level, x, y, xBins, yBins);
			return new TileData<T>(defn, values);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return null;
	}

	private String convertStreamToString(java.io.InputStream is) {
		try {
			InputStreamReader isr = new InputStreamReader(is, "UTF-8");
			java.util.Scanner s = new java.util.Scanner(isr);
			s.useDelimiter("\\A");
			String result = s.hasNext() ? s.next() : "";
			s.close();
			return result;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;

	}

	@Override
	public void serialize (TileData<T> tile, TilePyramid pyramid, OutputStream stream) throws IOException {

		TileIndex idx = tile.getDefinition();
		int z = idx.getLevel();
		int x = idx.getX();
		int y = idx.getY();

		TileIndex tileIndex = new TileIndex(z, x, y);
		Rectangle2D bbox = pyramid.getTileBounds(tileIndex);
		double centerLon = bbox.getCenterX();
		double centerLat = bbox.getCenterY();

		try {
			JSONObject jsonEntry = new JSONObject();
			jsonEntry.put("longitude", centerLon);
			jsonEntry.put("latitude", centerLat);
			jsonEntry.put("x", x);
			jsonEntry.put("y", y);
			jsonEntry.put("z", z);


			JSONArray bins = new JSONArray();
			for (T value: tile.getData()) {
				if (value == null)continue;
				bins.put(translateToJSON(value));
			}
			jsonEntry.put("bins", bins);

			JSONObject metadataEntry = new JSONObject();
			jsonEntry.put("metadata", metadataEntry);
			JSONObject tileSize = new JSONObject();
			tileSize.put("width", 1);
			tileSize.put("height", 1);
			metadataEntry.put("tilesize", tileSize);
			OutputStreamWriter writer = new OutputStreamWriter(stream, "UTF-8");
			writer.write(jsonEntry.toString());
			writer.close();
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}


	//	@override
	//	public JSONObject translateToJSON (Map<String, Integer> value) {
	//		JSONArray outputMap = new JSONArray();
	//		for (Entry<String, Integer> entry: value.getEntryMap()) {
	//			JSONObject entryObj = new JSONObject();
	//			entryObj.put(entry.getKey(), entry.getValue());
	//			outputMap.put(entryObj);
	//		}
	//		return outputMap;
	//	}
}
