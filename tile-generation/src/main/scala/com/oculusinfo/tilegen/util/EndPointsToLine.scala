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

package com.oculusinfo.tilegen.util

import com.oculusinfo.binning.BinIndex

/**
 * Class containing utility functions to convert two endpoints to a line 
 * (i.e., pixels as a sequence of bins) 
 * 
 * Used by RDDLineBinner to draw either straight lines or arcs 
 * for rendering edges in graph tiling applications.
 * 
 * Member variables:
 * 	_lenThres	Threshold of allowable length of line segment to render (in bins).  
 *  			If length > _lenThres then middle of line segment is faded-out.
 *  
 *  _tileLen	Current tile length for this tile-generation job (in bins)
 * 
 */
class EndPointsToLine(lenThres: Int = 1024, xBins: Int = 256, yBins: Int = 256) extends Serializable {
	
	private val _lenThres = lenThres
	private val _tileLen = Math.min(xBins,yBins)
	
	/**
	 * Determine all bins that are required to draw a line two endpoint bins.
	 *
	 * Uses Bresenham's algorithm for filling in the intermediate pixels in a line.
	 *
	 * From wikipedia
	 * 
	 * @param start
	 *        The start bin, in universal bin index coordinates (not tile bin
	 *        coordinates)
	 * @param end
	 *        The end bin, in universal bin index coordinates (not tile bin
	 *        coordinates)
	 * @param binValue
	 * 		  Original bin value of the endpoints                      
	 * @return Pairs of all (bin indices, bin values), in universal bin coordinates, falling on the direct
	 *         line between the two endoint bins.  If the line segment is longer than _maxLen then
	 *         the middle of the line is faded out.
	 */
	val endpointsToLineBins: (BinIndex, BinIndex, Double) => IndexedSeq[(BinIndex, Double)] =
		(start, end, binValue) => {
			
			val len = calcLen(start, end)
			val endsLength = _tileLen
			
			// Bresenham's algorithm
			val (steep, x0, y0, x1, y1) = getPoints(start, end)

			var x0_mid = 0
			var x1_mid = 0
			var x0_slope = 0.0
			var x1_slope = 0.0
			if (len > _lenThres) {
				// only draw line within endsLength bins away from an endpoint
				val lenXends = ((x1-x0)*(endsLength.toDouble/len)).toInt
				x0_mid = lenXends + x0
				x1_mid = x1 - lenXends
				x0_slope = -18.4/lenXends	// -18.4 == log(1e-8) -- need to use a very small number here, but > 0 (b/c log(0) = -Inf)
					//1.0/lenXends	// slope used for fading-out line pixels for long lines
					x1_slope = x0_slope //-x0_slope
			}
			
			val deltax = x1-x0
			val deltay = math.abs(y1-y0)
			var error = deltax>>1
			var y = y0
			val ystep = if (y0 < y1) 1 else -1

			// x1+1 needed here so that "end" bin is included in Sequence
			val pixels = Range(x0, x1+1).map(x =>
				{
					val ourY = y
					error = error - deltay
					if (error < 0) {
						y = y + ystep
						error = error + deltax
					}

					if (steep) new BinIndex(ourY, x)
					else new BinIndex(x, ourY)
				}
			)
			
			if (len > _lenThres) {
				// discard middle of line segment if endpoints are too far apart, and fade out ends
				if (steep)
					pixels.filter(bin => (bin.getY() <= x0_mid || bin.getY() >= x1_mid)).map(b =>
						{
							var scale = if (b.getY <= x0_mid) Math.exp((b.getY - x0)*x0_slope) //1.0 - Math.pow((b.getY - x0)*x0_slope, fadeExp)	// fade out line near src endpoint
							else Math.exp((x1 - b.getY)*x0_slope) //1.0 - Math.pow((x1 - b.getY)*x1_slope, fadeExp)					//  or fade out near dst endpoint
								scale = if (scale > 1.0) 1.0
								else if (scale < 0.0) 0.0
								else scale
							//val scale = if (b.getY <= x0_mid) x0_slope*(b.getY - x0_mid)
							//			else x1_slope*(b.getY - x1_mid)
							(b, scale*binValue)
						}
					)
				else
					pixels.filter(bin => (bin.getX() <= x0_mid || bin.getX() >= x1_mid)).map(b =>
						{
							var scale = if (b.getX <= x0_mid) Math.exp((b.getX - x0)*x1_slope) //1.0 - Math.pow((b.getX - x0)*x0_slope, fadeExp)	// fade out line near src endpoint
							else Math.exp((x1 - b.getX)*x1_slope) //1.0 - Math.pow((x1 - b.getX)*x1_slope, fadeExp)					//  or fade out near dst endpoint
								scale = if (scale > 1.0) 1.0
								else if (scale < 0.0) 0.0
								else scale
							//val scale = if (b.getX <= x0_mid) x0_slope*(b.getX - x0_mid)
							//			else x1_slope*(b.getX - x1_mid)
							(b, scale*binValue)
						}
					)
			} else {
				pixels.map(b => (b, binValue))	// line section is not too long, so don't 'fade out' (all pixels have scale = 1.0)
			}
		}

	/**
	 * Determine all bins that are required to draw an arc between two endpoint bins.
	 *
	 * Bresenham's Midpoint circle algorithm is used for filling in the intermediate pixels in an arc.
	 *
	 * From wikipedia
	 * 
	 * @param start
	 *        The start bin, in universal bin index coordinates (not tile bin
	 *        coordinates)
	 * @param end
	 *        The end bin, in universal bin index coordinates (not tile bin
	 *        coordinates)      
	 * @param binValue
	 * 		  Original bin value of the endpoints                      
	 * @return Pairs of all (bin indices, bin values), in universal bin coordinates, falling on the arc
	 *         line between the two endoint bins.  If the line segment is longer than _maxLen then
	 *         the middle of the arc is faded out.
	 */
	val endpointsToArcBins: (BinIndex, BinIndex, Double) => IndexedSeq[(BinIndex, Double)] =
		(start, end, binValue) => {
			var (x0, y0, x1, y1) = (start.getX(), start.getY(), end.getX(), end.getY())
			
			val len = calcLen(start, end)
			val endsLength = _tileLen
			
			val segments = if (len > _lenThres) 2 else 1	// if segments=2, only draw line within lenThres/2 bins away from an endpoint
			
			//---- Find centre of circle to be used to draw the arc
			val r = len		// set radius of circle = len for now  (TODO -- could make this a tunable parameter?)

			val halfPI = 0.5*Math.PI
			val theta1 = halfPI - Math.asin(len.toDouble/(2.0*r)); // angle from each endpoint to circle's centre (centre and endpoints form an isosceles triangle)
			val angleTemp = Math.atan2(y1-y0, x1-x0)-theta1;
			val xC = (x0 + r*Math.cos(angleTemp)).toInt		   // co-ords for circle's centre
			val yC = (y0 + r*Math.sin(angleTemp)).toInt
			//val xC_2 = x0 + (r*Math.cos(Math.atan2(dy, dx)+theta1)).toInt	//Note: 2nd possiblility for circle's centre
			//val yC_2 = y0 + (r*Math.cos(Math.atan2(dy, dx)+theta1)).toInt	//(corresponds to CCW arc, so not needed in this case)

			//---- Use Midpoint Circle algorithm to draw the arc
			var f = 1-r
			var ddF_x = 1
			var ddF_y = -2*r
			var x = 0
			var y = r

			// angles for start and end points of arc
			var midRad0 = 0.0
			var midRad1 = 0.0
			var x0_slope = 0.0
			var x1_slope = 0.0
			val fadeExp = 0.33	//0 to 1.  Lower value = faster fading-out
			var startRad = Math.atan2(y0-yC, x0-xC)
			var stopRad = Math.atan2(y1-yC, x1-xC)

			val bWrapNeg = (stopRad-startRad > Math.PI)
			if (bWrapNeg) startRad += 2.0*Math.PI	//note: this assumes using CW arcs only!
				//(otherwise would need to check if stopRad is negative here as well)

			if (segments == 2) {
				val segmentRad = endsLength.toDouble/r // angle of each arc segment (with arclenth = endsLength)
				midRad0 = startRad - segmentRad
				midRad1 = stopRad + segmentRad
				x0_slope = -18.4/segmentRad	// -18.4 == log(1e-8) -- need to use a very small number here, but > 0 (b/c log(0) = -Inf)
					//x0_slope = 1.0/segmentRad	// slope used for fading-out line pixels for long lines (note: +ve slope because arcs are CW)
					x1_slope = x0_slope //-x0_slope
			}
			
			val arcBins = scala.collection.mutable.ArrayBuffer[(BinIndex, Double)]()

			// calc points for the four vertices of circle
			saveArcPoint((xC, yC+r), halfPI)
			saveArcPoint((xC, yC-r), -halfPI)
			saveArcPoint((xC+r, yC), 0)
			saveArcPoint((xC-r, yC), Math.PI)

			while (x < y-1) {
				if (f >= 0) {
					y = y - 1
					ddF_y = ddF_y + 2
					f = f + ddF_y
				}
				x = x + 1
				ddF_x = ddF_x + 2
				f = f + ddF_x

				val newAngle = Math.atan2(y, x)
				saveArcPoint((xC+x, yC+y), newAngle)
				saveArcPoint((xC-x, yC+y), Math.PI - newAngle)
				saveArcPoint((xC+x, yC-y), -newAngle)
				saveArcPoint((xC-x, yC-y), -Math.PI + newAngle)

				if (x!=y) {
					saveArcPoint((xC+y, yC+x), halfPI - newAngle)
					saveArcPoint((xC-y, yC+x), halfPI + newAngle)
					saveArcPoint((xC+y, yC-x), -halfPI + newAngle)
					saveArcPoint((xC-y, yC-x), -halfPI - newAngle)
				}
			}

			//------ Func to save pixel on arc line
			def saveArcPoint(point: (Int, Int), angle: Double) = {
				var newAngle = angle
				if (bWrapNeg && newAngle < 0.0)
					newAngle += 2.0*Math.PI
				
				if (segments==1) {
					if (newAngle <= startRad && newAngle >= stopRad) {
						val b = new BinIndex(point._1, point._2)
						val bin = (b, binValue)		// arc section is not too long, so don't 'fade out' (all pixels have scale = 1.0)
							arcBins += bin
					}
				}
				else {
					if (newAngle <= startRad && newAngle >= midRad0) {
						val b = new BinIndex(point._1, point._2)
						var scale = Math.exp((startRad - newAngle)*x0_slope)  //1.0 - Math.pow((startRad - newAngle)*x0_slope, fadeExp)
						                                                      //var scale = x0_slope*(newAngle - midRad0)
							scale = if (scale > 1.0) 1.0
							else if (scale < 0.0) 0.0
							else scale
						val bin = (b, scale*binValue)
						arcBins += bin
					}
					else if (newAngle <= midRad1 && newAngle >= stopRad) {
						val b = new BinIndex(point._1, point._2)
						var scale = Math.exp((newAngle - stopRad)*x1_slope)   //1.0 - Math.pow((newAngle - stopRad)*x1_slope, fadeExp)
						                                                      //val scale = x1_slope*(newAngle - midRad1)
							scale = if (scale > 1.0) 1.0
							else if (scale < 0.0) 0.0
							else scale
						val bin = (b, scale*binValue)
						arcBins += bin
					}
				}
			}

			arcBins.toIndexedSeq	// seq of arc bins
		}
	
	
	/**
	 * Re-order coords of two endpoints for efficient implementation of Bresenham's line algorithm  
	 */	
	def getPoints (start: BinIndex, end: BinIndex): (Boolean, Int, Int, Int, Int) = {
		val xs = start.getX()
		val xe = end.getX()
		val ys = start.getY()
		val ye = end.getY()
		val steep = (math.abs(ye - ys) > math.abs(xe - xs))

		if (steep) {
			if (ys > ye) {
				(steep, ye, xe, ys, xs)
			} else {
				(steep, ys, xs, ye, xe)
			}
		} else {
			if (xs > xe) {
				(steep, xe, ye, xs, ys)
			} else {
				(steep, xs, ys, xe, ye)
			}
		}
	}
	
	
	/**
	 * Calc length of a line from two endpoints 
	 */	
	def calcLen(start: BinIndex, end: BinIndex): Int = {
		//calc integer length between to bin indices
		var (x0, y0, x1, y1) = (start.getX(), start.getY(), end.getX(), end.getY())
		val dx = x1-x0
		val dy = y1-y0
		Math.sqrt(dx*dx + dy*dy).toInt
	}
}
