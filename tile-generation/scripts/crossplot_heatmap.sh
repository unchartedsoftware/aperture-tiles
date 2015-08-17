#!/usr/bin/env bash

#----- Main Class for spark job to execute
MAIN_CLASS=com.oculusinfo.tiles.apps.GeoHeatmapPipelineApp

#----- Path and name of Main JAR
MAIN_JAR=../lib/tile-generation.jar

#----- Set Spark Master URL
JOB_MASTER=local

/opt/spark/bin/spark-submit \
    --conf spark.executor.extraClassPath=/opt/cloudera/parcels/CDH/lib/hbase/lib/htrace-core-3.1.0-incubating.jar \
    --driver-class-path /opt/cloudera/parcels/CDH/lib/hbase/lib/htrace-core-3.1.0-incubating.jar \
    --jars /opt/cloudera/parcels/CDH/lib/hbase/lib/htrace-core-3.1.0-incubating.jar \
    --master local[2] \
    --class =com.oculusinfo.tiles.apps.GeoHeatmapPipelineApp MAIN_JAR=../lib/tile-generation.jar \
    -start 2014/02/01.00:00:00.+0000 \
    -end 2014/03/01.00:00:00.+0000 \
    -levels '0,1,2,3,4' \
    -columnMap ./columns.properties
    -name heatmapExample_v1 \
    -description heatmapExampe_v1 \
    -partitions 200 \
    -source 'hdfs://some_hdfs_location' \
    -io hbase \
    -zookeeperquorum 'some_zookeeper_quorum_location' \
    -zookeeperport NNNN \
    -hbasemaster hbase_master_location:60000