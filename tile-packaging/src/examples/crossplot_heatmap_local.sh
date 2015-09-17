#!/usr/bin/env bash

export SPARK_SUBMIT_DRIVER_MEMORY=8g

$SPARK_HOME/bin/spark-submit \
    --master local[4] \
    --jars $SPARK_HOME/lib/spark-assembly-1.3.0-hadoop2.0.0-mr1-cdh4.2.0.jar \
    --class com.oculusinfo.tilegen.pipeline.examples.CrossplotPipelineApp ../lib/tile-generation-assembly.jar \
    -levels '0,1,2,3,4,5,6,7' \
    -columnMap ./crossplot_columns.properties \
    -name crossplot_example_v1 \
    -description "Crossplot pipeline app example" \
    -partitions 200 \
    -source '../datasets/julia'
