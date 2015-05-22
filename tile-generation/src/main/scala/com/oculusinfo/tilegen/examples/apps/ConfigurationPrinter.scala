package com.oculusinfo.tilegen.examples.apps

import java.io.{File, PrintWriter}
import java.util.{Set => JavaSet}
import java.util.{Map => JavaMap}
import java.util.{Iterator => JavaIterator}
import java.lang.{Iterable => JavaIterable}
import com.oculusinfo.tilegen.util.ArgumentParser

import scala.collection.JavaConversions._



/**
 * Created by nkronenfeld on 5/21/2015.
 */
object ConfigurationPrinter {
	private def semToPropArray (input: JavaIterable[JavaMap.Entry[String, String]]): Array[(String, String)] =
		input.toArray.map(entry => (entry.getKey, entry.getValue))

	private def semToPropArray (input: JavaIterator[JavaMap.Entry[String, String]]): Array[(String, String)] =
		input.toArray.map(entry => (entry.getKey, entry.getValue))

	private def aemToPropArray (input: JavaIterable[JavaMap.Entry[AnyRef, AnyRef]]): Array[(String, String)] =
		input.toArray.map(entry => (entry.getKey.toString, entry.getValue.toString))


	private def printProperties (props: Array[(String, String)], name: String): Unit = {
		println
		println
		println(name+" configuration:")
		props.sortBy(_._1.toString).foreach{case (key, value) =>
			println("\t\"%s\": \"%s\"".format(key, value))
		}
	}

	private def saveProperties (props: Array[(String, String)], file: String, name: String): Unit = {
		val writer = new PrintWriter((new File(file)))
		writer.println("""<?xml version="1.0" encoding="UTF-8"?>""")
		writer.println("<!-- Saved run-time configuration of "+name+" -->")
		writer.println("<configuration>")
		props.foreach{case (key, value) =>
			writer.println("\t<property>")
			writer.println("\t\t<name>"+key+"</name>")
			writer.println("\t\t<value>"+value+"</value>")
			writer.println("\t<property>")
		}
		writer.println("</configuration>")
		writer.flush()
		writer.close()
	}

	def main(args: Array[String]): Unit = {
		val argParser = new ArgumentParser(args)

		argParser.getStringOption("env", "Name of a file into which to save environment configuration options")
			.map { envFile =>
			saveProperties(semToPropArray(System.getenv.entrySet()), envFile, "Shell environment")
		}
		argParser.getStringOption("prop", "Name of a file into which to save java configuration options")
			.map { propFile =>
			saveProperties(aemToPropArray(System.getProperties.entrySet()), propFile, "Java environment")
		}

		val sparkFileOpt = argParser.getStringOption("spark", "Name of a file into which to save spark configuration options")
		val hadoopFileOpt = argParser.getStringOption("hadoop", "Name of a file into which to save hadoop configuration options")
		if (sparkFileOpt.isDefined || hadoopFileOpt.isDefined) {
			val sc = argParser.getSparkConnector().createContext(Some("Configuration Test"))
			sparkFileOpt.map { sparkFile =>
				saveProperties(sc.getConf.getAll, sparkFile, "Spark configuration")
			}
			hadoopFileOpt.map { hadoopFile =>
				saveProperties(semToPropArray(sc.hadoopConfiguration.iterator()), hadoopFile, "Hadoop configuration")
			}
		}
	}
}
