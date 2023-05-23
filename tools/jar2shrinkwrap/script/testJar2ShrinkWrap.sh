#!/bin/bash -x

export TOOLFOLDER=$PWD
java -jar $TOOLFOLDER/target/jar2shrinkwrap-11.0.0-SNAPSHOT.jar com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes com.sun.ts.tests.servlet.api.jakarta_servlet.filter



