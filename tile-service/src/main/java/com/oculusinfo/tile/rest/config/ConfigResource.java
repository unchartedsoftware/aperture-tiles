package com.oculusinfo.tile.rest.config;

import com.google.inject.Inject;
import com.oculusinfo.tile.rest.layer.LayerResource;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ConfigResource extends ServerResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(LayerResource.class);

    private ConfigService _service;

    @Inject
    public ConfigResource( ConfigService service ) {
        _service = service;
    }

    @Get
    public Representation getConfig() {
        try {

            String name = ( String ) getRequest().getAttributes().get( "name" );
            String path = ConfigResource.class.getResource("/config").toURI().getPath();
            File file = new File(path, name);
            String result = _service.replaceProperties(file);
            setStatus(Status.SUCCESS_OK);
            return new JsonRepresentation( result );

        } catch ( Exception e ) {
            throw new ResourceException( Status.SERVER_ERROR_INTERNAL,
                    "Unable to get configuration",
                    e );
        }
    }
}
