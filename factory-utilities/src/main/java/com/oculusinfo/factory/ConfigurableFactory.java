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
package com.oculusinfo.factory;

import java.io.PrintStream;
import java.security.MessageDigest;
import java.util.*;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a basis for factories that are configurable via JSON or
 * java property files.
 * 
 * This provides the standard glue of getting properties consistently in either
 * case, of documenting the properties needed by a factory, and, potentially, of
 * writing out a configuration.
 * 
 * @param <T> The type of object constructed by this factory.
 *
 * @author nkronenfeld
 */
abstract public class ConfigurableFactory<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableFactory.class);

	public static List<String> mergePaths (ConfigurableFactory<?> parent, List<String> childPath) {
		List<String> parentPath = null;
		if (null != parent) parentPath = parent.getRootPath();
		return mergePaths(parentPath, childPath);
	}
	public static List<String> mergePaths (List<String> parentPath, List<String> childPath) {
		List<String> fullPath = new ArrayList<>();
		if (null != parentPath) fullPath.addAll(parentPath);
		if (null != childPath) fullPath.addAll(childPath);
		return fullPath;
	}



	private String                                      _name;
	private Class<T>                                    _factoryType;
	private List<String>                                _rootPath;
	private ConfigurableFactory<?>                      _parent;
	private List<ConfigurableFactory<?>>                _children;
	private Set<ConfigurationProperty<?>>               _properties;
	private boolean                                     _configured;
	private JSONObject                                  _configurationNode;
	private Map<ConfigurationProperty<?>, Object>       _defaultValues;
	private boolean                                     _isSingleton;
	private T                                           _singletonProduct;
	private Map<ConfigurationProperty<?>, List<String>> _pathsByProperty;

	/**
	 * Create a factory
	 * 
	 * @param factoryType The type of object to be constructed by this factory.
	 *            Can not be null.
	 * @param parent The parent factory; all configuration nodes of this factory
	 *            will be under the parent factory's root configuration node.
	 * @param path The path from the parent factory's root configuration node to
	 *            this factory's root configuration node.
	 */
	protected ConfigurableFactory (Class<T> factoryType, ConfigurableFactory<?> parent, List<String> path) {
		this(null, factoryType, parent, path, false);
	}

	/**
	 * Create a factory
	 * 
	 * @param factoryType The type of object to be constructed by this factory.
	 *            Can not be null.
	 * @param parent The parent factory; all configuration nodes of this factory
	 *            will be under the parent factory's root configuration node.
	 * @param path The path from the parent factory's root configuration node to
	 *            this factory's root configuration node.
	 * @param isSingleton If true, this factory will only ever produce one
	 *            product, which it will return every time it is asked to
	 *            produce. Do note that if the factory is set to produce a
	 *            singleton, production may be a synchronized, blocking
	 *            operation.
	 */
	protected ConfigurableFactory (Class<T> factoryType, ConfigurableFactory<?> parent, List<String> path, boolean isSingleton) {
		this(null, factoryType, parent, path, isSingleton);
	}
    
	/**
	 * Create a factory
	 * 
	 * @param name A name by which this factory can be known, to be used to
	 *            differentiate it from other child factories of this factory's
	 *            parent that return the same type.
	 * @param factoryType The type of object to be constructed by this factory.
	 *            Can not be null.
	 * @param parent The parent factory; all configuration nodes of this factory
	 *            will be under the parent factory's root configuration node.
	 * @param path The path from the parent factory's root configuration node to
	 *            this factory's root configuration node.
	 */
	protected ConfigurableFactory (String name, Class<T> factoryType, ConfigurableFactory<?> parent, List<String> path) {
		this(name, factoryType, parent, path, false);
	}

	/**
	 * Create a factory
	 * 
	 * @param name A name by which this factory can be known, to be used to
	 *            differentiate it from other child factories of this factory's
	 *            parent that return the same type.
	 * @param factoryType The type of object to be constructed by this factory.
	 *            Can not be null.
	 * @param parent The parent factory; all configuration nodes of this factory
	 *            will be under the parent factory's root configuration node.
	 * @param path The path from the parent factory's root configuration node to
	 *            this factory's root configuration node.
	 * @param isSingleton If true, this factory will only ever produce one
	 *            product, which it will return every time it is asked to
	 *            produce. Do note that if the factory is set to produce a
	 *            singleton, production may be a synchronized, blocking
	 *            operation.
	 */
	protected ConfigurableFactory (String name, Class<T> factoryType, ConfigurableFactory<?> parent, List<String> path, boolean isSingleton) {
		_name = name;
		_factoryType = factoryType;
		_rootPath = Collections.unmodifiableList(mergePaths(parent, path));
		_children = new ArrayList<>();
		_configured = false;
		_properties = new HashSet<>();
		_pathsByProperty = new HashMap<>();
		_defaultValues = new HashMap<>();
		_isSingleton = isSingleton;
		_singletonProduct = null;
        
		//NOTE: this should not be set to the parent passed in cause the parent won't necessarily
		//be created before the children if you're doing a bottom up approach for some reason.
		_parent = null; 
	}

	/**
	 * Get the root node in the tree of configurables.
	 * @return Returns the root of the configurable factories, or this factory if no parent is set.
	 */
	public ConfigurableFactory<?> getRoot() {
		return (_parent != null)? _parent.getRoot() : this;
	}

	/**
	 * Get the root path for configuration information for this factory.
	 * 
	 * @return A list of strings describing the path to this factory's
	 *         configuration information. Guaranteed not to be null.
	 */
	public List<String> getRootPath () {
		return new ArrayList<>( _rootPath );
	}
	
	/**
	 * Get the name associated with the factory.
	 * @return A string name if provided upon construction, or else null if none was provided.
	 */
	public String getName() {
		return _name;
	}

	/**
	 * List out all properties directly expected by this factory.
	 */
	public Iterable<ConfigurationProperty<?>> getProperties () {
		return _properties;
	}

	/**
	 * Add a property to the list of properties used by this factory
	 * 
	 * @param property
	 * @param path
	 */
	public <PT> void addProperty (ConfigurationProperty<PT> property, List<String> path) {
		_properties.add(property);
		_pathsByProperty.put( property, path );
	}

	/** Get the full name of a given property, including its path in our factory configuration */
	public String getFullPropertyName (ConfigurationProperty<?> property) {
		if (null == property) return null;
		List<String> fullPath = new ArrayList<>();
		if (null != _rootPath) fullPath.addAll(_rootPath);
		if (null != _pathsByProperty.get(property)) fullPath.addAll(_pathsByProperty.get(property));
		fullPath.add(property.getName());
		return mkString(fullPath, ".");
	}

	/**
	 * Add a property to the list of properties used by this factory
	 *
	 * @param property
	 */
	public <PT> void addProperty (ConfigurationProperty<PT> property) {
		addProperty( property, new ArrayList<String>() );
	}

	/**
	 * Set the default value of a property for this factory, and this factory only.
	 */
	public <PT> void setDefaultValue (ConfigurationProperty<PT> property, PT defaultValue) {
		_defaultValues.put(property, defaultValue);
	}

	/**
	 * gets the default value for a given property for this factory.
	 */
	protected <PT> PT getDefaultValue (ConfigurationProperty<PT> property) {
		if (_defaultValues.containsKey(property)) {
			return property.getType().cast(_defaultValues.get(property));
		} else {
			return property.getDefaultValue();
		}
	}

	/**
	 * Return a SHA-256 hexcode representing the state of the configuration
	 * @return String representing the hexcode SHA-256 hash of the configuration state
	 */
	public String generateSHA256() {
		try {
			String propertyString = getFactoryString();
			// generate SHA-256 from the string
			MessageDigest md = MessageDigest.getInstance( "SHA-256" );
			md.update( propertyString.getBytes( "UTF-8" ) );
			byte[] digest = md.digest();
			// convert SHA-256 bytes to hex string
			return Hex.encodeHexString( digest );
		} catch ( Exception e ) {
			LOGGER.warn( "Error registering configuration to SHA", e );
			return "";
		}
	}

	/**
	 * Indicates if an actual value is recorded for the given property.
	 * 
	 * @param property The property of interest.
	 * @return True if the property is listed and non-default in the factory.
	 */
	public boolean hasPropertyValue (ConfigurationProperty<?> property) {
		return (_configured && null != _configurationNode && getPropertyNode( property ).has( property.getName() ) );
	}

	/**
	 * Get the value read at configuration time for the given property.
	 * 
	 * The behavior of this function is undefined if called before
	 * readConfiguration (either version).
	 */
	public <PT> PT getPropertyValue (ConfigurationProperty<PT> property) throws ConfigurationException {
		// if a value has not been configured for this property, return default
		if ( !hasPropertyValue( property ) ) {
			PT defaultValue = getDefaultValue(property);
			LOGGER.warn("Property {} unset.  Using default {}.", getFullPropertyName(property), defaultValue);
			return defaultValue;
		}
		try {
			return property.unencodeJSON( new JSONNode( getPropertyNode( property ) , property.getName() ) );
		} catch (JSONException e) {
			// Must not have been there.  Ignore, leaving as default.
			throw new ConfigurationException("Error reading property "+getFullPropertyName(property)+" from configuration "+_configurationNode);
		}
	}

	/**
	 * Add a child factory, to be used by this factory.
	 * 
	 * @param child The child to add.
	 */
	public void addChildFactory (ConfigurableFactory<?> child) {
		_children.add(child);
		child._parent = this;
	}

	/**
	 * Create the object provided by this factory.
	 * @return The object
	 */
	protected abstract T create() throws ConfigurationException;

	/**
	 * Get one of the goods managed by this factory.
	 * 
	 * This version returns a new instance each time it is called.
	 * 
	 * @param goodsType The type of goods desired.
	 */
	public <GT> GT produce (Class<GT> goodsType) throws ConfigurationException {
		return produce(null, goodsType);
	}

	/**
	 * Get one of the goods managed by this factory.
	 * 
	 * This version returns a new instance each time it is called.
	 * 
	 * @param name The name of the factory from which to obtain the needed
	 *            goods. Null indicates that the factory name doesn't matter.
	 * @param goodsType The type of goods desired.
	 */
	public <GT> GT produce (String name, Class<GT> goodsType) throws ConfigurationException {
		if (!_configured) {
			throw new ConfigurationException("Attempt to get value from uninitialized factory");
		}
		if ((null == name || name.equals(_name)) && goodsType.equals(_factoryType)) {
			if (_isSingleton) {
				if (null == _singletonProduct) {
					synchronized (this) {
						if (null == _singletonProduct) {
							_singletonProduct = create();
						}
					}
				}
				return goodsType.cast(_singletonProduct);
			} else {
				return goodsType.cast(create());
			}
		} else {
			for (ConfigurableFactory<?> child: _children) {
				GT result = child.produce(name, goodsType);
				if (null != result) return result;
			}
		}
		return null;
	}

	public <GT> ConfigurableFactory<GT> getProducer (Class<GT> goodsType) {
		return getProducer(null, goodsType);
	}

	// We are suppressing the warnings in the
	//    return this;
	// line. We have just, in the line before, checked that goodsType - which is
	// Class<GT> - matches _factoryType - which is Class<T> - so therefore, GT
	// and T must be the same, so this cast is guaranteed safe, even if the
	// compiler can't figure that out.
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <GT> ConfigurableFactory<GT> getProducer (String name, Class<GT> goodsType) {
		if ((null == name || name.equals(_name)) && goodsType.equals(_factoryType)) {
			return (ConfigurableFactory) this;
		} else {
			for (ConfigurableFactory<?> child: _children) {
				ConfigurableFactory<GT> result = child.getProducer(name, goodsType);
				if (null != result) return result;
			}
		}
		return null;
	}

	/**
	 * Initialize needed construction values from a properties list.
	 * 
	 * @param rootNode The root node of all configuration information for this
	 *            factory.
	 * @throws ConfigurationException If something goes wrong in configuration.
	 */
	public void readConfiguration (JSONObject rootNode) throws ConfigurationException {
		try {
			_configurationNode = getConfigurationNode(rootNode);
			for (ConfigurableFactory<?> child: _children) {
				child.readConfiguration(rootNode);
			}
			_configured = true;
		} catch (JSONException e) {
			throw new ConfigurationException("Error configuring factory "+this.getClass().getName(), e);
		}
	}

	public void writeConfigurationInformation (PrintStream stream, String prefix) {
		stream.println(prefix+"Configuration for "+this.getClass().getSimpleName()+" (node name "+_name+", path: "+mkString(_rootPath, ", ")+"):");
		prefix = prefix + "  ";
		for (ConfigurationProperty<?> property: _properties) {
			writePropertyValue(stream, prefix, property);
		}
		stream.println();
		for (ConfigurableFactory<?> child: _children) {
			child.writeConfigurationInformation(stream, prefix);
		}
	}

	public void writeConfigurationInformation (PrintStream stream) {
		writeConfigurationInformation(stream, "");
	}

	/**
	 * Get the explicit configuration JSON, containing ALL properties.
	 * @return JSONObject containing all properties used by the configuration
	 */
	public JSONObject getExplicitConfiguration() {
		JSONObject config = new JSONObject();
		return generateConfigurationObj( config );
	}

	/**
	 * Get the JSON object used to configure this factory.
	 *
	 * @return The configuring JSON object, or null if this factory has not yet
	 *         been configured.
	 */
	protected JSONObject getConfigurationNode () {
		return _configurationNode;
	}

	/**
	 * Gets the class of object produced by this factory.
	 */
	protected Class<? extends T> getFactoryType () {
		return _factoryType;
	}

	private JSONObject getPropertyNode( ConfigurationProperty<?> property ) {
		if ( _pathsByProperty.get( property ) == null ) {
			return new JSONObject();
		}
		JSONObject node;
		List<String> path = new ArrayList<>( _pathsByProperty.get( property ) );
		if ( path.isEmpty() ) {
			return _configurationNode;
		} else {
			String subPath;
			JSONObject currentNode = _configurationNode;
			while ( path.size() > 1 ) {
				subPath = path.remove( 0 );
				currentNode = currentNode.optJSONObject( subPath );
				if ( currentNode == null ) {
					return new JSONObject();
				}
			}
			node = currentNode.optJSONObject( path.get(0) );
			if ( node == null ) {
				return new JSONObject();
			}
			return node;
		}
	}

	/*
	 * Gets the JSON node with all this factory's configuration information
	 * 
	 * @param rootNode The root JSON node containing all configuration
	 * information.
	 */
	private JSONObject getConfigurationNode (JSONObject rootNode) throws JSONException {
		return getLeafNode(rootNode, _rootPath);
	}

	/**
	 * Get the sub-node of a root node specified by a given path.
	 * 
	 * @param rootNode The root JSON object whose sub-node is desired.
	 * @param path A list of keys to follow from the root node to find the
	 *            desired leaf.
	 * @return The leaf node, or null if any branch along the path is missing.
	 */
	public static JSONObject getLeafNode (JSONObject rootNode, List<String> path) {
		JSONObject target = rootNode;
		for (String subpath: path) {
			if (target.has( subpath )) {
				try {
					target = target.getJSONObject( subpath );
				} catch (JSONException e) {
					// Node is of the wrong type; default everything.
					target = null;
					break;
				}
			} else {
				target = null;
				break;
			}
		}
		return target;
	}

	private <PT> void writePropertyValue (PrintStream stream, String prefix, ConfigurationProperty<PT> property) {
		if (hasPropertyValue(property)) {
			stream.println(prefix+property.getName()+": "+property.encode(getDefaultValue(property))+" (DEFAULT)");
		} else {
			PT value;
			try {
				value = property.unencodeJSON(new JSONNode(_configurationNode, property.getName()));
				stream.println(prefix+property.getName()+": "+property.encode(value));
			} catch (JSONException|ConfigurationException e) {
				stream.println(prefix+property.getName()+": "+property.encode(getDefaultValue(property))+" (DEFAULT - read error)");
			}
		}
	}

	private String mkString (List<?> list, String separator) {
		if (null == list) return "null";

		boolean first = true;
		String result = "";
		for (Object elt: list) {
			if (first) first = false;
			else result += separator;
			result = result + elt;
		}
		return result;
	}

	private String getFullPropertyString( ConfigurationProperty<?> property, String name ) {
		StringBuilder sb = new StringBuilder();
		for ( String subPath : _rootPath ) {
			sb.append( subPath );
			sb.append(".");
		}
		if ( _pathsByProperty.get( property ) != null ) {
			List<String> attributePath = new ArrayList<>( _pathsByProperty.get( property ) );
			for ( String subPath : attributePath ) {
				sb.append( subPath );
				sb.append( "." );
			}
		}
		sb.append( name );
		return sb.toString();
	}

	private String getFactoryString() {
		StringBuilder sb = new StringBuilder();
		for ( ConfigurationProperty<?> prop : _properties ) {
			sb.append( getFullPropertyString( prop, prop.getName() ) );
			sb.append( ":" );
			try {
				sb.append(getPropertyValue(prop));
			} catch (ConfigurationException e) {
				sb.append("<ERROR>");
			}
		}
		for ( ConfigurableFactory<?> child: _children ) {
			sb.append( child.getFactoryString() );
		}
		return sb.toString();
	}

	private JSONObject addJSONPathAndReturnLeaf( JSONObject config, List<String> path ) throws JSONException {
		// if a path does not exist in the json object, create it
		JSONObject node = config;
		for ( String subpath : path ) {
			if ( !node.has( subpath ) ) {
				node.put( subpath, new JSONObject() );
			}
			node = node.getJSONObject( subpath );
		}
		// return the leaf node of the path
		return node;
	}

	private void addPropertyUnderPath( JSONObject config, ConfigurationProperty<?> property ) throws JSONException, ConfigurationException {
		// get the properties path
		List<String> fullPath = getRootPath();
		fullPath.addAll(_pathsByProperty.get( property ) );
		// ensure the path exists by adding it if it doesn't
		// append root path to start of property path since they are relative to the
		// current factories path
		JSONObject node = addJSONPathAndReturnLeaf( config, fullPath );
		// add the property to the leaf node of the path
		node.put(property.getName(), getPropertyValue(property));
	}

	private JSONObject generateConfigurationObj( JSONObject config ) {
		try {
			for ( ConfigurationProperty<?> prop : _properties ) {
				// add property to the config object under its path
				addPropertyUnderPath( config, prop );
			}
			for ( ConfigurableFactory<?> child: _children ) {
				addJSONPathAndReturnLeaf( config, child.getRootPath() );
				child.generateConfigurationObj( config );
			}
		} catch ( JSONException | ConfigurationException e ) {
			LOGGER.warn( "Error occurred while generating configuration JSON", e );
		}
		return config;
	}

}
