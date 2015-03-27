---
section: Docs
subsection: Development
chapter: How-To
topic: Installation
permalink: docs/development/how-to/installation/
layout: submenu
---

Installation and Compilation
============================

The instructions on this page are intended for developers who want to install the Aperture Tiles source code and build their own custom projects.

For quick examples of the capabilities of Aperture Tiles, see the following topics:

- [Demos](../../../../demos/): Access fully functional demonstrations of Aperture Tiles from your web browser.
- [Download](../../../../download): Access a pre-built distribution designed to help you quickly understand the process of creating an Aperture Tiles application. Instructions for using these packages are available on the [Quick Start](../quickstart) page. 

## <a name="prerequisites"></a> Prerequisites ##

This project has the following prerequisites:

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<th scope="col" width="20%">Component</th>
			<th scope="col" width="30%">Required</th>
			<th scope="col" width="50%">Notes</th>
		</thead>
		<tr>
			<td style="vertical-align: text-top" class="description">Operating System</td>
			<td style="vertical-align: text-top" class="description">Linux or OS X</td>
			<td style="vertical-align: text-top" class="description">Windows support available with <a href="https://cygwin.com/">Cygwin</a> or DOS command prompt; precludes the use of Hadoop/HBase.</td>
		</tr>
		<tr>
			<td style="vertical-align: text-top" class="description">Languages</td>
			<td style="vertical-align: text-top" class="description"><a href="http://www.scala-lang.org/">Scala</a> v2.10.3
				<br><a href="http://www.java.com/">Java</a> (JDK v1.7+)
			</td>
			<td style="vertical-align: text-top" class="description"></td>
		</tr>
		<tr>
			<td style="vertical-align: text-top" class="description" rowspan="2">Cluster Computing Framework</td>
			<td style="vertical-align: text-top" class="description"><a href="http://spark.incubator.apache.org//">Apache Spark</a><br>v1.0.0+</td>
			<td style="vertical-align: text-top" class="description">
				You must configure the version of Hadoop with which Spark will be working (if applicable).
				<p>The latest version of Spark may cause class path issues if you compile from source code. We recommend using a pre-built Spark package.</p>
			</td>
		</tr>
		<tr>
			<td style="vertical-align: text-top" class="description"><a href="http://hadoop.apache.org/">Hadoop</a> (<em>optional</em>):
				<ul>
					<li><a href="http://www.cloudera.com/content/cloudera/en/products-and-services/cdh.html">Cloudera</a> v4.6 (<em>recommended)</em></li>
					<li><a href="http://hadoop.apache.org/docs/r1.2.1/index.html">Apache</a></li>
					<li><a href="http://www.mapr.com/products/apache-hadoop">MapR</a></li>
					<li><a href="http://hortonworks.com/">HortonWorks</a></li>
				</ul>
			</td>
			<td style="vertical-align: text-top" class="description">Some Hadoop distributions automatically install Apache Spark. Upgrade to v1.0.0+ if the installation is older.</td>
		</tr>
		<tr>
			<td style="vertical-align: text-top" class="description">Web Server</td>
			<td style="vertical-align: text-top" class="description"><a href="http://tomcat.apache.org/">Apache Tomcat</a> or <a href="http://www.eclipse.org/jetty/">Jetty</a></td>
			<td style="vertical-align: text-top" class="description">The Tile Server and Tile Client, built using the <a href="http://restlet.org/">Restlet</a> web framework, require a servlet-compatible web server.</td>
		</tr>
		<tr>
			<td style="vertical-align: text-top" class="description">Build Automation</td>
			<td style="vertical-align: text-top" class="description"><a href="http://www.eclipse.org/jetty/">Gradle</a><br>(requires <a href="http://nodejs.org/">Node.js</a>)</td>
			<td style="vertical-align: text-top" class="description">
				The Node.js Windows installer has a known issue where it fails to install the following directory. To work around this issue, create the directory, then re-run the Aperture Tiles build
				<p><code>C:\Users\&lt;UserName&gt;\AppData\Roaming\npm</code>
			</td>
		</tr>
	</table>
</div>

<img src="../../../../img/architecture.png" class="screenshot" alt="Aperture Tiles Architecture Diagram"/>

## <a name="source-code"></a> Source Code ##

The Aperture Tiles source code is available on [GitHub](https://github.com/unchartedsoftware/aperture-tiles/tree/master).

### Dependencies ###

Aperture Tiles is dependent on the *master* branch of Aperture JS source code, which you can also download from [GitHub](https://github.com/unchartedsoftware/aperturejs/tree/master).

### <a name="project-structure"></a> Project Structure ###

Aperture Tiles is made up of the following sub-projects:

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<th scope="col" width="25%">Sub-Project</th>
			<th scope="col" width="75%">Description</th>
		</thead>
		<tr>
			<td class="property">math-utilities</td>
			<td class="description">Basic, underlying Java utilities (for angles, linear algebra and statistics) to aid in processing data.</td>
		</tr>
		<tr>
			<td class="property">geometric-utilities</td>
			<td class="description">Advanced math utilities for processing geometry and geographic problems.</td>
		</tr>
		<tr>
			<td class="property">binning-utilities</td>
			<td class="description">Basic substrate of tiling, bin data structures, bin processing and basic bin storage classes.</td>
		</tr>
		<tr>
			<td class="property">factory-utilities</td>
			<td class="description">Factory system to allow construction and configuration of generic objects.</td>
		</tr>
		<tr>
			<td class="property">tile-generation</td>
			<td class="description">Spark-based tools to generate tile pyramids from raw data.</td>
		</tr>
		<tr>
			<td class="property">tile-rendering</td>
			<td class="description">Library that handles turning tile data into useful output formats, generally graphical.</td>
		</tr>
		<tr>
			<td class="property">tile-service</td>
			<td class="description">Web service to serve tiles from tile pyramids to web clients.</td>
		</tr>
		<tr>
			<td class="property">spark-tile-utilities</td>
			<td class="description">Optional services for allowing the tile server to communicate directly with Spark.</td>
		</tr>
		<tr>
			<td class="property">annotation-service</td>
			<td class="description">Optional services for adding annotations to Aperture Tiles visualizations.</td>
		</tr>
		<tr>
			<td class="property">tile-client</td>
			<td class="description">Simple web client to display tiles from tile pyramids.</td>
		</tr>
		<tr>
			<td class="property">tile-packaging</td>
			<td class="description">Packaged assembly of the tile generation service for the <a href="../quickstart/">Quick Start</a> example on this site.</td>
		</tr>
		<tr>
			<td class="property">tile-examples</td>
			<td class="description">Example applications.</td>
		</tr>
		<tr>
			<td class="property">gradle</td>
			<td class="description">Gradle build system support.</td>
		</tr>
		<tr>
			<td class="property">docs</td>
			<td class="description">Source files for the documentation on this website.</td>
		</tr>
	</table>
</div>

### <a name="building-project"></a> Building the Project ###

#### <a name="hbase-version"></a> Specifying Your Hadoop/HBase Version ####

**NOTE**: If you plan to run Apache Spark only in standalone mode on single machine, you can skip this step.

Prior to building the project, you must specify which version of Hadoop and/or HBase you have installed (if applicable): 

1. Review the *Deployment Variants* section of the *aperture-tiles/***build.gradle** file to check for valid settings for your version.
2. If your version is not included, you must build a new case for it. See the comments in the file for more details.

#### Installing Dependencies ####

Install the [Aperture JS](https://github.com/unchartedsoftware/aperturejs/tree/master) project by running the following command in your root Aperture JS directory:
	
```bash
mvn build
```

**NOTE**: This step requires [Apache Maven](http://maven.apache.org/).

#### <a name="compiling"></a> Compiling the Aperture Tiles Projects ####

To build Aperture Tiles, run the following command in your root Aperture Tiles directory:

```bash
gradlew install -PbuildType=<buildType>
``` 

Where:

<div class="props">
	<table class="summaryTable" width="100%">
		<thead>
			<th scope="col" width="25%">Sub-Project</th>
			<th scope="col" width="75%">Description</th>
		</thead>
		<tr>
			<td class="property">buildType</td>
			<td class="description">
				A case in the <strong>build.gradle</strong> file that specifies which versions of Hadoop/HBase and Spark you are using (e.g., <em>cdh5.1.2</em>).
				<p>If you do not specify a <strong>buildType</strong>, the default value (<em>cdh4.6.0</em>) in <em>aperture-tiles/</em><strong>gradle.properties</strong> is used.</p>
			</td>
		</tr>
	</table>
</div>

This will compile all the project components and install .jar files for each project into your local Gradle repository on your build machine.

## <a name="next-steps"></a> Next Steps ##

For details on generating tile sets from raw data, see the [Tile Generation](../generation) topic.