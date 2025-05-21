package com.luketn;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.luketn.aws.LambdaS3Client;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class LocalRunner {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0);

        EntryPoint entryPoint = new EntryPoint(LambdaS3Client.createAws());
        server.createContext("/java-lambda-template", exchange -> {
            try {
                ApplicationLoadBalancerRequestEvent requestEvent = new ApplicationLoadBalancerRequestEvent();

                //convert exchange to request event
                requestEvent.setPath(exchange.getRequestURI().getPath());
                requestEvent.setHttpMethod(exchange.getRequestMethod());

                //handle query string parameters
                String query = exchange.getRequestURI().getQuery();
                if (query != null) {
                    Map<String, String> queryParameters = parseQueryString(query);
                    requestEvent.setQueryStringParameters(queryParameters);
                }

                if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    byte[] body = exchange.getRequestBody().readAllBytes();
                    String base64EncodedBody = Base64.getEncoder().encodeToString(body);
                    requestEvent.setBody(base64EncodedBody);
                    requestEvent.setIsBase64Encoded(true);
                }

                ApplicationLoadBalancerResponseEvent response = entryPoint.handleRequest(requestEvent, new LocalContext());

                response.getHeaders().forEach((key, value) -> exchange.getResponseHeaders().add(key, value));
                if (response.getBody().isEmpty()) {
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    byte[] bytes;
                    if (response.getIsBase64Encoded()) {
                        bytes = Base64.getDecoder().decode(response.getBody());
                    } else {
                        bytes = response.getBody().getBytes(StandardCharsets.UTF_8);
                    }
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                }
                exchange.close();
            } catch (IOException e) {
                System.out.println("Error handling request: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println(" Server started on http://localhost:8001/java-lambda-template");
    }


    private static Map<String, String> parseQueryString(String query) {
        Map<String, String> queryParameters = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            String key = idx > 0 ? pair.substring(0, idx) : pair;
            String value = idx > 0 && pair.length() > idx + 1 ? pair.substring(idx + 1) : null;
            queryParameters.put(key, value);
        }
        return queryParameters;
    }

    public static class LocalLambdaLogger implements LambdaLogger {
        @Override
        public void log(String message) {
            System.out.println(message);
        }

        @Override
        public void log(byte[] message) {
            System.out.println(new String(message));
        }

        @Override
        public void log(String message, LogLevel logLevel) {
            log(message);
        }

        @Override
        public void log(byte[] message, LogLevel logLevel) {
            log(message);
        }
    }

    public static class LocalContext implements Context {
        @Override
        public String getAwsRequestId() {
            return "";
        }

        @Override
        public String getLogGroupName() {
            return "";
        }

        @Override
        public String getLogStreamName() {
            return "";
        }

        @Override
        public String getFunctionName() {
            return "";
        }

        @Override
        public String getFunctionVersion() {
            return "";
        }

        @Override
        public String getInvokedFunctionArn() {
            return "";
        }

        @Override
        public CognitoIdentity getIdentity() {
            return null;
        }

        @Override
        public ClientContext getClientContext() {
            return null;
        }

        @Override
        public int getRemainingTimeInMillis() {
            return 0;
        }

        @Override
        public int getMemoryLimitInMB() {
            return 0;
        }

        @Override
        public LambdaLogger getLogger() {
            return new LocalLambdaLogger();
        }
    }
}

