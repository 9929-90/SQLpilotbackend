# ---------- Stage 1: Build ----------

FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom first (better caching)

COPY pom.xml .
RUN mvn -B -q -e -DskipTests dependency:go-offline

# Copy source

COPY src ./src

# Build jar

RUN mvn clean package -DskipTests

# ---------- Stage 2: Run ----------

FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy jar from builder

COPY --from=builder /app/target/sql-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java","-jar","app.jar"]
