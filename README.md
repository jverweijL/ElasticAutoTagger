# ElasticAutoTagger
A sample for a module and rest-service for enabling auto-tagging to Liferay DXP.

Watch this youtube video about [auto-tagging](https://youtu.be/Pxu06zgiNLc?list=PLp6cS8SjamlP9YYKLrY_iOAGw3b62PNr-) first

## Requirements
 - Liferay IDE
 - Liferay DXP
 - Elasticsearch (not embedded)

## How to install/setup tagger-service rest webservice
 1. First create a war file of the tagger-service
 2. Install the war file in your application-server (this example uses Tomcat)
 3. Configure the rest-service (see also [externalized properties](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html))
 
 ```
 com.liferay.tagger.elasticservice.port=9300
 com.liferay.tagger.elasticservice.host=127.0.0.1
 com.liferay.tagger.elasticservice.clustername=liferay-cluster
 ```

 4. To test the rest-service try `curl http://127.0.0.1:8080/tagger-service/rest/test`
 5. To initialize the rest-service run `curl http://127.0.0.1:8080/tagger-service/rest/init`
 6. Now run `curl -X POST -F text="I love the little tree"  http://127.0.0.1:8080/tagger-service/rest/tags` and it should come back with `bonsai`

## How to install/setup ElasticTagger module
 1. First build a jar in the ElasticTagger project using gradle
 1. Deploy the jar into the Liferay deploy folder
 1. Configure the new module by adding the following parameters in your portal-ext.properties
 
```
com.liferay.tagger.service.url=http://127.0.0.1:8080/tagger-service/rest/tags
com.liferay.tagger.has-tag=autotag
```
 4. If you don't specify `com.liferay.tagger.has-tag` it will auto-tag on each and every webcontent item
