package com.oculusinfo.binning.io;

import java.util.List;

import com.oculusinfo.factory.ConfigurableFactory;

/**
 * A helper factory to enable getting data from data paths within the configuration phase.<br>
 * For example, if all configuration data is under some root path, then this factory
 * can be added as the root of the configurable hierarchy to give everyone a different path,
 * without having to modify the path values for each factory directly. This factory doesn't create anything.
 * 
 * @author cregnier
 *
 */
public class EmptyConfigurableFactory extends ConfigurableFactory<Object> {

	public EmptyConfigurableFactory(String name, ConfigurableFactory<?> parent, List<String> path) {
		super(name, Object.class, parent, path);
	}

	@Override
	protected Object create() {
		return new Object();
	}

	/**
	 * Overridden in order to make this public so others can compose trees
	 */
	@Override
	public void addChildFactory(ConfigurableFactory<?> child) {
		super.addChildFactory(child);
	}
}
