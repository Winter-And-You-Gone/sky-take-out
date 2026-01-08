package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sky.alioss")
public class AliyunOSSProperties {
    private String endpoint;
    private String bucketName;
    private String region;
    private String customDomain;
}
