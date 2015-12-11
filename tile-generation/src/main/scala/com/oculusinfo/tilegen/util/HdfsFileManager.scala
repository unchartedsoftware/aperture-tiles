/*
 * Copyright © 2013-2015 Uncharted Software Inc.
 *
 * Property of Uncharted™, formerly Oculus Info Inc.
 * http://uncharted.software/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tilegen.util

import java.io.{OutputStreamWriter, BufferedWriter}
import java.net.URI

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{Path, FileSystem}
import org.apache.hadoop.io.compress.{GzipCodec, CompressionCodecFactory, CompressionCodec}
import grizzled.slf4j.Logger


/**
 * Manages HDFS "sessions"
 *
 * A caller may access many files in arbitrary order within a session.
 * We want to keep all of the files open until the session is complete
 * for efficiency.
 */
object HdfsFileManager {

	val sessionMap = new java.util.HashMap[Int, HdfsSession]()
	var nextId = 0

	/**
	 * Get a reference to an HDFS session with the specified session ID, if specified.
	 * Create a new HDFS session, if not specified
	 * @param sessionId The session ID
	 * @param hdfsUri The URI to HDFS
	 * @return The session
	 */
	def getSession(sessionId: Option[Int], hdfsUri: String) : HdfsSession = {
		// Get the session if it already exists
		var newSessionId = -1
		var session : HdfsSession = null
		this.synchronized {
			if (sessionId != None && sessionMap.containsKey(sessionId.get))
				session = sessionMap.get(sessionId.get)
			else {
				newSessionId = nextId
				nextId += 1
			}
		}

		// If the session doesn't exist, create it and put it in the session map
		if (newSessionId != -1) {
			session = new HdfsSession(newSessionId, hdfsUri)
			this.synchronized {
				sessionMap.put(newSessionId, session)
			}
		}

		// Return the session
		session
	}

	/**
	 * End the session and close all of the related files for that session
	 * @param session The session to close
	 */
	def endSession(session: HdfsSession): Unit = {
		if (session == null)
			return
		this.synchronized {
			sessionMap.remove(session.id)
		}
		session.close()
	}
}

/**
 * An HDFS "session"
 *
 * A caller may access many files in arbitrary order within a session.
 * We want to keep all of the files open until the session is complete
 * for efficiency.
 *
 * @param hdfsUri HDFS location
 */
class HdfsSession(val id: Integer, hdfsUri: String) {

	private val logger = Logger(this.getClass.getName)
	private var fileMap = new java.util.HashMap[String, HdfsFile]()

	// Connect to HDFS file system
	logger.debug("Attempting to connect to HDFS at URI: " + hdfsUri)
	val uri = new URI(hdfsUri)
	val configuration = new Configuration()
	val hdfs = FileSystem.get(uri, configuration)
	val codecFactory = new CompressionCodecFactory(configuration)
	val codec = codecFactory.getCodecByClassName(classOf[GzipCodec].getName)
	logger.debug("Successfully connected to HDFS at URI: " + hdfsUri)

	/**
	 * Get a reference to a file object with the given file name, if one exists.
	 * Create and return a new file object, if one doesn't exist.
	 * @param fileName The name of the file to get or create
	 * @return The file
	 */
	def getFile(fileName: String) : HdfsFile = {
		// Get the file, if it exists
		var file : HdfsFile = null
		this.synchronized {
			if (fileMap == null)
				throw new Exception("Session is closed.")
			if (fileMap.containsKey(fileName))
				file = fileMap.get(fileName)
		}

		// If the file doesn't exist, create it and put it in the map
		if (file == null) {
			file = new HdfsFile(fileName, codec, hdfs)
			this.synchronized {
				if (fileMap == null)
					throw new Exception("Session is closed.")
				fileMap.put(fileName, file)
			}
		}

		// Return the file
		file
	}

	/**
	 * Close the session
	 */
	def close() : Unit = {
		// Get a reference to the existing file map and set the one in the session to be null
		// so that no one else can operate on the session
		var localFileMap: java.util.HashMap[String, HdfsFile] = null
		this.synchronized {
			localFileMap = fileMap
			fileMap = null
		}

		// Close all the files in the session, if it's still open
		if (localFileMap != null) {
			import scala.collection.JavaConversions._
			for ((fileName, file) <- localFileMap) {
				file.writer.close()
			}
		}
	}
}

/**
 * Class that encapsulates the utilities necessary to operate on an HDFS file.
 *
 * @param fileName Name of the file
 * @param hdfs The HDFS file system
 */
class HdfsFile(fileName: String, codec: CompressionCodec, hdfs: FileSystem) {

	private val logger = Logger(this.getClass.getName)

	private val pathObj = new Path(fileName)
	private val dataOs =
		if (hdfs.exists(pathObj)) {
			logger.debug("File: " + fileName + " already exists. Append to it.")
			hdfs.append(pathObj)
		}
		else {
			logger.debug("File: " + fileName + " does not exist. Create a new file")
			hdfs.create(pathObj, false /*throw if file exists*/)
		}
	private val os = codec.createOutputStream(dataOs)

	// The buffered writer to expose
	val writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"))
}
