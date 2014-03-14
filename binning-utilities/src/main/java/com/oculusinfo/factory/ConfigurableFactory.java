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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import com.oculusinfo.factory.properties.ListProperty;



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
    private static class PropertyValue<PT> {
        PT _value;
        boolean _default;
        PropertyValue () {
            _default = true;
            _value = null;
        }
    }



    private String                                          _name;
    private Class<T>                                        _factoryType;
    private List<String>                                    _rootPath;
    private Map<ConfigurationProperty<?>, PropertyValue<?>> _properties;
    private List<ConfigurableFactory<?>>                    _children;
    private boolean                                         _configured;
    private String                                          _propertyPrefix;



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
        this(null, factoryType, parent, path);
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
        _name = name;
        _factoryType = factoryType;
        List<String> rootPath = new ArrayList<>();
        if (null != parent) {
            rootPath.addAll(parent.getRootPath());
        }
        if (null != path) {
            rootPath.addAll(path);
        }
        _rootPath = Collections.unmodifiableList(rootPath);
        _properties = new HashMap<>();
        _children = new ArrayList<>();
        _configured = false;
        _propertyPrefix = null;
    }

    private <PT> void putPropertyValueObject (ConfigurationProperty<PT> property, PropertyValue<PT> value) {
        _properties.put(property, value);
    }

    // We only ever allow putting PropertyValue objects that match type to their
    // property into the _properties object, so this method encapsulates getting
    // them out with the same matching generic. The warning suppression is
    // allowed because this object is the only one allowed to put them in (in
    // the above put... method. Besides, it's
    // the only one that even knows about them), so since it is guaranteed safe,
    // the extraction also should be.
    @SuppressWarnings({"rawtypes", "unchecked"})
    private <PT> PropertyValue<PT> getPropertyValueObject (ConfigurationProperty<PT> property) {
        return (PropertyValue) _properties.get(property);
    }


    /**
     * Get the root path for configuration information for this factory.
     * 
     * @return A list of strings describing the path to this factory's
     *         configuration information. Guaranteed not to be null.
     */
    public List<String> getRootPath () {
        return _rootPath;
    }

    /**
     * List out all properties directly expected by this factory.
     */
    protected Iterable<ConfigurationProperty<?>> getProperties () {
        return _properties.keySet();
    }

    /**
     * Add a property to the list of properties used by this factory
     * 
     * @param property
     */
    protected <PT> void addProperty (ConfigurationProperty<PT> property) {
        putPropertyValueObject(property, new PropertyValue<PT>());
    }

    /**
     * Indicates if an actual value is recorded for the given property.
     * 
     * @param property The property of interest.
     * @return True if the property is listed and non-default in the factory.
     */
    protected boolean hasPropertyValue (ConfigurationProperty<?> property) {
        return _properties.containsKey(property) && !_properties.get(property)._default;
    }

    /**
     * Get the value read at configuration time for the given property.
     * 
     * The behavior of this function is undefined if called before
     * readConfiguration (either version).
     */
    protected <PT> PT getPropertyValue (ConfigurationProperty<PT> property) {
        PropertyValue<PT> value = getPropertyValueObject(property);
        if (null == value || value._default) {
            return property.getDefaultValue();
        } else {
            return value._value;
        }
    }

    /**
     * Sets the value of a property directly.
     * <em>This should be used very sparingly</em> - the general intention is
     * that properties are read from a configuration file. This method is so
     * that factorys can create "derived" property values, and is essentially an
     * end-run around the normal rules.
     */
    protected <PT> void setPropertyValue (ConfigurationProperty<PT> property, PT value) {
        PropertyValue<PT> valueObj = getPropertyValueObject(property);
        if (null == valueObj) {
            valueObj = new PropertyValue<PT>();
            putPropertyValueObject(property, valueObj);
        }
        valueObj._value = value;
        valueObj._default = false;
    }

    /**
     * Add a child factory, to be used by this factory.
     * 
     * @param child The child to add.
     */
    protected void addChildFactory (ConfigurableFactory<?> child) {
        _children.add(child);
    }

    /**
     * Create the object provided by this factory.
     * @return The object 
     */
    protected abstract T create ();

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
            return goodsType.cast(create());
        } else {
            for (ConfigurableFactory<?> child: _children) {
                GT result = child.produce(goodsType);
                if (null != result) return result;
            }
        }
        return null;
    }



    // /////////////////////////////////////////////////////////////////////////
    // Section: Reading from and writing to a properties file
    //

    /**
     * Initialize needed construction values from a properties list.
     * 
     * @param properties The properties list from which to configure this
     *            factory.
     * @throws ConfigurationException If something goes wrong in configuration,
     *             or if configuration is called twice.
     */
    public void readConfiguration (Properties properties) throws ConfigurationException {
        if (_configured) {
            throw new ConfigurationException("Attempt to configure factory "+this+" twice");
        }

        for (ConfigurationProperty<?> property: _properties.keySet()) {
            readProperty(properties, property);
        }
        for (ConfigurableFactory<?> child: _children) {
            child.readConfiguration(properties);
        }

        _configured = true;
    }

    /*
     * Get the fully qualified name of the given property
     * 
     * @param property The property of interest
     * 
     * @return The fully qualified name, including the path of this factory, and
     * the property itself.
     */
    private String getPropertyName (ConfigurationProperty<?> property) {
        if (null == _propertyPrefix) {
            String prefix = "";
            for (String pathElt: _rootPath)
                prefix = prefix + pathElt + ".";
            _propertyPrefix = prefix;
        }

        return _propertyPrefix + "." + property.getName();
    }

    private <PT> void readProperty (Properties properties, ConfigurationProperty<PT> property) throws ConfigurationException {
        if (property instanceof ListProperty)
            readListProperty(properties, (ListProperty<?>) property);
        else
            readUnaryProperty(properties, property);
    }

    private <PT> void readUnaryProperty (Properties properties, ConfigurationProperty<PT> property) throws ConfigurationException {
        String key = getPropertyName(property);
        if (properties.containsKey(key)) {
            PropertyValue<PT> value = getPropertyValueObject(property);
            value._value = property.unencode(properties.getProperty(getPropertyName(property)));
            value._default = false;
        }
    }

    private <PT> void readListProperty (Properties properties, ListProperty<PT> property) throws ConfigurationException {
        String baseKey = getPropertyName(property);
        List<String> entryPropertyNames = new ArrayList<>();
        int index = 0;
        while (properties.containsKey(baseKey+"."+index)) {
            entryPropertyNames.add(baseKey+"."+index);
           ++index;
        }
        if (0 == index) {
            if (properties.containsKey(baseKey)) {
                // Singleton value; just use the one
                entryPropertyNames.add(baseKey);
                ++index;
            }
        }

        if (index > 0) {
            PropertyValue<List<PT>> valueObj = getPropertyValueObject(property);
            List<PT> value = new ArrayList<>(index);
            for (String entryPropertyName: entryPropertyNames) {
                value.add(property.unencodeEntry(properties.getProperty(entryPropertyName)));
            }
            valueObj._value = value;
            valueObj._default = false;
        }
    }



    // /////////////////////////////////////////////////////////////////////////
    // Section: Reading from and writing to a JSON file
    //

    /**
     * Initialize needed construction values from a properties list.
     * 
     * @param rootNode The root node of all configuration information for this
     *            factory.
     * @throws ConfigurationException If something goes wrong in configuration,
     *             or if configuration is called twice.
     */
    public void readConfiguration (JSONObject rootNode) throws ConfigurationException {
        if (_configured) {
            throw new ConfigurationException("Attempt to configure factory "+this+" twice");
        }

        try {
            JSONObject factoryNode = getConfigurationNode(rootNode);
            for (ConfigurationProperty<?> property: _properties.keySet()) {
                readProperty(factoryNode, property);
            }
            for (ConfigurableFactory<?> child: _children) {
                child.readConfiguration(rootNode);
            }

            _configured = true;
        } catch (JSONException e) {
            throw new ConfigurationException("Error configuring factory "+this.getClass().getName(), e);
        }
    }

    /*
     * Gets the JSON node with all this factory's configuration information
     * 
     * @param rootNode The root JSON node containing all configuration
     * information.
     */
    private JSONObject getConfigurationNode (JSONObject rootNode) throws JSONException {
        JSONObject target = rootNode;
        for (String pathElt: _rootPath) {
            if (target.has(pathElt)) {
                try {
                    target = target.getJSONObject(pathElt);
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

    private <PT> void readProperty (JSONObject factoryNode, ConfigurationProperty<PT> property) throws ConfigurationException {
        try {
            if (null != factoryNode) {
                PropertyValue<PT> valueObj = getPropertyValueObject(property);
                valueObj._value = property.unencodeJSON(new JSONNode(factoryNode, property.getName()));
                valueObj._default = false;
            }
        } catch (JSONException e) {
            // Must not have been there.  Ignore, leaving as default.
        }
    }




    public void writeConfigurationInformation (PrintStream stream) {
        writeConfigurationInformation(stream, "");
    }

    private <PT> void writePropertyValue (PrintStream stream, String prefix, ConfigurationProperty<PT> property) {
        PropertyValue<PT> value = getPropertyValueObject(property);
        if (value._default) {
            stream.println(prefix+property.getName()+": "+property.encode(property.getDefaultValue())+" (DEFAULT)");
        } else {
            stream.println(prefix+property.getName()+": "+property.encode(value._value));
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
    public void writeConfigurationInformation (PrintStream stream, String prefix) {
        stream.println(prefix+"Configuration for "+this.getClass().getSimpleName()+" (node name "+_name+", path: "+mkString(_rootPath, ", ")+"):");
        prefix = prefix + "  ";
        for (ConfigurationProperty<?> property: _properties.keySet()) {
            writePropertyValue(stream, prefix, property);
        }
        stream.println();
        for (ConfigurableFactory<?> child: _children) {
            child.writeConfigurationInformation(stream, prefix);
        }
    }
}
