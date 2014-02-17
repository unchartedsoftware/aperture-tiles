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



import java.util.ArrayList;
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

    /**
     * Sometimes one just needs access to the raw json object - such as when one
     * is constructing another such object. This method provides said access.
     * 
     * @return The raw JSON object describing the metadata of our layer.
     */
    public JSONObject getRawData () {
        return _metaData;
    }


    private JSONObject getLevelMaximumsObject () {
        JSONObject metaInfo;
        try {
            metaInfo = _metaData.getJSONObject("meta");
        } catch (JSONException e) {
            LOGGER.log(Level.WARNING,
                       "Missing meta object in pyramid metadata.");
            return null;
        }

        JSONObject maxes;
        try {
            maxes = metaInfo.getJSONObject("levelMaximums");
        } catch (JSONException e1) {
            try {
                // Some older tiles use this instead - included for backwards
                // compatibility. No real users should ever reach this.
                maxes = metaInfo.getJSONObject("levelMaxFreq");
            } catch (JSONException e2) {
                LOGGER.log(Level.WARNING,
                           "Missing level bounds in pyramid metadata.");
                return null;
            }
        }
        return maxes;
    }

    /**
     * Get a map, indexed by level, of the maximum value for each level
     * 
     * @return A map from level to maximum value of that level. May be empty,
     *         but should never be null.
     */
    public Map<Integer, String> getLevelMaximums () {
        JSONObject maxes = getLevelMaximumsObject();

        Map<Integer, String> byLevel = new HashMap<Integer, String>();
        if (null != maxes) {
            for (Iterator<?> i = maxes.keys(); i.hasNext();) {
                Object rawKey = i.next();
                try {
                    int key = Integer.parseInt(rawKey.toString());
                    String value = maxes.getString(rawKey.toString());
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

    /**
     * Get the maximum value of a given level
     * 
     * @param level
     *            The level of interest
     * @return The maximum value of that level, or null if no maximum is
     *         properly specified for that level.
     */
    public String getLevelMaximum (int level) {
        JSONObject maxes = getLevelMaximumsObject();

        if (null != maxes) {
            for (Iterator<?> i = maxes.keys(); i.hasNext();) {
                Object rawKey = i.next();
                try {
                    int key = Integer.parseInt(rawKey.toString());
                    if (key == level) {
                        return maxes.getString(rawKey.toString());
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
        JSONObject maxes = getLevelMaximumsObject();

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
}
