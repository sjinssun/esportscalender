package com.example.esportscalendar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EsportscalendarApplication {

	public static void main(String[] args) {
		SpringApplication.run(EsportscalendarApplication.class, args);
	}

}
//.run 하면 내부적으로 applicationcontext 생성, @repository, @controller, @service 등을 Bean으로 등록함