package com.andromeda.createURLShortner.CreateURLLambda;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class App implements RequestHandler<Map<String, Object>, Map<String, String>>
{

	private final ObjectMapper objectMapper = new ObjectMapper();
	
	private final S3Client s3client =  S3Client.builder().build();
	
	@Override
	public Map<String, String> handleRequest(Map<String, Object> input, Context context) {
		String body = input.get("body").toString();
		Map<String, String> bodyMap;
		try 
		{
			bodyMap = objectMapper.readValue(body, Map.class);
		}
		catch(JsonProcessingException e) 
		{
			throw new RuntimeException("Error parsing JSON body: " + e.getMessage(), e);
		}
		
		String originalURL = bodyMap.get("originalUrl");
		String expirationTime = bodyMap.get("expirationTime");
		
		long expirationTimeInSeconds = Long.parseLong(expirationTime);
		
		UrlData urlData = new UrlData(originalURL, expirationTimeInSeconds);
		String shortURLCode = UUID.randomUUID().toString().substring(0, 8);
		
		try 
		{
			String urlDataJson = objectMapper.writeValueAsString(urlData);
			
			PutObjectRequest request = PutObjectRequest.builder()
					.bucket("torumen-url-shortener")
					.key(shortURLCode + ".json")
					.build();
			s3client.putObject(request, RequestBody.fromString(urlDataJson));
		}
		catch (Exception e)
		{
			throw new RuntimeException("Error Saving Data to S3: " + e.getMessage(), e);
		}
		
		
		// Fake response
		
		Map<String, String> response = new HashMap<>();
		response.put("code", shortURLCode);
		
		return response;
	}
}
