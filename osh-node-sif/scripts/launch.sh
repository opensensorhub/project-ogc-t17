#!/bin/bash
java -Xms1G -Xmx2G -Dlogback.configurationFile=./logback.xml -cp "lib/*" org.sensorhub.impl.SensorHub config.json db
