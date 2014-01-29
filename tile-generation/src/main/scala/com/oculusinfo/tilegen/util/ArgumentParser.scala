/**
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

class ArgumentParser (args: Array[String]) {
  val argsMap = args.sliding(2).filter(_(0).startsWith("-")).map(p =>
    (p(0).toLowerCase() -> p(1))).toMap
  val argDescriptionsMap = MutableMap[String, (String, String)]()



  def usage: Unit = {
    argDescriptionsMap.keySet.toList.sorted.foreach(key => {
      val (argType, description) = argDescriptionsMap(key)
      println(key+"\t["+argType+"]")
      println(prefixDescLines(description, "\t"))
    })
  }



  private def prefixDescLines (description: String, prefix: String): String =
    description.split('\n').mkString(prefix, "\n"+prefix, "")

  private def getArgumentInternal[T] (key: String,
                                      description: String,
                                      argType: String,
                                      conversion: String => T,
                                      default: Option[T]): T = {
    argDescriptionsMap += key -> (argType, description)
    val valOpt = argsMap.get("-"+key.toLowerCase())
    if (valOpt.isEmpty) {
      if (default.isEmpty) {
        val message = "Missing argument -"+key+"\n"+prefixDescLines(description, "\t")
        throw new MissingArgumentException(message)
      }
      default.get
    } else {
      try {
        conversion(valOpt.get)
      } catch {
        case e: Exception => {
          println("Bad argument -"+key+": got "+valOpt.get+", expected a "+argType)
          if (default.isEmpty) {
            val message = ("Bad argument value for -"+key+" - got "+valOpt.get
                           +", expected a "+argType+"\n"
                           +prefixDescLines(description, "\t"))
            throw new MissingArgumentException(message)
          } else {
            println("\tAssuming default value of "+default.get)
            default.get
          }
        }
      }
    }
  }



  // ///////////////////////////////////////////////////////////////////////////
  // Simple argument functions
  // Basic functions to pull out basic argument types
  //

  /**
   * Simple function to get a string argument from the argument list.
   *
   * @param key
   *        The text (case-insensitive) of the key to look for in the argument
   *        list.  In use, the key should be prefaced by a "-"; as an argument
   *        to this function, it should not.
   * @param description
   *        A description of this argument, for purposes of helping the user to
   *        use it correctly
   * @param default The default value.  If None, argument is not specified in
   *        the argument list, an exception is thrown; if Some, this default
   *        value will be used if the argument is absent, or if there is an
   *        error parsing the argument.
   */
  def getStringArgument (key: String,
                         description: String,
                         default: Option[String] = None): String =
    getArgumentInternal[String](key, description, "string",
                                _.toString, default)

  /**
   * Simple function to get a integer-valued argument from the argument list.
   *
   * @param key
   *        The text (case-insensitive) of the key to look for in the argument
   *        list.  In use, the key should be prefaced by a "-"; as an argument
   *        to this function, it should not.
   * @param description
   *        A description of this argument, for purposes of helping the user to
   *        use it correctly
   * @param default The default value.  If None, argument is not specified in
   *        the argument list, an exception is thrown; if Some, this default
   *        value will be used if the argument is absent, or if there is an
   *        error parsing the argument.
   */
  def getIntArgument (key: String,
                      description: String,
                      default: Option[Int] = None): Int =
    getArgumentInternal[Int](key, description, "int", _.toInt, default)

  /**
   * Simple function to get a integer-valued argument from the argument list.
   *
   * @param key
   *        The text (case-insensitive) of the key to look for in the argument
   *        list.  In use, the key should be prefaced by a "-"; as an argument
   *        to this function, it should not.
   * @param description
   *        A description of this argument, for purposes of helping the user to
   *        use it correctly
   * @param default The default value.  If None, argument is not specified in
   *        the argument list, an exception is thrown; if Some, this default
   *        value will be used if the argument is absent, or if there is an
   *        error parsing the argument.
   */
  def getLongArgument (key: String,
                       description: String,
                       default: Option[Long] = None): Long =
    getArgumentInternal[Long](key, description, "int", _.toLong, default)

  /**
   * Simple function to get a double-valued argument from the argument list.
   *
   * @param key
   *        The text (case-insensitive) of the key to look for in the argument
   *        list.  In use, the key should be prefaced by a "-"; as an argument
   *        to this function, it should not.
   * @param description
   *        A description of this argument, for purposes of helping the user to
   *        use it correctly
   * @param default The default value.  If None, argument is not specified in
   *        the argument list, an exception is thrown; if Some, this default
   *        value will be used if the argument is absent, or if there is an
   *        error parsing the argument.
   */
  def getDoubleArgument (key: String,
                         description: String,
                         default: Option[Double] = None): Double =
    getArgumentInternal[Double](key, description, "dobule", _.toDouble, default)

  /**
   * Simple function to get an argument from the argument list whose value is a
   * sequence of integers.  The list must contain no whitespace, and uses the
   * specified separator to separate elements.
   *
   * @param key
   *        The text (case-insensitive) of the key to look for in the argument
   *        list.  In use, the key should be prefaced by a "-"; as an argument
   *        to this function, it should not.
   * @param description
   *        A description of this argument, for purposes of helping the user to
   *        use it correctly
   * @param separator
   *        A character that should be used by the user to separate elements of
   *        their value list
   * @param default The default value.  If None, argument is not specified in
   *        the argument list, an exception is thrown; if Some, this default
   *        value will be used if the argument is absent, or if there is an
   *        error parsing the argument.
   */
  def getIntSeqArgument (key: String,
                         description: String,
                         separator: Char = ',',
                         default: Option[Seq[Int]] = None): Seq[Int] =
    getArgumentInternal[Seq[Int]](key, description, "seq[int]",
                        _.split(separator).map(_.toInt).toSeq,
                        default)
  /**
   * Simple function to get an argument from the argument list whose value is a
   * list of strings.  The list uses the specified separator to separate
   * elements.
   *
   * @param key
   *        The text (case-insensitive) of the key to look for in the argument
   *        list.  In use, the key should be prefaced by a "-"; as an argument
   *        to this function, it should not.
   * @param description
   *        A description of this argument, for purposes of helping the user to
   *        use it correctly
   * @param separator
   *        A character that should be used by the user to separate elements of
   *        their value list
   * @param default The default value.  If None, argument is not specified in
   *        the argument list, an exception is thrown; if Some, this default
   *        value will be used if the argument is absent, or if there is an
   *        error parsing the argument.
   */
  def getStringSeqArgument (key: String,
                            description: String,
                            separator: Char = ',',
                            default: Option[Seq[String]] = None): Seq[String] =
    getArgumentInternal[Seq[String]](key, description, "seq[string]",
                                     _.split(separator).map(_.toString).toSeq,
                                     default)



  // ///////////////////////////////////////////////////////////////////////////
  // Complex argument functions
  // These functions standardize some arguments across applications
  //
  def getSparkConnector: SparkConnector = 
    new GeneralSparkConnector(
      getStringArgument("spark",
                        "Spark master location (default is \"local\")",
                        Some("local")),
      getStringArgument("sparkhome",
                        "Spark home location (defaults to ${SPARK_HOME}",
                        Some(System.getenv("SPARK_HOME"))),
      Some(getStringArgument("user",
                             "spark user name (defaults to login name)",
                             Some(System.getProperty("user.name")))))
}
