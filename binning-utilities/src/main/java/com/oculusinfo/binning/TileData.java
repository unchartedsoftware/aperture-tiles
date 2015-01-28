package com.oculusinfo.binning;

import java.io.Serializable;
import java.util.Collection;

/**
 * Created by nkronenfeld on 1/27/2015.
 */
public interface TileData<T> extends Serializable {
	/**
	 * A catalog of available types of ways to store tiles available in the system.
	 */
	public static enum StorageType {
		/** Tiles stored as dense arrays */
		Dense(DenseTileData.class),
		/** Tiles stored as sparse maps, indexed by bin */
		Sparse(SparseTileData.class);

		private Class<? extends TileData> _tileClass;
		private StorageType (Class<? extends TileData> tileClass) {
			_tileClass = tileClass;
		}
		public Class<? extends TileData> tileClass () {
			return _tileClass;
		}
	}



	/**
	 * Get the tile index defining which tile is associated with this data
	 *
	 * @return The TileIndex of the tile
	 */
	TileIndex getDefinition();

	/**
	 * Set the value of a particular bin in this tile
	 *
	 * @param x The x coordinate of the bin to be changed.
	 * @param y The y coordinate of the bin to be changed.
	 * @param value The value to which to set the bin in question.
	 */
	void setBin (int x, int y, T value);

	/**
	 * Get the value of a particular bin in this tile.
	 *
	 * @param x The x coordinate of the bin to be changed.
	 * @param y The y coordinate of the bin to be changed.
	 * @return The value of the bin in question.
	 */
	T getBin(int x, int y);

	/**
	 * Get the properties listed in the metadata of this tile.
	 *
	 * @return A collection of listed properties.  This can be null if
	 *         there are no listed properties.
	 */
	Collection<String> getMetaDataProperties();

	/**
	 * Get the value of the given metadata property.
	 *
	 * @param property The property of interest
	 * @return The value of the given property, or null if the property
	 *         isn't listed in the tile's metadata.
	 */
	String getMetaData(String property);

	/**
	 * Sets the value for a given property in the tile's metadata.
	 *
	 * @param property The property of interest
	 * @param value The value of said property
	 */
	void setMetaData(String property, Object value);
}
