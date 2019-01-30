Pre-requirements
----------------
You need:
- Java SDK (Java 7+, tested with up to Java 8)
- Apache Ant (Tested with 1.10.5, older version might work as well)
- MySQL database (Tested with MariaDB Ver 15.1 Distrib 10.1.37, but any MySQL-compatible database should work)
- Java Servlet Container Server (Tested with Apache Tomcat 9.0.6, but other versions should work as well)

Additional compile time dependencies (please make sure your tomcat.home variable has been set correctly in your build.properties):
- servlet-api.jar
- catalina-ant.jar

Compile-Howto
-------------

1) Clone this repository and the ca_frontend repository (i.e., https://github.com/otula/apilta/tree/master/ca_frontend).

2) Modify the build.properties file (for this service) and the build-core.properties (for ca_frontend) to match your compile setup.

3) Modify sql username and password values in database.properties file found in ca_frontend/conf.

4) Modify system.properties file found in ca_frontend/conf and update core.tut.pori.properties.bind_address and service.tut.pori.users.register_password.

5) Optionally, check the README file at ca_frontend root directory for other options for the core files.

6) There are a few choices to compile the application. The compiled .war and .jar files can be found in the dist directory if you want to manually copy them to your server.

   a. Compile/deploy the application, e.g. run `ant -f build.xml package` in the uimahalliService root directory.    
   b. Instead of step `a.`, you may use the custom-service ant script. Compilation in this case is done by running `ant -f build-custom-service.xml package` in the uimahalliService root directory. See notes below.

### Custom Service Ant Script
Folder `conf/custom-service` contains the minimal amount of configuration files that need to be modified for the uimahalliService to work. Variables that contain text `custom-service` should be modified. The build property file `conf/custom-service/build.properties_custom-service` includes the minimal required options for uimahalliService.

Please note that steps 3 and 4 on the instructions on *initial-setup* below will not be applicable as those files will not be copied to the service package. Also, *accessing the service* will be different, and will match to the name you specified on property `app.name` in file `conf/custom-service/build.properties_custom-service` (i.e., without further changes, the url portion will be "custom-service" instead of "CAFrontEnd").

Initial-Setup
-------------

1) The core SQL script can be found in ca_frontend/db_scripts/sql (db_initial.db). Add it to your database.

2) uimahalliService/sql contains the database file (10_database_initial.sql) for this service. Add it your database.

3) Go to http://yourserveraddress:port/CAFrontEnd/register.html and create a new user, the registeration password is whatever you typed to service.tut.pori.users.register_password. The "CAFrontEnd" URI part might be different if you modified the application name in any of the build property files.

4) Optionally, add the existing dataset (database_dump.sql) from kiiaudata repository (https://github.com/otula/kiiaudata/tree/master/datasets). Note: if you use a pre-existing dataset, the "user_id" value in the table "uh_locations" and "uh_meters" must match a valid user on your setup. You can check the user_ids of your created users from the "users" table (all tables are in ca_frontend database).

Accessing The Service
---------------------

- The Measurement web page is available at http://yourserveraddress:port/CAFrontEnd/MittariWeb
- The REST APIs are available at http://yourserveraddress:port/CAFrontEnd/rest/ (e.g. GET http://yourserveraddress:port/CAFrontEnd/rest/uimahalli/MeasurementInterface)

### Example Content for Posting New Data
```
{"meters":[{"gauges":[{"id":"00000000-0000-0000-0000-000000000000","gaugeValues":[{"value":"3900000","updated":"2019-01-15T12:00:00+0200"}]}],"id":"3dbdd94e000104e0"}]}
```

General Notes
-------------

The ca_frontend was originally developed for content analysis purposes. Thus, it contains files, settings and database tables (e.g. back ends, tasks, Apache Solr configurations) not required for this particular service. You can freely remove the unnecessary parts or simply ignore the settings (or leave to default values).

