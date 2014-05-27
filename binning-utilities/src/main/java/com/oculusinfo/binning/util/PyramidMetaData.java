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
package com.oculusinfo.binning.util;



import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.impl.AOITilePyramid;
import com.oculusinfo.binning.impl.WebMercatorTilePyramid;



/**
 * This is a wrapper around a JSON object containing a layer's metadata. It is
 * intended to encapsulate, properly type, and simplify access to the metadata.
 * 
 * This version is intended to be immutable. The user is capable of retrieving
 * the raw JSON and altering it, of course, but they are explicitly not supposed
 * to.
 * 
 * @author nkronenfeld
 */
public class PyramidMetaData {
	private static final Logger LOGGER = Logger.getLogger(PyramidMetaData.class.getName());



	private JSONObject          _metaData;



	public PyramidMetaData (JSONObject metaData) {
		_metaData = metaData;
	}

	public PyramidMetaData (String metaData) throws JSONException {
		_metaData = new JSONObject(metaData);
	}

    public PyramidMetaData (String name,
                            String description,
                            Integer tileSize,
                            String scheme,
                            String projection,
                            Integer minZoom,
                            Integer maxZoom,
                            Rectangle2D bounds,
                            Collection<Pair<Integer, String>> levelMins,
                            Collection<Pair<Integer, String>> levelMaxes) throws JSONException {
	    _metaData = new JSONObject();
	    if (null != name)
	        _metaData.put("name", name);
	    if (null != description)
	        _metaData.put("description", description);
	    if (null != tileSize)
	        _metaData.put("tilesize", tileSize);
	    if (null != scheme)
	        _metaData.put("scheme", scheme);
	    if (null != projection)
	        _metaData.put("projection", projection);
	    if (null != minZoom)
	        _metaData.put("minzoom", minZoom);
	    if (null != maxZoom)
	        _metaData.put("maxZoom", maxZoom);
	    if (null != bounds) {
	        JSONArray metaDataBounds = new JSONArray();
	        metaDataBounds.put(bounds.getMinX());
	        metaDataBounds.put(bounds.getMinY());
	        metaDataBounds.put(bounds.getMaxX());
	        metaDataBounds.put(bounds.getMaxY());
	        _metaData.put("bounds", metaDataBounds);
	    }
	    if (null != levelMins || null != levelMaxes) {
	        JSONObject metaMeta = new JSONObject();
	        _metaData.put("meta", metaMeta);
            if (null != levelMins) {
                JSONObject levelMinMap = new JSONObject();
                metaMeta.put("levelMinimums", levelMinMap);
                for (Pair<Integer, String> entry: levelMins) {
                    levelMinMap.put(""+entry.getFirst(), entry.getSecond());
                }
            }
            if (null != levelMaxes) {
                JSONObject levelMaxMap = new JSONObject();
                metaMeta.put("levelMaximums", levelMaxMap);
                for (Pair<Integer, String> entry: levelMaxes) {
                    levelMaxMap.put(""+entry.getFirst(), entry.getSecond());
                }
            }
	    }
	}
	/**
	 * Sometimes one just needs access to the raw json object - such as when one
	 * is constructing another such object. This method provides said access.
	 * 
	 * @return The raw JSON object describing the metadata of our layer.
	 */
	public JSONObject getRawData () {
		return _metaData;
	}



	/**
     * Get the user-readable name of the tile pyramid described by this metadata
     * object.
     */
	public String getName () {
	    return _metaData.optString("name", "unknown");
	}

	/**
     * Get the user-readable description of the tile pyramid described by this
     * metadata object.
     */
	public String getDescription () {
	    return _metaData.optString("description", "unknown");
	}

	/**
     * Get the size of tiles in the tile pyramid described by this metadata
     * object, in bins. This is deprecated in favor of {@link #getTileSizeX()}
     * and {@link #getTileSizeY()}.
     */
	@Deprecated
	public int getTileSize () {
	    return _metaData.optInt("tilesize", 256);
	}

    /**
     * Get the horizontal size of tiles in the tile pyramid described by this
     * metadata object, in bins.
     */
    public int getTileSizeX () {
        return _metaData.optInt("tilesizex", _metaData.optInt("tilesize", 256));
    }

    /**
     * Get the vertical size of tiles in the tile pyramid described by this
     * metadata object, in bins.
     */
    public int getTileSizeY () {
        return _metaData.optInt("tilesizey", _metaData.optInt("tilesize", 256));
    }

	/**
     * Get the tile index scheem used for the tile pyramid described by this
     * metadata object. At the moment, the only (and default) supported value is
     * "TMS".
     */
	public String getScheme () {
	    return _metaData.optString("scheme", "TMS");
	}

	/**
     * Get the name of the projection used for the tile pyramid described by
     * this metadata object.
     */
	public String getProjection () {
	    return _metaData.optString("projection", "unprojected");
	}

    /**
     * Get the minimum zoom level supported by the tile pyramid described by
     * this metadata object.
     */
    public int getMinZoom () {
        return _metaData.optInt("minzoom", 0);
    }

    /**
     * Get the maximum zoom level supported by the tile pyramid described by
     * this metadata object.
     */
    public int getMaxZoom () {
        return _metaData.optInt("maxzoom", 0);
    }

    /**
     * Get the bounds of the area covered by the tile pyramid described by this
     * metadata object, in raw data coordinates.
     */
    public Rectangle2D getBounds () {
        JSONArray metaDataBounds = _metaData.optJSONArray("bounds");
        if (null == metaDataBounds) return null;
        if (metaDataBounds.length()<4) return null;
        double minx = metaDataBounds.optDouble(0);
        double miny = metaDataBounds.optDouble(1);
        double maxx = metaDataBounds.optDouble(2);
        double maxy = metaDataBounds.optDouble(3);
        return new Rectangle2D.Double(minx, miny, maxx-minx, maxy-miny);
    }

    /**
     * Get the tiling projection used to create the tile pyramid described by
     * this metadata object.
     */
    public TilePyramid getTilePyramid () {
        try {
            String projection = _metaData.getString("projection");
            if ("EPSG:4326".equals(projection)) {
                JSONArray bounds = _metaData.getJSONArray("bounds");
                double xMin = bounds.getDouble(0);
                double yMin = bounds.getDouble(1);
                double xMax = bounds.getDouble(2);
                double yMax = bounds.getDouble(3);
                return new AOITilePyramid(xMin, yMin, xMax, yMax);
            } else if ("EPSG:900913".equals(projection) || "EPSG:3857".equals(projection)) {
                return new WebMercatorTilePyramid();
            }
        } catch (JSONException e) {
            LOGGER.log(Level.WARNING, "Bad projection data in tile pyramid", e);
        }
        return null;
    }

    /**
     * Get a list of all levels described by the metadata of the given tile
     * pyramid.
     * 
     * @return A list of levels. This should never be null, but may be empty if
     *         an error is encountered getting the level information.
     */
    public List<Integer> getLevels () {
        JSONObject maxes = getLevelExtremaObject(true);

        List<Integer> levels = new ArrayList<Integer>();
        if (null != maxes) {
            for (Iterator<?> i = maxes.keys(); i.hasNext();) {
                Object rawKey = i.next();
                try {
                    int key = Integer.parseInt(rawKey.toString());
                    levels.add(key);
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "Unparsable level " + rawKey + ".");
                }
            }

            Collections.sort(levels);
        }

        return levels;
    }



    private JSONObject getLevelExtremaObject (boolean max) {
		JSONObject metaInfo;
		try {
			metaInfo = _metaData.getJSONObject("meta");
		} catch (JSONException e) {
			LOGGER.log(Level.WARNING,
			           "Missing meta object in pyramid metadata.");
			return null;
		}

		JSONObject extrema;
		try {
			if (max) extrema = metaInfo.getJSONObject("levelMaximums");
			else extrema = metaInfo.getJSONObject("levelMinimums");
		} catch (JSONException e1) {
			try {
				// Some older tiles use this instead - included for backwards
				// compatibility. No real users should ever reach this.
				if (max) extrema = metaInfo.getJSONObject("levelMaxFreq");
				else extrema = metaInfo.getJSONObject("levelMinFreq");
			} catch (JSONException e2) {
				LOGGER.log(Level.WARNING,
				           "Missing level bounds in pyramid metadata.");
				return null;
			}
		}
		return extrema;
	}

	private Map<Integer, String> getLevelExtrema (boolean max) {
		JSONObject extrema = getLevelExtremaObject(max);

		Map<Integer, String> byLevel = new HashMap<Integer, String>();
		if (null != extrema) {
			for (Iterator<?> i = extrema.keys(); i.hasNext();) {
				Object rawKey = i.next();
				try {
					int key = Integer.parseInt(rawKey.toString());
					String value = extrema.getString(rawKey.toString());
					byLevel.put(key, value);
				} catch (NumberFormatException e) {
					LOGGER.log(Level.WARNING, "Unparsable level "+rawKey+".");
				} catch (JSONException e) {
					LOGGER.log(Level.WARNING, "Error reading level maximum value for level "+rawKey+".");
				}
			}
		}
		return byLevel;
	}

	public String getLevelExtremum (boolean max, int level) {
		JSONObject extrema = getLevelExtremaObject(max);

		if (null != extrema) {
			for (Iterator<?> i = extrema.keys(); i.hasNext();) {
				Object rawKey = i.next();
				try {
					int key = Integer.parseInt(rawKey.toString());
					if (key == level) {
						return extrema.getString(rawKey.toString());
					}
				} catch (NumberFormatException e) {
					LOGGER.log(Level.WARNING, "Unparsable level " + rawKey
					           + ".");
				} catch (JSONException e) {
					LOGGER.log(Level.WARNING,
					           "Error reading level maximum value for level "
					           + rawKey + ".");
				}
			}
		}
		return null;
	}

	/**
	 * Get a map, indexed by level, of the maximum value for each level
	 * 
	 * @return A map from level to maximum value of that level. May be empty,
	 *         but should never be null.
	 */
	public Map<Integer, String> getLevelMaximums () {
		return getLevelExtrema(true);
	}

	/**
	 * Get a map, indexed by level, of the minimum value for each level
	 * 
	 * @return A map from level to minimum value of that level. May be empty,
	 *         but should never be null.
	 */
	public Map<Integer, String> getLevelMinimums () {
		return getLevelExtrema(false);
	}

	/**
	 * Get the maximum value of a given level
	 * 
	 * @param level
	 *            The level of interest
	 * @return The maximum value of that level, or null if no maximum is
	 *         properly specified for that level.
	 */
	public String getLevelMaximum (int level) {
		return getLevelExtremum(true, level);
	}

	/**
	 * Get the minimum value of a given level
	 * 
	 * @param level
	 *            The level of interest
	 * @return The minimum value of that level, or null if no maximum is
	 *         properly specified for that level.
	 */
	public String getLevelMinimum (int level) {
		return getLevelExtremum(false, level);
	}


	/**
     * Create a new metadata object, which is a combination of the current with
     * the specified new level.
     */
	public PyramidMetaData addLevel (int level, String min, String max) throws JSONException {
	    JSONObject newBase = JsonUtilities.deepClone(_metaData);
	    if (!newBase.has("meta"))
	        newBase.put("meta", new JSONObject());
	    JSONObject meta = newBase.getJSONObject("meta");
	    if (!meta.has("levelMinimums"))
	        meta.put("levelMinimums", new JSONObject());
	    if (!meta.has("levelMaximums"))
	        meta.put("levelMaximums", new JSONObject());
	    meta.getJSONObject("levelMinimums").put(""+level, min);
	    meta.getJSONObject("levelMaximums").put(""+level, max);
	    return new PyramidMetaData(newBase);
	}

	@Override
	public String toString () {
	    return _metaData.toString();
	}
}
