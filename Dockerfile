# ==========================================
# Build stage
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copia arquivos do wrapper Maven e pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Garante permissão de execução do wrapper
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copia o código-fonte
COPY src/ src/

# Empacota a aplicação ignorando os testes unitários no build da imagem
RUN ./mvnw clean package -DskipTests -B

# Extrai os layers do Spring Boot jar
RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted/

# ==========================================
# Runtime stage
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Cria usuário não-root para segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Fly.io monitora ou mapeia a porta da aplicação (default 80 no application.yml)
ENV SERVER_PORT=80 \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Copia os layers do builder com as devidas permissões
COPY --from=builder --chown=appuser:appgroup /app/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /app/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /app/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /app/extracted/application/ ./

USER appuser

EXPOSE 80

# Spring Boot 3.3+ / 4.x JarLauncher no path org.springframework.boot.loader.launch.JarLauncher
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
