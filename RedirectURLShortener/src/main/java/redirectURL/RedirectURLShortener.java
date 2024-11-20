package redirectURL;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class RedirectURLShortener implements RequestHandler<Map<String, Object>, Map<String, Object>>{

	private final ObjectMapper objectMapper = new ObjectMapper();
	
	private final S3Client s3client = S3Client.builder().build();
	
	@Override
	public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
		String params = (String) input.get("rawPath");
		String code = params.replace("/", "");
		
		if(code == null || code == "") {
			throw new IllegalArgumentException("Invalid input: Missing shortURLCode.");
		}
		
		GetObjectRequest request = GetObjectRequest.builder()
				.bucket("torumen-url-shortener")
				.key(code + ".json")
				.build();
		
		InputStream s3ObjectStream;
		
		try {
			s3ObjectStream = s3client.getObject(request);
		}catch(Exception e) {
			throw new RuntimeException("Error fetching data from S3." + e.getMessage(), e);
		}
		
		URLData urlData;
		try {
			urlData = objectMapper.readValue(s3ObjectStream, URLData.class);
		}catch(Exception e) {
			throw new RuntimeException("Error Deserialising Data" + e.getMessage(), e);
		}
		
		long currentTimeInSeconds = System.currentTimeMillis() / 1000;
		
		Map<String, Object> response = new HashMap<>();
		if(urlData.getExpirationTime() < currentTimeInSeconds ) {
			response.put("statusCode", 410);
			response.put("body", "This URL has expired");
			return response;
		}
		
		response.put("statusCode", 302);
		Map<String, String> headers = new HashMap<>();
		headers.put("Location", urlData.getOriginalUrl());
		response.put("headers", headers);
		
		return response;
	}
}
