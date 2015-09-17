#!/usr/bin/env bash

$SPARK_HOME/bin/spark-submit \
    --master local[2] \
    --jars $SPARK_HOME/lib/spark-assembly-1.3.0-hadoop2.0.0-mr1-cdh4.2.0.jar \
    --class com.oculusinfo.tilegen.pipeline.examples.GeoHeatmapPipelineApp ../lib/tile-generation-assembly.jar \
    -levels '0,1,2,3,4' \
    -start 2015/02/01.00:00:00.GMT+00:00 \
    -end 2015/08/25.00:00:00.GMT+00:00 \
    -columnMap ./geo_columns.properties \
    -name geo_heatmap_example_v1 \
    -description "Geo heatmap pipeline app example" \
    -partitions 200 \
    -source '../datasets/timestamp_geo.csv'
