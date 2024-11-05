package com.example.download_upload_gcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
public class DownloadUploadGcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(DownloadUploadGcpApplication.class, args);
	}

}
