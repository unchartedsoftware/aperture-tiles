#!/bin/bash

* This script scrubs a spark log to find the timing of the various stages of a spark job

FILE=$1

echo "Description	Stage id	Duration (s)"
grep Stage ${FILE} | grep finished | awk '\
{
	stage = sprintf("%.4d", $6)

	task = sprintf("%s %s %s", $7, $8, $9)

    timePeriod = $13
	rawTime = $12
	if ("m" == timePeriod)
		time = rawTime * 60
	else if ("s" == timePeriod)
		time = rawTime
	else if ("ms" == timePeriod)
		time = rawTime / 1000
	else
		time = -1

	print task, "\t", stage, "\t", time
}'