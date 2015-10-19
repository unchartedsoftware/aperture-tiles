package com.oculusinfo.tile.rest.layer;

import com.google.inject.Inject;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by wmayo on 2015-10-08.
 */
public class KMLLayerResource extends ServerResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(LayerResource.class);

	private LayerService _service;

	@Inject
	public KMLLayerResource( LayerService service ) {
		_service = service;
	}

	@Get
	public Representation getKMLLayer() {
		try {
			String layerId = ( String ) getRequest().getAttributes().get( "layer" );
			int kmlId = Integer.parseInt( ( String) getRequest().getAttributes().get("kmlId"));
			String fileName = ( String ) getRequest().getAttributes().get("kmlFile");
			JSONObject layer = _service.getLayerJSON(layerId);
			JSONObject kmlDef = layer.getJSONObject("public").getJSONArray("kml").getJSONObject(kmlId);
			String path = new File(new File(kmlDef.getString("dir")), fileName).getPath();
			File kmlFile = new File(getClass().getClassLoader().getResource(path).toURI());

			return new FileRepresentation(kmlFile, MediaType.APPLICATION_KML);
		} catch (Exception e) {
			throw new ResourceException( Status.SERVER_ERROR_INTERNAL,
				"Unable to load desired kml file",
				e );
		}
	}
}
