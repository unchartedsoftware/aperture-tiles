---
section: Docs
permalink: docs/development/installation/
subsection: Development
chapter: How-To
topic: Installation
layout: submenu
---

Installation and Compilation
============================

The instructions on this page are intended for developers who want to install the Aperture Tiles source code and build their own custom projects.

For quick examples of the capabilities of Aperture Tiles, see the following topics:

- [Demos](../../../demos/): Access fully functional demonstrations of Aperture Tiles from your web browser.
- [Download](../../../download): Access a pre-built distribution designed to help you quickly understand the process of creating an Aperture Tiles application. Instructions for using these packages are available on the [Quick Start](../quickstart) page. 

## <a name="prerequisites"></a> Prerequisites ##

This project has the following prerequisites:

<div class="props">
	<nav>
		<table class="summaryTable" width="100%">
			<thead >
				<th scope="col" width="20%">Component</th>
				<th scope="col" width="30%">Required</th>
				<th scope="col" width="50%">Notes</th>
			</thead>
			<tr>
				<td style="vertical-align: text-top" class="attributes">Operating System</td>
				<td style="vertical-align: text-top" class="nameDescription">
					<div class="description">
						Linux or OS X
					</div>
				</td>
				<td style="vertical-align: text-top" class="nameDescription">
					<div class="description">Windows support available with <a href="https://cygwin.com/">Cygwin</a> or DOS command prompt; precludes the use of Hadoop/HBase.</div>
				</td>
			</tr>
			<tr>
				<td style="vertical-align: text-top" class="attributes">Languages</td>
				<td style="vertical-align: text-top" class="nameDescription">
					<div class="description">
						<a href="http://www.scala-lang.org/">Scala</a> v2.10.3
						<br><a href="http://www.java.com/">Java</a> (JDK v1.7+)
					</div>
				</td>
				<td style="vertical-align: text-top" class="nameDescription">
					<div class="description"></div>
				</td>
			</tr>
			<tr>
				<td style="vertical-align: text-top" class="attributes" rowspan="2">Cluster Computing Framework</td>
				<td style="vertical-align: text-top" class="nameDescription">
					<div class="description">
						<a href="http://spark.incubator.apache.org//">Apache Spark</a><br>v1.0.0+
					</div>
				</td>
				<td style="vertical-align: text-top" class="nameDescription">
					<div class="description">
						<p>You must configure the version of Hadoop with which Spark will be working (if applicable).</p>
							<p>The latest version of Spark may cause class path issues if you compile from source code. We recommend using a pre-built Spark package.</p>
					</div>
				</td>
			</tr>
			<tr>
				<td style="vertical-align: text-top" class="nameDescription">
					<div class="description">
						<a href="http://hadoop.apache.org/">Hadoop</a> (<em>optional</em>):
						<ul>
							<li><a href="http://www.cloudera.com/content/cloudera/en/products/cdh.html">Cloudera</a>  v4.6 (<em>recommended)</em></li>
							<li><a href="http://hadoop.apache.org/docs/r1.2.1/index.html">Apache</a></li>
							<li><a href="http://www.mapr.com/products/apache-hadoop">MapR</a></li>
							<li><a href="http://hortonworks.com/">HortonWorks</a></li>
						</ul>
					</div>
				</td>
				<td style="vertical-align: text-top" class="nameDescription">
					<div class="description">
						Some Hadoop distributions automatically install Apache Spark. Upgrade to v1.0.0+ if the installation is older.
					</div>
				</td>
			</tr>
			<tr>
				<td style="vertical-align: text-top" class="attributes">Web Server</td>
				<td style="vertical-align: text-top" class="nameDescription">
					<div class="description">
						<a href="http://tomcat.apache.org/">Apache Tomcat</a> or <a href="http://www.eclipse.org/jetty/">Jetty</a>
					</div>
				</td>
				<td style="vertical-align: text-top" class="nameDescription">
					<div class="description">The Tile Server and Tile Client, built using the <a href="http://restlet.org/">Restlet</a> web framework, require a servlet-compatible web server.</div>
				</td>
			</tr>
			<tr>
				<td style="vertical-align: text-top" class="attributes">Build Automation</td>
				<td style="vertical-align: text-top" class="nameDescription">
					<div class="description">
						<a href="http://www.eclipse.org/jetty/">Gradle</a><br>(requires <a href="http://nodejs.org/">Node.js</a>)
					</div>
				</td>
				<td style="vertical-align: text-top" class="nameDescription">
					<div class="description">The Node.js Windows installer has a known issue where it fails to install the following directory. To work around this issue, create the directory, then re-run the Aperture Tiles build
					<p><code>C:\Users\&lt;UserName&gt;\AppData\Roaming\npm</code></div>
				</td>
			</tr>
		</table>
	</nav>
</div>

<img src="../../../img/architecture.png" class="screenshot" alt="Aperture Tiles Architecture Diagram"/>

## <a name="source-code"></a> Source Code ##

The Aperture Tiles source code is available on [GitHub](https://github.com/unchartedsoftware/aperture-tiles/tree/master).

### Dependencies ###

Aperture Tiles is dependent on the *master* branch of Aperture JS source code, which you can also download from [GitHub](https://github.com/unchartedsoftware/aperturejs/tree/master).

### <a name="project-structure"></a> Project Structure ###

Aperture Tiles is made up of the following sub-projects:

<div class="props">
	<nav>
		<table class="summaryTable" width="100%">
			<thead>
				<th scope="col" width="25%">Sub-Project</th>
				<th scope="col" width="75%">Description</th>
			</thead>
			<tr>
				<td class="attributes">math-utilities</td>
				<td class="attributes">Basic, underlying Java utilities (for angles, linear algebra and statistics) to aid in processing data.</td>
			</tr>
			<tr>
				<td class="attributes">geometric-utilities</td>
				<td class="attributes">Advanced math utilities for processing geometry and geographic problems.</td>
			</tr>
			<tr>
				<td class="attributes">binning-utilities</td>
				<td class="attributes">Basic substrate of tiling, bin data structures, bin processing and basic bin storage classes.</td>
			</tr>
			<tr>
				<td class="attributes">factory-utilities</td>
				<td class="attributes">Factory system to allow construction and configuration of generic objects.</td>
			</tr>
			<tr>
				<td class="attributes">tile-generation</td>
				<td class="attributes">Spark-based tools to generate tile pyramids from raw data.</td>
			</tr>
			<tr>
				<td class="attributes">tile-rendering</td>
				<td class="attributes">Library that handles turning tile data into useful output formats, generally graphical.</td>
			</tr>
			<tr>
				<td class="attributes">tile-service</td>
				<td class="attributes">Web service to serve tiles from tile pyramids to web clients.</td>
			</tr>
			<tr>
				<td class="attributes">spark-tile-utilities</td>
				<td class="attributes">Optional services for allowing the tile server to communicate directly with Spark.</td>
			</tr>
			<tr>
				<td class="attributes">annotation-service</td>
				<td class="attributes">Optional services for adding annotations to Aperture Tiles visualizations.</td>
			</tr>
			<tr>
				<td class="attributes">tile-client</td>
				<td class="attributes">Simple web client to display tiles from tile pyramids.</td>
			</tr>
			<tr>
				<td class="attributes">tile-packaging</td>
				<td class="attributes">Packaged assembly of the tile generation service for the <a href="../quickstart/">Quick Start</a> example on this site.</td>
			</tr>
			<tr>
				<td class="attributes">tile-examples</td>
				<td class="attributes">Example applications.</td>
			</tr>
			<tr>
				<td class="attributes">gradle</td>
				<td class="attributes">Gradle build system support.</td>
			</tr>
			<tr>
				<td class="attributes">docs</td>
				<td class="attributes">Source files for the documentation on this website.</td>
			</tr>
		</table>
	</nav>
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

<div class="details props">
	<div class="innerProps">
		<ul class="methodDetail" id="MethodDetail">
			<dl class="detailList params">
				<dt>buildType</dt>
				<dd>A case in the <strong>build.gradle</strong> file that specifies which versions of Hadoop/HBase and Spark you are using (e.g., <em>cdh5.1.2</em>).<p class="list-paragraph">If you do not specify a <strong>buildType</strong>, the default value (<em>cdh4.6.0</em>) in <em>aperture-tiles/</em><strong>gradle.properties</strong> is used.</p></dd>
			</dl>
		</ul>
	</div>
</div>

This will compile all the project components and install .jar files for each project into your local Gradle repository on your build machine.

## <a name="next-steps"></a> Next Steps ##

For details on generating tile sets from raw data, see the [Tile Generation](../generation) topic.