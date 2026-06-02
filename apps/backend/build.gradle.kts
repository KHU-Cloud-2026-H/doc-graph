plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.5"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
	kotlin("kapt") version "2.2.21"
}

group = "com.docgraph"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencyManagement {
	imports {
		mavenBom("org.testcontainers:testcontainers-bom:2.0.5")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	runtimeOnly("org.postgresql:postgresql")
	implementation("org.springframework.boot:spring-boot-flyway")
	implementation("org.flywaydb:flyway-core")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.retry:spring-retry:2.0.12")
	implementation("org.springframework:spring-aspects")
	implementation("io.github.openfeign.querydsl:querydsl-kotlin:7.1")
	implementation("io.github.openfeign.querydsl:querydsl-jpa:7.1")
	kapt("io.github.openfeign.querydsl:querydsl-apt:7.1:jpa")
	kapt("jakarta.annotation:jakarta.annotation-api")
	kapt("jakarta.persistence:jakarta.persistence-api")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("io.mockk:mockk:1.13.13")
	testImplementation("com.ninja-squad:springmockk:5.0.1")
	testImplementation("org.wiremock:wiremock-standalone:3.10.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
	sourceSets.main {
		kotlin.srcDir(layout.buildDirectory.dir("generated/source/kapt/main"))
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
	// 단일 test JVM이 distinct Spring 컨텍스트(~42)를 무한 누적 → heap OOM. JVM을 N개 클래스마다 재시작해
	// fork당 동시 생존 컨텍스트를 bound(default heap 수용). fork 내에선 cache.maxSize 유지라 eviction churn 없음.
	setForkEvery(10)
	systemProperty("spring.datasource.hikari.maximum-pool-size", "2")
	// 컨테이너 공유 테스트 클래스(slice+component ≈ 40)가 컨텍스트 캐시 기본 한도(32)를 넘겨
	// eviction → pool 개폐 churn으로 full `test` 실행 시 DB 커넥션 단절 flaky. 한도를 컨텍스트 수 위로.
	systemProperty("spring.test.context.cache.maxSize", "64")
	// 캐시된 컨텍스트마다 외부 HTTP 어댑터가 java.net.http.HttpClient(SelectorManager 스레드)를 생성·미회수해
	// full `test`에서 누적 → OOM. 테스트 JVM은 실 어댑터를 끄고 stub을 쓰며,
	// 실 어댑터를 wire로 검증하는 contract 테스트만 @TestPropertySource로 true override.
	systemProperty("adapter.http.real", "false")
}

tasks.register<Test>("unitTest") {
	description = "@Tag(\"unit\") 테스트만 실행"
	group = "verification"
	testClassesDirs = sourceSets["test"].output.classesDirs
	classpath = sourceSets["test"].runtimeClasspath
	useJUnitPlatform { includeTags("unit") }
}

tasks.register<Test>("sliceTest") {
	description = "@Tag(\"slice\") 테스트만 실행"
	group = "verification"
	testClassesDirs = sourceSets["test"].output.classesDirs
	classpath = sourceSets["test"].runtimeClasspath
	useJUnitPlatform { includeTags("slice") }
}

tasks.register<Test>("componentTest") {
	description = "@Tag(\"component\") 테스트만 실행"
	group = "verification"
	testClassesDirs = sourceSets["test"].output.classesDirs
	classpath = sourceSets["test"].runtimeClasspath
	useJUnitPlatform { includeTags("component") }
}
