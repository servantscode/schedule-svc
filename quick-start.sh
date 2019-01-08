#!/bin/bash

SERVICE=$1
echo "Configuring service $SERVICE"

files="Dockerfile deploy.env docker-compose.yml settings.gradle src/main/java/org/servantscode/hello/rest/ServiceIdentificationFilter.java"

for file in $files; do 
  echo "Updating $file"
  sed -i "s/hello/$SERVICE/g" $file
done

mv src/main/java/org/servantscode/hello src/main/java/org/servantscode/$SERVICE

SERVICE="$(tr '[:lower:]' '[:upper:]' <<< ${SERVICE:0:1})${SERVICE:1}"
DATE=`eventDate +%D`
USER=`whoami`
printf "Service: $SERVICE\nCreator: $USER\nCreated: $DATE" > README.md

git remote set-url origin git@github.com:servantscode/$SERVICE.git
git push
