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

import com.google.inject.Inject;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.tile.rendering.LayerConfiguration;
import com.oculusinfo.tile.rest.ImageOutputRepresentation;
import com.oculusinfo.tile.rest.QueryParamDecoder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import java.awt.image.BufferedImage;
import java.util.*;

public class TileResource extends ServerResource {

	public enum ResponseType {
		Image,
		Tile
	}
	public enum ExtensionType {
		png(ResponseType.Image, MediaType.IMAGE_PNG),
		jpg(ResponseType.Image, MediaType.IMAGE_JPEG),
		jpeg(ResponseType.Image, MediaType.IMAGE_JPEG),
		json(ResponseType.Tile, MediaType.APPLICATION_JSON);

		private ResponseType _responseType;
		private MediaType _mediaType;
		ExtensionType (ResponseType responseType, MediaType mediaType) {
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

    /**
     * Tilesets defined by tile indices, or tile bounds may be specified as request parameters.
     * @param query request parameter JSONObject.
     * @return Set<TileIndex> set of tile indices specified by the set or bound parameters. If
     * none are specified, return empty set.
     */
	private Collection<TileIndex>  parseTileSetDescription( JSONObject query ) {

        Set<TileIndex> indices = new HashSet<>();
        try {
            if ( null != query ) {
                // Check for specifically requested tiles
                if ( query.has("tileset") ) {
                    JSONArray tileSets = query.getJSONArray( "tileset" );
                    for ( int i = 0; i < tileSets.length(); i++ ) {
                        String[] tileDescriptions = tileSets.getString( i ).split( "\\|" );
                        for ( String tileDescription : tileDescriptions ) {
                            TileIndex index = TileIndex.fromString( tileDescription );
                            if ( null != index ) {
                                indices.add( index );
                            }
                        }
                    }
                }

                // Check for simple bounds
                Integer minX = query.optInt( "minX" );
                Integer maxX = query.optInt( "maxX" );
                Integer minY = query.optInt( "minY" );
                Integer maxY = query.optInt( "maxY" );
                Integer minZ = query.optInt( "minZ" );
                Integer maxZ = query.optInt( "maxZ" );

                TileIndex minTile = TileIndex.fromString( query.optString( "mintile" ) );
                TileIndex maxTile = TileIndex.fromString( query.optString( "maxtile" ) );

                if (null == minTile && null != minX && null != minY && null != minZ) {
                     minTile = new TileIndex(minZ, minX, minY);
                }
                if (null == maxTile && null != maxX && null != maxY && null != maxZ) {
                     maxTile = new TileIndex(maxZ, maxX, maxY);
                }
                if (null != minTile && null != maxTile) {
                    for ( int z = minTile.getLevel(); z <= maxTile.getLevel(); ++z ) {
                        for ( int x = minTile.getX(); x <= maxTile.getX(); ++x ) {
                            for ( int y = minTile.getY(); y <= maxTile.getY(); ++y ) {
                                if ( null == indices )
                                    indices = new HashSet<>();
                                indices.add( new TileIndex( z, x, y ) );
                            }
                        }
                    }
                }
            }

        } catch ( Exception e ) {
            e.printStackTrace();
        }
		return indices;
	}


    /**
     * GET request. Returns a tile from a layer at specified level, xIndex, yIndex. Currently
     * supports png/jpg image formats and JSON data tiles.
     */
	@Get
	public Representation getTile() throws ResourceException {

		try {
			// No alternate versions supported. But if we did:
			String version = (String) getRequest().getAttributes().get("version");
            if ( version == null ) {
                version = LayerConfiguration.DEFAULT_VERSION;
            }
			String layer = (String) getRequest().getAttributes().get("layer");
			String levelDir = (String) getRequest().getAttributes().get("level");
			int zoomLevel = Integer.parseInt(levelDir);
			String xAttr = (String) getRequest().getAttributes().get("x");
			int x = Integer.parseInt(xAttr);
			String yAttr = (String) getRequest().getAttributes().get("y");
			int y = Integer.parseInt(yAttr);
			TileIndex index = new TileIndex(zoomLevel, x, y);
            String ext = (String) getRequest().getAttributes().get("ext");
			ExtensionType extType = ExtensionType.valueOf(ext.trim().toLowerCase());

            // decode and build JSONObject from request parameters
            JSONObject decodedQueryParams = QueryParamDecoder.decode( getRequest().getResourceRef().getQuery() );

            // parse parameters for tile sets or tile bounds
			Collection<TileIndex> tileSet = parseTileSetDescription( decodedQueryParams );
			tileSet.add(index);

			if (null == extType) {
				setStatus(Status.SERVER_ERROR_INTERNAL);
			} else if (ResponseType.Image.equals(extType.getResponseType())) {

				BufferedImage tile = _service.getTileImage( layer, index, tileSet, decodedQueryParams );
				ImageOutputRepresentation imageRep = new ImageOutputRepresentation(extType.getMediaType(), tile);
				setStatus(Status.SUCCESS_OK);
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
                result.put("version", version);
				result.put("tile", _service.getTileObject( layer, index, tileSet, decodedQueryParams ));
				setStatus(Status.SUCCESS_OK);
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
