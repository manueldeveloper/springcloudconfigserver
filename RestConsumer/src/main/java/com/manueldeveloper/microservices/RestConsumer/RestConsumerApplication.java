package com.manueldeveloper.microservices.RestConsumer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@EnableCircuitBreaker
@EnableZuulProxy
@EnableDiscoveryClient
@SpringBootApplication
public class RestConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(RestConsumerApplication.class, args);
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
	@RequestMapping("/names")
	public Collection<String> getReservationNames(){
		
		ParameterizedTypeReference<Resources<Reservation>> ptr= new ParameterizedTypeReference<Resources<Reservation>>() {};
		
		ResponseEntity<Resources<Reservation>> entity= this.request.exchange("http://config-server-client/reservations", HttpMethod.GET, null, ptr);
		
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
