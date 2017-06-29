package com.liferay.service.tagger.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;

@Service
public class ElasticService {
	
	@Value("${com.liferay.tagger.elasticservice.host}")
	private String elastichost;
	@Value("${com.liferay.tagger.elasticservice.port}")
	private Integer elasticport;
	@Value("${com.liferay.tagger.elasticservice.clustername}")
	private String clustername;

	private static Client esClient = null;
	private static String index = "liferay-autotagger";
	
	public String testESConnection() {
		return getESClient().prepareSearch(index).setSize(0).execute().actionGet().toString();
	}
	
	public String init() {
		
		String response = "";
		//create index if not already done
		try {
			getESClient()
				.admin()
				.indices()
				.prepareCreate(index)
				.get();
		} catch (Exception ex) {
			response += index + " " + ex.getMessage();
		}
		
		///create basic mapping
		try {
			getESClient().admin().indices().preparePutMapping(index)   
	        .setType("my-type")                                
	        .setSource("{\n" +                              
	                "  \"properties\": {\n" +
	                "    \"message\": {\n" +
	                "      \"type\": \"string\"\n" +
	                "    }\n" +
	                "  }\n" +
	                "}")
	        .get();
		} catch (Exception ex) {
			response += " | " + index + " " + ex.getMessage();
		}
		
		//reqister a sample query
		try {
		getESClient().prepareIndex(index, ".percolator","bonsai")
	        .setSource("{\"query\" : {\"match\" : {\"message\" : \"bonsai tree\"}}}")
	        .get();
		} catch (Exception ex) {
			response += " | " + index + " " + ex.getMessage();
		}
		
		// try reverse query with simple document
		if (!response.isEmpty()) {
			response += " | ";
		}
		response += "tags found: " + this.getReverseQueryResultFromText("going for a walk in the tree");
		
		if (!response.isEmpty()) {
			response += " | ";
		}
		return response + "init done";
	}
	
	public String getList() {
		// TODO Add paging 
		return getESClient().prepareSearch(index).setTypes(".percolator").setQuery(QueryBuilders.matchAllQuery()).get().toString();
	}
	
	public String removeQuery(String id) {
		// TODO what if it goes wrong
		return getESClient().prepareDelete(index, ".percolator", id).get().toString();
	}
	
	public String upsertSimpleQuery(String id, String query) {
		QueryBuilder qb = QueryBuilders.matchQuery("message", query);
		
		try {
			XContentBuilder json = XContentFactory.jsonBuilder()
			        .startObject()
		            .field("query", qb) // Register the query
		        .endObject();
			System.out.println(json.string());
			return this.upsertQuery(id, json);
		} catch (Exception ex) {
			return ex.getMessage();
		}
		
		//System.out.println(json.string());
	}
	
	public String upsertLuceneQuery(String id, String query) {
		QueryBuilder qb = QueryBuilders.simpleQueryStringQuery(query);
		
		try {
			XContentBuilder json = XContentFactory.jsonBuilder()
			        .startObject()
		            .field("query", qb) // Register the query
		        .endObject();
			System.out.println(json.string());
			return this.upsertQuery(id, json);
		} catch (Exception ex) {
			return ex.getMessage();
		}
	}
	
	public String upsertJSONQuery(String id, String jsonquery) {
		System.out.println(jsonquery);
		QueryBuilder qb = QueryBuilders.wrapperQuery(jsonquery);
		
		
		try {
			XContentParser parser = JsonXContent.jsonXContent.createParser(jsonquery);
			
			XContentBuilder json = JsonXContent.contentBuilder()//; //.prettyPrint();
					.startObject()
					.field("query") // Register the query
					.copyCurrentStructure(parser)
					.endObject();
	        //json.copyCurrentStructure(parser);
	
			System.out.println(json.string());
			
			return this.upsertQuery(id, json);
		} catch (Exception ex) {
			return ex.getMessage();
		}
	}
	
	public String upsertQuery(String id, XContentBuilder query) {
		
		try {
			//Index the query = register it in the percolator
			
			IndexRequest indexRequest = new IndexRequest(index, ".percolator", id)
			        .source(query);
			UpdateRequest updateRequest = new UpdateRequest(index, ".percolator", id)		
			        .doc(query)
			        .upsert(indexRequest);              
			getESClient().update(updateRequest).get();
			
			return ("done");
			
			
		} catch (Exception ex) {
			return ex.getMessage();
		}
	}
	
	public String getReverseQueryResultFromText(String text) {
		
		String cleantext = text.replaceAll("\"", "").replaceAll("'", "");
		
		PercolateResponse response = getESClient()
										.preparePercolate()
										.setIndices(index)
										.setDocumentType("my-type")
										.setSource("{\"doc\" : {\"message\" : \""+ cleantext + "\"}}").execute().actionGet();
		
		String result = "";
		//Iterate over the results
		for(PercolateResponse.Match match : response) {
		    //We assume the id is the tagname
			if (result != "") {
				result += ",";
			}
			result += match.getId().toString();
		}
		return result;
	}
	
	private Client getESClient() {
		if (esClient == null) {
			try {
				Settings settings = Settings
										.settingsBuilder()
										//.put("client.transport.ignore_cluster_name", true)
										.put("client.transport.sniff", true)
										.put("cluster.name",clustername)
										.build();
						
				esClient = TransportClient
							.builder()
							.settings(settings)
							.build()
							.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(elastichost),elasticport));
			} catch (UnknownHostException ex) {
				esClient = null;
			}
		}
		return esClient;
	}

	

	
}
