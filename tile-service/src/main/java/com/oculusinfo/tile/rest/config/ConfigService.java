package com.oculusinfo.tile.rest.config;

import java.io.File;
import java.net.URISyntaxException;

/**
 * The ConfigService is responsible for replacing property keys in templated configuration files with values from a properties file.
 *
 * @author danielabar
 */
public interface ConfigService {

    /**
     * Given a configFile (usually JSON format but could be any text file) such as:
     * [
     *      {
     *          "id": "some-id",
     *          "private": {
     *              "data": {
     *                  "pyramidio": {
     *                      "type": "hbase",
     *                      "hbase.zookeeper.quorum": "${hbase.zookeeper.quorum}",
     *                      "hbase.zookeeper.port": "${hbase.zookeeper.port}"
     *                  }
     *              }
     *          }
     *      }
     * ]
     *
     * And environment variable pointing to a properties file TILE_CONFIG_PROPERTIES=/path/to/app.properties
     *
     * And app.properties
     *  hbase.zookeeper.quorum=some.host
     *  hbase.zookeeper.port=55555
     *
     *  Then this method will return
     *  * [
     *      {
     *          "id": "some-id",
     *          "private": {
     *              "data": {
     *                  "pyramidio": {
     *                      "type": "hbase",
     *                      "hbase.zookeeper.quorum": "some.host",
     *                      "hbase.zookeeper.port": "55555"
     *                  }
     *              }
     *          }
     *      }
     * ]
     *
     * If TILE_CONFIG_PROPERTIES environment variable is not set, then this method will attempt to
     * read a file named default-config.properties from the classpath and use that for the replacements.
     *
     * If default-config.properties is not found, then this method will return the String contents
     * of configFile exactly as is, i.e. with no replacements.
     *
     * @param configFile The configuration file containing tokens to be replaced
     * @return String contents of configFile with tokens replaced by properties
     * @throws ConfigException If unable to read configuration or properties file
     */
    String replaceProperties(File configFile) throws ConfigException;

    /**
     * Find a configuration file in resources/config if it exists.
     * @param name Name of the file to check for, example views.json
     * @return File if it exists, null otherwise.
     * @throws URISyntaxException
     */
    File findResourceConfig(String name) throws URISyntaxException;

}
