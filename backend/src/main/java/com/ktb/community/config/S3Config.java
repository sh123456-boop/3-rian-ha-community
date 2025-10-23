package com.ktb.community.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Value("${aws.region}")
    String region;
    @Value("${aws.access_key}")
    String access_key;
    @Value("${aws.secret_key}")
    String secret_key;

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(access_key, secret_key)
                ))
                .build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                // 후에 ec2 배포시 DefaultCredentialsProvider 방식으로 자격증명을 다시 해야함.
                // ec2에 적절한 iam 역할 부여 필수
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(access_key, secret_key)
                ))
                .build();
    }

}
