plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.yourcompany.worklisten"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yourcompany.worklisten"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Jetpack Compose（稳定版本）
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.0")
    implementation("androidx.compose.runtime:runtime:1.5.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.0")
    implementation("androidx.navigation:navigation-compose:2.7.5")
	implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.0")
    
    // Material Components (必须添加，支持传统XML布局中的Material主题)
    implementation("com.google.android.material:material:1.10.0")

    // 添加Material图标依赖
    implementation("androidx.compose.material:material-icons-extended:1.5.0")

    // Room 数据库（稳定版本）
    implementation("androidx.room:room-runtime:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")
    implementation(libs.androidx.datastore.core.android)
    ksp("androidx.room:room-compiler:2.5.2")

    // OpenCSV（CSV 文件解析，稳定版本）
    implementation("com.opencsv:opencsv:5.7.1")

    // Apache POI（Excel 文件解析，稳定版本）
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("org.apache.xmlbeans:xmlbeans:5.1.1")
    implementation("org.apache.commons:commons-compress:1.22")

    // Charset detection - a maintained fork of juniversalchardet
    implementation("com.github.albfernandez:juniversalchardet:2.4.0")

    // Javax Inject for Provider
    implementation("javax.inject:javax.inject:1")

    // Android TTS（文本转语音）
    implementation("androidx.core:core:1.10.0")

    // 其他依赖
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("com.google.accompanist:accompanist-permissions:0.30.1")

    // Paging 3（分页加载）
    implementation("androidx.paging:paging-runtime:3.2.0")
    implementation("androidx.paging:paging-compose:1.0.0-alpha20")

    // Coil (for image loading)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Room Paging
    implementation("androidx.room:room-paging:2.5.2")

    // DataStore (for settings)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // 测试依赖
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:3.12.4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.0")
    // 添加androidx.test.core依赖，用于测试中获取ApplicationContext
    testImplementation("androidx.test:core:1.5.0")
}