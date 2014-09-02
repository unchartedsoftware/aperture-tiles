/*
 * Copyright (c) 2013 Oculus Info Inc.
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


define(function (require) {
    "use strict";



    var HtmlRenderer = require('./HtmlRenderer'),
        HtmlNodeLayer = require('../../HtmlNodeLayer'),
        HtmlLayer = require('../../HtmlLayer'),
        Util = require('../../../util/Util'),
        idNumber = 0,
        GenericHtmlRenderer;



    GenericHtmlRenderer = HtmlRenderer.extend({
        ClassName: "GenericHtmlRenderer",

        init: function( map, spec ) {

            this._super( map, this.parseInputSpec( spec ) );
            this.id = idNumber++;
            this.createStyles();    // inject css directly into DOM
            this.createNodeLayer(); // instantiate the node layer data object
            this.createHtmlLayer(); // instantiate the html visualization layer
        },


        parseInputSpec: function() {
            console.error( this.ClassName+'::parseInputSpec() has not been overloaded, no configurable css has been set.');
            return false;
        },


        createStyles: function() {
            console.error( this.ClassName+'::createStyles() has not been overloaded, no configurable css has been set.');
            return false;
        },


        createHtml: function( data ) {
            console.error( this.ClassName+'::createHtml() has not been overloaded, no html will be provided to layer');
            return "";
        },


        getSelectableElement: function() {
            console.error( this.ClassName+'::getSelectableElement() has not been overloaded, returning ""');
            return "";
        },


        registerLayer: function( layerState ) {

            var that = this;
            this._super( layerState );
            this.layerState.addListener( function( fieldName ) {

                switch (fieldName) {

                    case "click":
                        if ( layerState.has('click') ) {
                            // add click state classes
                            that.addClickStateClassesGlobal();
                        } else {
                            // remove click state classes
                            that.removeClickStateClassesGlobal();
                            //that.details.destroy();
                        }
                        break;
                }
            });

        },


        createNodeLayer: function() {

            /*
                 Instantiate the html node layer. This holds the tile data as it comes in from the tile service. Here
                 we set the x and y coordinate mappings that are used to position the individual nodes on the map. In this
                 example, the data is geospatial and is under the keys 'latitude' and 'longitude'. The idKey
                 attribute is used as a unique identification key for internal managing of the data. In this case, it is
                 the tilekey.
             */
            this.nodeLayer = new HtmlNodeLayer({
                map: this.map,
                xAttr: 'longitude',
                yAttr: 'latitude',
                idKey: 'tilekey'
            });
        },


        createHtmlLayer : function() {

            /*
                Here we create and attach an individual html layer to the html node layer. For every individual node
                of data in the node layer, the html function will be executed with the 'this' context that of the node.
             */
            var that = this;
            this.nodeLayer.addLayer( new HtmlLayer({
                html: function() {
                    var $tile = $('<div class="aperture-tile aperture-tile-'+this.tilekey+'"></div>'),
                        $content = that.createHtml( this );
                    if ( that.spec.summary ) {
                        $content = $content.add('<div class="count-summary"></div>');
                    }
                    return $tile.append( $content );
                }
            }));
        },


        addClickStateClassesLocal: function( $elem, value, tilekey ) {

            if ( !this.layerState.has('click') ) {
                return;
            }

            var idKey = this.spec.idKey,
                click = this.layerState.get('click'),
                selectedValue = click[idKey],
                entryValue = value[idKey],
                clickedTilekey = click.tilekey;

            if ( entryValue === selectedValue ) {
                if ( tilekey === clickedTilekey ) {
                    $elem.addClass('clicked-primary');
                } else {
                    $elem.addClass('clicked');
                }
            } else {
                 $elem.addClass('greyed');
            }
        },


        addClickStateClassesGlobal: function() {

            var SELECTABLE_ELEMENT_CLASS = this.getSelectableElement(),
                click = this.layerState.get('click'),
                selectedValue = click[this.spec.idKey],
                parsedValues = click.tilekey.split(','),
                level = parseInt( parsedValues[0], 10 ),
                xIndex = parseInt( parsedValues[1], 10 ),
                yIndex = parseInt( parsedValues[2], 10 ),
                escapedTilekey = level + "\\," + xIndex +"\\," + yIndex,
                $elements = this.map.getRootElement().find('.aperture-tile').find('.'+SELECTABLE_ELEMENT_CLASS ),
                $primaryElement = this.map.getRootElement().find( '.aperture-tile-'+escapedTilekey).find('.'+SELECTABLE_ELEMENT_CLASS );

            $elements.filter( function() {
                return $(this).text() !== selectedValue;
            }).addClass('greyed').removeClass('clicked-secondary clicked-primary');

            $elements.filter( function() {
                return $(this).text() === selectedValue;
            }).removeClass('greyed clicked-primary').addClass('clicked-secondary');

            $primaryElement.filter( function() {
                return $(this).text() === selectedValue;
            }).removeClass('greyed clicked-secondary').addClass('clicked-primary');
        },


        removeClickStateClassesGlobal: function() {
            var SELECTABLE_ELEMENT_CLASS = this.getSelectableElement();
            this.map.getRootElement().find('.'+SELECTABLE_ELEMENT_CLASS).removeClass('greyed clicked clicked-primary');
        },


        clickOn: function( value ) {

            var idKey = this.spec.idKey,
                click = {
                    value : value,
                    tilekey : this.layerState.get('tileFocus')
                };
            click[idKey] = value[idKey];
            this.layerState.set('click', click );
        },


        clickOff: function() {
            this.layerState.set('click', null);
        },


        setMouseEventCallbacks: function( $element, data, value ) {

            var that = this,
                spec = this.spec;

            function createSummariesCallbacks() {
                var j,
                    summary,
                    html = '<div>';
                for ( j=0; j<spec.summary.length; j++ ) {
                    summary = spec.summary[j];
                    html += '<font color="'+summary.color+'">' + summary.prefix + value[ summary.countKey ] + '</font><br>';
                }
                html += '</div>';

                $element.mouseover( function( event ) {
                    $element.closest('.aperture-tile').find(".count-summary").html( html );
                });
                $element.mouseout( function( event ) {
                    $element.closest('.aperture-tile').find(".count-summary").html("");
                });
            }

            if ( spec.summary ) {
                createSummariesCallbacks();
            }

            Util.dragSensitiveClick( $element, function( event ) {
                // process click
                that.clickOn( value );
                // create details here so that only 1 is created
                //that.createDetailsOnDemand( data );
                // prevent event from going further
                event.stopPropagation();
            });

        },


        centreForDetails: function( data ) {
            var map = this.map,
                viewportPixel = map.getViewportPixelFromCoord( data.longitude, data.latitude ),
                panCoord = map.getCoordFromViewportPixel( viewportPixel.x + map.getTileSize(),
                                                          viewportPixel.y + map.getTileSize() );
            map.panToCoord( panCoord.x, panCoord.y );
        },


        createDetailsOnDemand: function( data ) {

            var clickState = this.layerState.get('click'),
                map = this.map,
                value = clickState.value,
                tilePos = map.getMapPixelFromCoord( data.longitude, data.latitude ),
                detailsPos = {
                    x: tilePos.x + 256,
                    y: map.getMapHeight() - tilePos.y
                },
                $details;

            $details = this.details.create( detailsPos, value, $.proxy( this.clickOff, this ) );
            Util.enableEventPropagation( $details, ['onmouseup'] );
            map.getRootElement().append( $details );

            this.centreForDetails( data );
        },


        generateBlendedClass: function( str, value, subspec, subIndex ) {
        	var i,
        		blend, num = subspec.blend.length,
        		val, sum = 0, result = str;

        	for ( i=0; i<num; i++ ) {
        		blend = subspec.blend[i];
        		if ( subIndex ) {
        		    val = ( value[blend.countKey][subIndex] / value[subspec.countKey][subIndex] ) * 100;
        		} else {
        		    val = ( value[blend.countKey] / value[subspec.countKey] ) * 100;
        		}
                val = ( i === num - 1 ) ? 100 - sum : Math.round( val / 10 ) * 10;
                result += "-" + val;
                sum += val;
            }
            return result;
        },


        generateBlendedAttributes: function( blend ) {

            var NUM_INCS = 10,
                NUM_ENTRIES = blend.length,
                permutations,
                colors = [],
                hoverColors = [],
                result = [], i;

            function toString( n ) {
                return n +"0";
            }

            function permute( obj, sum, depth ) {
                var i, m = NUM_INCS-sum;
                if ( depth === NUM_ENTRIES-1 ) {
                    return toString( NUM_INCS - sum );
                }
                for ( i=0; i<=m; i++ ) {
                    obj[toString(i)] = permute( {}, sum + i, depth+1 );
                }
                return obj;
            }

            function getPermutationLists( list, obj, accum ) {
                var key, accumCopy;
                if ( typeof obj !== "object" ) {
                    accum.push( parseInt( obj, 10 ) / 100 );
                    list.push( accum );
                } else {
                    for ( key in obj ) {
                        if ( obj.hasOwnProperty( key ) ) {
                            accumCopy = accum.slice(0);
                            accumCopy.push( parseInt( key, 10 ) / 100 );
                            getPermutationLists( list, obj[key], accumCopy );
                        }
                    }
                }
            }

            function getPermutationString( perm ) {
                var i, str = "";
                for ( i=0; i<perm.length; i++ ) {
                    str += "-" + Math.round( perm[i]*100 );
                }
                return str;
            }

            function getPermutations() {
                var permutations = permute({}, 0, 0),
                    strList = [];
                getPermutationLists( strList, permutations, [] );
                return strList;
            }

            permutations = getPermutations();

            for ( i=0; i<blend.length; i++ ) {
                colors.push( blend[i].color );
                hoverColors.push( blend[i].hoverColor );
            }
            for ( i=0; i<permutations.length; i++ ) {
                result.push({
                    color: Util.blendHex( colors, permutations[i] ),
                    hoverColor : Util.blendHex( hoverColors, permutations[i] ),
                    suffix : getPermutationString( permutations[i] )
                });
            }
            return result;
        },

        generateBlendedCss: function( blend, elementClass, attribute ) {

            var blends = this.generateBlendedAttributes( blend ),
                parentClass = this.getSelectableElement(),
                className,
                css = "",
                i;
            for ( i=0; i<blends.length; i++ ) {
                blend = blends[i];
                className = elementClass+blend.suffix+'-'+this.id;
                css += '.'+className+' {'+attribute+':'+blend.color+';}' +
                       '.'+parentClass+':hover .'+className+' {'+attribute+':'+blend.hoverColor+';}' +
                       '.greyed .'+className+'{'+attribute+':'+Util.hexGreyscale( blend.color )+';}' +
                       '.clicked-secondary .'+className+' {'+attribute+':'+blend.color+';}' +
                       '.clicked-primary .'+className+' {'+attribute+':'+blend.hoverColor+';}';
            }
            return css;
        },

        generateCss: function( subSpec, elementClass, attribute ) {

            var parentClass = this.getSelectableElement(),
                className,
                css = "",
                color,
                hoverColor,
                i;
            for (i=0; i<subSpec.length; i++) {
                color = subSpec[i].color;
                hoverColor = subSpec[i].hoverColor;
                className = elementClass+i+'-'+this.id;
                css += '.'+className+' {'+attribute+':'+color+';}';
                css += '.'+parentClass+':hover .'+className+' {'+attribute+':'+hoverColor+';}';
                css += '.greyed .'+className+' {'+attribute+':'+ Util.hexGreyscale( color ) +';}';
                css += '.clicked-secondary .'+className+' {'+attribute+':'+ color +';}';
                css += '.clicked-primary .'+className+' {'+attribute+':'+ hoverColor +';}';
            }
            return css;
        }
    });

    return GenericHtmlRenderer;

});