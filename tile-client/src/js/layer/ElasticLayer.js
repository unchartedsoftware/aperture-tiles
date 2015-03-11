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

( function() {

  "use strict";

  var Layer = require('./Layer'),
    LayerUtil = require('./LayerUtil'),
    HtmlTileLayer = require('./HtmlTileLayer'),
    PubSub = require('../util/PubSub'),
    popover = require('../ui/popover');

  /**
   * Instantiate an ElasticLayer object.
   * @class ElasticLayer
   * @augments Layer
   * @classdesc A client rendered layer object. Uses ElasticSearch queries to draw
   * geospatial data on a map.
   *
   * @param {Object} spec - The specification object.
   * <pre>
   * {
     *     opacity  {float}    - The opacity of the layer. Default = 1.0
     *     enabled  {boolean}  - Whether the layer is visible or not. Default = true
     *     zIndex   {integer}  - The z index of the layer. Default = 1000
     *     renderer {Renderer} - The tile renderer object. (optional)
     *     html {String|Function|HTMLElement|jQuery} - The html for the tile. (optional)
     * }
   * </pre>
   */
  function ElasticLayer( spec ) {
    // call base constructor
    Layer.call( this, spec );
    // set reasonable defaults
    this.zIndex = ( spec.zIndex !== undefined ) ? spec.zIndex : 749;
    this.domain = "client";
    this.source = spec.source;
    if ( spec.renderer ) {
      this.renderer = spec.renderer;
    }
    if ( spec.html ) {
      this.html = spec.html;
    }
    var size = new OpenLayers.Size(21,25);
    var offset = new OpenLayers.Pixel(-(size.w/2), -size.h);
    this.icon = new OpenLayers.Icon('http://dev.openlayers.org/img/marker.png', size, offset);
    this.markers = [];
    this.iconData = {externalGraphic: 'http://dev.openlayers.org/img/marker.png',
      graphicHeight: 25, graphicWidth: 21,
      graphicXOffset: -12, graphicYOffset: -25};

    this.featureFunction = spec.featureRenderFunction;
    this.featureDestroyFunction = spec.featureDestroyFunction;
  }

  var parser = function parser(hits){
    return _.map(hits, function(hit){
      if(hit._source.locality[0]){
        var latlon = hit._source.locality[0].coordinates.split(",");
        return {lat:Number(latlon[0]),lon:Number(latlon[1])};
      }
      else{
        return {lat:null,lon:null};
      }
    })
  }

  ElasticLayer.prototype = Object.create( Layer.prototype );

  ElasticLayer.prototype.buildFeatureVectors = function(hits){
    var that = this;
    return _.map(this.source.data,function(data){return that.buildFeatureVector(data,that)});
  }

  /*
  * Take in an Elasticsearch hit and return an OpenLayers point */
  var parseHitIntoPoint = function parseHitIntoPoint(hit,that){

    var epsg4326 =  new OpenLayers.Projection("EPSG:4326");
    var projectTo = that.map.olMap.getProjectionObject();
    if(hit._source.locality[0]){
      var coordinate = hit._source.locality[0].coordinates.split(",");
      return new OpenLayers.Geometry.Point(Number(coordinate[1]),Number(coordinate[0])).transform(epsg4326, projectTo);
    }
    else{
      return null;
    }
  }

  ElasticLayer.prototype.buildFeatureVector = function(ad,that){
    var loc = parseHitIntoPoint(ad,that);
    return new OpenLayers.Feature.Vector(
      loc,
      ad,
      null
    );
  }

  /*
  * Transforms a list of latlon coordinates into OpenLayers lonlat objects
  * */
  ElasticLayer.prototype.buildMarkers = function(coordinates) {

    var marker;
    var that = this;
    this.markers = [];

    _.each(coordinates,function(coordinate){
      marker = new OpenLayers.Marker(new OpenLayers.LonLat(coordinate.lon,coordinate.lat)
        .transform(new OpenLayers.Projection("EPSG:4326"),that.map.olMap.getProjectionObject()),that.icon.clone());
      that.markers.push(marker);
    });

  }

  function createPopup(feature) {
    feature.popup = new OpenLayers.Popup("pop",
      feature.geometry.getBounds().getCenterLonLat(),
      null,
      '<div class="markerContent">'+feature.cluster[0].attributes._source.cluster.name+'</div>',
      null,
      true,
      function() { controls['selector'].unselectAll(); }
    );
    //feature.popup.closeOnMove = true;
    feature.layer.map.addPopup(feature.popup);
  }

  function createPopover(feature) {

    var pt = feature.geometry.clone().transform(feature.layer.map.projection,feature.layer.map.displayProjection)
    var coord =  new OpenLayers.LonLat(pt.x,pt.y).transform(feature.layer.map.displayProjection, feature.layer.map.projection);
    feature.popup = new OpenLayers.Popup.Popover(
      "popup-" + feature.id,
      coord,
      "content",
      "title",
      null,
      feature.cluster
    )
    feature.layer.map.addPopup(feature.popup, true);
  }

  function destroyPopup(feature) {
    feature.popup.destroy();
    feature.popup = null;
  }

  /**
   * Activates the layer object. This should never be called manually.
   * @memberof ElasticLayer
   * @private
   */
  ElasticLayer.prototype.activate = function() {

    var that = this;
    // add the new layer
    //this.olLayer = new OpenLayers.Layer.Markers("Markers");
    this.olLayer = new OpenLayers.Layer.Vector("Overlay",
      {strategies:[ new OpenLayers.Strategy.Cluster({distance: 20})],
      });

    this.map.olMap.addLayer( this.olLayer );

    this.olLayer.addFeatures(this.buildFeatureVectors(this.source.data));

    //this.buildMarkers(this.source.coordinates);
    //
    //_.each(this.markers,function(marker){
    //  that.olLayer.addMarker(marker);
    //})


    var controls = {
      selector: new OpenLayers.Control.SelectFeature( this.olLayer,
        { onSelect: createPopover,
          onUnselect: destroyPopup,
          clickout:true
        })
    };

    this.map.olMap.addControl(controls['selector']);

    controls['selector'].activate();


    this.setZIndex( this.zIndex );
    this.setOpacity( this.opacity );
    this.setEnabled( this.enabled );
    this.setTheme( this.map.getTheme() );

    if ( this.renderer ) {
      this.renderer.meta = this.source.meta.meta;
      this.renderer.map = this.map;
      this.renderer.parent = this;
    }
  };

  /**
   * Dectivates the layer object. This should never be called manually.
   * @memberof ClientLayer
   * @private
   */
  ElasticLayer.prototype.deactivate = function() {
    if ( this.olLayer ) {
      this.map.olMap.removeLayer( this.olLayer );
      this.olLayer.destroy();
    }
  };

  /**
   * Updates the theme associated with the layer.
   * @memberof ClientLayer
   *
   * @param {String} theme - The theme identifier string.
   */
  ElasticLayer.prototype.setTheme = function( theme ) {
    this.theme = theme;
  };

  /**
   * Get the current theme for the layer.
   * @memberof ClientLayer
   *
   * @returns {String} The theme identifier string.
   */
  ElasticLayer.prototype.getTheme = function() {
    return this.theme;
  };

  /**
   * Set the z index of the layer.
   * @memberof ClientLayer
   *
   * @param {integer} zIndex - The new z-order value of the layer, where 0 is front.
   */
  ElasticLayer.prototype.setZIndex = function ( zIndex ) {
    // we by-pass the OpenLayers.Map.setLayerIndex() method and manually
    // set the z-index of the layer dev. setLayerIndex sets a relative
    // index based on current map layers, which then sets a z-index. This
    // caused issues with async layer loading.
    this.zIndex = zIndex;
    $( this.olLayer.div ).css( 'z-index', zIndex );
    PubSub.publish( this.getChannel(), { field: 'zIndex', value: zIndex });
  };

  /**
   * Get the layers zIndex.
   * @memberof ClientLayer
   *
   * @returns {integer} The zIndex for the layer.
   */
  ElasticLayer.prototype.getZIndex = function () {
    return this.zIndex;
  };

  module.exports = ElasticLayer;
}());
