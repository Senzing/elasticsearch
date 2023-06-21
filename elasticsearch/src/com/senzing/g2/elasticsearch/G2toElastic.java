package com.senzing.g2.elasticsearch;

import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest.Builder;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.util.*;

import com.senzing.g2.engine.G2JNI;
import com.senzing.g2.engine.Result;

import java.io.StringReader;
import java.io.InputStream;
import java.io.Reader;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.client.RestClient;
import org.apache.http.HttpHost;
import org.apache.commons.io.IOUtils;

public class G2toElastic
{
	public static void main(String[] args)
	{
		// define ElasticSearch index information
		//String elasticSearchClustername = "elasticsearch_test";
		String elasticSearchHostname = System.getenv("ELASTIC_HOSTNAME");		// The hostname for the elasticsearch instance
		int elasticSearchPortNumber = Integer.parseInt(System.getenv("ELASTIC_PORT"));	// the exposed port for elasticsearch
		String elasticSearchIndexName = System.getenv("ELASTIC_INDEX_NAME");		// This value can be whatever you want, adhering to elasticsearch's index syntax
		
		System.out.println("****Program started****");
		System.out.println("Initalizing G2");

		//****************************Creating G2Engine instance********************
		// define G2 connecting information
		String moduleName = "G2ElasticSearch";
		boolean verboseLogging = false;
		String SENZING_ENGINE_CONFIGURATION_JSON = System.getenv("SENZING_ENGINE_CONFIGURATION_JSON");
		int returnValue = 0;
		
		//System.out.println(SENZING_ENGINE_CONFIGURATION_JSON);

		// Connect to the G2 Engine
		System.out.println("Connecting to G2 engine.");
		G2JNI g2Engine = new G2JNI();
		returnValue = g2Engine.init(moduleName, SENZING_ENGINE_CONFIGURATION_JSON, verboseLogging);
		if (returnValue != 0)
		{
			System.out.println("Could not connect to G2");
			System.out.println("Return Code = "+returnValue);
			System.out.println("Exception Code = "+g2Engine.getLastExceptionCode());
			System.out.println("Exception = "+g2Engine.getLastException());
			return;
		}

		try {
			//****************************Creating elasticsearch objects********************
			System.out.println("Making elasticsearch clients");
			// Create the low-level client
			RestClient restClient = RestClient.builder(
			    new HttpHost(elasticSearchHostname, elasticSearchPortNumber)).build();

			// Create the transport with a Jackson mapper
			ElasticsearchTransport transport = new RestClientTransport(
			    restClient, new JacksonJsonpMapper());

			// And create the API client
			ElasticsearchClient esClient = new ElasticsearchClient(transport);

			ElasticsearchIndicesClient eiClient = new ElasticsearchIndicesClient(transport);
				
			BulkIngester<Void> ingester = BulkIngester.of(b -> b
				.client(esClient)
				.maxOperations(25)				// This setting changes how many documents get sent at a times
				.flushInterval(250, TimeUnit.MILLISECONDS)	// This setting changes how often the ingester gets flushed
			);
    			
			long g2EntityFlag = g2Engine.G2_ENTITY_INCLUDE_RECORD_JSON_DATA;
			long g2ExportFlag = g2Engine.G2_EXPORT_INCLUDE_ALL_ENTITIES;
			Result<Long> exportHandle = new Result<Long>();
			
			returnValue = g2Engine.exportJSONEntityReport(g2EntityFlag | g2ExportFlag, exportHandle);
			if(returnValue!=0){
				System.out.println("Could not export JSON report");
				System.out.println("Return Code = "+returnValue);
				System.out.println("Exception Code = "+g2Engine.getLastExceptionCode());
				System.out.println("Exception = "+g2Engine.getLastException());
				return;
			}
			StringBuffer entity = new StringBuffer();
			
			System.out.println("Indexing entities");
			while(true){
				g2Engine.fetchNext(exportHandle.getValue(), entity);
				if(entity.length()<=0)
					break;
				G2EntityData entityData = new G2EntityData(entity.toString());
				Reader input = new StringReader(entityData.getRecordData());
				BinaryData data = BinaryData.of(IOUtils.toByteArray(input), ContentType.APPLICATION_JSON);
				
				// This ingester does bulk indexes
				ingester.add(op -> op
					.index(idx -> idx
						.index(elasticSearchIndexName)
						.document(data)
					)
				);
				/* The IndexRequest does single indexes
				IndexRequest entityRequest = IndexRequest.of(i -> i
				    .index(elasticSearchIndexName)
				    .withJson(input)
				);
				IndexResponse entityResponse = esClient.index(entityRequest);*/
			}
			ingester.close();
			System.out.println("Finished indexing");	
			
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}

		// close the G2 engine instance
		System.out.println("Closing G2 engine interface.");
		if (g2Engine != null)
		{
			returnValue = g2Engine.destroy();
			if (returnValue != 0)
			{
				System.out.println("Could not disconnect from G2");
				System.out.println("Return Code = "+returnValue);
				System.out.println("Exception Code = "+g2Engine.getLastExceptionCode());
				System.out.println("Exception = "+g2Engine.getLastException());
				return;
			}
			g2Engine = null;
		}
		System.out.println("****Program complete****");
		System.exit(0);
	}
}
