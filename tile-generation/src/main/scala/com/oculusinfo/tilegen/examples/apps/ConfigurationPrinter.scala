package com.oculusinfo.tilegen.examples.apps

import com.oculusinfo.tilegen.util.ArgumentParser

import scala.collection.JavaConversions._



/**
 * Created by nkronenfeld on 5/21/2015.
 */
object ConfigurationPrinter {
	def main(args: Array[String]): Unit = {
		val argParser = new ArgumentParser(args)
		val sc = argParser.getSparkConnector().createContext(Some("Configuration Test"))

		println
		println
		println("Spark Configuration:")
		sc.getConf.getAll.sortBy(_._1).foreach { case (key, value) =>
			println("\t\"%s\": \"%s\"".format(key, value))
		}

		println
		println
		println("Hadoop configuration:")
		sc.hadoopConfiguration.iterator().toList.map(entry => (entry.getKey, entry.getValue)).sortBy(_._1).foreach { case (key, value) =>
			println("\t\"%s\": \"%s\"".format(key, value))
		}

		println
		println
		println("Environment variables:")
		System.getProperties.entrySet().toList.map(entry => (entry.getKey, entry.getValue)).sortBy(_._1.toString).foreach{case (key, value) =>
			println("\t\"%s\": \"%s\"".format(key, value))
		}
	}
}
