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
package com.oculusinfo.tile.util;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;

import java.awt.Rectangle;

/**
 * A TileData instance that wraps and provides a subset view of the data of another TileData. Effectively
 * this allows sourcing a higher level tile's data contained within a parent tile using the parent's data
 * at a lower resolution.
 *
 * @author robharper
 */
public class TileDataView<T> extends TileData<T> {
    private final TileData<T> _source;
    private final Rectangle _bounds;

    /**
     * Creates a TileDataView of an existing TileData given an alternate tile index. The new index
     * must be contained within the source at a higher level.
     * @param source
     * @param targetIndex
     */
    public TileDataView(TileData<T> source, TileIndex targetIndex) {
        // TileData view of the target index but with a lower bin count (calculated as a fraction of source bins using level difference)
        super(new TileIndex(targetIndex.getLevel(), targetIndex.getX(), targetIndex.getY(),
                source.getDefinition().getXBins() / ((int)Math.pow(2, targetIndex.getLevel() - source.getDefinition().getLevel())),
                source.getDefinition().getYBins() / ((int)Math.pow(2, targetIndex.getLevel() - source.getDefinition().getLevel()))
        ));

        TileIndex sourceIndex = source.getDefinition();

        //calculate the tile tree multiplier to go between tiles at each level.
        //this is also the number of x/y tiles in the base level for every tile in the scaled level
        int tileTreeMultiplier = (int)Math.pow(2, targetIndex.getLevel() - sourceIndex.getLevel());

        int baseLevelFirstTileY = sourceIndex.getY() * tileTreeMultiplier;

        //the y tiles are backwards, so we need to shift the order around by reversing the counting direction
        int yTileIndex = ((tileTreeMultiplier - 1) - (targetIndex.getY() - baseLevelFirstTileY)) + baseLevelFirstTileY;

        //figure out which bins to use for this tile based on the proportion of the base level tile within the scale level tile
        int xBinStart = (int)Math.floor(sourceIndex.getXBins() * (((double)(targetIndex.getX()) / tileTreeMultiplier) - sourceIndex.getX()));
        int yBinStart = ((int)Math.floor(sourceIndex.getYBins() * (((double)(yTileIndex) / tileTreeMultiplier) - sourceIndex.getY())) ) ;

        _source = source;
        _bounds = new Rectangle(xBinStart, yBinStart, sourceIndex.getXBins() / tileTreeMultiplier, sourceIndex.getYBins() / tileTreeMultiplier);
    }

    public TileDataView(TileData<T> source, Rectangle bounds) {
        super(new TileIndex(source.getDefinition().getLevel(), source.getDefinition().getX(), source.getDefinition().getY(), bounds.width, bounds.height));

        _source = source;
        _bounds = bounds;
    }

    public T getBin (int x, int y) {
        return _source.getBin(x + _bounds.x, y + _bounds.y);
    }
}
