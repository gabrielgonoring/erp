# ==========================================
# Build stage
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copia arquivos do wrapper Maven e pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Garante permissão de execução do wrapper e baixa dependências
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copia o código-fonte
COPY src/ src/

# Empacota a aplicação ignorando os testes unitários no build da imagem
RUN ./mvnw clean package

# Extrai os layers do Spring Boot jar
RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted/

# ==========================================
# Runtime stage
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Cria usuário não-root para segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Configurações otimizadas para 2 Cores e 1GB de RAM com Virtual Threads (Java 21)
# - MaxRAMPercentage=70.0: ~716MB de Heap, reservando ~308MB para Metaspace, CodeCache, Stacks e SO (evita OOM Killer)
# - InitialRAMPercentage=50.0: Heap inicial de ~512MB para evitar GC precoce no boot
# - ActiveProcessorCount=2: Garante que a JVM dimensione pools e GC para exatamente 2 vCPUs
# - UseG1GC + UseStringDeduplication: Reduz consumo de memória deduplicando Strings comuns
# - MaxMetaspaceSize=192m + ReservedCodeCacheSize=64m: Previne vazamento de memória off-heap
# - jdk.virtualThreadScheduler.parallelism=2: Ajusta carrier threads do Project Loom para os 2 núcleos
ENV SERVER_PORT=8080 \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:ActiveProcessorCount=2 -XX:MaxRAMPercentage=70.0 -XX:InitialRAMPercentage=50.0 -XX:MinRAMPercentage=50.0 -XX:MaxMetaspaceSize=192m -XX:ReservedCodeCacheSize=64m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxGCPauseMillis=100 -XX:+ParallelRefProcEnabled -Djdk.virtualThreadScheduler.parallelism=2 -Djdk.virtualThreadScheduler.maxPoolSize=256 -Djava.security.egd=file:/dev/./urandom"

# Copia os layers do builder com as devidas permissões
COPY --from=builder --chown=appuser:appgroup /app/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /app/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /app/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /app/extracted/application/ ./

USER appuser

EXPOSE 8080

# Executa aplicação repassando sinais do sistema operacional
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]