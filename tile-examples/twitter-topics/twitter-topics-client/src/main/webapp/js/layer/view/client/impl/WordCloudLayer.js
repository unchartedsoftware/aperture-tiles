


define(function (require) {
    "use strict";

    var MAX_CACHE_ENTRIES = 1024,
		cloudCache = {
			entries : {},
			lruStack : []
		},
    	WordCloudLayer;


	WordCloudLayer = aperture.Layer.extend( 'aperture.WordCloudLayer',
	/** @lends aperture.WordCloudLayer# */
	{
		/**
		 * @augments aperture.Layer
		 * @requires a vector canvas
		 * @class Creates a layer displaying text at specific locations.

		 * @mapping {Number=1} label-count
		 *   The number of labels to be drawn.

		 * @mapping {Number=1} label-visible
		 *   The visibility of a label.

		 * @mapping {String} text
		 *   The text to be displayed.

		 * @mapping {String='black'} fill
		 *   The color of a label.

		 * @mapping {Number=0} x
		 *   The horizontal position at which the label will be anchored.

		 * @mapping {Number=0} y
		 *   The vertical position at which the label will be anchored.

		 * @mapping {Number=0} offset-x
		 *   The offset along the x-axis by which to shift the text after it has been positioned at (x,y).

		 * @mapping {Number=0} offset-y
		 *   The offset along the y-axis by which to shift the text after it has been positioned at (x,y).

		 * @mapping {Number=1.0} opacity
		 *   How opaque the label will be in the range [0,1].

		 * @mapping {'middle'|'start'|'end'} text-anchor
		 *   How the label is aligned with respect to its x position.

		 * @mapping {'middle'|'top'|'bottom'} text-anchor-y
		 *   How the label is aligned with respect to its y position.

		 * @mapping {'horizontal'|'vertical'| Number} orientation
		 *   The orientation of the text as a counter-clockwise angle of rotation, or constants 'vertical'
		 *   or 'horizontal'.

		 * @mapping {String='Arial'} font-family
		 *   One or more comma separated named font families,
		 *   starting with the ideal font to be used if present.

		 * @mapping {Number=10} font-size
		 *   The font size (in pixels).

		 * @mapping {String='normal'} font-weight
		 *   The font weight as a valid CSS value.

		 * @mapping {String='none'} font-outline
		 *   The colour of the outline drawn around each character of text. 
		 *   
		 * @mapping {Number=3} font-outline-width
		 *   The width of the outline drawn around each character of text, if font-outline is not none.
		 *   
		 * @mapping {Number=1.0} font-outline-opacity
		 *   How opaque the font outline will be.
		 *   
		 * @constructs
		 * @factoryMade
		 */
		init : function(spec, mappings){
			aperture.Layer.prototype.init.call(this, spec, mappings);
		},

		canvasType : aperture.canvas.VECTOR_CANVAS,

		getFromCache :  function(hash) {
			if (cloudCache.entries[hash] === undefined) {
				return null;
			} else {
				// add 
				cloudCache.lruStack.splice( cloudCache.lruStack.indexOf(hash), 1);
				cloudCache.lruStack.push(hash);
				return cloudCache.entries[hash];
			}
		},

		addToCache : function( cloud, hash ) {

			if (cloudCache.entries[hash] === undefined) {

				while ( cloudCache.lruStack.length >= MAX_CACHE_ENTRIES) {
					// pop from front of stack (LRU), and delete entry
					console.log("pop");
					delete cloudCache.entries[cloudCache.lruStack.shift()];
				}

				// add new entry to LRU stack and cache
				cloudCache.lruStack.push(hash);
				cloudCache.entries[hash] = cloud;
			}
		},


		spiralPosition : function( pos ) {

			var pi2 = 2 * Math.PI,
				circ = pi2 * pos.radius,
				inc = ( pos.arcLength > circ/10) ? circ/10 : pos.arcLength,
				da = inc / pos.radius,
				nt = (pos.t+da);

			if (nt > pi2) {
				nt = nt % pi2;
				pos.radius = pos.radius + pos.radiusInc;
			}

			pos.t = nt;			
			pos.x = pos.radius * Math.cos(nt);		
			pos.y = pos.radius * Math.sin(nt);
			return pos; 
        },


        intersectsAWord : function( p, d, c, bb ) {
        	var w, box = {
        		x:p.x,
        		y:p.y,
        		height:d.height,
        		width:d.width
        	};

        	function boxTest( a, b ) {
			  	return (Math.abs(a.x - b.x) * 2 < (a.width + b.width)) &&
			           (Math.abs(a.y - b.y) * 2 < (a.height + b.height));
			}

			function overlapTest( a, b ) {
				return ( a.x + a.width/2 > b.x+b.width/2 ||
					     a.x - a.width/2 < b.x-b.width/2 ||
					     a.y + a.height/2 > b.y+b.height/2 ||
					     a.y - a.height/2 < b.y-b.height/2 );
			}

        	for (w=0; w<c.length; w++) {
        		if ( c[w] !== null && boxTest( box, c[w] ) ) {
        			return { 
        				result : true,
        			    type: 'word' 
        			};
        		}
        	}

        	// make sure it doesn't intersect the border;
        	if ( overlapTest( box, bb ) ) {
        		return { 
        			result : true,
        		 	type: 'border' 
        		};
        	}

        	return { 
        		result : false 
        	};
        },


        getFontSize : function( g, str, font ) {
			
			font.x = 0;
			font.y = 0;
			font.opacity = 0;
			font.str = str;
			var temp = g.text(0, 0, str);
			g.update(temp, font);
			var bb = temp.getBBox();
			g.remove(temp);
			return {
				width: bb.width,
				height: bb.height
			};
		},


        generateCloud : function( g, words, frequencies, itemCount, font, bb ) {

        	function strHash(str) {
				var hash = 0, i, chr, len;
			 	if (str.length == 0) return hash;
			 	for (i = 0, len = str.length; i < len; i++) {
				    chr   = str.charCodeAt(i);
				    hash  = ((hash << 5) - hash) + chr;
				    hash |= 0; // Convert to 32bit integer
				}
				return hash;
			}

	        // Assemble into ordered list
	        var wordCounts = [],
	            hash = "",
	            cloud, sum, i, j,
	            word, count, dim,
                fontSize, pos, scale,
                fontRange, borderCollisions = 0,
                intersection;;


			for (j=0; j<itemCount; j++) {
	        	wordCounts.push({
	        		word: words[j],
	        		count: frequencies[j]
	        	});
	        	hash += words[j] +"-"+ frequencies[j] +":";
	        }
	        hash += font.maxFontSize +":"+ font.minFontSize;

	        hash = strHash(hash);
	        // see if we already created the cloud
	        cloud = this.getFromCache(hash);
	        if ( cloud !== null ) {
	        	return cloud;
	        }
	        cloud = [];

	        // Get mean				
	        sum = 0;
	        for (j=0; j<itemCount; j++) {
	        	sum += frequencies[j];
	        }

	        wordCounts.sort(function(a, b){return b.count-a.count});

	        // Assemble word cloud
			scale = Math.log(sum),
			fontRange = (font.maxFontSize - font.minFontSize);

	        // assemble words in cloud
			for (j=0; j<itemCount; j++) {	

				word = wordCounts[j].word;
	        	count = wordCounts[j].count;
	        	
                fontSize = ( (count/sum) * fontRange * scale) + (font.minFontSize * (count / sum));
                fontSize = Math.min( Math.max( fontSize, font.minFontSize), font.maxFontSize );

	        	font['font-size'] = fontSize;

	        	dim = this.getFontSize( g, word, font );
	        	// add horizontal buffer
	        	dim.width += 4;
	        	// downsize vertical buffer as it always is too much
	        	dim.height -= dim.height * 0.25;
	        	
	        	pos = {
					radius : 1,
					radiusInc : 2,
					arcLength : 2,				
					x : 0,
					y : 0,
					t : 0
				};

	        	while( true ) {

	        		// increment spiral
	        		pos = this.spiralPosition(pos);
	        		// test for intersection
	        		intersection = this.intersectsAWord(pos, dim, cloud, bb);

	        		if ( intersection.result === false ) {

	        			cloud[j] = {
	        				word: word,
	        				fontSize: fontSize,
	        				x:pos.x,
	        				y:pos.y,
	        				width: dim.width,
	        				height: dim.height
	        			};
	        			break;

	        		} else {

	        			if ( intersection.type === 'border' ) {
	        				// if we hit border, extend arc length
	        				pos.arcLength = pos.radius;
	        				borderCollisions++;
	        				if ( borderCollisions > 20 ) {
	        					// bail
	        					cloud[j] = null;
	        					break;
	        				}
	        				
	        			}
	        		}
	        	}
	        }

	        this.addToCache(cloud, hash);

	        return cloud;
        },


		render : function(changeSet) {

			var node, i, j, n, g,
			    labels, toProcess,
			    words, frequencies, itemCount, index,
			    width, height, opacity, offsetX, offsetY,
			    outlineColor, oopacity, outlineWidth,
			    fontFamily, fontWeight, minFontSize, maxFontSize,
			    cursor, xPoint, yPoint, visible, textAnchor, vAlign,
			    font, boundingBox, cloud, label, fillColor, fontSize,
			    str, wordX, wordY, attr, fattr;

			// Create a list of all additions and changes.
			toProcess = changeSet.updates;

			for (i=0; i < toProcess.length; i++){
				node = toProcess[i];

				labels = node.userData.labels = node.userData.labels || [];

				// Get the number of labels to be rendered.
				words = this.valueFor('words', node.data, [], 0),
				frequencies = this.valueFor('frequencies', node.data, [], 0),
				itemCount = (words.length <= frequencies.length) ? words.length : frequencies.length,
				index;

				g = node.graphics;
				
				// remove any extraneous labels
				for (index=itemCount; index < labels.count; index++){
					g.remove(labels[index].back);
					g.remove(labels[index].front);
				}
				
				labels.length = itemCount;
				
				width = this.valueFor('width', node.data, 256, 0);
				height  = this.valueFor('height', node.data, 256, 0);
				opacity = this.valueFor('opacity', node.data, '', 0);
				offsetX = this.valueFor('offset-x', node.data, 0, 0);
				offsetY = this.valueFor('offset-y', node.data, 0, 0);
				outlineColor = this.valueFor('font-outline', node.data, 'none', 0);
				oopacity = this.valueFor('font-outline-opacity', node.data, 1.0, 0);
				outlineWidth = outlineColor !== 'none' && this.valueFor('font-outline-width', node.data, 3, 1);
            	fontFamily = this.valueFor('font-family', node.data, "Arial", 0);
				fontWeight = this.valueFor('font-weight', node.data, "normal", 0);
				minFontSize = this.valueFor('min-font-size', node.data, 12, 0);
	        	maxFontSize = this.valueFor('max-font-size', node.data, 24, 0);
				cursor = this.valueFor('cursor', node.data, 'default', 0);
				xPoint = offsetX + (this.valueFor('x', node.data, 0, 0) * node.width) + (node.position[0]||0);
				yPoint = offsetY + (this.valueFor('y', node.data, 0, 0) * node.height) + (node.position[1]||0);
				visible = !!this.valueFor('label-visible', node.data, true, 0);
				textAnchor = 'middle';
				vAlign = 0;
				font = {
					'font-family': fontFamily,
					'font-weight': fontWeight,
					minFontSize : minFontSize,
					maxFontSize : maxFontSize,
					'transform': ''
				};
				boundingBox = {
					width:width, 
					height:height, 
					x: 0, 
					y:0
				};
				cloud = this.generateCloud( g, words, frequencies, itemCount, font, boundingBox );
				for (index=0; index < itemCount; index++) {
				
					label = labels[index];
					
					// if not visible, or cloud word could not fit
					if (!visible || cloud[index] === null ){
						if (label) {
							g.remove(label.back);
							g.remove(label.front);
							labels[index] = null;
						}
						// Since all the labels are re-rendered on each update, there is
						// nothing more to do if the label is not visible.
						continue;
					}

					// Make the outline and fill colour the same.
					fillColor = this.valueFor('fill', node.data, '#000000', index);
					
					fontSize = cloud[index].fontSize;
					str = cloud[index].word;
					wordX = xPoint + cloud[index].x;
					wordY = yPoint + cloud[index].y;

					attr = {
							'x': wordX,
							'y': wordY,
							'text': str,
							'stroke': 'none',
							'font-family': fontFamily,
							'font-size': fontSize,
							'font-weight': fontWeight,
							'text-anchor': textAnchor,
							'opacity': opacity,
							'cursor': cursor
							};
					fattr;

					if (!label) {
						label = labels[index] = {};
					}

					
					// if outlined we create geometry behind the main text.
					if (outlineWidth) {
						fattr = aperture.util.extend({
							'fill': fillColor
						}, attr);
									
						if (oopacity !== '' && oopacity != null && oopacity !== 1) {
							if (opacity !== '' && opacity != null) {
								oopacity = Math.min(1.0, opacity * oopacity);
							}
						} else {
							oopacity = opacity;
						}
						
						attr['opacity']= oopacity !== 1? oopacity : '';
						attr['stroke-width']= outlineWidth;
						attr['stroke']= outlineColor;
						attr['stroke-linecap']= 'round';
						attr['stroke-linejoin']= 'round';
					} else {
						if (label.front) {
							g.remove(label.front);
							label.front = null;
						}
						attr['stroke']= 'none';
						attr['fill']= fillColor;
					}
					
					index = [index];
									
					// always deal with the back one first.
					if (!label.back) {
						
						label.back = g.text(xPoint, yPoint, str);
						g.data(label.back, node.data, index);
						g.update(label.back, attr);
						g.apparate(label.back, changeSet.transition);
						
					} else {
						g.update(label.back, attr, changeSet.transition);
					}
					
					
					// then the front.
					if (outlineWidth) {
						if (!label.front) {
							label.front = g.text(xPoint, yPoint, str);
							
							g.data(label.front, node.data, index);
							g.update(label.front, fattr);
							g.apparate(label.front, changeSet.transition);
						} else {
							g.update(label.front, fattr, changeSet.transition);
						}
					}
				}					
			}
			
		}
	});

	return WordCloudLayer;

});