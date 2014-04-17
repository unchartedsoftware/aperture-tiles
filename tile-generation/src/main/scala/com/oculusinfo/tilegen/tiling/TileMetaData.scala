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

package com.oculusinfo.tilegen.tiling



import java.awt.geom.Rectangle2D

import scala.collection.Seq
import scala.collection.Map
import scala.collection.mutable.{Map => MutableMap}
import scala.util.parsing.json.JSON



object TileMetaData {
	def parse (source: String): Option[TileMetaData] = {
		val metaData = if (null == source) None
		else JSON.parseFull(source)

		metaData match {
			case None => {
				println("Could not parse json source.  Source is:")
				println(source)
				None
			}
			case Some(value) => {
				val map = value.asInstanceOf[Map[String, Any]]
				val name = map.getOrElse("name", "unknown").asInstanceOf[String]
				val description = map.getOrElse("description", "unknown").asInstanceOf[String]
				val tileSize = map.getOrElse("tilesize", 256).asInstanceOf[Number].intValue()
				val scheme = map.getOrElse("scheme", "TMS").asInstanceOf[String]
				val projection = map.getOrElse("projection", "unprojected").asInstanceOf[String]
				// Min and max zoom are necessary, should never be absent
				val minZoom = map("minzoom").asInstanceOf[Number].intValue()
				val maxZoom = map("maxzoom").asInstanceOf[Number].intValue()
				val rawBounds = map("bounds").asInstanceOf[List[Any]]
				val bounds = {
					val minx = rawBounds(0).asInstanceOf[Number].doubleValue()
					val miny = rawBounds(1).asInstanceOf[Number].doubleValue()
					val maxx = rawBounds(2).asInstanceOf[Number].doubleValue()
					val maxy = rawBounds(3).asInstanceOf[Number].doubleValue()
					new Rectangle2D.Double(minx, miny, maxx-minx, maxy-miny)
				}


				var metaMap = map.getOrElse("meta", Map[String, Any]()).asInstanceOf[Map[String, Any]]

				var levelMinimums = MutableMap[Int, String]()
				var minMap = metaMap.getOrElse("levelMinimums",
				                               // levelMinFreq is for backwards compatibility
				                               metaMap.getOrElse("levelMinFreq",
				                                                 Map[String, Any]())
				).asInstanceOf[Map[String, Any]]
				minMap.foreach(p =>
					{
						val level = p._1.asInstanceOf[String].toInt
						val entry = p._2.toString

						if (levelMinimums.contains(level)) {
							if (levelMinimums(level) != entry)
								throw new java.text.ParseException("Inconsistent minimum for level "+level+": "
									                                   +levelMinimums(level)+" and "+entry, level)
							else println("Duplicate minimum entry for level "+level+": "+entry)
						}

						levelMinimums += level -> entry
					}
				)

				var levelMaximums = MutableMap[Int, String]()
				var maxMap = metaMap.getOrElse("levelMaximums",
				                               // levelMaxFreq is for backwards compatibility
				                               metaMap.getOrElse("levelMaxFreq",
				                                                 Map[String, Any]())
				).asInstanceOf[Map[String, Any]]
				maxMap.foreach(p =>
					{
						val level = p._1.asInstanceOf[String].toInt
						val entry = p._2.toString

						if (levelMaximums.contains(level)) {
							if (levelMaximums(level) != entry)
								throw new java.text.ParseException("Inconsistent maximum for level "+level+": "
									                                   +levelMaximums(level)+" and "+entry, level)
							else println("Duplicate maximum for level "+level+": "+entry)
						}

						levelMaximums += level -> entry
					}
				)

				Some(new TileMetaData(name, description, tileSize, scheme, projection,
				                      minZoom, maxZoom, bounds,
				                      levelMinimums.toSeq.sortBy(_._1),
				                      levelMaximums.toSeq.sortBy(_._1)))
			}
		}
	}
}

class TileMetaData (val name: String,
                    val description: String,
                    val tileSize: Int,
                    val scheme: String,
                    val projection: String,
                    val minZoom: Int,
                    val maxZoom: Int,
                    val bounds: Rectangle2D,
                    val levelMins: Seq[(Int, String)],
                    val levelMaxes: Seq[(Int, String)]
) {
	def getName: String = name
	def getDescription: String = description

	def addLevel (level: Int, min: String, max: String): TileMetaData = {
		new TileMetaData(name, description, tileSize, scheme, projection,
		                 minZoom min level, maxZoom max level,
		                 bounds,
		                 ((level, min) +: levelMins).sortBy(_._1),
		                 ((level, max) +: levelMaxes).sortBy(_._1)
		)
	}

	override def toString: String = {
		("{\n"
			 + "    \"name\":\""+name+"\",\n"
			 + "    \"description\":\""+description+"\",\n"
			 + "    \"tilesize\":"+tileSize+",\n"
			 + "    \"scheme\":\""+scheme+"\",\n"
			 + "    \"projection\":\""+projection+"\",\n"
			 + "    \"minzoom\":"+minZoom+",\n"
			 + "    \"maxzoom\":"+maxZoom+",\n"
			 + "    \"bounds\": [ %f, %f, %f, %f ],\n"
			 .format(bounds.getMinX(), bounds.getMinY(),
			         bounds.getMaxX(), bounds.getMaxY())
			 + "    \"meta\": {\n"
			 + "        \"levelMinimums\": {\n"
			 + levelMins.map{
				 case (level: Int, min: String) =>
					 "            \""+level+"\": \""+min.replace("\"", "\\\"")+"\""
			 }.mkString("", ",\n", "\n")
			 + "        },\n"
			 + "        \"levelMaximums\": {\n"
			 + levelMaxes.map{
				 case (level: Int, max: String) =>
					 "            \""+level+"\": \""+max.replace("\"", "\\\"")+"\""
			 }.mkString("", ",\n", "\n")
			 + "        }\n"
			 + "    }\n"
			 + "}\n")
	}
}
