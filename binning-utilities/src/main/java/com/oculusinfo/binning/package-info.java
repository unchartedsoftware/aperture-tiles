/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/**
 * <p>
 * Tiling takes a basic concept of mapping - hierarchical partitioning of 
 * data, for quick display at any zoom level - and applies it to arbitrary 
 * data.
 * </p>
 * 
 * <p>
 * Tiling applies to datasets that can be fit into a rectilinear space.  At 
 * the moment, this project only supports 1- and 2-dimensional tiling 
 * problems.  Tiles are rectilinear subsets of the entire data space, and are 
 * composed of a rectilinear set of bins.
 * </p>
 * 
 * <p>
 * The key lesson one should take from the mapping case is that, in order to 
 * avoid artifacts at tile and bin boundaries, a boundary at any given level 
 * should also be a boundary of at least the same importance at all lower 
 * levels (where tile boundaries are defined to be more "important" than bin 
 * boundaries).  So a bin boundary on level 1 should also be a bin boundary 
 * on level 2, 3, etc....  And at some level (say, for the sake of argument, 
 * level 6), that boundary will be a tile boundary; in all levels below 6, 
 * then, that boundary should be a tile boundary.
 * </p>
 * <p>
 * The implications of this are that:
 * <ul>
 * <li> At every level, there should be an integral number of tiles per tile
 * in the level above it; similarly with bins. </li>
 * <li> The lower this integer is, the more fine-grained control the user will 
 * have over how much they can zoom in or out. </li>
 * <li> The number of bins per tile in each dimension should be a multiple of 
 * this integral number - in fact, it shoudld generally be a power of this 
 * number (though one can construct very wierd tile pyramids which are 
 * legitimate, but for which this isn't true - the literal limitation is that 
 * the number of bins/tile is the product of the number of tiles in level n+1 
 * per tile in level n as n goes from the current level down to the higher-
 * numbered level m where a tile in level m exactly matches a bin in the 
 * current level).</li>
 * </ul>
 * </p>
 * <p>
 * In the general mapping case, the simplest and most useful solution has been 
 * generally to take simple powers of two - each level contains twice as many 
 * tiles in each dimension as the level above, and each tile is composed of a 
 * number of bins in each dimension that is also a power of 2 (any power of 2, 
 * really).  This gives the user the best control, with no artifacts that seem 
 * to appear and disappear as one zooms in and out.  We have, for the moment, 
 * continued this practice in our usage of tiling with arbitrary data sets.
 * </p>
 * 
 * <p>
 * This package contains the basic interfaces and data structures used when 
 * tiling data.  They are:
 * 
 * <ul>
 * <li> {@link com.oculusinfo.binning.TileIndex}, 
 * {@link com.oculusinfo.binning.BinIndex}, and 
 * {@link com.oculusinfo.binning.TileAndBinIndices}, which are used to specify
 * which tile and/or bin is meant. </li>
 * <li> {@link com.oculusinfo.binning.TileData}, {@link com.oculusinfo.binning.impl.DenseTileData}, and
 * {@link com.oculusinfo.binning.impl.SparseTileData} which store the data comprising a tile </li>
 * <li> {@link com.oculusinfo.binning.TilePyramid}, which indicates the 
 * mapping, or projection, from the raw data into tile coordinates, </li>
 * <li> {@link com.oculusinfo.binning.PyramidComparator}, which can be used 
 * to linearly order a (2-D) tile set </li> 
 * <li> {@link com.oculusinfo.binning.TileIterator},
 * {@link com.oculusinfo.binning.BinIterator}, and 
 * {@link com.oculusinfo.binning.TileIterable}, which are used to list all 
 * tiles in a geometric region
 * </ul>
 * </p>
 * 
 * <p>
 * Note that, in general, tiles are ordered from bottom-left to top-right, to 
 * conform to the TMS standard in mapping applications, while bins with a tile 
 * are ordered from top-left to bottom-right, to conform with standard bitmap
 * ordering.  While this is confusing, it is unfortunately necessary to be 
 * able to use standard mapping tools to display this data.
 * </p>
 */
package com.oculusinfo.binning;
