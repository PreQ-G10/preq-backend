plugins {
    kotlin("jvm") version "2.2.0"
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

group = "preq"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.jetbrains.kotlin:kotlin-reflect")


    implementation("com.pgvector:pgvector:0.1.4")
    implementation("com.microsoft.onnxruntime:onnxruntime:1.17.0")
    implementation("org.bytedeco:javacv-platform:1.5.10")
    implementation("net.sourceforge.tess4j:tess4j:5.10.0")
    implementation("com.cloudinary:cloudinary-http44:1.38.0")
    implementation(platform("ai.djl:bom:0.27.0"))
    implementation("ai.djl:api")
    implementation("ai.djl.pytorch:pytorch-engine")
    implementation("ai.djl.pytorch:pytorch-model-zoo")

    implementation("ai.djl:basicdataset")
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")

    runtimeOnly("org.postgresql:postgresql")
}

tasks.test {
    useJUnitPlatform()
}
ktlint {
    version.set("1.6.0")
}
kotlin {
    jvmToolchain(21)
}