//package uk.ac.rothamsted.knetminer.backend.cypher;
//
//import org.neo4j.driver.v1.AuthTokens;
//import org.neo4j.driver.v1.Driver;
//import org.neo4j.driver.v1.GraphDatabase;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * TODO: comment me!
// *
// * @author brandizi
// * <dl><dt>Date:</dt><dd>30 Jan 2019</dd></dl>
// *
// */
//@Configuration
//public class SpringConfiguration
//{
//	@Bean ( name = "neoDriver" )
//	public Driver getNeoDriver ( 
//		@Value ( "#{ systemProperties [ 'knetminer.neo4j.url' ] }" ) String url, 
//		@Value ( "#{ systemProperties [ 'knetminer.neo4j.user' ] }" )	String user,
//		@Value ( "#{ systemProperties [ 'knetminer.neo4j.password' ] }" ) String password 
//	)
//	{
//		return GraphDatabase.driver ( url, AuthTokens.basic ( url, password ) );
//	}
//}
