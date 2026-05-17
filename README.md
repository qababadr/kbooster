# 🚀 Kbooster

Kbooster is a Kotlin-based code generation library built on top of KSP, designed to simplify boilerplate generation in Android projects while abiding by clean architecture principles.

---

## ✨ Features

- UseCases generation with hilt Module
- Validation
- Networking on top on Ktor
- Data Cryptography on Top of security-crypto

---

## 📦 Installation

### 1. Add JitPack repository

In your `settings.gradle`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

---

### 2. Add KSP plugin (Project level)

```kotlin
plugins {
    alias(libs.plugins.ksp) apply false
}
```

---

### 3. Apply KSP plugin (App module)

```kotlin
plugins {
    alias(libs.plugins.ksp)
}
```

---

### 4. Add dependencies

```kotlin
dependencies {
    implementation(libs.kbooster)
    implementation(libs.kbooster.core)
    ksp(libs.kbooster.processor)
}
```

---

### 5. Configure generated sources

Add this in your **app `build.gradle`**:

```kotlin
androidComponents {
    onVariants { variant ->
        val kspKotlinDir = layout
            .buildDirectory
            .dir("generated/ksp/${variant.name}/kotlin")
            .get()
            .asFile

        variant.sources.java?.apply {
            addStaticSourceDirectory(kspKotlinDir.absolutePath)
        }
    }
}
```

---

## ⚙️ How It Works

Kbooster uses KSP (Kotlin Symbol Processing) to generate Kotlin code at build time.

Generated sources are automatically added to your project and compiled with your app.

---

## 🧪 Build & Run

```bash
./gradlew build
```

Generated files will be located in:

```
build/generated/ksp/<variant>/kotlin
```

---

## ⚠️ Notes

- Ensure KSP version matches your Kotlin version
- Always sync Gradle after adding dependencies
- If generation fails, try a clean build:

```bash
./gradlew clean build
```

---

## 💡 Troubleshooting

**No code generated?**
- Check `ksp(libs.kbooster.processor)` is added
- Verify annotation usage is correct
- Ensure generated source directory is configured properly

---

## 🧲 Generating UseCases Example:
```kotlin
import com.badrqaba.kbooster.core.annotation.Usecaseable
import com.badrqaba.kbooster.core.model.DIScope

@Usecaseable(
    generateDI = true,
    diScope = DIScope.SINGLETON
)
interface ProductRepository {

    suspend fun getProductsIds(page: Int): List<Long>
}
```
## 🧲 Generating Validations Example:
```kotlin
import com.badrqaba.kbooster.core.annotation.Email
import com.badrqaba.kbooster.core.annotation.EqualsTo
import com.badrqaba.kbooster.core.annotation.Length
import com.badrqaba.kbooster.core.annotation.Max
import com.badrqaba.kbooster.core.annotation.Min
import com.badrqaba.kbooster.core.annotation.MinLength
import com.badrqaba.kbooster.core.annotation.Pattern
import com.badrqaba.kbooster.core.annotation.Range
import com.badrqaba.kbooster.core.annotation.Required
import com.badrqaba.kbooster.core.annotation.Validatable

@Validatable
data class MainFormState(
    @Required(message = "Username is required")
    val username: String,

    @Email(
        regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$",
        message = "Invalid email format"
    )
    val email: String,

    @MinLength(
        value = 6,
        message = "Password must be at least 6 characters"
    )
    val password: String,

    @EqualsTo(
        field = "password",
        message = "Passwords do not match"
    )
    val confirmPassword: String,

    @Length(
        min = 3,
        max = 20,
        message = "Nickname must be between 3 and 20 characters"
    )
    val nickname: String,

    @Min(
        value = 18.0,
        message = "You must be at least 18"
    )
    val age: Int,

    @Max(
        value = 120.0,
        message = "Invalid age"
    )
    val maxAgeCheck: Int,

    @Range(
        min = 1000.0,
        max = 9999.0,
        message = "Code must be 4 digits"
    )
    val verificationCode: Int,

    @Pattern(
        value = "^[0-9]{10}$",
        message = "Phone must be 10 digits"
    )
    val phone: String
)
```

## 📄 License

MIT License

Copyright (c) 2026 Kbooster by Qaba Badr

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
