echo "Building java lambda with binary dependencies only for ARM 64 lambda env (minimizing bundle)..."
mvn --batch-mode \
    -P crt-linux-arm64 \
    clean package \
    -DskipTests \
    -Dmaven.test.skip=true

echo "Built shaded maven JAR:"
ls -lah ./target/java-lambda-template.jar
