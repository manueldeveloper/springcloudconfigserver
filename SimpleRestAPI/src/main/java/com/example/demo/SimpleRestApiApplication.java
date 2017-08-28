package com.example.demo;

import java.util.Collection;
import java.util.stream.Stream;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class SimpleRestApiApplication {

	@Bean
	CommandLineRunner commandLineRunner(ReservationRepository reservationRepository) {
		
		return strings -> {
			Stream.of("Josh", "Peter", "Susi")
			.forEach(n -> reservationRepository.save(new Reservation(n)));
		};
	}
	
	public static void main(String[] args) {
		SpringApplication.run(SimpleRestApiApplication.class, args);
	}
}

@RepositoryRestResource
interface ReservationRepository extends JpaRepository<Reservation, Long>{
	@RestResource(path="by-name")
	Collection<Reservation> findByReservationName(@Param("rn") String rn);
}


@Entity
class Reservation{
	
	@Id
	@GeneratedValue
	private Long id;
 	private String reservationName;
 	
 	public Reservation() {
 	}
 	
 	public Reservation(String n) {
 		this.reservationName= n;
 	}
 	
 	public Long getId() {
 		return id;
 	}
 	
 	public String getReservationName() {
 		return reservationName;
 	}
 	
 	@Override
 	public String toString() {
 		return "Reservation{" +
 				"id= " + id +
 				", reservationName= '" + reservationName + "'";
 	}
}


@RefreshScope
@RestController
class MessageRestController {
	
	@Value("${msg:Hello world - Config Server is not working..pelase check}")
	private String msg;
	
	@RequestMapping("/msg")
	String getMsg() {
		return this.msg;
	}
}
