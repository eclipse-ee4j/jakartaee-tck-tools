#!/bin/bash -x

##[ Imports ]####################

source "$(dirname $0)/../common/backup.sh"

##[ Main ]#######################

#GF_VER=`cat glassfish.version | tr '\n' '|'`
TCK_VER=`cat jakartaeetck.version | tr '\n' '|'`
echo "Version Info:Eclipse GF URL : https://ci.eclipse.org/jakartaee-tck/job/build-glassfish/lastSuccessfulBuild/artifact/appserver/distributions/glassfish/target/glassfish.zip ; JakartaEETCK-$TCK_VER"

for f in *junitreports.tar.gz
do
  tar zxf "$f" -C $WORKSPACE  --strip-components=5
  touch */*.xml
done

ls -ltr
cd jakartaeetck-bundles/
ls -ltr

rm -rf $WORKSPACE/jakarta-jakartaeetckinfo.txt
touch $WORKSPACE/jakarta-jakartaeetckinfo.txt
export BUNDLE_URL="http://download.eclipse.org/ee4j/jakartaee-tck/jakartaee9-eftl/staged-900"

for tck_bundle in *tck*.zip
do
  export NAME=$tck_bundle
  echo '***********************************************************************************' >> $WORKSPACE/jakarta-jakartaeetckinfo.txt
  echo '***                        TCK bundle information                               ***' >> $WORKSPACE/jakarta-jakartaeetckinfo.txt
  echo "*** Name:       ${NAME}                                     ***" >> $WORKSPACE/jakarta-jakartaeetckinfo.txt
  echo "*** Bundle Copied to URL:    ${BUNDLE_URL}/${NAME} ***"  >> $WORKSPACE/jakarta-jakartaeetckinfo.txt
  echo '*** Date and size: '`stat -c "date: %y, size(b): %s" ${NAME}`'        ***'>> $WORKSPACE/jakarta-jakartaeetckinfo.txt
  echo "*** SHA256SUM: "`sha256sum ${NAME} | awk '{print $1}'`' ***' >> $WORKSPACE/jakarta-jakartaeetckinfo.txt
  echo '***                                                                             ***' >> $WORKSPACE/jakarta-jakartaeetckinfo.txt
  echo '***********************************************************************************' >> $WORKSPACE/jakarta-jakartaeetckinfo.txt
done


scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null jakarta-jakartaeetck-9.0.0.zip genie.jakartaee-tck@build.eclipse.org:/home/data/httpd/download.eclipse.org/ee4j/jakartaee-tck/jakartaee9-eftl/staged-900
scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null $WORKSPACE/jakartaeetck.version genie.jakartaee-tck@build.eclipse.org:/home/data/httpd/download.eclipse.org/ee4j/jakartaee-tck/jakartaee9-eftl/staged-900
scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null $WORKSPACE/jakarta-jakartaeetckinfo.txt genie.jakartaee-tck@build.eclipse.org:/home/data/httpd/download.eclipse.org/ee4j/jakartaee-tck/jakartaee9-eftl/staged-900

ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null genie.jakartaee-tck@build.eclipse.org ls -lt /home/data/httpd/download.eclipse.org/ee4j/jakartaee-tck/jakartaee9-eftl/staged-900

ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null genie.jakartaee-tck@build.eclipse.org mkdir /home/data/httpd/download.eclipse.org/ee4j/jakartaee-tck/jakartaee9-eftl/staged-900/history

# backup limits the number of zips kept to 5, instead we will temporily switch to backup_no_delete which doesn't limit the number of zips.
backup_no_delete jakarta-jakartaeetck-9.0.0.zip \
       genie.jakartaee-tck@build.eclipse.org \
       /home/data/httpd/download.eclipse.org/ee4j/jakartaee-tck/jakartaee9-eftl/staged-900/history

rm -rf jakarta-jakartaeetck-9.0.0.zip
rm -rf jakarta-jakartaee-smoke-9.0.0.zip

