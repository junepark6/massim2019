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

echo $classpath

javac -cp $classpath $1 -verbose -Xlint:unchecked



#javac -cp .:$srcDir:src/:$teamMSdir:$eismassim:$javaagents:$teamMSagentsdir src/main/java/TeamMS/agents/Agent.java -verbose -Xlint:unchecked

#javac -cp .:src/: src/main/java/TeamMS/agents/ExploratoryAgent.java -verbose -Xlint:unchecked
#javac -cp .:src/:../../../massim_2019/:src/main/java/TeamMS/ src/main/java/TeamMS/Scheduler.java -verbose -Xlint:unchecked
#javac -cp .:src/:massimdir:teamMSdir:projectdir: src/main/java/TeamMS/Scheduler.java -verbose -Xlint:unchecked


## TO SEE INTO A JAR FILE:
#(base) morganfine-morris@dyn218026 massim2019 % jar tf /Users/morganfine-morris/Documents/GraduateSchool/IntelligentAgents/massim-2019-2.0/javaagents/javaagents-2019-1.0-jar-with-dependencies.jar
# massim/javaagents/
# massim/javaagents/agents/
# massim/javaagents/Main.class
# massim/javaagents/Scheduler$AgentConf.class
# massim/javaagents/MailService.class
# massim/javaagents/agents/BasicAgent.class
# massim/javaagents/agents/Agent.class
# massim/javaagents/Scheduler.class


# massim/eismassim/
# massim/eismassim/entities/
# massim/eismassim/EnvironmentInterface.class
# massim/eismassim/Log.class
# massim/eismassim/entities/ScenarioEntity.class
# massim/eismassim/EISEntity.class