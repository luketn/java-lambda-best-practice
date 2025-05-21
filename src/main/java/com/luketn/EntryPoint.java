package com.luketn;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;
import com.luketn.aws.LambdaS3Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.crac.Core;
import org.crac.Resource;

import static com.luketn.util.EnvironmentUtils.getEnv;

/**
 * Lambda handler for processing orders and storing receipts in S3.
 */
public class EntryPoint implements RequestHandler<ApplicationLoadBalancerRequestEvent, ApplicationLoadBalancerResponseEvent>, Resource {
    private static final Logger logger = LoggerFactory.getLogger(EntryPoint.class);

    private final LambdaS3Client s3Client;
    private int counter;
    private long s3InitializationTime;

    public EntryPoint() {
        this(LambdaS3Client.createAws());
    }

    public EntryPoint(LambdaS3Client s3Client) {
        this.s3Client = s3Client;
        this.counter = 0;

        Core.getGlobalContext().register(this);
    }

    @Override
    public ApplicationLoadBalancerResponseEvent handleRequest(ApplicationLoadBalancerRequestEvent event, Context context) {
        try {
            String bucketName = getEnv("TEST_BUCKET_NAME", "luketn-java-lambda-template");

            // Upload the receipt to S3
            var dontUpload = event.getQueryStringParameters() != null && "true".equals(event.getQueryStringParameters().get("dont_upload"));
            long timeTaken = 0;
            if (dontUpload) {
                log("Skipping S3 upload because of query parameter 'dont_upload'");
            } else {
                log("About to upload to bucket " + bucketName);
                var start = System.currentTimeMillis();
                s3Client.upload(bucketName, "test", "Hello");
                var end = System.currentTimeMillis();
                timeTaken = end - start;
                log("Successfully ran demo lambda and stored an object in S3 bucket " + bucketName + " in " + (timeTaken) + "ms");
            }


            String timingInfo;
            if (counter == 0) {
                var approxInitTime = 900; // normal cold init time = ~450ms, SnapStart restore time ~900ms
                var s3InitTime = this.s3InitializationTime;
                var totalTimeApprox = approxInitTime + s3InitTime + timeTaken;
                timingInfo = "Cold! Total time ~%dms (s3 init time %dms, s3 upload time %dms, approx lambda SnapStart restore %dms)".formatted(totalTimeApprox, s3InitTime, timeTaken, approxInitTime);
            } else {
                timingInfo = "Warm! Total time %dms (%d%s)".formatted(timeTaken, counter, counter == 1 ? " prior invocation" : " prior invocations");
            }
            counter++;
            log("Executing lambda took " + timingInfo + ": ");

            ApplicationLoadBalancerResponseEvent applicationLoadBalancerResponseEvent = new ApplicationLoadBalancerResponseEvent();
            applicationLoadBalancerResponseEvent.setStatusCode(200);
            applicationLoadBalancerResponseEvent.setIsBase64Encoded(false);
            applicationLoadBalancerResponseEvent.setBody("Test lambda ran successfully. %s!".formatted(timingInfo));

            applicationLoadBalancerResponseEvent.setHeaders(
                    java.util.Collections.singletonMap("Content-Type", "text/plain")
            );
            return applicationLoadBalancerResponseEvent;

        } catch (Exception e) {
            context.getLogger().log("Unexpected error running the lambda: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void log(String coldInfo) {
        logger.trace(coldInfo);
    }


    public static void main(String[] args) {
        EntryPoint entryPoint = new EntryPoint();
        ApplicationLoadBalancerRequestEvent event = new ApplicationLoadBalancerRequestEvent();
        event.setPath("/java-lambda-template");
        entryPoint.handleRequest(event, null);
        System.exit(0);
    }

    @Override
    public void beforeCheckpoint(org.crac.Context<? extends Resource> context) {
        this.s3InitializationTime = this.s3Client.initializationTime();

        logger.info("About to create SnapStart snapshot. Took {}ms to initalize S3 client.", this.s3InitializationTime);

        //Prime the lambda by running a handler event to upload to S3
        logger.info("Priming lambda by running a handler event to upload to S3.");
        EntryPoint entryPoint = new EntryPoint();
        entryPoint.handleRequest(new ApplicationLoadBalancerRequestEvent(), null);
        logger.info("Finished priming lambda by running a handler event to upload to S3.");
    }

    @Override
    public void afterRestore(org.crac.Context<? extends Resource> context) {
        this.counter = 0;
        this.s3InitializationTime = 0;
        System.out.println("Restored SnapStart snapshot.");
    }
}

