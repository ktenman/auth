plugins {
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("jvm") version "2.4.21"
	kotlin("plugin.spring") version "2.4.21"
	id("org.graalvm.buildtools.native") version "1.1.18"
}

group = "ee.tenman"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-session-data-redis")
	implementation("redis.clients:jedis")
	implementation("org.jetbrains.kotlin:kotlin-reflect") // Spring AOT inspects Kotlin data classes with it
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations.all {
	exclude(group = "io.lettuce")
}

graalvmNative {
	binaries {
		named("main") {
			// the default target needs AVX2 on x86-64 and ARMv8.1 on ARM, which a VPS may not expose
			buildArgs.add("-march=compatibility")
			// optimise for size: a 22 MiB smaller binary and 12 MiB less memory, for about 15% more CPU per request
			buildArgs.add("-Os")
		}
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
