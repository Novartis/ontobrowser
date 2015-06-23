Building and Deploying OntoBrowser
---
## System Requirements
* [Java Development Kit (JDK)](http://www.oracle.com/technetwork/java/javase/overview/) version 8 (or above) - [Download](http://www.oracle.com/technetwork/java/javase/downloads/)
* [Apache Maven](http://maven.apache.org) version 3 (or above) - [Download](http://maven.apache.org/download.cgi)
* [Graphviz](http://www.graphviz.org) version 2.28 (or above) - [Download](http://www.graphviz.org/Download.php)
* [Wildfly](http://wildfly.org) version 8.2 (or above) - [Download](http://wildfly.org/downloads/) [Install](https://docs.jboss.org/author/display/WFLY8/Getting+Started+Guide)

## Database Requirements
OntoBrowser requires access to a relational database supported by Hibernate (the ORM used by Wildfly) e.g. Oracle, MySQL, PostgreSQL etc... See [Supported Databases](https://developer.jboss.org/wiki/SupportedDatabases2) for more information.

**Note:** OntoBrowser has been extensively tested and deployed in production environments using Oracle databases (versions 10g and 11g). It is recommend that system testing is performed prior to using any non-Oracle databases in a production environment.

## Database setup
### Create Schema
The SQL DDL scripts to create the OntoBrowser database schema are located in the [sql](../sql) directory of the project. Use the corresponding DDL script for the selected database e.g. [create_schema_oracle.sql](../sql/create_schema_oracle.sql) for Oracle databases.

The following is an example on how to create the OntoBrowser schema using Oracle's SQL*Plus command line tool:

```bash
$ sqlplus ontobrowser@DEV @create_schema_oracle.sql
```

See the [database design](./database_design.pdf) documentation for the more information regarding the database schema. 

### Insert initial data
The SQL DML scripts to insert initial data into the OntoBrowser database schema are located in the [sql](../sql) directory of the project. Use the corresponding DML script for the selected database e.g. [insert_initial_data_oracle.sql](../sql/insert_initial_data_oracle.sql) for Oracle databases

The following is an example on how to insert initial data into the OntoBrowser schema using Oracle's SQL*Plus command line tool:

```bash
$ sqlplus ontobrowser@DEV @insert_initial_data_oracle.sql
```

The initial data consists of:

* Creating system user/curator
* Setting up version tracking
* Defining the OBO builtin relationship types i.e. is_a, union_of, disjoint_from, part_of etc...
* Inserting common datasources of nomenclature e.g. CDISC SEND, INHAND etc...

## Wildfly Setup
### Install JDBC Driver 
As recommended by the Wildfly [documentation](https://docs.jboss.org/author/display/WFLY8/DataSource+configuration),  install the database type 4 JDBC driver by copying the jar file to the `$JBOSS_HOME/standalone/deployments` directory e.g.

```bash
cp ojdbc7.jar $JBOSS_HOME/standalone/deployments
```
Note: the latest Oracle JDBC driver can be downloaded from the Oracle [website](http://www.oracle.com/technetwork/database/features/jdbc/).
### Datasource Setup
See the [DataSource Configuration](https://docs.jboss.org/author/display/WFLY8/DataSource+configuration) Wildfly documentation. The JNDI name of the datasource must be specifed as `java:jboss/datasources/ontobrowser`.

Below is an example configuration for an Oracle database (from the `$JBOSS_HOME/standalone/configuration/standalone.xml` configuration file):

```xml
<datasource jndi-name="java:jboss/datasources/ontobrowser" pool-name="ontobrowser">
	<connection-url>jdbc:oracle:thin:@localhost:1521:dev</connection-url>
	<driver>ojdbc7.jar</driver>
	<pool>
		<min-pool-size>1</min-pool-size>
		<max-pool-size>8</max-pool-size>
		<prefill>true</prefill>
	</pool>
	<security>
		<user-name>ontobrowser</user-name>
		<password>secret</password>
	</security>
	<validation>
		<valid-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.oracle.OracleValidConnectionChecker"/>
		<stale-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.oracle.OracleStaleConnectionChecker"/>
		<exception-sorter class-name="org.jboss.jca.adapters.jdbc.extensions.oracle.OracleExceptionSorter"/>
	</validation>
</datasource>
```
### Email Setup
By default Wildfly is configured to send email using STMP on localhost port 25. See the [Mail Service Configuration](http://www.mastertheboss.com/jboss-server/jboss-configuration/jboss-mail-service-configuration) documentation to configure a remote STMP server. 

### OntoBrowser Configuration
Edit the `$JBOSS_HOME/standalone/configuration/standalone.xml` configuration file and add the following XML to the `<bindings>` element of the `urn:jboss:domain:naming:2.0` subsystem configuration. Change configuration values to suit the deployment environment:

```xml
<!-- external URL for OntoBrowser web app. Check VirtualHost section in apache conf -->
<simple name="java:global/ontobrowser/url" value="http://localhost/ontobrowser/" type="java.net.URL"/>
<!-- username of system/default curator. Check the CURATOR table in the database -->
<simple name="java:global/ontobrowser/curator/system" value="SYSTEM" type="java.lang.String"/>
<!-- local filesystem path for Lucene index -->
<simple name="java:global/ontobrowser/index/path" value="${jboss.server.data.dir}/ontobrowser/index" type="java.lang.String"/>
<!-- path to dot command line program. Check local installation of Graphviz. -->
<simple name="java:global/ontobrowser/dot/path" value="/usr/local/bin/dot" type="java.lang.String"/>
<!-- external URI for ontologies exported in OWL format -->
<simple name="java:global/ontobrowser/export/owl/uri" value="http://localhost/ontobrowser/ontologies" type="java.net.URL"/>
<!-- boolean flag indicating if OntoBrowser is using an Oracle database -->
<simple name="java:global/ontobrowser/database/oracle" value="true" type="boolean"/>
```

See the [Naming Subsystem Configuration](https://docs.jboss.org/author/display/WFLY8/Naming+Subsystem+Configuration) Wildfly documentation for more information.

### Additional Wildfly configuration (optional)
By default Stateless Session Bean (SLSB) pooling is not enabled in Wildfly. When deploying OntoBrowser in a production environment is it recommended to enable SLSB pooling to prevent a Graphviz dot process being spawned for each request.

Edit the `$JBOSS_HOME/standalone/configuration/standalone.xml` configuration file and add the following XML to the `<session-bean>` element of the `urn:jboss:domain:ejb3:2.0` subsystem:

```xml
<stateless>
    <bean-instance-pool-ref pool-name="slsb-strict-max-pool"/>
</stateless>
```

The resulting XML configuration should be:

```xml
<subsystem xmlns="urn:jboss:domain:ejb3:2.0">
	<session-bean>
		<stateless>
			<bean-instance-pool-ref pool-name="slsb-strict-max-pool"/>
		</stateless>
		...
	</session-bean>
	...
```

By default the Hibernate secondary cache expiration *max-idle* timeout is set to 100,000 milliseconds i.e. 1 minute 40 seconds. In a production environment it is recommended to increase the expiration *max-idle* and *lifespan* timeout to more appropriate values. Note: setting timeout values to -1 disables expiration and hence the cache entries will never expire (however they can still be evicted if *max-entries* is exceeded).

The example Hibernate secondary cache configuration below (from the `$JBOSS_HOME/standalone/configuration/standalone.xml` configuration file) increases the default eviction *max-entries* value from the default of 10,000 to 100,000 and disables  expiration for both the *entity* and *local-query* caches:

```xml
<cache-container name="hibernate" default-cache="local-query" module="org.hibernate">
	<local-cache name="entity">
		<transaction mode="NON_XA"/>
		<eviction strategy="LRU" max-entries="100000"/>
		<expiration lifespan="-1" max-idle="-1"/>
	</local-cache>
	<local-cache name="local-query">
		<transaction mode="NONE"/>
		<eviction strategy="LRU" max-entries="100000"/>
		<expiration lifespan="-1" max-idle="-1"/>
	</local-cache>
	<local-cache name="timestamps">
		<transaction mode="NONE"/>
		<eviction strategy="NONE"/>
	</local-cache>
</cache-container>
```

## Apache Basic Authentication and Proxy Setup (optional)
In a production environment it is recommended to perform the user authentication using a web server (e.g. Apache) located in a [DMZ](http://en.wikipedia.org/wiki/DMZ_(computing)).  Alternatively if installing OntoBrowser on a corporate intranet it is recommended to use a corporate single sign-on (SSO) system for user authentication.

The following example Apache configuration protects the `/ontobrowser` location using [Basic access ](http://en.wikipedia.org/wiki/Basic_access_authentication) and proxies requests (using AJP) to Wildfly running on the same machine:

```xml
<Location /ontobrowser/>
    AuthType Basic
    AuthName "OntoBrowser"
    AuthBasicProvider dbd
    AuthDBDUserPWQuery "SELECT password FROM curator WHERE username = %s"
    Require valid-user
    ProxyPass ajp://localhost:8009/ontobrowser/
    ProxyPassReverse ajp://localhost:8009/ontobrowser/
</Location>
```

Note: the configuration above can alternatively be defined in a `<VirtualHost>` container.

The following is an example Apache DBD configuration for an Oracle database with a SID of DEV:

```
DBDriver oracle
DBDParams user=ontobrowser,pass=secret,server=DEV
DBDMin  2
DBDKeep 4
DBDMax  10
DBDExptime 300
```

For more details see the Apache [mod_authn_dbd](http://httpd.apache.org/docs/2.2/mod/mod_authn_dbd.html) and [mod_dbd](http://httpd.apache.org/docs/2.2/mod/mod_dbd.html) documentation.

## Building and Deploying OntoBrowser
1. Download the project from GitHub: https://github.com/Novartis/ontobrowser/archive/master.zip
2. Unzip the master.zip file
3. Build and package the project using Maven i.e. `mvn package`
4. Copy the `ontobrowser.war` file (located in the `target` directory) to Wildfly's `deployments` directory

**Note:** If using a non-Oracle database perform the changes listed in the [non-oracle_changes.md](./non-oracle_changes.md) file before building the project.

The following bash commands provides and example on how to perform the steps above on a Unix based operating system:

```bash
$ curl -s -S -O -L https://github.com/Novartis/ontobrowser/archive/master.zip
$ unzip master.zip
$ cd ontobrowser-master
$ mvn package
$ cp target/ontobrowser.war $JBOSS_HOME/standalone/deployments
```

Note: Configuring Maven to use a proxy maybe required if behind a corporate firewall. See the Maven documentation on [configuring proxies](http://maven.apache.org/guides/mini/guide-proxies.html).

See the Wildfly [Getting Started Guide](https://docs.jboss.org/author/display/WFLY8/Getting+Started+Guide) on how to startup Wildfly. Below is an example for Unix based operating systems:

```bash
$ cd $JBOSS_HOME/bin
$ ./standalone.sh
```

## Loading an Ontology
Ontologies can be loaded into OntoBrowser using the `/ontobrowser/ontologies` [RESTful](http://en.wikipedia.org/wiki/Representational_state_transfer) web service. The web service only supports the PUT method for loading ontologies and only accepts [OBO formatted](http://oboformat.googlecode.com/svn/trunk/doc/GO.format.obo-1_2.html) data.

The following example downloads the [Mouse adult gross anatomy](http://www.obofoundry.org/cgi-bin/detail.cgi?id=adult_mouse_anatomy) ontology then loads it into OntoBrowser using the web service:

```bash
$ curl -s -S -O -L http://purl.obolibrary.org/obo/ma.obo
$ curl -s -S -H "Content-Type: application/obo;charset=utf-8" -X PUT --data-binary "@ma.obo" -u SYSTEM "http://localhost/ontobrowser/ontologies/Mouse%20adult%20gross%20anatomy"
```

Note: Proxy parameters or environment variables maybe be require when downloading behind a corporate firewall.

## Setup a Controlled Vocabulary
An example SQL script to setup a *controlled vocabulary* is provided in the [sql](../sql) directory of the project: [insert_crtld_vocab_example.sql](../sql/insert_crtld_vocab_example.sql). The example defines the [SEND Specimen](http://evs.nci.nih.gov/ftp1/CDISC/SEND/SEND%20Terminology.html#CL.C77529.SPEC) code list in the database as a *controlled vocabulary* so the terms from the code list can be subsequently loaded (and then mapped to the *Mouse adult gross anatomy* ontology loaded previously).

The following is an example on how to run the example SQL script using Oracle's SQL*Plus command line tool:

```bash
$ sqlplus ontobrowser@DEV @insert_crtld_vocab_example.sql
```

## Loading Controlled Vocabulary Terms
The recommend technique to load *controlled vocabulary* terms is in batch using a dedicated [ETL](http://en.wikipedia.org/wiki/Extract,_transform,_load) tool e.g. [Informatica](http://www.informatica.com), [Kettle](http://community.pentaho.com/projects/data-integration/), [Talend](https://www.talend.com) etc...

The *controlled vocabulary* terms must be loaded into the `CTRLD_VOCAB_TERM` table.

## Add a Curator
The SQL DML scripts to add a curator to the OntoBrowser database schema are located in the [sql](../sql) directory of the project. Use the corresponding DML script for the selected database e.g. [insert_curator_oracle.sql](../sql/insert_curator_oracle.sql) for Oracle databases.

The following is an example on how to add a curator with the username *smith* to the database using Oracle's SQL*Plus command line tool:

```bash
$ sqlplus ontobrowser@DEV @insert_curator_oracle.sql 'smith'
```

