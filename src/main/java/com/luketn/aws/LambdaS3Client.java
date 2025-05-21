package com.luketn.aws;

import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface LambdaS3Client {
    void upload(String bucketName, String key, String data);
    long initializationTime();

    static LambdaS3Client createAws() {
        return new LambdaS3ClientAws();
    }
    static LambdaS3Client createLocal() {
        return new LambdaS3ClientLocal();
    }

    class LambdaS3ClientAws implements LambdaS3Client {
        private final S3Client s3Client = intialize();
        private long initializationTime;

        private S3Client intialize() {
            long start = System.currentTimeMillis();
            S3Client client = S3Client.builder()
                        .region(Region.of(System.getenv(SdkSystemSetting.AWS_REGION.environmentVariable())))
                        .httpClient(UrlConnectionHttpClient.create())
                        .build();
            long end = System.currentTimeMillis();
            initializationTime = end - start;
            return client;
        }

        @Override
        public long initializationTime() {
            return this.initializationTime;
        }

        public void upload(String bucketName, String key, String data) {
            try {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data.getBytes(StandardCharsets.UTF_8)));
            } catch (S3Exception e) {
                throw new RuntimeException("Failed to upload receipt to S3: " + e.awsErrorDetails().errorMessage(), e);
            }
        }
    }

    //create another static class that writes files to the local directory 's3/bucket/key'
    class LambdaS3ClientLocal implements LambdaS3Client {
        @Override
        public void upload(String bucketName, String key, String data) {
            System.out.println("Writing to local directory: " + bucketName + "/" + key);
            try {
                Path bucketPath = Paths.get("xavier-java-lambda-template", "s3", bucketName);
                Files.createDirectories(bucketPath);
                Path filePath = Paths.get(bucketPath.toString(), key);
                Files.writeString(filePath, data);
            } catch (Exception e) {
                throw new RuntimeException("Failed to write to local directory: " + e.getMessage(), e);
            }
        }

        @Override
        public long initializationTime() {
            return 0;
        }

    }
}