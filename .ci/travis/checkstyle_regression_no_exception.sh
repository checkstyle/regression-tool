#!/bin/bash
BRANCH_COMMIT="$1"
MASTER_COMMIT="$2"
CHECKSTYLE_VERSION="$3"

if [ ! -d checkstyle ]; then
    git clone https://github.com/checkstyle/checkstyle
fi

if [ ! -d contribution ]; then
    git clone --depth=1 https://github.com/checkstyle/contribution
fi
if [ ! -z "$CHECKSTYLE_VERSION" ]; then
    sed -i'' "s/<checkstyle\.version>.*<\/checkstyle\.version>/<checkstyle\.version>${CHECKSTYLE_VERSION}<\/checkstyle\.version>/g" \
    contribution/checkstyle-tester/pom.xml
fi

cd checkstyle
CHECKSTYLE_PATH="$(pwd)"
git branch test-branch
git checkout test-branch
git reset --hard "${BRANCH_COMMIT}"
git checkout master
git reset --hard "${MASTER_COMMIT}"
cd ..
mvn clean package -Passembly
java -jar target/regression-tool-1.0-SNAPSHOT-all.jar -r "$CHECKSTYLE_PATH" -p test-branch -t contribution/checkstyle-tester
RESULT="$?"
cat config-test-branch-*.xml
rm config-test-branch-*.xml

if [ "$?" != "0" ] || [ "$RESULT" != "0" ]; then
    exit "1";
fi
