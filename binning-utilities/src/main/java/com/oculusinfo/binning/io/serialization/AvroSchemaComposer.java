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
package com.oculusinfo.binning.io.serialization;


import org.apache.avro.Schema;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;



/**
 * Fluent tool for loading up a set of avro schemas and generating a composite
 * from them based on alias resolution
 * 
 * Usage notes: Schemas may not be mutually recursive. Leaf schemas must be fully
 * defined. Schemas must be inserted from leaves up. Alias references must be
 * fully qualified.
 * 
 * Based on http://www.infoq.com/articles/ApacheAvro.
 */
public class AvroSchemaComposer {

	private final Map<String, Schema> schemas    = new HashMap<String, Schema>();
	private Schema                    mostRecent = null;



	/** Return the most recently add item. **/
	public Schema resolved () {
		return mostRecent;
	}

	private String resolveSchema (String sc) {
		String result = sc;
		for (Map.Entry<String, Schema> entry: schemas.entrySet())
			result = replace(result, entry.getKey(), entry.getValue()
			                 .toString());
		return result;
	}

	private static String replace (String str, String pattern, String replace) {
		StringBuffer result = new StringBuffer();
		int e = str.indexOf(pattern, 0);
		if (e < 0) {
			return str;
		}

		result.append(str.substring(0, e - 1));
		result.append(replace);
		result.append(str.substring(e + pattern.length() + 1));
		return result.toString();
	}



	/** Register a new schema with this repository. **/
	public AvroSchemaComposer add (Schema schema) {
		for (String alias: schema.getAliases()) {
			schemas.put(alias, schema);
		}
		schemas.put(schema.getFullName(), schema);
		mostRecent = schema;
		return this;
	}


	/** Load a schema, directly from the string. **/
	public AvroSchemaComposer add (String schemaString) {
		try {
			String completeSchema = resolveSchema(schemaString);
			Schema schema = new Schema.Parser().parse(completeSchema);
			this.add(schema);
		} catch (Exception e) {
			throw new RuntimeException("Error loading schema:" + schemaString,
			                           e);
		}
		return this;
	}

	/** Load a schema from an input stream. **/
	public AvroSchemaComposer add (InputStream in) throws IOException {
		StringBuffer out = new StringBuffer();
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			out.append(new String(b, 0, n));
		}
		return add(out.toString());
	}

	/** Load a schema from a file. **/
	public AvroSchemaComposer add (File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		try {
			return add(fis);
		} catch (Exception e) {
			fis.close();
			throw new RuntimeException(
			                           "Error loading schema " + file.getName(),
			                           e.getCause());
		}
	}


	/** Load a schema from a file (specified as a string). **/
	public AvroSchemaComposer addFile (String file) throws IOException {
		return add(new File(file));
	}


	/** Load a schema via the class-loader resource mechanism. **/
	public AvroSchemaComposer addResource (String path) throws IOException {
		try {
			// Try with the system class loader first and use the current class loader if that
			// fails.
			InputStream stream = AvroSchemaComposer.class.getClassLoader().getResourceAsStream(path);
			if (stream == null) {
				stream = getClass().getResourceAsStream(path);
			}
			add(stream);
		} catch (Exception e) {
			throw new RuntimeException("Error loading schema " + path,
			                           e.getCause());
		}
		return this;
	}
}
