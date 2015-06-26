package com.oculusinfo.tile.rest.config;

import com.google.inject.Inject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import java.io.File;

public class ConfigResource extends ServerResource {

    private ConfigService _service;

    @Inject
    public ConfigResource( ConfigService service ) {
        _service = service;
    }

    @Get
    public Representation getConfig() {
        try {
            String result = "{}";
            String name = ( String ) getRequest().getAttributes().get( "name" );
            File matchingFile = _service.findResourceConfig(name);
            if (matchingFile != null) {
                result = _service.replaceProperties(matchingFile);
            }
            setStatus(Status.SUCCESS_OK);
            return new JsonRepresentation( result );

        } catch ( Exception e ) {
            throw new ResourceException( Status.SERVER_ERROR_INTERNAL,
                    "Unable to get configuration",
                    e );
        }
    }
}
