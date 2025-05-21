# Java Lambda Template
This repository provides a template for creating AWS Lambda functions using Java.

## Configuration
After experimenting, 2GB RAM seems like it produces optimal performance/price point for Java (at least an SDK lambda like this).

## Results
Best results for a cold Java lambda S3 upload so far are around ~2s.

There are 2 mysteries:
1. What is the AWS SDK doing during the 1s of SDK init time?
2. Why does the first upload take ~500ms compared with subsequent excellent ~25ms.

```Output
Test lambda ran successfully. Cold! Total time ~1995ms (s3 init time 1039ms, s3 upload time 506ms, approx lambda init 450ms)!
Test lambda ran successfully. Cold! Total time ~1961ms (s3 init time 1032ms, s3 upload time 479ms, approx lambda init 450ms)!
Test lambda ran successfully. Warm! Total time 24ms (3 prior invocations)!
```

```CloudWatch Logs
2025-05-21T03:29:26.330Z
INIT_START Runtime Version: java:21.v38 Runtime Version ARN: arn:aws:lambda:ap-southeast-2::runtime:81e4ff5669ca00936ae2ebcd7e3ee4b820d9f1dec101bbabbb706dc9e1481298
2025-05-21T03:29:27.829Z
START RequestId: 314220ad-40f4-4204-843c-713576ded59a Version: $LATEST
2025-05-21T03:29:28.374Z
END RequestId: 314220ad-40f4-4204-843c-713576ded59a
2025-05-21T03:29:28.374Z
REPORT RequestId: 314220ad-40f4-4204-843c-713576ded59a	Duration: 544.92 ms	Billed Duration: 545 ms	Memory Size: 2048 MB	Max Memory Used: 174 MB	Init Duration: 1494.57 ms	
```
