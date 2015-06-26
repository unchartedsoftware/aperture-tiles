package com.oculusinfo.tile.rest.config;

/**
 * An exception during layer configuration
 *
 * @author danielabar
 */
public class ConfigException extends Exception {

    public ConfigException () {
        super();
    }

    public ConfigException (String message) {
        super(message);
    }

    public ConfigException (Throwable cause) {
        super(cause);
    }

    public ConfigException (String message, Throwable cause) {
        super(message, cause);
    }
}
