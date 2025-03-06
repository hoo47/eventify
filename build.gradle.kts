plugins {
    id("java-library")
    id("maven-publish")
}

group = "io.github.event"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // 핵심 의존성
    api("org.slf4j:slf4j-api:1.7.36")
    implementation("com.google.protobuf:protobuf-java:3.21.12") // 최신 버전 사용 권장
    
    // Lombok 의존성 추가
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")
    
    // 선택적 의존성 (컴파일 시에만 필요, 런타임에는 사용자가 제공)
    compileOnly("javax.transaction:javax.transaction-api:1.3")
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.1.0")
    
    // JDBC 관련 의존성 추가 (순수 JDBC)
    testImplementation("com.h2database:h2:2.2.224")
    testImplementation("com.zaxxer:HikariCP:5.0.1")
    
    // 테스트 의존성
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testImplementation("org.junit.platform:junit-platform-commons:1.8.2")
    testImplementation("javax.transaction:javax.transaction-api:1.3")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.mockito:mockito-core:4.8.0")
    testImplementation("org.mockito:mockito-junit-jupiter:4.8.0")
    
    // RabbitMQ 및 JSON 의존성 (중복 제거)
    implementation("com.rabbitmq:amqp-client:5.16.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4")
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            pom {
                name.set("Event Bus")
                description.set("트랜잭션 단계별 이벤트 처리를 지원하는 이벤트 버스")
                url.set("https://github.com/hoo47/eventify")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("hoo47")
                        name.set("hoo47")
                        email.set("sihookang47@gmail.com")
                    }
                }
            }
        }
    }
}
