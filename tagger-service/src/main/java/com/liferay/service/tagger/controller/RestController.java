package com.liferay.service.tagger.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.liferay.service.tagger.service.ElasticService;

@Controller
@ResponseBody
public class RestController {
	
	@Autowired
	private ElasticService esService;
	@Value("${com.liferay.tagger.elasticservice.host}")
	private String elastichost;
	@Value("${com.liferay.tagger.elasticservice.port}")
	private Integer elasticport;
	@Value("${com.liferay.tagger.elasticservice.clustername}")
	private String clustername;

	@RequestMapping("/rest/version")
    public String version() {
		//TODO auto-update based on git version/tag
        return "version 0.1";
    }
	
	@RequestMapping("/rest/test")
    public String index() {
        return "Greetings from Spring Boot!";
    }
	
	@RequestMapping("/rest/test/connection")
    public String connection() {
		try {
			return esService.testESConnection();
		} catch (Exception ex) {
			return elastichost + ":" + elasticport + "|" + clustername + "|" + ex.getMessage();
		}
    }
	
	@RequestMapping("/rest/init")
    public String init() {
		try {
			return esService.init();
		} catch (Exception ex) {
			return elastichost + ":" + elasticport + "|" + clustername + "|" + ex.getMessage();
		}
    }
	
	@RequestMapping(value = "/rest/tags", method = RequestMethod.POST)	
    public String getTags(@RequestParam String text) {
		try {
			return esService.getReverseQueryResultFromText(text);
		} catch (Exception ex) {
			return elastichost + ":" + elasticport + "|" + clustername + "|" + ex.getMessage();
		}
    }
	
	@RequestMapping(value = "/rest/tags/list", method = RequestMethod.GET)	
    public String getTagsList() {
		try {
			return esService.getList();
		} catch (Exception ex) {
			return elastichost + ":" + elasticport + "|" + clustername + "|" + ex.getMessage();
		}
    }
	
	@RequestMapping(value = "/rest/tags/{id}", method = RequestMethod.DELETE)	
    public String removeQuery(@PathVariable String id) {
		try {
			return esService.removeQuery(id);
		} catch (Exception ex) {
			return elastichost + ":" + elasticport + "|" + clustername + "|" + ex.getMessage();
		}
    }
	
	@RequestMapping(value = "/rest/tags/{id}", method = RequestMethod.POST)	
    public String upsertQuery(@PathVariable String id,@RequestParam String query) {
		try {
			return esService.upsertSimpleQuery(id,query);
		} catch (Exception ex) {
			return elastichost + ":" + elasticport + "|" + clustername + "|" + ex.getMessage();
		}
    }
	
	@RequestMapping(value = "/rest/tags/{id}/json", method = RequestMethod.POST)	
    public String upsertJSONQuery(@PathVariable String id,@RequestParam String jsonquery) {
		try {
			return esService.upsertJSONQuery(id,jsonquery);
		} catch (Exception ex) {
			return elastichost + ":" + elasticport + "|" + clustername + "|" + ex.getMessage();
		}
    }
	
	@RequestMapping(value = "/rest/tags/{id}/lucene", method = RequestMethod.POST)	
    public String upsertLuceneQuery(@PathVariable String id,@RequestParam String query) {
		try {
			// queries like "+John -Doe OR Janette"
			return esService.upsertLuceneQuery(id,query);
		} catch (Exception ex) {
			return elastichost + ":" + elasticport + "|" + clustername + "|" + ex.getMessage();
		}
    }
	
	@RequestMapping(value = "/rest/user/{uid}", method = RequestMethod.POST)	
    public String upsertUserQuery(@PathVariable String uid,@RequestParam String query) {
		try {
			return esService.upsertSimpleUserQuery(uid,query);
		} catch (Exception ex) {
			return elastichost + ":" + elasticport + "|" + clustername + "|" + ex.getMessage();
		}
    }
	
	@RequestMapping(value = "/rest/alerts", method = RequestMethod.POST)	
    public String getAlerts(@RequestParam String text) {
		try {
			return esService.getAlerts(text);
		} catch (Exception ex) {
			return elastichost + ":" + elasticport + "|" + clustername + "|" + ex.getMessage();
		}
    }
	
}
