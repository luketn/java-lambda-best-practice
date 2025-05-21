echo "Building java lambda..."
mvn --batch-mode \
    clean package \
    -DskipTests \
    -Dmaven.test.skip=true

echo "Built shaded maven JAR:"
ls -lah ./target/java-lambda-template.jar

echo "Running with local runner (note you need to paste in environment variable AWS credentials first)..."
AWS_REGION=ap-southeast-2 \
   java \
   -XX:StartFlightRecording=filename=recording.jfr,duration=120s,settings=ProfilingIntense.jfc \
   -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints \
   -XX:FlightRecorderOptions=stackdepth=256 \
   -cp ./target/java-lambda-template.jar \
   com.luketn.LocalRunner
jmc recording.jfr