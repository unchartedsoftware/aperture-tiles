/**
 * Copyright (C) 2013 Oculus Info Inc. 
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
package com.oculusinfo.tile.spi.impl.pyramidio.json;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oculusinfo.tile.spi.JsonTileService;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TileIterator;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.impl.AOITilePyramid;
import com.oculusinfo.binning.impl.WebMercatorTilePyramid;
import com.oculusinfo.binning.io.Pair;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.TileSerializer;

/**
 * Right now this one has a hard-coded TileSerializer. 
 * This could chanage.
 * 
 * @author dgray
 *
 */
@Singleton
public class JsonTileServiceImpl implements JsonTileService {

	
	private Map<String, JSONObject> _metadataCache = 
			Collections.synchronizedMap(new HashMap<String, JSONObject>());
	
	private final Logger _logger = LoggerFactory.getLogger(getClass());

	private PyramidIO _pyramidIo;
	private TileSerializer<List<Pair<String, Integer>>> _serializer;
	
	@Inject
	JsonTileServiceImpl (PyramidIO pyramidIo, TileSerializer<List<Pair<String, Integer>>> serializer) {
		_pyramidIo = pyramidIo;
		_serializer = serializer;
	}
	private static Comparator<Pair<String, Integer>> PairComparator = new Comparator<Pair<String, Integer>>() {
	
		public int compare(Pair<String, Integer> hashtag1, Pair<String, Integer> hashtag2) {
		
			return hashtag2.getSecond().compareTo(hashtag1.getSecond());
		}
	
	};

	@Override
	public JSONObject getTiles(JSONObject parameters) {
		try {
			String layer = parameters.getString("layer");
			JSONObject bounds = parameters.getJSONObject("bounds");
			int level = parameters.getInt("level");
			
			// Not sure what kind of bounds object parameters or whatever the API will accept (Jesse?).
			double xmin = bounds.getDouble("xmin");
			double ymin = bounds.getDouble("ymin");
			double xmax = bounds.getDouble("xmax");
			double ymax = bounds.getDouble("ymax");
			
			TilePyramid tilePyramid = null;
			JSONObject metadata = getMetadata(layer);
			// TODO: get the projection from the Metadata
			// Use the projection to instantiate a TilePyramid - this will get us tile bounds 
			String projString = metadata.getString("projection");
			
			if(projString.equals("EPSG:900913")){
				tilePyramid = new WebMercatorTilePyramid();
			}
			else if(projString.equals("EPSG:4326")){
				JSONArray mapBounds = metadata.getJSONArray("bounds");
				tilePyramid = new AOITilePyramid(mapBounds.getInt(0), mapBounds.getInt(1), 
						mapBounds.getInt(2), mapBounds.getInt(3));
			}
		
			TileIterator tileIterator = new TileIterator(tilePyramid, level, new Rectangle2D.Double(xmin, ymin, xmax-xmin, ymax-ymin));


			JSONObject result = new JSONObject();
			
			// Create the hashtag array.
			JSONArray dataArray = new JSONArray();
			TilePyramid mercatorPyramid = new WebMercatorTilePyramid();
			while (tileIterator.hasNext()) {
				TileIndex tIndex = tileIterator.next();
				
				Rectangle2D tileBounds = mercatorPyramid.getTileBounds(tIndex);

				//TileSerializer<List<Pair<String, Integer>>> serializer = new StringIntArrayJSONSerializer();
				List<TileData<List<Pair<String, Integer>>>> datum = _pyramidIo.readTiles(layer,
						_serializer,
						Collections.singleton(tIndex));
				
				for (TileData<List<Pair<String, Integer>>> tileData : datum){
					for (List<Pair<String, Integer>> pairList : tileData.getData()){
						if (pairList.size() > 0){
							JSONObject tileObj = new JSONObject();
							JSONArray tagArray = new JSONArray();
							
							@SuppressWarnings("unchecked")
							Pair<String,Integer>[] pairArray = pairList.toArray(new Pair[pairList.size()]);

							Arrays.sort(pairArray, PairComparator);
							
							for (Pair<String, Integer> pair : pairArray){
								JSONObject tagPair = new JSONObject();
								tagPair.put("text", pair.getFirst());
								tagPair.put("value", pair.getSecond());
								tagArray.put(tagPair);
							}
							
							tileObj.put("longitude", tileBounds.getCenterX());
							tileObj.put("latitude", tileBounds.getCenterY());
							tileObj.put("hashtags", tagArray);
	
							String tileId = tIndex.getLevel() + "/" + tIndex.getX() + "/" + tIndex.getY();
							JSONObject indexObj = new JSONObject();
							indexObj.put("level", tIndex.getLevel());
							indexObj.put("x", tIndex.getX());
							indexObj.put("y", tIndex.getY());
							tileObj.put("index", indexObj);
							tileObj.put("id", tileId);
							dataArray.put(tileObj);
						}
					}
				}
			}

			result.put("json", dataArray);
			result.put("layer", layer);
			return result;
			
		} catch (Exception e) {
			_logger.warn("Failed to construct json result object.", e);
			return new JSONObject();
		} 

	}
	

	/**
	 * @param layer
	 * @param pyramidIo 
	 * @return
	 */
	private JSONObject getMetadata(String layer) {
		JSONObject metadata = _metadataCache.get(layer);
		if(metadata == null){
			try {
				String s = _pyramidIo.readMetaData(layer);
				metadata = new JSONObject(s);
				_metadataCache.put(layer, metadata); 
			} catch (Exception e) {
				_logger.error("Metadata for layer '" + layer + "' is missing or corrupt.");
				_logger.debug("Metadata error: ", e);
			}
		}
		return metadata;
	}

}
