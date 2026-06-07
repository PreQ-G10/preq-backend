plugins {
    kotlin("jvm") version "2.2.0"
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
    id("jacoco")
}

extra["tomcat.version"] = "10.1.55"
extra["netty.version"] = "4.2.13.Final"
extra["spring-security.version"] = "6.5.9"

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
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    implementation("com.pgvector:pgvector:0.1.4")
    implementation("org.hibernate.orm:hibernate-spatial")
    implementation("com.microsoft.onnxruntime:onnxruntime:1.17.0")
    implementation("org.bytedeco:javacv-platform:1.5.10")
    implementation("net.sourceforge.tess4j:tess4j:5.10.0")
    implementation("com.drewnoakes:metadata-extractor:2.19.0")
    implementation("com.cloudinary:cloudinary-http44:1.38.0")
    implementation(platform("ai.djl:bom:0.31.1"))
    implementation("ai.djl:api:0.31.1")
    implementation("ai.djl.pytorch:pytorch-engine")

    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")

    runtimeOnly("org.postgresql:postgresql:42.7.11")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.0".toBigDecimal()
            }
        }
    }
}
ktlint {
    version.set("1.6.0")
}
kotlin {
    jvmToolchain(21)
}
