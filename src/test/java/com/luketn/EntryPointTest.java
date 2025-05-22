package com.luketn;

import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;
import com.luketn.aws.LambdaS3Client;
import com.luketn.util.EnvironmentUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EntryPointTest {
    @Test
    public void testHandleRequest() {
        // Mock dependencies
        LambdaS3Client mockS3Client = mock(LambdaS3Client.class);
        try (MockedStatic<EnvironmentUtils> mockedEnvUtils = mockStatic(EnvironmentUtils.class)) {
            mockedEnvUtils.when(() -> EnvironmentUtils.getEnv("TEST_BUCKET_NAME", "luketn-java-lambda-template")).thenReturn("custom-bucket-name");

            // Create the request event
            ApplicationLoadBalancerRequestEvent requestEvent = new ApplicationLoadBalancerRequestEvent();

            // Instantiate EntryPoint with mocked dependencies
            EntryPoint entryPoint = new EntryPoint(mockS3Client);

            // Invoke the handleRequest method
            ApplicationLoadBalancerResponseEvent responseEvent = entryPoint.handleRequest(requestEvent, new LocalRunner.LocalContext());

            // Capture upload parameters
            ArgumentCaptor<String> bucketNameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockS3Client).upload(bucketNameCaptor.capture(), keyCaptor.capture(), bodyCaptor.capture());

            // Validate S3 upload call
            assertEquals("custom-bucket-name", bucketNameCaptor.getValue());
            assertEquals("test", keyCaptor.getValue());
            assertEquals("Hello", bodyCaptor.getValue());

            // Validate response
            assertNotNull(responseEvent);
            assertEquals(200, responseEvent.getStatusCode());
            assertFalse(responseEvent.getIsBase64Encoded());
            String expectedStart = "Test lambda ran successfully.";
            assertEquals(expectedStart, responseEvent.getBody().substring(0, expectedStart.length()));
            assertEquals(Collections.singletonMap("Content-Type", "text/plain"), responseEvent.getHeaders());
        }
    }

    @Test
    public void testHandleRequestWithoutUpload() {
        // Mock dependencies
        LambdaS3Client mockS3Client = mock(LambdaS3Client.class);
        try (MockedStatic<EnvironmentUtils> mockedEnvUtils = mockStatic(EnvironmentUtils.class)) {
            mockedEnvUtils.when(() -> EnvironmentUtils.getEnv("TEST_BUCKET_NAME", "luketn-java-lambda-template")).thenReturn("custom-bucket-name");

            // Create the request event with query parameter "dont_upload=true"
            ApplicationLoadBalancerRequestEvent requestEvent = new ApplicationLoadBalancerRequestEvent();
            requestEvent.setQueryStringParameters(Collections.singletonMap("dont_upload", "true"));

            // Instantiate EntryPoint with mocked dependencies
            EntryPoint entryPoint = new EntryPoint(mockS3Client);

            // Invoke the handleRequest method
            ApplicationLoadBalancerResponseEvent responseEvent = entryPoint.handleRequest(requestEvent, new LocalRunner.LocalContext());

            // Verify upload was never called
            verify(mockS3Client, never()).upload(anyString(), anyString(), anyString());

            // Validate response
            assertNotNull(responseEvent);
            assertEquals(200, responseEvent.getStatusCode());
            assertFalse(responseEvent.getIsBase64Encoded());
            String expectedStart = "Test lambda ran successfully.";
            assertEquals(expectedStart, responseEvent.getBody().substring(0, expectedStart.length()));
            assertEquals(Collections.singletonMap("Content-Type", "text/plain"), responseEvent.getHeaders());
        }
    }

    @Test()
    public void testHandleRequestWithException() {
        // Mock dependencies
        LambdaS3Client mockS3Client = mock(LambdaS3Client.class);
        try (MockedStatic<EnvironmentUtils> mockedEnvUtils = mockStatic(EnvironmentUtils.class)) {
            mockedEnvUtils.when(() -> EnvironmentUtils.getEnv("TEST_BUCKET_NAME", "luketn-java-lambda-template")).thenReturn("custom-bucket-name");

            // Simulate S3 upload throwing an exception
            doThrow(new RuntimeException("S3 upload failed")).when(mockS3Client).upload(anyString(), anyString(), anyString());

            // Create the request event
            ApplicationLoadBalancerRequestEvent requestEvent = new ApplicationLoadBalancerRequestEvent();

            // Instantiate EntryPoint with mocked dependencies
            EntryPoint entryPoint = new EntryPoint(mockS3Client);

            // Invoke the handleRequest method, expecting an exception
            assertThrows(RuntimeException.class, () ->
                    entryPoint.handleRequest(requestEvent, new LocalRunner.LocalContext())
            );
        }
    }
}