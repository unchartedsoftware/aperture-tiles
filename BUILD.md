## Aperture-Tiles Build Information

### Overview

Aperture-Tiles is made up of 6 projects:

 * math-utilities - as it sounds, some basic, underlying java utilities to aid 
   in processing data.  Some angle utilities, some linear algebra, and a little 
   statistics.
 * geometric-utilities - some more advanced math for processing geometry - 
   mostly dealing with processing of geographic problems.
 * binning-utilities - the basic substrate of tiling, what a bin is, how to 
   bin, and basic bin storage classes.
 * tile-generation - A project to generate tile pyramids from raw data
 * tile-service - a web server to serve tiles from tile pyramids to web clients
 * tile-client - a web client to display tiles from tile pyramids

### Setup

All aperture-tiles projects build with Maven.  We build with Maven 3.1.0, but 
other versions may work.

The tile-generation project in particular has a few prerequisites (some 
strictly necessary, some optional).  Generally, one needs to set up:

 * Hadoop/HDFS - much too complex to describe here - see hadoop itself for 
   installation instructions.  There are flavors of Hadoop out there:
   [Cloudera](http://www.cloudera.com/content/cloudera/en/products/cdh.html), 
   [Apache](http://hadoop.apache.org/docs/r1.2.1/index.html), 
   [MapR](http://www.mapr.com/products/apache-hadoop),
   [HortonWorks](http://hortonworks.com/), and probably a host of others.  If 
   you want to run large tiling jobs, you'll want a cluster of machines set up
   with Hadoop and Spark on which to run them.  If you're running small jobs, 
   or are willing to take a long time to do them, you can run Spark on a single
   node and just read to and write from the file system, in which case you
   don't need this step - but it makes things a lot easier, and lets you do a
   lot more.

 * HBase - HBase acts much like a database which stores its data in Hadoop.  If
   your tile sets are too many or too big to store on one machine, we have 
   found HBase to be a good way to store them.  That being said, reducing data 
   to tile sets shrinks it considerably, so this is not terribly necessary.

 * [Spark](http://spark.incubator.apache.org/) - Spark is "... an open source 
   cluster computing system that aims to make data analytics fast...".  It also 
   makes them very easy to develop and generalize.  We have mostly been working 
   with Spark 0.7.2 so far, so we won't yet guarantee this system will work 
   with the latest version... but testing that is on our short list.

   When you set up Spark, you have to tell it the version of Hadoop with which 
   it will be working.  See Spark build instructions for how to do this.

 * Aperture-core - Though this project is a piece of Aperture, the tile-service
   and tile-client projects do need the core.  Currently, the best way to get 
   the core is to download it from the Aperture web site -
   [aperturejs.com](aperturejs.com).  You will need to use the "Download 
   Source" option, and run the maven installation on that, so that the 
   tile-service and tile-client projects' builds can find what they need.

Finally, one has tell the tile-generation to build relative to the version of 
Hadoop and/or HBase one has installed.  This is done by setting
<hadoop-version> and <hbase-version> in the general aperture-tiles project 
build file, the file pom.xml in this directory.

### Actually building

To build all projects, type 'mvn install' in this directory.  This will install 
.jar files for each project into your local maven repostitory on your build 
machine.

In actually running tiling jobs, the default run scripts in the tile-generation 
project expect to find the jars in your maven repository.  Modifying them to 
look for the jars elsewhere if you don't want them there isn't hard, but you're 
on your own for that part.  But if you want to go through all that, just use 
'mvn package' instead.

### Running

Two of the projects herein yield runnable results: tile-generation and 
tile-client. Each has a README.txt file describing how to run its contents.

Happy tiling!
