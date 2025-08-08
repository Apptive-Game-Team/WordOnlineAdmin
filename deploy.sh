#!/bin/bash

PROJECT_ROOT=$(pwd)

git checkout main
git pull origin

BUILD_PATH=$(ls $PROJECT_ROOT/*.jar)
JAR_NAME=$(basename $BUILD_PATH)

DEPLOY_PATH=~/deploy/word-online-admin

CURRENT_PID=$(pgrep -f $JAR_NAME)

if [ -z "$CURRENT_PID" ]
then
  sleep 1
else
  kill -15 $CURRENT_PID
  sleep 5
fi

cp $BUILD_PATH $DEPLOY_PATH

nohup java -Xmx256m -jar $DEPLOY_PATH/$JAR_NAME > $DEPLOY_PATH/app.log 2>&1 &
