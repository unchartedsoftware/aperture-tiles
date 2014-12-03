#!/usr/bin/env bash

# Determine our base directory
BASE_DIR="$(cd `dirname $0`/..; pwd)"

# Set up the Spark classpath to include Aperture-tiles libraries
LIB=${BASE_DIR}/lib
export SPARK_CLASSPATH=${LIB}/tile-generation.jar:${LIB}/binning-utilities.jar:${LIB}/scala-library.jar:${LIB}/math-utilities.jar:${LIB}/json.jar
export SPARK_CLASSPATH=${SPARK_CLASSPATH}:${LIB}/hbase.jar

# Make sure Spark environment is set
if [ "a" == "a""$SPARK_HOME" ]; then
  echo SPARK_HOME not set.  Please set SPARK_HOME environment variable and try again.
  exit 1
fi

if [ "a" == "a""$MASTER" ]; then
  echo Spark Master not set.  Please set MASTER environment variable and try again.
  exit 1
fi

if [ "a" == "a""$SPARK_MEM" ]; then
  echo SPARK_MEM not set.  Please set SPARK_MEM environment variable and try again.
fi

echo Running Spark from $SPARK_HOME
echo Running Spark on $MASTER
echo Running Spark with $SPARK_MEM
echo Running Spark with classpath=$SPARK_CLASSPATH
echo Running Spark with env=$SPARK_JAVA_OPTS

$SPARK_HOME/bin/spark-class $*
