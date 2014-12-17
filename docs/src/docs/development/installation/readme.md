---
section: Docs
subtitle: Development
chapter: Installation
permalink: docs/development/installation/index.html
layout: submenu
---

Installation and Compilation
============================

The instructions on this page are intended for developers who want to install the Aperture Tiles source code and build their own custom projects.

For quick examples of the capabilities of Aperture Tiles:

- See the [Demos](../../../demos/) page to access fully functional demonstrations of Aperture Tiles from your web browser.
- See the [Download](../../../download) page to access a pre-built distribution designed to help you quickly get started using Aperture Tiles and to understand the high-level process of creating an Aperture Tiles application. Instructions for using the packages to project a Julia set fractal on an X/Y plot are available on the [Quick Start](../quickstart) page. 

## <a name="prerequisites"></a> Prerequisites ##

This project has the following prerequisites:

- **Operating System**: Linux or OS X. Windows support is available through [Cygwin](https://cygwin.com/) or the DOS command prompt, but precludes the use of Hadoop/HBase.
- **Languages**: [Scala](http://www.scala-lang.org/) version 2.10.3 and [Java](http://www.java.com/) (JDK version 1.7+).
- **Cluster Computing**: Facilitates large tiling jobs. If you only run small jobs (datasets that fit in a single machine's memory) or if wait times are not an issue, you can omit Hadoop/HDFS/HBase and run Spark on a single node on your local file system.
	-   [Apache Spark](http://spark.incubator.apache.org/) version 1.0.0 or greater. You will need to configure the version of Hadoop with which Spark will be working (if applicable). <p class="list-paragraph">NOTE: In the latest version of Spark, class path issues may arise if you compile Spark from the source code. For this reason, we recommend using one of the pre-built Spark packages.</p>
	-   [Hadoop/HDFS/HBase](http://hadoop.apache.org/) (*Optional*) - Choose your preferred flavor:
		- [Cloudera](http://www.cloudera.com/content/cloudera/en/products/cdh.html) (*Recommended*) version 4.6 
		- [Apache](http://hadoop.apache.org/docs/r1.2.1/index.html)
		- [MapR](http://www.mapr.com/products/apache-hadoop)
		- [HortonWorks](http://hortonworks.com/)<p class="list-paragraph">NOTE: Some cluster computing software may automatically install Apache Spark. If the installed version is older than 1.0.0, you must upgrade to 1.0.0 or greater.</p>
-  **Web Server**: The Tile Server and Tile Client, built using the [Restlet](http://restlet.org/) web framework, require a servlet-compatible web server. Choose your preferred implementation:
	- [Apache Tomcat](http://tomcat.apache.org/)
	- [Jetty](http://www.eclipse.org/jetty/)
-   **Build Automation**: Two tools are required to build Aperture Tiles and its dependency, [Aperture JS](http://aperturejs.com). Ensure that each is configured properly on your system. 
	- *Aperture JS*: [Apache Maven](http://maven.apache.org/) version 3.1.0 (other 3.x versions may work)
	- *Aperture Tiles*: [Gradle](http://www.gradle.org/), which also requires [Node.js](http://nodejs.org/)<p class="list-paragraph">NOTE: The Windows installer for Node.js v0.10.33 has a known issue in that fails to install the following directory, which in turn will cause your Aperture Tiles build to fail. To work around this issue, simply create the directory, then re-run the Aperture Tiles build</p><p><code>C:\Users\<UserName>\AppData\Roaming\npm</code></p>

<img src="../../../img/architecture.png" class="screenshot" alt="Aperture Tiles Architecture Diagram"/>

## <a name="source-code"></a> Source Code ##

The Aperture Tiles source code is available on [GitHub](https://github.com/oculusinfo/aperture-tiles/tree/master). Aperture Tiles is dependent on the *master* branch of Aperture JS source code, which you can also download from [GitHub](https://github.com/oculusinfo/aperturejs/tree/master).

### <a name="project-structure"></a> Project Structure ###

Aperture Tiles is made up of the following sub-projects:

<div class="details props">
	<div class="innerProps">
		<ul class="methodDetail" id="MethodDetail">
			<dl class="detailList params">
				<dt>math-utilities</dt>
				<dd>Basic, underlying Java utilities (for angles, linear algebra and statistics) to aid in processing data.</dd>
				
				<dt>geometric-utilities</dt>
				<dd>Advanced math utilities for processing geometry and geographic problems.</dd>
				
				<dt>binning-utilities</dt>
				<dd>Basic substrate of tiling, bin data structures, bin processing and basic bin storage classes.</dd>
				
				<dt>tile-generation</dt>
				<dd>Spark-based tools to generate tile pyramids from raw data.</dd>
				
				<dt>tile-service</dt>
				<dd>Web service to serve tiles from tile pyramids to web clients.</dd>
				
				<dt>spark-tile-utilities</dt>
				<dd>Optional services for allowing the tile server to communicate directly with Spark.</dd>
				
				<dt>annotation-service</dt>
				<dd>Optional services for adding annotations to Aperture Tiles visualizations.</dd>
				
				<dt>tile-client</dt>
				<dd>Simple web client to display tiles from tile pyramids.</dd>
				
				<dt>tile-packaging</dt>
				<dd>Packaged assembly of the tile generation service for the <a href="../quickstart/">Quick Start</a> example on this site.</dd>
				
				<dt>tile-client-template</dt>
				<dd>Starter template for creating a Tile Client and Server application.</dd>
				
				<dt>tile-examples</dt>
				<dd>Example applications.</dd>
				
				<dt>gradle</dt>
				<dd>Gradle build system support.</dd>
				
				<dt>docs</dt>
				<dd>Source files for the documentation on this website.</dd>
			</dl>
		</ul>
	</div>
</div>
 
### <a name="building-project"></a> Building the Project ###

#### <a name="hbase-version"></a> Specifying Your Hadoop/HBase Version ####

NOTE: If you plan to run Apache Spark only in standalone mode on single machine, you can skip this step.

Prior to building the project, you need to specify the version of Hadoop and/or HBase installed (if applicable). Review the *Deployment Variants* section of the *aperture-tiles/***build.gradle** file to check for valid settings for your version.

If your version is not included, you must build a new case for it. See the comments in the file for more details.

#### <a name="compiling"></a> Compiling the Aperture Tiles Projects ####

Before you compile the Aperture Tiles source code, you must install the Aperture JS project:

- Run the `mvn build` command in your root Aperture JS directory.

Once the Aperture JS installation is complete:

- Run the `gradlew install -PbuildType=<buildType>` command in your root Aperture Tiles directory, where `buildType` is a case in the **build.gradle** file that specifies which versions of Hadoop/HBase and Spark you are using (e.g., *cdh5.1.2*).<p class=list-paragraph">NOTE: If you do not specify a buildType, the default value (<em>cdh4.6.0</em>) in <em>aperture-tiles/</em><strong>gradle.properties</strong> is used.</p>

This will compile all the project components and install .jar files for each project into your local Gradle repository on your build machine.

## <a name="next-steps"></a> Next Steps ##

For details on generating tile sets from raw data, see the [Tile Generation](../generation) topic.