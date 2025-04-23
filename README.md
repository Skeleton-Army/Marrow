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

2. **Add Marrow as a dependency**
   
   Still in the same `build.gradle` file, add one of the following lines inside the `dependencies` block:

   ```gradle
   dependencies {
      implementation 'com.github.Skeleton-Army:Marrow:(VERSION)' // Recommended
      // OR
      implementation 'com.github.Skeleton-Army:Marrow:main-SNAPSHOT' // Snapshot version – not recommended for production
   }
   ```

3. **Sync your project with Gradle**
   
   In Android Studio, click **"Sync Now"** when prompted, or go to `File > Sync Project with Gradle Files` to apply the changes.

## 💡 Ideas?
If you have suggestions or want to discuss improvements, feel free to open an [issue](https://github.com/Skeleton-Army/Marrow/issues) or start a discussion.
