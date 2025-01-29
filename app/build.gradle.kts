plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.alpnr"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.alpnr"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)



    // Network libraries

    //OKHttp3
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    //Retrofit
    implementation (libs.retrofit)
    implementation (libs.converter.gson)

    implementation (libs.gson)


    implementation (libs.imagepicker)


    implementation ("com.google.api-client:google-api-client-android:1.22.0"){
        exclude(module = "httpclient")
    }

    implementation ("com.google.http-client:google-http-client-gson:1.20.0"){
        exclude(module = "httpclient")
    }


    implementation ("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
}

