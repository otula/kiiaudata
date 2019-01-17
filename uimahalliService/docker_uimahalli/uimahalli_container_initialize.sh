#!/bin/bash

BASEDIR=$(dirname $(realpath -s $0))
LOCAL_IP=127.0.0.1
TOMCAT_HTTP_PORT=51000
TOMCAT_AJP_PORT=51001
MYSQL_PORT=51002
DOCKER_NETWORK=uimahalli_network
#Docker volumes
VOLUME_MYSQL_DATABASE=$BASEDIR/mysql/databases
VOLUME_MYSQL_INITDB=$BASEDIR/mysql/sql
VOLUME_TOMCAT_WEBAPPS=$BASEDIR/tomcat/webapps

#Docker environment variables
MYSQL_ROOT_PASSWORD=password
MYSQL_USER=user
MYSQL_PASSWORD=password
CA_FRONTEND_SCHEMA=ca_frontend

docker network create --driver bridge $DOCKER_NETWORK
docker run --name uimahalli-mysql --restart unless-stopped --network=$DOCKER_NETWORK -v $VOLUME_MYSQL_DATABASE:/var/lib/mysql -v $VOLUME_MYSQL_INITDB:/docker-entrypoint-initdb.d -e MYSQL_DATABASE=$CA_FRONTEND_SCHEMA -e MYSQL_USER=$MYSQL_USER -e MYSQL_PASSWORD=$MYSQL_PASSWORD -e MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD -it -d -p $LOCAL_IP:$MYSQL_PORT:3306 mariadb
docker run --name uimahalli-tomcat --restart unless-stopped --network=$DOCKER_NETWORK -v $VOLUME_TOMCAT_WEBAPPS:/usr/local/tomcat/webapps -it -d -p $LOCAL_IP:$TOMCAT_AJP_PORT:8009 -p $TOMCAT_HTTP_PORT:8080 tomcat:8.5-jre8-alpine
