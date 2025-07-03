package com.radcns.examples.s3.config;

import com.radcns.examples.s3.config.properties.AwsProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.utils.StringUtils;

@Slf4j
@Configuration
@AllArgsConstructor
public class S3Config {

    private final AwsProperties awsProperties;

    @Bean("awsCredentials")
    public AwsCredentialsProvider awsCredentialsProvider() {
            return DefaultCredentialsProvider.create();
    }

    @Bean("s3AsyncClient")
    public S3AsyncClient s3AsyncClient(@Qualifier("awsCredentials") AwsCredentialsProvider awsCredentialsProvider) {
        return S3AsyncClient
                .builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(Region.of(awsProperties.getRegion()))
                .build();
    }

    @Bean("s3Client")
    public S3Client s3Client(@Qualifier("awsCredentials") AwsCredentialsProvider awsCredentialsProvider) {
        return S3Client
                .builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(Region.of(awsProperties.getRegion()))
                .build();
    }

}
