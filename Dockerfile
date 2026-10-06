# ===== Bước 1: build file .jar bằng Maven =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml trước và tải thư viện -> lần build sau nếu pom không đổi thì Docker dùng lại cache, build nhanh hơn
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Copy source rồi build (bỏ qua test vì test cần kết nối DB)
COPY src ./src
RUN mvn -q -B package -DskipTests

# ===== Bước 2: image chạy app, chỉ chứa JRE + file .jar (nhẹ hơn nhiều so với image build) =====
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Gói free của Render chỉ có 512MB RAM -> giới hạn JVM dùng tối đa 75% RAM của container
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
