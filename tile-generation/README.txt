Influent Spark Distributed Tiling


It is recommended to review the distributed-tiling-overview.pdf before 
attempting to build and run the tile generator. 


== Requirements ==
Requires:  Spark version 0.7.2; Scala version 2.9.3
Optional:  HBASE; Hadoop (multiple possible versions)



== System configuration ==

Edit both the spark and aperture-tile build files to use the version of Hadoop 
and HBase installed on your system(s).
    * Spark determines with which version of Hadoop to build by looking in 
      $SPARK_HOME/project/SparkBuild.scala.  Instructions for what to change 
      are contained therein.
    * Aperture-tile determines the proper Hadoop and HBase version in the root 
      pom.xml, ../pom.xml from the directory containing this README.  Look at
      the hadoop.version and hbase.version properties.

If you are building your own tiles, we include a spark-run script that 
simplifies running spark jobs, making sure all the necessary libraries are 
included, etc.  It is not necessary to use this file - one can set things in 
$SPARK_HOME/conf/spark-env.sh, for instance - but we include it because we find
it usefule.  To set use it, one needs the following environment variables set 
properly:
    * SCALA_HOME
    * SPARK_HOME
    * MASTER
    * MESOS_NATIVE_LIBRARY
This script looks for the .jar files it needs in the local Maven repository -
so in order to use it, one first must run 'mvn install' over the whole 
aperture-tiles project..



== Running a tiling job ==

=== Tabular data === 

If your data is in numeric and in tabular form - character-separated files 
(CSV, or Comma-separated-values, for instance) - the simplest way to process 
your data is probably to use the CSVBinner.

To use the CSVBinner, one needs to create a set of properties files describing 
the binning job.  The first such properties file is a base file, describing 
the general characteristics of the data.  The second describes the specific
attributes you want tiled, and the levels of tiling required.  Both use the 
standard java property file format, which means they can include comments
(lines beginning with '#' are considered comments).

The base file can include the following properties:

The output of the binning process will be a collection of AVRO tile data files 
in the specified location (hbase or local filesystem).


To execute the tiling job use the spark-run script passing in the base and and 
tiling property files:

spark-run com.oculusinfo.tilegen.examples.apps.CSVBinner -d /Users/slangevin/data/twitter/dataset-base.bd /Users/slangevin/data/twitter/dataset.lon.lat.bd 


The following properties must be defined in the base property file if they are 
used at all (if one uses a non-hbase tileio type, for instance, one doesn't 
need to set the hbase connection configuration properties):

    spark.connection.url
        The location of the spark master.  Use "local" for spark standalone.
        Defaults to "spark://localhost:7077"  <-- TODO change default in code!
 
    spark.connection.home
        The file system location of Spark in the remote location (and,
        necessarily, on the local machine too)
        Defaults to "/opt/spark-0.7.2"   <-- TODO change default in code!

    spark.connection.user
        A user name to stick in the job title so people know who is running the job
 
    oculus.tileio.type
        The way in which tiles are written - either hbase (to write to hbase,
        see hbase. properties above to specify where) or file to write to the
        local file system
        Default is file   <-- TODO change default in code!

    hbase.zookeeper.quorum
        If tiles are written to hbase, the zookeeper quorum location needed to
        connect to hbase.
  
    hbase.zookeeper.port
        If tiles are written to hbase, the port through which to connect to
        zookeeper

    hbase.master
        If tiles are written to hbase, the location of the hbase master to
        which to write them

The rest of the configuration properties can be set in either file - in fact, 
the same file can be used as both inputs.  However, typically, we expect the 
first group to be defined in the base file, and the second in the tiling file,
as breaking them up this way leads to a reasonable separation for tiling 
multiple attributes on the same dataset.

  Group 1:
    oculus.binning.source.location
        The path to the data file or files to be binned

    oculus.binning.prefix
        A prefix to be prepended to the name of every pyramid location, used to
        separate this run of binning from previous runs.
        If not present, no prefix is used.

    oculus.binning.parsing.separator
        The character or string to use as a separator between columns in input data files
        Default is a tab 
 
    oculus.binning.parsing.<field>.index
        The column number of the described field in the input data files
        This field is mandatory for every field type to be used

    oculus.binning.parsing.<field>.fieldType
        The type of value expected in the column specified by
        oculus.binning.parsing.<field>.index.  Default is to treat the column as
        containing real, double-precision values.  Other possible types are:
            constant or zero - treat the column as containing 0.0 (the
                column doesn't actually have to exist)
            int - treat the column as containing integers
            long - treat the column as containing double-precision integers
            date - treat the column as containing a date.  The date will be
                parsed and transformed into milliseconds since the
                standard java start date (using SimpleDateFormatter).
                Default format is yyMMddHHmm, but this can be overridden
                using the oculus.binning.parsing.<field>.dateFormat
            propertyMap - treat the column as a property map.  Further
                information is then needed to get the specific property.  All
                four of the following properties must be present to read the
                property.
                oculus.binning.parsing.<field>.property - the name of the
                    property to read
                oculus.binning.parsing.<field>.propertyType - equivalent to
                    fieldType
                oculus.binning.parsing.<field>.propertySeparator - the
                    character or string to use to separate one property from
                    the next
                oculus.binning.parsing.<field>.propertyValueSeparator - the
                    character or string used to separate a property key from
                    its value

    oculus.binning.parsing.<field>.fieldScaling
        How the field values should be scaled.  Default is to leave values as
        they are.  Other possibilities are:
            log - take the log of the value
                  (oculus.binning.parsing.<field>.fieldBase is used, just as
                  with fieldAggregation)

    oculus.binning.parsing.<field>.fieldAggregation
        The method of aggregation to be used on values of the X field.
        Default is addition.  Other possible aggregation types are:
            min - find the minimum value
            max - find the maximum value
            log - treat the number as a  logarithmic value; aggregation of
                 a and b is log_base(base^a+base^b).  Base is taken from
                 property oculus.binning.parsing.<field>.fieldBase, and defaults to e

  Group 2:
    oculus.binning.name
        The name of the output data tile pyramid

    oculus.binning.projection
        The type of projection to use when binning data.  Possible values are:
            EPSG:4326 - bin linearly over the whole range of values found (default)
            EPSG:900913 - web-mercator projection (used for geographic values only)

    oculus.binning.xField
        The field to use as the X axis value

    oculus.binning.yField
        The field to use as the Y axis value.  Defaults to none (i.e., a
        density strip of x data)

    oculus.binning.valueField
        The field to use as the bin value
        Default is to count entries only

    oculus.binning.levels.<order>
        This is an array property - i.e., if one wants to bin levels in three groups,
        then one should have oculus.binning.levels.0, oculus.binning.levels.1, and
        oculus.binning.levels.2.  Each is a description of the leels to bin in that
        group - a comma-separate list of individual integers, or ranges of integers
        (described as start-end).  So "0-4,6,8" would mean levels 0, 1, 2, 3, 4, 6,
        and 8.
        If only one levels set is needed, the .0 is not required.
        If there are multiple level sets, the parsing of the raw data is only done
        once, and is cached for use with each level set.
        This property is mandatory, and has no default.

    oculus.binning.consolidationPartitions
        The number of partitions into which to consolidate data when binning it.
        If left out, spark will automatically choose something (hopefully a
        reasonable something); this parameter is only needed for fine-tuning failing
        processes

=== Examples ===

We include a few example configuration files in the data subdirectory.

twitter-local-base.bd is an example base property file for a locally stored 
dataset of ID, TIME, LATITUDE, LONGITUDE, with the tiles also output to the
local file system.

twitter-hdfs-base.bd is an example base property file for the same dataset,
but stored in hdfs, and output to hbase.

twitter-lon-lat.bd is an example tiling file that takes either of the base 
files above, and tells the system to tile levels 0-9 of the longitude and
latitude data contained therein.



