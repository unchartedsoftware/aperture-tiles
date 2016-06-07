---
section: Docs
subsection: Development
chapter: Getting Started
topic: Installation
permalink: docs/development/getting-started/installation/
layout: chapter
---

Installation and Compilation
============================

The following installation instructions are intended for developers who want to use Aperture Tiles to build their own custom applications. The [complete installation](#complete-install) process requires several third-party tools and access to the Aperture Tiles source code.

For developers who want to quickly install a pre-configured example Aperture Tiles application with minimal modification, a set of [packaged distributions](#packaged-distributions) are available as an alternative.

## Supported Platforms ##

Aperture Tiles is compatible with Linux and OS X.

Compatibility for Windows is available through [Cygwin](https://cygwin.com/) or the DOS command prompt. Note however, that this platform is not compatible with Hadoop/HBase.

<h6 class="procedure">To use Aperture Tiles with Apache Spark in standalone mode in Windows</h6>

-   [Download](https://spark.apache.org/downloads.html) and install a pre-built version of Apache Spark with the following properties:
    1.  **Spark release**: 1.3.0
    2.  **Package type**: Pre-built for Hadoop 2.4

## Prerequisites ##

Aperture Tiles requires the following third-party tools:

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:20%;">Component</th>
            <th scope="col" style="width:30%;">Required</th>
            <th scope="col" style="width:50%;">Notes</th>
        </tr>
    </thead>
    <tr>
        <td style="vertical-align: text-top" class="description">Languages</td>
        <td style="vertical-align: text-top" class="description">
            <a href="http://www.scala-lang.org/">Scala</a> v2.10.3
            <br><a href="http://www.java.com/">Java</a> (JDK v1.7+)
        </td>
        <td style="vertical-align: text-top" class="description"></td>
    </tr>
    <tr>
        <td style="vertical-align: text-top" class="description">Cluster Computing Framework</td>
        <td style="vertical-align: text-top" class="description">
            <a href="http://spark.incubator.apache.org/">Apache Spark</a>
            <br>v1.3
        </td>
        <td style="vertical-align: text-top" class="description">
            Specify the version of Hadoop with which Spark will be working (if applicable).
            <p>Spark may cause class path issues if you compile from source code. We recommend using a pre-built Spark package.</p>
        </td>
    </tr>
    <tr>
        <td style="vertical-align: text-top" class="description">Build Automation</td>
        <td style="vertical-align: text-top" class="description">
            <a href="http://nodejs.org/">Node.js</a>
        </td>
        <td style="vertical-align: text-top" class="description">
            The Node.js Windows installer has a known issue where it fails to install the following directory. To work around this issue, create the directory manually.
            <p><code>C:\Users\&lt;UserName&gt;\AppData\Roaming\npm</code></p>
        </td>
    </tr>
    <tr>
        <td style="vertical-align: text-top" class="description">Web Server</td>
        <td style="vertical-align: text-top" class="description"><a href="http://tomcat.apache.org/">Apache Tomcat</a> or <a href="http://www.eclipse.org/jetty/">Jetty</a></td>
        <td style="vertical-align: text-top" class="description">The Tile Server and Tile Client, built using the <a href="http://restlet.org/">Restlet</a> web framework, require a servlet-compatible web server.</td>
    </tr>
</table>

If you intend to work with datasets that cannot fit in the memory of a single machine or if you wish to avoid wait times, we recommend you also install the following tools to enable Aperture Tiles to work with particularly large datasets.

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:20%;">Component</th>
            <th scope="col" style="width:30%;">Required</th>
            <th scope="col" style="width:50%;">Notes</th>
        </tr>
    </thead>
    <tr>
        <td style="vertical-align: text-top" class="description">Cluster Computing Framework</td>
        <td style="vertical-align: text-top" class="description">
            A <a href="http://hadoop.apache.org/">Hadoop</a> distribution:
            <ul class="table">
                <li><a href="http://www.cloudera.com/content/www/en-us/downloads/cdh.html">Cloudera</a> v5.4.7 (<em>recommended)</em></li>
                <li><a href="http://hadoop.apache.org/">Apache</a></li>
                <li><a href="http://www.mapr.com/products/apache-hadoop">MapR</a></li>
                <li><a href="http://hortonworks.com/">HortonWorks</a></li>
            </ul>
        </td>
        <td style="vertical-align: text-top" class="description">Some Hadoop distributions automatically install Apache Spark. Upgrade to v1.3 if the installation is older.</td>
    </tr>
</table>

<img src="../../../../img/architecture.png" class="screenshot" alt="Aperture Tiles Architecture Diagram"/>

## Complete Install ##

### Source Code ###

The source code repository for Aperture Tiles is available on [GitHub](https://github.com/unchartedsoftware/).

<h6 class="procedure">To work with the most recent stable release of the Aperture Tiles source code</h6>

1.  Clone the Aperture Tiles repository to an *aperture-tiles/* directory in your Git project folder:

    ```bash
    git clone https://github.com/unchartedsoftware/aperture-tiles.git
    ```

2.  Check out the *master* branch:

    ```bash
    git checkout master
    ```

### Building the Project ###

Once you have cloned the Aperture Tiles repository, you can build the project.

1. [Specify your Hadoop/HBase version](#specifying-your-hadoophbase-version) (if applicable)
2. [Compile Aperture Tiles](#compiling-the-aperture-tiles-projects)

#### Specifying Your Hadoop/HBase Version ####

**NOTE**: If you plan to run Apache Spark only in standalone mode on single machine, you can skip this step.

Prior to building the project, you must specify which version of Hadoop and/or HBase you have installed (if applicable): 

1. Review the *Deployment Variants* section of the [build.gradle](https://github.com/unchartedsoftware/aperture-tiles/blob/master/build.gradle) file in [aperture-tiles/](https://github.com/unchartedsoftware/aperture-tiles/tree/master) to check for valid settings for your version.
2. If your version is not included, build a new case for it. See the comments in the file for more details.

#### Compiling the Aperture Tiles Projects ####

Aperture Tiles builds with [Gradle](https://gradle.org/). 

<h6 class="procedure">To install Gradle and build Aperture Tiles</h6>

-   Execute the following command in your root Aperture Tiles directory:

    ```bash
    gradlew install -PbuildType=<buildType>
    ``` 

    Where:

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:25%;">Sub-Project</th>
                <th scope="col" style="width:75%;">Description</th>
            </tr>
        </thead>
        <tr>
            <td class="property">buildType</td>
            <td class="description">
                A case in the <strong>build.gradle</strong> file that specifies which versions of Hadoop/HBase and Spark you are using (e.g., <em>cdh5.1.2</em>).
                <p>If you do not specify a <strong>buildType</strong>, the default value (<em>cdh5.4.7</em>) in <em>aperture-tiles/</em><strong>gradle.properties</strong> is used.</p>
            </td>
        </tr>
    </table>

This will compile all the project components and install .jar files for each project into a local Gradle repository on your build machine.

##### Project Structure #####

Aperture Tiles is made up of the following sub-projects:

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:25%;">Sub-Project</th>
            <th scope="col" style="width:75%;">Description</th>
        </tr>
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
        <td class="description">Packaged assembly of the tile generation service for the <a href="../quick-start/">Quick Start</a> example on this site.</td>
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

## Packaged Distributions ##

The Aperture Tiles packaged distributions are intended for use with the [Quick Start](../quick-start/) example workflow. These distributions require a subset of the complete installation [prerequisites](#prerequisites).

### Prerequisites ###

The packaged distributions of Aperture Tiles require only a subset of the [prerequisites](#prerequisites) for complete installations.

<h6 class="procedure">To install the prerequisites for the packaged distributions</h6>

1.  Install [Apache Spark](http://spark.incubator.apache.org/).
2.  If you want to understand how Aperture Tiles works with particularly large datasets, install your preferred flavor of [Hadoop](http://hadoop.apache.org/):
    -   [Cloudera](http://www.cloudera.com/content/www/en-us/downloads/cdh.html) v5.4.7 (*recommended*)
    -   [Apache](http://hadoop.apache.org/docs/r1.2.1/index.html)
    -   [MapR](http://www.mapr.com/products/apache-hadoop)
    -   [HortonWorks](http://hortonworks.com/)

### Download the Distributions ###

The Aperture Tiles packaged distributions enable you to create a sample Aperture Tiles application that explores a Julia set fractal visualization.

<h6 class="procedure">To download the packaged distributions</h6>

1.  Download and save the following files:
    -   [Tile Generator](../../../../download/#tile-generator): Creates the Julia set data and generates a set of tiles
    -   [Tile Quick Start Application](../../../../download/#tile-quick-start-application): Serves as an example application that you can quickly deploy after minimal modification
2.  Extract the contents of each file to your local machine.
3.  See the [Quick Start](../quick-start/) topic for information on configuring each packaged distribution.

## Next Steps ##

For a guide on quickly configuring and deploying an example Aperture application, see the [Quick Start](../quick-start) topic.