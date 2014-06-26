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
package com.oculusinfo.tilegen.examples.datagen

/**
 * Provides a multifractal function based on Perlin noise.  
 */
object Multifractal {
	/**
	 * Perlin noise function - based on C code posted on Ken Perlin's web site:
	 * {@link http://mrl.nyu.edu/~perlin/noise/}.
	 */
	def noise(px: Double, py: Double, pz: Double) = {
		val X = Math.floor(px).toInt & 255 // FIND UNIT CUBE THAT
		val Y = Math.floor(py).toInt & 255 // CONTAINS POINT.
		val Z = Math.floor(pz).toInt & 255

		val x = px - Math.floor(px).toInt // FIND RELATIVE X,Y,Z
		val y = py - Math.floor(py).toInt // OF POINT IN CUBE.
		val z = pz - Math.floor(pz).toInt

		val u = fade(x) // COMPUTE FADE CURVES
		val v = fade(y) // FOR EACH OF X,Y,Z.
		val w = fade(z)

		val A = p(X) + Y
		val AA = p(A) + Z
		val AB = p(A + 1) + Z // HASH COORDINATES OF
		val B = p(X + 1) + Y // OF THE 8 CUBE CORNERS
		val BA = p(B) + Z
		val BB = p(B + 1) + Z

		lerp(w, lerp(v, lerp(u, grad(p(AA), x, y, z), // AND ADD
		                     grad(p(BA), x - 1, y, z)), // BLENDED
		             lerp(u, grad(p(AB), x, y - 1, z), // RESULTS
		                  grad(p(BB), x - 1, y - 1, z))), // FROM 8
		     lerp(v, lerp(u, grad(p(AA + 1), x, y, z - 1), // CORNERS
		                  grad(p(BA + 1), x - 1, y, z - 1)), // OF CUBE
		          lerp(u, grad(p(AB + 1), x, y - 1, z - 1), grad(
			               p(BB + 1), x - 1, y - 1, z - 1))));
	}
	
	/**
	 * Generates a multifractal based on perlin noise.  H represent the noise increment, lacunarity is the gap between
	 * frequencies and octaves is the number of frequencies.  Based on C code from Texture and Modeling - A Procedural 
	 * Approach 3rd. Ed. 
	 */
	def fBm(px: Double, py: Double, pz: Double, H: Double = 0.0, octaves: Int = 1, lacunarity: Double = 1.0 ) = {
		def processOctave(x: Double, y: Double, z: Double, octave: Int): Double = {
			if (octave < octaves) {
				noise(x, y, z) * Math.pow(lacunarity, -H * octave) + processOctave(x * lacunarity, y * lacunarity, z * lacunarity, octave + 1)
			} else {
				0.0
			}
		}
		processOctave(px, py, pz, 0)
	}
	
	private val permutation =
		Array(151, 160, 137, 91,
		      90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103,
		      30, 69, 142, 8, 99, 37, 240, 21, 10, 23, 190, 6, 148, 247, 120,
		      234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32, 57,
		      177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68,
		      175, 74, 165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83,
		      111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245,
		      40, 244, 102, 143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76,
		      132, 187, 208, 89, 18, 169, 200, 196, 135, 130, 116, 188, 159, 86,
		      164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250, 124, 123,
		      5, 202, 38, 147, 118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47,
		      16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213, 119, 248, 152, 2,
		      44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9, 129, 22, 39,
		      253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104, 218,
		      246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162,
		      241, 81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181,
		      199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150,
		      254, 138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128,
		      195, 78, 66, 215, 61, 156, 180)

	private val p = permutation ++ permutation

	private def fade(t: Double) = { t * t * t * (t * (t * 6 - 15) + 10) }

	private def lerp(t: Double, a: Double, b: Double) = { a + t * (b - a) }

	private def grad(hash: Int, x: Double, y: Double, z: Double) = {
		val h = hash & 15
		val u = if (h < 8) x else y
		val v = if (h < 4) y else if (h == 12 || h == 14) x else z
		(if ((h & 1) == 0) u else -u) + (if ((h & 2) == 0) v else -v)
	}
}
