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
package com.oculusinfo.binning.impl;

import java.util.Collection;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;


/**
 * A TileData instance that wraps and provides a subset view of the data of another TileData. Effectively
 * this allows sourcing a higher level tile's data contained within a parent tile using the parent's data
 * at a lower resolution.
 *
 * @author robharper
 */
public class SubTileDataView<T> implements TileData<T> {
	private static final long serialVersionUID = -4853326888627107022L;



	/**
     * Factory method that creates a SubTileDataView given a source tile and a target tile index which must be contained
     * within the source tile. The returned view will appear to users as a tile at the target index but with data sourced
     * from the given source parent tile.
     * @param source The source tile for the data
     * @param targetIndex The index of the tile within (child of) the source to emulate with the view
     * @param <T> The type of tile data
     * @return A view TileData object representing the target index
     */
    public static <T> SubTileDataView<T> fromSourceAbsolute(TileData<T> source, TileIndex targetIndex) {
        TileIndex sourceIndex = source.getDefinition();
        int levelDelta = targetIndex.getLevel() - sourceIndex.getLevel();

        if (levelDelta < 0) {
            throw new IllegalArgumentException("Target index must be greater than or equal to source index in absolute tile view");
        }
        if (targetIndex.getX() >> levelDelta != sourceIndex.getX()) {
            throw new IllegalArgumentException("Target index be for tile contained within source tile");
        }
        if (targetIndex.getY() >> levelDelta != sourceIndex.getY()) {
            throw new IllegalArgumentException("Target index be for tile contained within source tile");
        }

        int tileCountRatio = 1 << levelDelta;

        // Fraction of the way through the tile that the subview should begin
        // In the case of y the axis is inverted so the end of the desired tile is the beginning of the next, hence +1
        float xPosFraction = ((float)targetIndex.getX() / tileCountRatio) - sourceIndex.getX();
        float yPosFraction = ((float)(targetIndex.getY()+1) / tileCountRatio) - sourceIndex.getY();

        int xBinStart = (int)(xPosFraction * sourceIndex.getXBins());
        // Flip the bin counts (y axis opposite direction to bin y axis)
        int yBinStart = sourceIndex.getYBins() - (int)(yPosFraction * sourceIndex.getYBins());

        return new SubTileDataView<>(source,
                new TileIndex(targetIndex.getLevel(), targetIndex.getX(), targetIndex.getY(), sourceIndex.getXBins()/tileCountRatio, sourceIndex.getYBins()/tileCountRatio),
                xBinStart, yBinStart);
    }



    private final TileData<T> _source;
    private final TileIndex   _index;
    private final int         _xOffset;
    private final int         _yOffset;

    private SubTileDataView(TileData<T> source, TileIndex index, int xOffset, int yOffset) {
        _source = source;
        _index = index;
        _xOffset = xOffset;
        _yOffset = yOffset;
    }

    @Override
    public TileIndex getDefinition() {
        return _index;
    }

	@Override
	public T getDefaultValue () { return _source.getDefaultValue(); }

    @Override
    public T getBin (int x, int y) {
        if (x < 0 || x >= getDefinition().getXBins()) {
            throw new IllegalArgumentException("Bin x index is outside of tile's valid bin range");
        }
        if (y < 0 || y >= getDefinition().getYBins()) {
            throw new IllegalArgumentException("Bin y index is outside of tile's valid bin range");
        }
        return _source.getBin(x + _xOffset, y + _yOffset);
    }

    @Override
    public void setBin(int x, int y, T value) {
        if (x < 0 || x >= getDefinition().getXBins()) {
            throw new IllegalArgumentException("Bin x index is outside of tile's valid bin range");
        }
        if (y < 0 || y >= getDefinition().getYBins()) {
            throw new IllegalArgumentException("Bin y index is outside of tile's valid bin range");
        }
        _source.setBin(x + _xOffset, y + _yOffset, value);
    }

    @Override
    public Collection<String> getMetaDataProperties () {
        return _source.getMetaDataProperties();
    }

    @Override
    public String getMetaData (String property) {
        return _source.getMetaData(property);
    }

    @Override
    public void setMetaData (String property, Object value) {
        // We don't allow this because anyone trying wouldn't have a complete view of the tile into which
        // the metadata would be set.
        throw new UnsupportedOperationException("Cannot set metadata on a view into another tile");
    }
}
