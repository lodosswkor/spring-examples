package com.radcns.examples.s3.config.properties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aws.s3")
@RequiredArgsConstructor
public class AwsProperties {
    private String region;
    private String bucket;
    private String uploadPath;
}
