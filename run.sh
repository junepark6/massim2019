#!/bin/bash

IntelligentAgentsDir="/Users/morganfine-morris/Documents/GraduateSchool/IntelligentAgents/"
teamMSdir=$IntelligentAgentsDir"projects/project3/massim2019/src/main/java/TeamMS/"
javaagentsdir=$IntelligentAgentsDir"projects/project3/massim2019/src/main/java/"
projectdir=$IntelligentAgentsDir"projects/project3/massim2019/"
massimdir=$IntelligentAgentsDir"projects/massim2019/"
eismassim=$IntelligentAgentsDir"massim-2019-2.0/eismassim/eismassim-4.1-jar-with-dependencies.jar"
javaagents=$IntelligentAgentsDir"massim-2019-2.0/javaagents/javaagents-2019-1.0-jar-with-dependencies.jar"
protocol=$IntelligentAgentsDir"massim-2019-2.0/protocol/protocol-2.1.jar"

# every possible classpath that might be useful
classpath=$javaagentsdir:$teamMSagentsdir:$teamMSdir:$eismassim:$javaagents:$teamMSdir:$protocol

java -cp $classpath $1

