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
    
    // 테스트 의존성
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("javax.transaction:javax.transaction-api:1.3")
    
    // RabbitMQ 및 JSON 의존성 (중복 제거)
    implementation("com.rabbitmq:amqp-client:5.16.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4")

    implementation("org.springframework:spring-context:5.3.20")
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
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
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
