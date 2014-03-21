/*
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
package com.oculusinfo.tile.rest.tile;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import oculus.aperture.common.rest.ApertureServerResource;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;

import com.google.inject.Inject;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.tile.rest.ImageOutputRepresentation;

public class TileResource extends ApertureServerResource {

	public static enum ResponseType {
		Image,
		Tile
	}
	public static enum ExtensionType {
		png(ResponseType.Image, MediaType.IMAGE_PNG),
		jpg(ResponseType.Image, MediaType.IMAGE_JPEG),
		jpeg(ResponseType.Image, MediaType.IMAGE_JPEG),
		json(ResponseType.Tile, MediaType.APPLICATION_JSON);

		private ResponseType _responseType;
		private MediaType _mediaType;
		private ExtensionType (ResponseType responseType, MediaType mediaType) {
			_responseType = responseType;
			_mediaType = mediaType;
		}
		public ResponseType getResponseType () {
			return _responseType;
		}
		public MediaType getMediaType () {
			return _mediaType;
		}
	}
	
	private TileService _service;
	
	
	@Inject
	public TileResource(TileService service) {
		this._service = service;
	}
	
	@Post("json")
	public Representation getLayer(String jsonData) throws ResourceException {

		try {
			JSONObject jsonObj = new JSONObject(jsonData);
			
			String host = getRequest().getResourceRef().getPath();
			
			host = host.substring(0, host.lastIndexOf("layer"));
			
			JSONObject layerInfo = _service.getLayer(host, jsonObj);
			
			return new JsonRepresentation(layerInfo);
			
		} catch (JSONException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
			                            "Unable to create JSON object from supplied options string", e);
		}
	}

    private Integer getIntQueryValue (Form query, String key) {
        String stringValue = query.getFirstValue(key, true, null);
        if (null == stringValue) return null;
        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                        "Parameter "+key+" had non-integer value \""+stringValue+"\"");
        }
    }

    private TileIndex getTileIndexQueryValue (Form query, String key) {
        String stringValue = query.getFirstValue(key, true, null);
        if (null == stringValue) return null;
        return TileIndex.fromString(stringValue);
    }

    private Collection<TileIndex>  parseTileSetDescription (Form query) {
        Set<TileIndex> indices = null;

        if (null != query) {
            // Check for specifically requested tiles
            String[] tileSets = query.getValuesArray("tileset", true);
            for (String tileSetDescription: tileSets) {
                String[] tileDescriptions = tileSetDescription.split("\\|");
                for (String tileDescription: tileDescriptions) {
                    TileIndex index = TileIndex.fromString(tileDescription);
                    if (null != index) {
                        if (null == indices) {
                            indices = new HashSet<>();
                        }
                        indices.add(index);
                    }
                }
            }

            // Check for simple bounds
            Integer minX = getIntQueryValue(query, "minx");
            Integer maxX = getIntQueryValue(query, "maxx");
            Integer minY = getIntQueryValue(query, "miny");
            Integer maxY = getIntQueryValue(query, "maxy");
            Integer minZ = getIntQueryValue(query, "minz");
            Integer maxZ = getIntQueryValue(query, "maxz");

            TileIndex minTile = getTileIndexQueryValue(query, "mintile");
            TileIndex maxTile = getTileIndexQueryValue(query, "maxtile");

            if (null == minTile && null != minX && null != minY && null != minZ) {
                minTile = new TileIndex(minZ, minX, minY);
            }
            if (null == maxTile && null != maxX && null != maxY && null != maxZ) {
                maxTile = new TileIndex(maxZ, maxX, maxY);
            }
            if (null != minTile && null != maxTile) {
                for (int z=minTile.getLevel(); z <= maxTile.getLevel(); ++z) {
                    for (int x=minTile.getX(); x <= maxTile.getX(); ++x) {
                        for (int y=minTile.getY(); y <= maxTile.getY(); ++y) {
                            if (null == indices)
                                indices = new HashSet<>();
                                indices.add(new TileIndex(z, x, y));
                        }
                    }
                }
            }
        }
        return indices;
    }

	@Get
	public Representation getTile() throws ResourceException {

		try {
			// No alternate versions supported. But if we did:
			//String version = (String) getRequest().getAttributes().get("version");
			String id = (String) getRequest().getAttributes().get("id");
			String layer = (String) getRequest().getAttributes().get("layer");
			String levelDir = (String) getRequest().getAttributes().get("level");
			int zoomLevel = Integer.parseInt(levelDir);
			String xAttr = (String) getRequest().getAttributes().get("x");
			int x = Integer.parseInt(xAttr);
			String yAttr = (String) getRequest().getAttributes().get("y");
			int y = Integer.parseInt(yAttr);
            TileIndex index = new TileIndex(zoomLevel, x, y);


            Collection<TileIndex> tileSet = parseTileSetDescription(getRequest().getResourceRef().getQueryAsForm());
            if (null == tileSet) {
                tileSet = new HashSet<>();
            }
            tileSet.add(index);

			UUID uuid = null;
			if( !"default".equals(id) ){ // Special indicator - no ID.
				uuid = UUID.fromString(id);
			}

			String ext = (String) getRequest().getAttributes().get("ext");
			ExtensionType extType = ExtensionType.valueOf(ext.trim().toLowerCase());
			if (null == extType) {
				setStatus(Status.SERVER_ERROR_INTERNAL);
			} else if (ResponseType.Image.equals(extType.getResponseType())) {
				BufferedImage tile = _service.getTileImage(uuid, layer, index, tileSet);
				ImageOutputRepresentation imageRep = new ImageOutputRepresentation(extType.getMediaType(), tile);

				setStatus(Status.SUCCESS_CREATED);
				return imageRep;
			} else if (ResponseType.Tile.equals(extType.getResponseType())) {
				// We return an object including the tile index ("index") and 
				// the tile data ("data").
				//
				// The data should include index information, but it has to be 
				// there for tiles with no data too, so we can't count on it.
				JSONObject result = new JSONObject();
				JSONObject tileIndex = new JSONObject();
				tileIndex.put("level", zoomLevel);
				tileIndex.put("xIndex", x);
				tileIndex.put("yIndex", y);
				result.put("index", tileIndex);
				result.put("tile", _service.getTileObject(uuid, layer, index, tileSet));

				setStatus(Status.SUCCESS_CREATED);
				return new JsonRepresentation(result);
			} else {
				setStatus(Status.SERVER_ERROR_INTERNAL);
			}

			return null;
		} catch (Exception e){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
			                            "Unable to interpret requested tile from supplied URL.", e);
		}
	}
}
