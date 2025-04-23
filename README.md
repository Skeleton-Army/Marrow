# Marrow

[![](https://jitpack.io/v/Skeleton-Army/marrow.svg?label=Tag)](https://jitpack.io/#Skeleton-Army/Marrow)

Welcome to the official utility package of FTC Team ☠ **Skeleton Army #23644** ☠.

This library contains reusable systems and helpful modules to speed up development, reduce clutter, and boost performance on and off the field.

We also **highly** recommend using [Sloth](https://github.com/Dairy-Foundation/Sloth) for *really* fast upload times.

## 🧠 Why Use This?

- Clean, reusable code that follows best practices

- Designed with modularity and performance in mind

- Saves time so you can focus on strategy and innovation

## 🚀 Getting Started

To install **Marrow** in your project:

1. **Add JitPack to your repositories**
   
   In your `TeamCode/build.gradle`, add the following inside the `repositories` block (above `dependencies`):

   ```gradle
   repositories {
       mavenCentral()
       maven { url "https://jitpack.io" }
   }
   ```

3. **Add Marrow as a dependency**
   
   Still in the same `build.gradle` file, add one of the following lines inside the `dependencies` block:

   ```gradle
   dependencies {
      implementation 'com.github.Skeleton-Army:Marrow:(VERSION)' // Recommended
      // OR
      implementation 'com.github.Skeleton-Army:Marrow:main-SNAPSHOT' // Snapshot version – not recommended for production
   }
   ```

5. **Sync your project with Gradle**
   
   In Android Studio, click **"Sync Now"** when prompted, or go to `File > Sync Project with Gradle Files` to apply the changes.

## 🔄 Updating the Library

Marrow is published through [JitPack](https://jitpack.io), which means version updates depend on JitPack successfully building the latest commits.

> ⚠️ **Heads up:** Sometimes JitPack may not automatically build the latest version of the library (especially if no one has triggered a build yet).

### ✅ How to Force an Update

If you're not seeing the latest changes, you can manually trigger a build on JitPack:

1. Go to the [Marrow JitPack page](https://jitpack.io/#Skeleton-Army/Marrow)

2. Click **"Look Up"** for the `main` branch or your desired tag version

3. If it's not built yet, JitPack will queue a build for you

4. Once complete, your project will be able to resolve the latest version

## 💡 Ideas?
If you have suggestions or want to discuss improvements, feel free to open an [issue](https://github.com/Skeleton-Army/Marrow/issues) or start a discussion.
