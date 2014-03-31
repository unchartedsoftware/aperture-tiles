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



import scala.collection.mutable.{Map => MutableMap}

import com.oculusinfo.tilegen.spark.SparkConnector
import com.oculusinfo.tilegen.spark.GeneralSparkConnector


class MissingArgumentException (message: String, cause: Throwable)
		extends Exception(message, cause)
{
	def this (message: String) = this(message, null)
	def this (cause: Throwable) = this(null, cause)
	def this () = this(null, null)
}

abstract class KeyValueArgumentSource {
	def properties: Map[String, String]

	@transient val argDescriptionsMap = MutableMap[String, (String, String, Option[_], Boolean, Boolean)]()

	private var distributed: Boolean = false


	def debug: Unit =
		properties.foreach(pair => println("key: "+pair._1+", value: "+pair._2))


	private def prefixDescLines (description: String, prefix: String): String =
		description.split('\n').mkString(prefix, "\n"+prefix, "")

	def usage: Unit = {
		if (null == argDescriptionsMap || distributed)
			throw new Exception("Attempt to determine usage on worker node")
		argDescriptionsMap.keySet.toList.sorted.foreach(key =>
			{
				val (argType, description, value, defaulted, error) = argDescriptionsMap(key)

				if (error) {
					println(key+"\t["+argType+"] NOT FOUND")
					println(prefixDescLines(description, "\t"))
				} else {
					val defaultedString = if (defaulted) {
						"[default]"
					} else {
						""
					}
					println(key+"\t["+argType+"] ("+value.get+defaultedString+")")
				}
			}
		)
	}



	/**
     * Indicates that this object is used in a distributed computation, or not.
	 * When in a distributed computation, nothing is saved (so that nothing need 
     * be returned to the master object)
     */
	def setDistributedComputation (distributed: Boolean): Unit = {
		this.distributed = distributed
	}


	private def getInternal[T] (key: String,
	                            description: String,
	                            argType: String,
	                            conversion: String => T,
	                            default: Option[T]): Option[T] = {
		var result: Option[T] = default
		var defaulted = true
		try {
			val valOpt = properties.get(key.toLowerCase())
			if (!valOpt.isEmpty) {
				result = Some(conversion(valOpt.get))
				defaulted = false
			}

			if (!distributed && null != argDescriptionsMap)
				argDescriptionsMap(key) = (argType, description, result, defaulted, false)
			result
		} catch {
			case e: Exception => {
				if (!distributed && null != argDescriptionsMap)
					argDescriptionsMap(key) = (argType, description, None, false, true)
				throw e
			}
		}
	}

	/* Get a sequence of values from a single key */
	private def getSequencePropInternal[T] (key: String,
	                                        description: String,
	                                        argType: String,
	                                        conversion: String => T,
	                                        separator: String = ",",
	                                        default: Option[Seq[T]] = Some(Seq[T]())): Option[Seq[T]] = {
		getInternal[Seq[T]](key, description, "Seq["+argType+"]",
		                    _.split(separator).map(conversion).toSeq,
		                    default)
	}

	/* Get a sequence of keys */
	private def getPropSequenceInternal[T] (key: String,
	                                        description: String,
	                                        argType: String,
	                                        conversion: String => T,
	                                        default: Option[Seq[T]] = Some(Seq[T]())): Option[Seq[T]] = {
		val lcKey = key.toLowerCase
		var result: Option[Seq[T]] = default
		var defaulted = true

		try {
			val entries = properties.keySet.filter(_.startsWith(lcKey))
			entries.size match {
				case 0 => {}
				case 1 => {
					result = Some(Seq(conversion(properties(entries.head))))
					defaulted = false;
				}
				case _ => {
					val maxEntry = entries.map(_.substring(key.length+1).toInt).reduce(_ max _)
					result = Some(Range(0, maxEntry+1).map(index =>
						              conversion(properties(lcKey+"."+index))
					              ).toSeq)
					defaulted = false;
				}
			}
			if (!distributed && null != argDescriptionsMap)
				argDescriptionsMap(key) = ("seq["+argType+"]", description, result, defaulted, false)
			result
		} catch {
			case e: Exception => {
				if (!distributed && null != argDescriptionsMap)
					argDescriptionsMap(key) = ("seq["+argType+"]", description, None, false, true)
				throw e
			}
		}
	}





	// ///////////////////////////////////////////////////////////////////////////
	// Simple argument functions
	// Basic functions to pull out basic argument types
	//

	/**
	 * Simple function to get a string property
	 *
	 * @param key
	 *        The text (case-insensitive) of the property for which to look.  
	 * @param description
	 *        A description of this property, for purposes of helping the user 
	 *        to use it correctly
	 * @param default The default value.  If None, and the property is not 
	 *        specified in the properties, an exception is thrown; if Some, 
	 *        this default value will be used if the property is absent, or if 
	 *        there is an error parsing the property.
	 */
	def getString (key: String,
	               description: String,
	               default: Option[String] = None): String =
		getInternal[String](key, description, "string",
		                    _.toString, default).get

	/**
	 * Simple function to get an optional string property.
	 * 
	 * The only functional difference between this and the above getString is 
	 * that this version allows a default of None, which the above does not.
	 */
	def getStringOption (key: String,
	                     description: String,
	                     default: Option[String] = None): Option[String] =
		getInternal[String](key, description, "string",
		                    _.toString, default)

	/**
	 * Simple function to get a list of strings out of a single property,  The 
	 * list uses the specified separator to separate elements.  All arguments 
	 * to this method are as in {@link #getString}, except those listed 
	 * below
	 *
	 * @param separator
	 *        A character that should be used by the user to separate elements of
	 *        the value list
	 */
	def getStringSeq (key: String,
	                  description: String,
	                  separator: String = ",",
	                  default: Option[Seq[String]] = Some(Seq[String]())): Seq[String] =
		getSequencePropInternal[String](key, description, "string", _.toString,
		                                separator, default).get

	/**
	 * Simple function to get a sequence of related properties.  This sequence 
	 * must be in one of two forms:
	 * 
	 * <ol>
	 * <li> A single-element list can just have the stated key, as is </li>
	 * <li> Otherwise, the list must be of properties of the form 
	 * &lt;key&gt;.&lt;index&gt;, where &lt;index&gt; is a number; the list is 
	 * 0-based, and no indices may be skipped.
	 * </ol>
	 * 
	 * All arguments are as in {@link #getString}.
	 */
	def getStringPropSeq (key: String,
	                      description: String,
	                      default: Option[Seq[String]] = Some(Seq[String]())): Seq[String] =
		getPropSequenceInternal(key, description, "string", _.toString, default).get



	/**
	 * Just like {@link #getString}, except it returns an Int
	 */
	def getInt (key: String,
	            description: String,
	            default: Option[Int] = None): Int =
		getInternal[Int](key, description, "int", _.toInt, default).get

	/**
	 * Just like {@link #getStringOption}, except it returns an Int
	 */
	def getIntOption (key: String,
	                  description: String,
	                  default: Option[Int] = None): Option[Int] =
		getInternal[Int](key, description, "int", _.toInt, default)

	/**
	 * Just like {@link #getStringSeq}, except it returns a sequence of Ints
	 */
	def getIntSeq (key: String,
	               description: String,
	               separator: String = ",",
	               default: Option[Seq[Int]] = Some(Seq[Int]())): Seq[Int] =
		getSequencePropInternal[Int](key, description, "int", _.toInt,
		                             separator, default).get

	/**
	 * Just like {@link #getStringPropSeq}, except it returns a sequence of Ints
	 */
	def getIntPropSeq (key: String,
	                   description: String,
	                   default: Option[Seq[Int]] = Some(Seq[Int]())): Seq[Int] =
		getPropSequenceInternal(key, description, "int", _.toInt, default).get



	/**
	 * Just like {@link #getString}, except it returns a Long
	 */
	def getLong (key: String,
	             description: String,
	             default: Option[Long] = None): Long =
		getInternal[Long](key, description, "long", _.toLong, default).get

	/**
	 * Just like {@link #getStringOption}, except it returns a Long
	 */
	def getLongOption (key: String,
	                   description: String,
	                   default: Option[Long] = None): Option[Long] =
		getInternal[Long](key, description, "long", _.toLong, default)

	/**
	 * Just like {@link #getStringSeq}, except it returns a sequence of Longs
	 */
	def getLongSeq (key: String,
	                description: String,
	                separator: String = ",",
	                default: Option[Seq[Long]] = Some(Seq[Long]())): Seq[Long] =
		getSequencePropInternal[Long](key, description, "long", _.toLong,
		                              separator, default).get

	/**
	 * Just like {@link #getStringPropSeq}, except it returns a sequence of Longs
	 */
	def getLongPropSeq (key: String,
	                    description: String,
	                    default: Option[Seq[Long]] = Some(Seq[Long]())): Seq[Long] =
		getPropSequenceInternal(key, description, "long", _.toLong, default).get



	/**
	 * Just like {@link #getString}, except it returns a Double
	 */
	def getDouble (key: String,
	               description: String,
	               default: Option[Double] = None): Double =
		getInternal[Double](key, description, "double", _.toDouble, default).get

	/**
	 * Just like {@link #getStringOption}, except it returns a Double
	 */
	def getDoubleOption (key: String,
	                     description: String,
	                     default: Option[Double] = None): Option[Double] =
		getInternal[Double](key, description, "double", _.toDouble, default)

	/**
	 * Just like {@link #getStringSeq}, except it returns a sequence of Doubles
	 */
	def getDoubleSeq (key: String,
	                  description: String,
	                  separator: String = ",",
	                  default: Option[Seq[Double]] = Some(Seq[Double]())): Seq[Double] =
		getSequencePropInternal[Double](key, description, "double", _.toDouble,
		                                separator, default).get

	/**
	 * Just like {@link #getStringPropSeq}, except it returns a sequence of Doubles
	 */
	def getDoublePropSeq (key: String,
	                      description: String,
	                      default: Option[Seq[Double]] = Some(Seq[Double]())): Seq[Double] =
		getPropSequenceInternal(key, description, "double", _.toDouble, default).get



	/**
	 * Just like {@link #getString}, except it returns a Boolean
	 */
	def getBoolean (key: String,
	                description: String,
	                default: Option[Boolean] = None): Boolean =
		getInternal[Boolean](key, description, "boolean", toBoolean(_), default).get

	/**
	 * Just like {@link #getStringOption}, except it returns a Boolean
	 */
	def getBooleanOption (key: String,
	                      description: String,
	                      default: Option[Boolean] = None): Option[Boolean] =
		getInternal[Boolean](key, description, "boolean", toBoolean(_), default)

	/**
	 * Just like {@link #getStringSeq}, except it returns a sequence of Booleans
	 */
	def getBooleanSeq (key: String,
	                   description: String,
	                   separator: String = ",",
	                   default: Option[Seq[Boolean]] = Some(Seq[Boolean]())): Seq[Boolean] =
		getSequencePropInternal[Boolean](key, description, "boolean", toBoolean(_),
		                                 separator, default).get

	/**
	 * Just like {@link #getStringPropSeq}, except it returns a sequence of Booleans
	 */
	def getBooleanPropSeq (key: String,
	                       description: String,
	                       default: Option[Seq[Boolean]] = Some(Seq[Boolean]())): Seq[Boolean] =
		getPropSequenceInternal(key, description, "boolean", toBoolean(_), default).get


	private def toBoolean (value: String): Boolean = {
		val lowerValue = value.toLowerCase.trim
		if (lowerValue == "true".substring(0, lowerValue.length min "true".length)) {
			true
		} else if (lowerValue == "yes".substring(0, lowerValue.length min "yes".length)) {
			true
		} else if (lowerValue.map(c => '-' == c || ('0' <= c && c <= '9')).reduce(_ && _)) {
			0 != lowerValue.toInt
		} else {
			false
		}
	}



	/**
	 * Just like {@link #getString}, except it returns an arbitrary type
	 */
	def getTypedValue[T] (key: String,
	                      description: String,
	                      typeName: String,
	                      conversion: String => T,
	                      default: Option[T] = None): T =
		getInternal[T](key, description, typeName, conversion, default).get

	/**
	 * Just like {@link #getStringOption}, except it returns an arbitrary type
	 */
	def getTypedValueOption[T] (key: String,
	                            description: String,
	                            typeName: String,
	                            conversion: String => T,
	                            default: Option[T] = None): Option[T] =
		getInternal[T](key, description, typeName, conversion, default)

	/**
	 * Just like {@link #getStringSeq}, except it returns a sequence of an arbitrary type
	 */
	def getTypedValueSeq[T] (key: String,
	                         description: String,
	                         typeName: String,
	                         conversion: String => T,
	                         separator: String = ",",
	                         default: Option[Seq[T]] = Some(Seq[T]())): Seq[T] =
		getSequencePropInternal[T](key, description, typeName, conversion,
		                           separator, default).get

	/**
	 * Just like {@link #getStringPropSeq}, except it returns a sequence of Booleans
	 */
	def getTypedPropSeq[T] (key: String,
	                        description: String,
	                        typeName: String,
	                        conversion: String => T,
	                        default: Option[Seq[T]] = Some(Seq[T]())): Seq[T] =
		getPropSequenceInternal[T](key, description, typeName, conversion, default).get





	/**
	 * This gets a unique list of all the subproperty names for the given property. Each name is
	 * only the direct subproperty in case there's more sub-subproperties.
	 */
	def getSeqPropertyNames (property: String): Seq[String] = {
		val entries = properties.keySet.filter(_.startsWith(property))
		entries.size match {
			case 0 => Seq[String]()
			case _ => entries.map(_.substring(property.length + 1).split("\\.")(0)).toSeq.distinct
		}
	}



	// ///////////////////////////////////////////////////////////////////////////
	// Complex argument functions
	// These functions standardize some arguments across applications
	//
	def getSparkConnector (jars: Seq[Object] = SparkConnector.getDefaultLibrariesFromMaven): SparkConnector = {
		val sparkArgs = properties.filter(kv =>
			{
				kv._1.startsWith("spark") && "spark" != kv._1 && "sparkhome" != kv._1
			}
		)
		new GeneralSparkConnector(
			getString("spark",
			          "Spark master location (default is \"local\")",
			          Some("local")),
			getString("sparkhome",
			          "Spark home location (defaults to ${SPARK_HOME}",
			          Some(System.getenv("SPARK_HOME"))),
			Some(getString("user",
			               "spark user name (defaults to login name)",
			               Some(System.getProperty("user.name")))),
			jars,
			sparkArgs
		)
	}
}
