import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.6.21"
}

group = "com.example.qa.debugging"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
}

dependencies {
	testImplementation("com.squareup.okhttp3:okhttp:4.9.1")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
	testImplementation("org.junit.jupiter:junit-jupiter-params:5.4.2")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
	testImplementation("com.google.code.gson:gson:2.8.5")
	testImplementation("io.github.cdimascio:dotenv-kotlin:6.3.1")
	testImplementation("io.lettuce:lettuce-core:6.2.1.RELEASE")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
