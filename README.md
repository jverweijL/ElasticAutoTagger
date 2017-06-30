# ElasticAutoTagger
A Liferay module and rest-service for enabling auto-tagging to Liferay DXP

## Requirements
 - Liferay IDE
 - Liferay DXP
 - Elasticsearch (not embedded)

## How to install/setup rest-service
 1. First create a war file of the tagger-service
 1. Install the war file in your application-server (this example uses Tomcat)
 1. Configure the rest-service
 
 ```
 com.liferay.tagger.elasticservice.port=9300
 com.liferay.tagger.elasticservice.host=127.0.0.1
 com.liferay.tagger.elasticservice.clustername=liferay-cluster
 ```

## How to install/setup module
 1. First build a jar using gradle
 1. Deploy the jar into the Liferay deploy folder
 1. Configure the new module by adding the following parameters in your portal-ext.properties
 
```
com.liferay.tagger.service.url=http://127.0.0.1:8080/tagger-service/rest/tags
com.liferay.tagger.has-tag=autotag
```
 4. If you don't specify `com.liferay.tagger.has-tag` it will auto-tag on each and every webcontent item
