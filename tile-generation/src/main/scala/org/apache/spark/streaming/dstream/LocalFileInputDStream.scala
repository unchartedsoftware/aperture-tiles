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
package org.apache.spark.streaming.dstream

import java.io.ObjectInputStream
import org.apache.spark.rdd.UnionRDD
import java.io.FilenameFilter
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.StreamingContext
import java.io.File
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap
import org.apache.spark.streaming.Time
import java.io.IOException

/**
 * This is a custom DStream so that we can read directly from the local
 * filesystem using java io rather than using hadoop io. It is almost identical
 * to the org.apache.spark.streaming.dstream.FileInputDStream
 */
class LocalFileInputDStream(
	@transient ssc_ : StreamingContext,
	directory: String,
	filter: (File, String) => Boolean = LocalFileInputDStream.defaultFilter,
	newFilesOnly: Boolean = true)
		extends InputDStream[String](ssc_) {
	
	// Latest file mod time seen till any point of time
	private val lastModTimeFiles = new HashSet[String]()
	private var lastModTime = 0L

	@transient private var path_ : File = null
	@transient private var files = new HashMap[Time, Array[File]]
	
	override def start() {
		if (newFilesOnly) {
			lastModTime = graph.zeroTime.milliseconds
		} else {
			lastModTime = 0
		}
		logDebug("LastModTime initialized to " + lastModTime + ", new files only = " + newFilesOnly)
	}
	
	override def stop() { }

	/**
	 * Finds the files that were modified since the last time this method was called and makes
	 * a union RDD out of them. Note that this maintains the list of files that were processed
	 * in the latest modification time in the previous call to this method. This is because the
	 * modification time returned by the FileStatus API seems to return times only at the
	 * granularity of seconds. And new files may have the same modification time as the
	 * latest modification time in the previous call to this method yet was not reported in
	 * the previous call.
	 */
	override def compute(validTime: Time): Option[RDD[String]] = {
		assert(validTime.milliseconds >= lastModTime, "Trying to get new files for really old time [" + validTime + " < " + lastModTime)

		// Create the filter for selecting new files
		val newFilter = new FilenameFilter() {
			// Latest file mod time seen in this round of fetching files and its corresponding files
			var latestModTime = 0L
			val latestModTimeFiles = new HashSet[String]()

			override def accept(dir: File, name: String): Boolean = {
				if (!filter(dir, name)) {  // Reject file if it does not satisfy filter
					logDebug("Rejected by filter " + path)
					return false
				} else {              // Accept file only if
					val file = new File(dir, name)
					val modTime = file.lastModified()
					logDebug("Mod time for " + file + " is " + modTime)
					if (modTime < lastModTime) {
						logDebug("Mod time less than last mod time")
						return false  // If the file was created before the last time it was called
					} else if (modTime == lastModTime && lastModTimeFiles.contains(path.toString)) {
						logDebug("Mod time equal to last mod time, but file considered already")
						return false  // If the file was created exactly as lastModTime but not reported yet
					} else if (modTime > validTime.milliseconds) {
						logDebug("Mod time more than valid time")
						return false  // If the file was created after the time this function call requires
					}
					if (modTime > latestModTime) {
						latestModTime = modTime
						latestModTimeFiles.clear()
						logDebug("Latest mod time updated to " + latestModTime)
					}
					latestModTimeFiles += path.toString
					logDebug("Accepted " + path)
					return true
				}
			}
		}
		
		logDebug("Finding new files at time " + validTime + " for last mod time = " + lastModTime)
		val newFiles = path.listFiles(newFilter)
		logInfo("New files at time " + validTime + ":\n" + newFiles.mkString("\n"))
		println("New files at time " + validTime + ":\n" + newFiles.mkString("\n"))
		if (newFiles.size > 0) {
			// Update the modification time and the files processed for that modification time
			if (lastModTime != newFilter.latestModTime) {
				lastModTime = newFilter.latestModTime
				lastModTimeFiles.clear()
			}
			lastModTimeFiles ++= newFilter.latestModTimeFiles
			logDebug("Last mod time updated to " + lastModTime)
		}
		files += ((validTime, newFiles))
		Some(filesToRDD(newFiles))
	}

	/** Clear the old time-to-files mappings along with old RDDs */
	override def clearMetadata(time: Time) {
		super.clearMetadata(time)
		val oldFiles = files.filter(_._1 <= (time - rememberDuration))
		files --= oldFiles.keys
		logInfo("Cleared " + oldFiles.size + " old files that were older than " +
			        (time - rememberDuration) + ": " + oldFiles.keys.mkString(", "))
		logDebug("Cleared files are:\n" +
			         oldFiles.map(p => (p._1, p._2.mkString(", "))).mkString("\n"))
	}

	/** Generate one RDD from an array of files */
	def filesToRDD(files: Seq[File]): RDD[String] = {
		def getFileRDD(file: File): RDD[String] = {
			val source = scala.io.Source.fromFile(file)
			val lines = source.getLines.toArray
			val rdd = context.sparkContext.makeRDD(lines)
			source.close()
			rdd
		}
		new UnionRDD(
			context.sparkContext,
			files.map(getFileRDD(_))
		)
	}

	private def path: File = {
		if (path_ == null) path_ = new File(directory)
		path_
	}

	@throws(classOf[IOException])
	private def readObject(ois: ObjectInputStream) {
		logDebug(this.getClass().getSimpleName + ".readObject used")
		ois.defaultReadObject()
		generatedRDDs = new HashMap[Time, RDD[String]] ()
		files = new HashMap[Time, Array[File]]
	}

}

object LocalFileInputDStream {
	def defaultFilter(dir: File, name: String): Boolean = !name.startsWith(".") && !name.endsWith("_COPYING_")
}
