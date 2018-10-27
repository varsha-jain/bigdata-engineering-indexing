package com.example.demo.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


@RestController
//@RequestMapping("/api")
public class MainController {
	private static final String redisHost = "localhost";
	private static final Integer redisPort = 6379;
	
	//the jedis connection pool..
	private static JedisPool pool = null;
	
	public MainController() {
	    //configure our pool connection
	    pool = new JedisPool(redisHost, redisPort);

	}
	
	@RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }
	
	@RequestMapping(value="/plans/schema",method = RequestMethod.POST, consumes = "application/json")
	public ResponseEntity storeSchema(@RequestHeader HttpHeaders headers, @RequestBody String entity) throws ParseException, IOException{
	
		Jedis jedis = new Jedis("127.0.0.1", 6379);
		if(entity==null) {
		  return new ResponseEntity("No Schema received!",HttpStatus.BAD_REQUEST) ;		 
		}
		jedis.set("Plan_schema", entity );
//		System.out.println(entity);
		HttpHeaders httpHeaders = new HttpHeaders();
		return new ResponseEntity("Schema is posted successfully!", httpHeaders,HttpStatus.CREATED);
	}
	
	@RequestMapping(value="/plan", method=RequestMethod.POST, consumes = "application/json")
	public ResponseEntity addPlan(@RequestHeader HttpHeaders headers,@RequestBody String jsonFile)throws IOException, com.github.fge.jsonschema.core.exceptions.ProcessingException, com.github.fge.jsonschema.core.exceptions.ProcessingException, URISyntaxException{
		Jedis jedis = pool.getResource();
		String schemaFile = jedis.get("Plan_schema");
		if(isValid(schemaFile, jsonFile)){
			UUID idOne = UUID.randomUUID();
			String redisKey="plan-"+idOne;
			jedis.set(redisKey,jsonFile);
			return new ResponseEntity("Data Validation Successful! and New plan is created with ID: "+idOne,HttpStatus.CREATED);
		}
		else
			return new ResponseEntity("Data Validation Invalid ",HttpStatus.BAD_REQUEST);
	}
	
	
	@RequestMapping(value="/plan/{id}", method=RequestMethod.GET)
	public ResponseEntity getPlan(@PathVariable String id) throws ProcessingException, IOException{
		pool = new JedisPool(redisHost, redisPort);
		Jedis jedis = pool.getResource();
		System.out.println(id);
		String result = jedis.get(id);
		if(result == null || result.isEmpty()){
			return new ResponseEntity("No Data Found", HttpStatus.NOT_FOUND);
		}else{
			return new ResponseEntity(result, HttpStatus.ACCEPTED);
		}
	}
	
	@RequestMapping(value="/plan/{id}", method=RequestMethod.DELETE)
	public void deletePlan(@PathVariable String id)throws ProcessingException, IOException {
		Jedis jedis = pool.getResource();
		String result = jedis.get(id);
		jedis.del(id);
		
	}
	
	@RequestMapping(value="/plan/{id}", method=RequestMethod.PUT)
	public ResponseEntity updatePlan(@PathVariable String id, @RequestBody String entity)throws IOException, com.github.fge.jsonschema.core.exceptions.ProcessingException, com.github.fge.jsonschema.core.exceptions.ProcessingException, URISyntaxException{
		Jedis jedis = pool.getResource();
		String schemaFile = jedis.get("Plan_schema");
		
		if(schemaFile==null || id==null){
			return new ResponseEntity("No data found!", HttpStatus.BAD_REQUEST);
		}
		String sid = id;
		if(isValid(schemaFile, entity)){
			jedis.del(id);
			jedis.set(sid,entity);
			return new ResponseEntity("Updated Sucessfully", HttpStatus.CREATED);
		}
		
		return new ResponseEntity("Data Validation Invalid ",HttpStatus.BAD_REQUEST);
	}
	
	
	public boolean isValid(String schemaFile, String jsonFile) throws ProcessingException, IOException{
		if (ValidationUtils.isJsonValid(schemaFile, jsonFile)){
	    	return true;
	    }else{
	    	return false;
	    }
	}
}
	
