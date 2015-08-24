#!/usr/bin/env bash

$SPARK_HOME/spark-submit \
    --num-executors 12 \
    --executor-memory 20g \
    --executor-cores 4 \
    --conf spark.executor.extraClassPath=/opt/cloudera/parcels/CDH/lib/hbase/lib/htrace-core-3.1.0-incubating.jar \
    --driver-class-path /opt/cloudera/parcels/CDH/lib/hbase/lib/htrace-core-3.1.0-incubating.jar \
    --jars /opt/cloudera/parcels/CDH/lib/hbase/lib/htrace-core-3.1.0-incubating.jar \
    --master yarn-client \
    --class com.oculusinfo.tiles.apps.GeoHeatmapPipelineApp ../lib/tile-generation.jar \
    -start 2014/02/01.00:00:00.+0000 \
    -end 2014/03/01.00:00:00.+0000 \
    -levels '0,1,2,3,4' \
    -columnMap ./crossplot_columns.properties
    -name crossplot_example_v1 \
    -description "Crossplot pipeline app example" \
    -partitions 200 \
    -source 'hdfs://some.hdfs.location/some/data' \
    -zookeeperquorum some.quorum \
    -zookeeperport 12345 \
    -hbasemaster some.hbase.master:12345