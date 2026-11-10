# ==========================================
# Build stage
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copia arquivos do wrapper Maven e pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Garante permissão de execução do wrapper e baixa dependências com cache BuildKit
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && ./mvnw dependency:resolve -B || true

# Copia o código-fonte
COPY src/ src/

# Empacota a aplicação com cache BuildKit (skip de testes e javadocs para build rápido)
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw clean package -DskipTests -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -B -T 1C

# Extrai os layers do Spring Boot jar incluindo o launcher
RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted/

# ==========================================
# Runtime stage
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Cria usuário não-root para segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copia os layers do builder
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

# Treina e gera o arquivo CDS (Class Data Sharing) no mesmo ambiente JRE exato
# -Dspring.context.exit=onRefresh inicializa o contexto e encerra antes de subir portas ou conexões
RUN java -XX:ArchiveClassesAtExit=application.jsa \
         -Dspring.context.exit=onRefresh \
         org.springframework.boot.loader.launch.JarLauncher

# Garante permissões do appuser para todos os arquivos, incluindo o application.jsa gerado
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

# Configurações otimizadas para 2 Cores e 1GB de RAM com CDS e Virtual Threads (Java 21)
# - SharedArchiveFile=application.jsa: Mapeia metadados de classes pré-carregados (reduz boot em 30-50%)
# - MaxRAMPercentage=70.0: ~716MB de Heap, reservando ~308MB para Metaspace, CodeCache, Stacks e SO (evita OOM Killer)
# - InitialRAMPercentage=50.0: Heap inicial de ~512MB para evitar GC precoce no boot
# - ActiveProcessorCount=2: Garante que a JVM dimensione pools e GC para exatamente 2 vCPUs
# - UseG1GC + UseStringDeduplication: Reduz consumo de memória deduplicando Strings comuns
# - MaxMetaspaceSize=192m + ReservedCodeCacheSize=64m: Previne vazamento de memória off-heap
# - jdk.virtualThreadScheduler.parallelism=2: Ajusta carrier threads do Project Loom para os 2 núcleos
ENV SERVER_PORT=8080 \
    JAVA_OPTS="-XX:SharedArchiveFile=application.jsa -XX:+UseContainerSupport -XX:ActiveProcessorCount=2 -XX:MaxRAMPercentage=70.0 -XX:InitialRAMPercentage=50.0 -XX:MinRAMPercentage=50.0 -XX:MaxMetaspaceSize=192m -XX:ReservedCodeCacheSize=64m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxGCPauseMillis=100 -XX:+ParallelRefProcEnabled -Djdk.virtualThreadScheduler.parallelism=2 -Djdk.virtualThreadScheduler.maxPoolSize=256 -Djava.security.egd=file:/dev/./urandom"

# Executa aplicação repassando sinais do sistema operacional
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
