package com.newsai.news_ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication	// Đây là application Spring Boot chính.
public class NewsAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(NewsAiApplication.class, args);
		/* start application
					↓
			scan các class
					↓
			tạo Spring beans
					↓
			cấu hình web server
					↓
			start embedded server */
	}

}
