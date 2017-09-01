package com.manueldeveloper.microservices.RestConsumer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.sleuth.metric.SpanMetricReporter;
import org.springframework.cloud.sleuth.zipkin.HttpZipkinSpanReporter;
import org.springframework.cloud.sleuth.zipkin.ZipkinProperties;
import org.springframework.cloud.sleuth.zipkin.ZipkinSpanReporter;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import zipkin.Span;

@EnableCircuitBreaker
@EnableZuulProxy
@EnableDiscoveryClient
@SpringBootApplication
public class RestConsumerApplication {

	@Autowired
	private EurekaClient eurekaClient;
	
	@Autowired
	private SpanMetricReporter spanMetricReporter;
	
	@Autowired
	private ZipkinProperties zipkinProperties;
	
	@Value("${spring.sleuth.web.skipPattern}")
	private String skipPattern;
	
	
	public static void main(String[] args) {
		SpringApplication.run(RestConsumerApplication.class, args);
	}
	
	@Bean
	public ZipkinSpanReporter makeZipkinSpanReporter() {
		
		return new ZipkinSpanReporter() {
			
			private HttpZipkinSpanReporter delegate;
			private String baseUrl;
			
			@Override
			public void report(Span span) {
				
				InstanceInfo instance= eurekaClient.getNextServerFromEureka("zipkin-server", false);

				if( !(baseUrl != null && instance.getHomePageUrl().equals(baseUrl)) ) {
					
					baseUrl= instance.getHomePageUrl();
					
					new HttpZipkinSpanReporter(new RestTemplate(), baseUrl, zipkinProperties.getFlushInterval(), spanMetricReporter);
					
					if(!span.name.matches(skipPattern)) {
						delegate.report(span);
					}
				}
			}
		};
	}
}


@RestController
@RequestMapping("/reservations")
class ReservationApiGatewayRestController {
	
	@Autowired
	private RestTemplate request;
	
	public Collection<String> getReservationNamesFallback(){

		return new ArrayList<>();
	}
	
	@HystrixCommand(fallbackMethod = "getReservationNamesFallback")
	@RequestMapping(method = RequestMethod.GET, value = "/names")
	public Collection<String> getReservationNames(){
		
		ParameterizedTypeReference<Resources<Reservation>> ptr= new ParameterizedTypeReference<Resources<Reservation>>() {};
		
		// TODO Change it to use Eureka Service
		ResponseEntity<Resources<Reservation>> entity= this.request.exchange("http://localhost:8080/reservations", HttpMethod.GET, null, ptr);
		
		return entity
				.getBody()
				.getContent()
				.stream()
				.map(Reservation::getReservationName)
				.collect(Collectors.toList());
	}
	
	@Bean
	public RestTemplate restTemplate() {
	    return new RestTemplate();
	}
}


class Reservation{
	
	private String reservationName;
	
	
	public String getReservationName() {
		return this.reservationName;
	}
}
