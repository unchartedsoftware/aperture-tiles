/**
 * Copyright (c) 2013 Oculus Info Inc.
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
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.spi.impl.pyramidio.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.util.PyramidMetaData;
import com.oculusinfo.tile.spi.AvroJSONConverter;
import com.oculusinfo.tile.spi.ImageTileService;
import com.oculusinfo.tile.spi.impl.pyramidio.image.renderer.ImageRendererFactory;
import com.oculusinfo.tile.spi.impl.pyramidio.image.renderer.RenderParameter;
import com.oculusinfo.tile.spi.impl.pyramidio.image.renderer.TileDataImageRenderer;

/**
 * @author dgray
 *
 */
@Singleton
public class ImageTileServiceImpl implements ImageTileService {

	private static final Color COLOR_BLANK = new Color(255,255,255,0);
	
	private Map<String, JSONObject> _metadataCache = 
			Collections.synchronizedMap(new HashMap<String, JSONObject>());
	
	private final Logger _logger = LoggerFactory.getLogger(getClass());

	private Map<UUID, JSONObject> _uuidToOptionsMap = Collections.synchronizedMap(new HashMap<UUID, JSONObject>());
	private boolean _debug = false;
	
	@Inject
	private PyramidIO _pyramidIo;

	ImageTileServiceImpl () {
	}
	
	/* (non-Javadoc)
	 * @see com.oculusinfo.tile.spi.TileService#getLayer(String)
	 */
	public JSONObject getLayer(String hostUrl, JSONObject options) {
		try {
			String layer = options.getString("layer");
			PyramidMetaData metadata = getMetadata(layer);
			JSONObject result = new JSONObject();
		
			String[] names = JSONObject.getNames(metadata.getRawData());
			result = new JSONObject(metadata.getRawData(), names);
			
			UUID id = UUID.randomUUID();
			_uuidToOptionsMap.put(id, options);
			result.put("layer", layer);
			result.put("id", id);
			result.put("tms", hostUrl + "tile/" + id.toString() + "/");
			result.put("apertureservice", "/tile/" + id.toString() + "/");

			String rendererType = options.getString("renderer");
			
			TileDataImageRenderer renderer = ImageRendererFactory.getRenderer(rendererType, _pyramidIo);

			result.put("imagesPerTile", renderer.getNumberOfImagesPerTile(metadata));
			System.out.println("UUID Count after "+layer+": " + _uuidToOptionsMap.size());
			return result;
			
		} catch (JSONException e) {
			_logger.warn("Failed to apply layer parameters to json object.", e);
			return new JSONObject();
		} 

	}



	/* (non-Javadoc)
	 * @see com.oculusinfo.tile.spi.TileService#getTile(int, double, double)
	 */
	public BufferedImage getTileImage (UUID id, String layer, int zoomLevel, double x, double y) {
		PyramidMetaData metadata = getMetadata(layer);

		// DEFAULTS
		String rampType = "ware";
		String transform = "linear";
		String rendererType = "default";
		int rangeMin = 0;
		int rangeMax = 100;
		int currentImage = 0;
		
		if(id != null){
			// Get rendering options
			JSONObject options = _uuidToOptionsMap.get(id);
			
			try {
				rampType = options.getString("ramp");
			} catch (JSONException e2) {
				_logger.info("No ramp specified for tile request - using default.");
			}
			
			try {
				transform = options.getString("transform");
			} catch (JSONException e2) {
				_logger.info("No transform specified for tile request - using default.");
			}
			
			try {
				rendererType = options.getString("renderer");
			} catch (JSONException e2) {
				_logger.info("No renderer specified for tile request - using default.");
			}
			
			try {
				JSONArray legendRange = options.getJSONArray("legendRange");
				rangeMin = legendRange.getInt(0);
				rangeMax = legendRange.getInt(1);
			} catch (JSONException e3) {
				_logger.info("No ramp specified for tile request - using default.");
			}
	
			// "currentImage" is the index of an image in a series. e.g. a tile containing a time-series.
			try {
				currentImage = options.getInt("currentImage");
			} catch (JSONException e4) {
				_logger.info("No current image specified - using default");
			}
		}

		int dimension = 256;
		String maximumValue = metadata.getLevelMaximum(zoomLevel);


		BufferedImage bi;

		TileDataImageRenderer renderer = ImageRendererFactory.getRenderer(rendererType, _pyramidIo);
		TileIndex tileCoordinate = new TileIndex(zoomLevel, (int)x, (int)y);
		bi = renderer.render(new RenderParameter(layer, rampType, transform, rangeMin, rangeMax,
				dimension, maximumValue, tileCoordinate, currentImage));
		
		if (bi == null){
			bi = new BufferedImage(dimension, dimension, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = bi.createGraphics();
			g.setColor(COLOR_BLANK);
			g.fillRect(0, 0, 256, 256);
			//g.setColor(Color.red);
			//g.drawLine(1, 1, 254, 254);
			g.dispose();
		}
		
		if(_debug){
			Graphics2D g = bi.createGraphics();
			g.setColor(Color.white);
			g.fillRect(12, 3, 100, 15);
			g.setColor(Color.black);
			g.drawString("x = " + x + ", y = " + y, 15, 15);
			g.dispose();
		}
		
		return bi;
	}

	@Override
	public JSONObject getTileObject(UUID id, String layer, int zoomLevel, double x, double y) {
		TileIndex tileCoordinate = new TileIndex(zoomLevel, (int)x, (int)y);
		try {
    		InputStream tile = _pyramidIo.getTileStream(layer, tileCoordinate);
    		if (null == tile) return null;
    		return AvroJSONConverter.convert(tile);
		} catch (IOException e) {
		    _logger.warn("Exception getting tile for {}", tileCoordinate, e);
		} catch (JSONException e) {
            _logger.warn("Exception getting tile for {}", tileCoordinate, e);
        }
		return null;
	}

	/**
	 * @param layer
	 * @param pyramidIo 
	 * @return
	 */
	private PyramidMetaData getMetadata(String layer) {
		JSONObject metadata = _metadataCache.get(layer);
		if(metadata == null){
			try {
				String s = _pyramidIo.readMetaData(layer);
				metadata = new JSONObject(s);
				_metadataCache.put(layer, metadata);
			} catch (Exception e) {
				_logger.error("Metadata file for layer '" + layer + "' is missing or corrupt.");
				_logger.debug("Metadata error: ", e);
			}
		}
		return new PyramidMetaData(metadata);
	}
}
