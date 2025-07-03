package com.radcns.examples.s3;

import com.radcns.examples.s3.config.properties.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class S3Application {
	public static void main(String[] args) {
		SpringApplication.run(S3Application.class, args);
	}

}

@Slf4j
@RestController
@RequiredArgsConstructor
class SampleController {

	private final AwsProperties awsProperties;

	@GetMapping("/")
	public String hello() {
		log.info("region : {}", awsProperties.getRegion());
		log.info("bucket : {}", awsProperties.getBucket());
		log.info("upload-path : {}", awsProperties.getUploadPath());
		return "Hello World";
	}
}
