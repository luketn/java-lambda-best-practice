echo "Building java lambda..."
mvn --batch-mode \
    clean package \
    -DskipTests \
    -Dmaven.test.skip=true

echo "Built shaded maven JAR:"
ls -lah ./target/java-lambda-template.jar

echo "Running with local runner (note you need to paste in environment variable AWS credentials first)..."
time \
   AWS_REGION=ap-southeast-2 \
   JAVA_TOOL_OPTIONS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1" \
   java \
   -XX:+PrintFlagsFinal \
   -XX:StartFlightRecording=filename=recording.jfr,duration=120s,settings=local-runner-profile-intense-settings.jfc \
   -XX:+UnlockDiagnosticVMOptions \
   -XX:+DebugNonSafepoints \
   -XX:FlightRecorderOptions=stackdepth=256 \
   -cp ./target/java-lambda-template.jar \
   com.luketn.EntryPoint

jmc -open recording.jfr
