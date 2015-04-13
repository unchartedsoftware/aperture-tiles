/* Copyright (c) 2013 by Jean-François VIAL <http://about.me/Jeff_>

 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 SHALL COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @requires OpenLayers/Popup/Framed.js
 * @requires OpenLayers/Util.js
 * @requires OpenLayers/BaseTypes/Bounds.js
 * @requires OpenLayers/BaseTypes/Pixel.js
 * @requires OpenLayers/BaseTypes/Size.js
 */

/**
 * Class: OpenLayers.Popup.Popover
 *
 * Allow to use, in combination with Twitter Bootstrap and jQuery, a Bootstrap
 * Popover as an OpenLayers Popup
 *
 * Inherits from:
 *  - <OpenLayers.Popup.Framed>
 */
( function() {
  "use strict";

  OpenLayers.Popup.Popover =
    OpenLayers.Class(OpenLayers.Popup.Anchored, {
        autoSize: true,
        panMapIfOutOfView: true,
        fixedRelativePosition: false,
        title:'',
        popupHTML: '',
        // delta is about to adjust the popup position vs the marker
        delta:  {	// }
          x: 13,	// } ← TWEAK THESE TO FEAT YOUR NEEDS !
          y: -5	// }
        },
        // the popup basic dimensions
        dimensions: {	// }
          w: 300,		// } ← TWEAK THESE TO FEAT YOUR NEEDS !
          h: 200		// }
        },
        closeCallback: null,
        /*
         In order to display things right, you will need to add (and adapt) this piece of css :
         .popover-title span {
         display:block;
         float: left;
         width: 236px;
         }
         */
        initialize:function(id, lonlat, contentHTML, title, closeBoxCallback, data) {
          var args = [
            id,
            lonlat,
            new OpenLayers.Size(0, 0),
            contentHTML,
            {
              size: new OpenLayers.Size(0, 0),
              offset: new OpenLayers.Pixel(-(this.dimensions.w / 2) + this.delta.x, this.delta.y)
            },
            false,
            closeBoxCallback
          ];
          OpenLayers.Popup.Anchored.prototype.initialize.apply(this, args);
          this.title = title;
          this.contentDiv.className = this.contentDisplayClass;
          this.closeCallback = closeBoxCallback || function() {};
          this.data = data;
        },
        formatData: function(){
          this.setTitle();
          this.setContentHTML();
        },
        setTitle: function(){
           this.title = "Number of ads: "  + this.data.length;
        },
        setContentHTML: function() {
          this.contentHTML = "";

          var clustercounter = {};

          for(var i = 0; i<this.data.length; i++){
            var cluster = this.data[i].data.key;
            if( typeof clustercounter[cluster.id] !== "undefined" && clustercounter[cluster.id] !== null){
              clustercounter[cluster.id].count += 1;
            }
            else {
              clustercounter[cluster.id] = {};
              clustercounter[cluster.id].count = 0;
              clustercounter[cluster.id].name = cluster.name;
            }
          }

          var that = this;
          var buildHTML = function(clust, key){
            that.contentHTML = that.contentHTML.concat("<p>" + "Cluster ID: " + key + "</p>" + "<p>" + "Cluster Name: " + clust.name + "</p>"+ "<p>" + "Cluster Ads: " + clust.count + "</p>");
          };
          _.each(clustercounter,buildHTML);


        },
        draw: function(px) {
          this.formatData();
          this.map.paddingForPopups.bottom = 100;
          if (px === null) {
            if ((this.lonlat !== null) && (this.map !== null)) {
              px = this.map.getLayerPxFromLonLat(this.lonlat);
            }
          }

          this.popupHTML =
            '<div id="'+this.id+'" class="popover fade top in" style="overflow-y:scroll;position:absolute;z-index:10000; top: '+(px.y - this.dimensions.h + this.delta.y)+'px; left: '+(px.x - Math.ceil(this.dimensions.w/2) + this.delta.x)+'px; display: block;width:'+ this.dimensions.w+'px;height:'+ this.dimensions.h+'px;">'
          +'<div class="arrow"></div>'
          +'<h3 class="popover-title"><button class="close">&times;</button><span>'+this.title+'</span><div class="clearfix"></div></h3>'
          +'<div class="popover-content">'
          + this.contentHTML + '</div></div>';

          //var popup = this;
          //$(this.popupHTML).ready(function() {
          //  $(this).find(".close").on("click", function() {
          //    popup.hide();
          //    popup.closeCallback();
          //  });
          //});

          this.size = this.getRenderedDimensions();
          var p = $(this.popupHTML);
          this.div = p[0];

          if (this.panMapIfOutOfView) {
            this.panIntoView();
          }
          return this.div;
        },
      destroy: function(){
        this.id = null;
        this.lonlat = null;
        this.size = null;
        this.contentHTML = null;

        this.backgroundColor = null;
        this.opacity = null;
        this.border = null;

        if (this.closeOnMove && this.map) {
          this.map.events.unregister("movestart", this, this.hide);
        }

        this.events.destroy();
        this.events = null;

        if (this.map !== null) {
          this.map.removePopup(this);
        }

        this.map = null;
        this.div = null;

        this.autoSize = null;
        this.minSize = null;
        this.maxSize = null;
        this.padding = null;
        this.panMapIfOutOfView = null;
      },
      getRenderedDimensions: function() {
          var elm = $($(this.popupHTML).css({top: -999999, left: -99999}).appendTo('body')[0]);
          var w = elm.outerWidth();
          var h = elm.outerHeight();
          elm.remove();
          return new OpenLayers.Size(w, h);
      },

        calculateNewPx:function(px) {
          var newPx = OpenLayers.Popup.Anchored.prototype.calculateNewPx.apply(
            this, arguments
          );
          return newPx;
        },

        calculateRelativePosition: function() {
          return "tr";
        },

        CLASS_NAME: "OpenLayers.Popup.Popover"
    });
}());
