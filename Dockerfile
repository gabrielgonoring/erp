# ==========================================
# Build stage
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copia arquivos do wrapper Maven e pom.xml para cache de dependências
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Garante permissão de execução do wrapper e converte finais de linha se necessário
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copia o código-fonte
COPY src/ src/

# Empacota a aplicação ignorando os testes unitários no build da imagem
RUN ./mvnw clean package -DskipTests -B

# Extrai os layers do Spring Boot jar para otimizar startup e cache (Layered Jar)
RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted/ || \
    cp target/*.jar app.jar

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

# Copia os arquivos extraídos ou o fallback do JAR
COPY --from=builder --chown=appuser:appgroup /app/extracted/dependencies/ ./ 2>/dev/null || true
COPY --from=builder --chown=appuser:appgroup /app/extracted/spring-boot-loader/ ./ 2>/dev/null || true
COPY --from=builder --chown=appuser:appgroup /app/extracted/snapshot-dependencies/ ./ 2>/dev/null || true
COPY --from=builder --chown=appuser:appgroup /app/extracted/application/ ./ 2>/dev/null || true
COPY --from=builder --chown=appuser:appgroup /app/app.jar ./app.jar 2>/dev/null || true

USER appuser

EXPOSE 80

# Executa usando o launcher de layers ou fallback do jar
ENTRYPOINT ["sh", "-c", "if [ -f org.springframework.boot.loader.launch.JarLauncher ]; then exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher; elif [ -f app.jar ]; then exec java $JAVA_OPTS -jar app.jar; else exec java $JAVA_OPTS org.springframework.boot.loader.JarLauncher; fi"]
