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



import java.util.Properties

import scala.collection.JavaConverters._



/**
 * This class wraps a Java properties object, so as to make it easy for users
 * to grab properties cast as non-string primitives and lists thereof.
 */
object PropertiesWrapper {
  private val VALID_TRUE_VALUES = Set("y", "ye", "yes", "t", "tr", "tru", "true")
  private def getBooleanValue (value: String): Boolean =
    VALID_TRUE_VALUES.contains(value.toLowerCase)

  def debugProperties (properties: Properties): Unit = {
    properties.stringPropertyNames.asScala.foreach(property =>
      println("Property \""+property+"\" : \""+properties.getProperty(property.toString)+"\"")
    )
  }
}

class PropertiesWrapper (properties: Properties) extends Serializable {
  import PropertiesWrapper._

  def getProperty (property: String, default: String): String = {
    val value = properties.getProperty(property)
    if (null == value) default else value
  }

  def getOptionProperty (property: String): Option[String] = {
    val value = properties.getProperty(property)
    if (null == value) None else Some(value)
  }

  def getSeqProperty (property: String): Seq[String] = {
    val entries = properties.stringPropertyNames.asScala.filter(_.startsWith(property))
    entries.size match {
      case 0 => Seq[String]()
      case 1 => Seq(getOptionProperty(entries.head).get)
      case _ => {
	val maxEntry = entries.map(_.substring(property.length+1).toInt).reduce(_ max _)
	Range(0, maxEntry+1).map(index => getOptionProperty(property+"."+index).get).toSeq
      }
    }
  }

  /**
   * Gets a map of property names -> values for any properties that start with the given property string.
   * The names in the map are the remaining property name after the given property base name is cut off,
   * so the resulting name may still contain sub properties.
   */
  def getSeqPropertyMap(property: String): Map[String, String] = {
    val entries = properties.stringPropertyNames.asScala.filter(_.startsWith(property))
    entries.size match {
      case 0 => Map[String, String]()
      case _ => {
        entries.map{entry =>
          val name = entry.substring(property.length + 1)
          val value = getOptionProperty(entry).get
          name -> value
        }.toMap
      }
    }
  }
  
  /**
   * This gets a unique list of all the subproperty names for the given property. Each name is
   * only the direct subproperty in case there's more sub-subproperties.
   */
  def getSeqPropertyNames(property: String): Seq[String] = {
    val entries = properties.stringPropertyNames.asScala.filter(_.startsWith(property))
    entries.size match {
      case 0 => Seq[String]()
      case _ => entries.map(_.substring(property.length + 1).split("\\.")(0)).toSeq.distinct
    }
    
  }

  def getTypedOptionProperty[T] (property: String, conversion: String => T): Option[T] =
    getOptionProperty(property).map(conversion)

  def getTypedProperty[T] (property: String, default: T, conversion: String => T): T =
    getTypedOptionProperty(property, conversion).getOrElse(default)

  def getTypedSeqProperty[T] (property: String, conversion: String => T): Seq[T] =
    getSeqProperty(property).map(conversion)




  def getIntProperty (property: String, default: Int): Int =
    getTypedProperty(property, default, _.toInt)

  def getIntOptionProperty (property: String): Option[Int] =
    getTypedOptionProperty(property, _.toInt)

  def getIntSeqProperty (property: String): Seq[Int] =
    getTypedSeqProperty(property, _.toInt)



  def getDoubleProperty (property: String, default: Double): Double =
    getTypedProperty(property, default, _.toDouble)

  def getDoubleOptionProperty (property: String): Option[Double] =
    getTypedOptionProperty(property, _.toDouble)

  def getDoubleSeqProperty (property: String): Seq[Double] =
    getTypedSeqProperty(property, _.toDouble)




  def getBooleanProperty (property: String, default: Boolean): Boolean =
    getTypedProperty(property, default, getBooleanValue(_))

  def getBooleanOptionProperty (property: String): Option[Boolean] =
    getTypedOptionProperty(property, getBooleanValue(_))

  def getBooleanSeqProperty (property: String): Seq[Boolean] =
    getTypedSeqProperty(property, getBooleanValue(_))




  def debug: Unit = PropertiesWrapper.debugProperties(properties)
}
