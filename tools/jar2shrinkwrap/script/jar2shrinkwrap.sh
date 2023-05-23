#!/bin/bash -x

export TOOLFOLDER=$PWD
echo "TOOLFOLDER = $TOOLFOLDER"

if [ "$1" == "-?" ]
then
      echo "show usage info"
      exit 1
elif [  -z "$tckroot" ]
then
      echo "Environment variable tckroot needs to be set to root development folder that will be populated with TCK."
      echo "export tckroot=/tmp/tckwork"
      exit 1
else
      echo "using $tckroot."
      cd $tckroot
fi


mkdir -p $tckroot
cd $tckroot

# setup jakartaeetck folder with build of Platform TCK
[ ! -d "jakartaeetck" ] && wget https://download.eclipse.org/jakartaee/platform/10/jakarta-jakartaeetck-10.0.2.zip -O jakarta-jakartaeetck-10.0.2.zip && unzip jakarta-jakartaeetck-10.0.2.zip 
cd $tckroot/jakartaeetck/dist/com/sun/ts/tests/servlet

find -name *.war -exec java -DtargetFolder=$PWD -jar $TOOLFOLDER/target/jar2shrinkwrap-11.0.0-SNAPSHOT.jar {} +


