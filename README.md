# Marrow

[![](https://jitpack.io/v/Skeleton-Army/marrow.svg?label=Tag)](https://jitpack.io/#Skeleton-Army/Marrow)

**Introducing the official utility library from FTC Team ☠ **Skeleton Army #23644** ☠ — built by competitors, for competitors.**

**Marrow** is a collection of modular, high-performance tools designed to streamline development, eliminate boilerplate, and help your team move faster—on and off the field.

We also ***highly*** recommend using [Sloth](https://github.com/Dairy-Foundation/Sloth) for *really* fast upload times and quick iterations.

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
   
   In the same `build.gradle` file, add one of the following lines inside the `dependencies` block:

   ```gradle
   dependencies {
      implementation 'com.github.Skeleton-Army:Marrow:(VERSION)' // Recommended
      // OR
      implementation 'com.github.Skeleton-Army:Marrow:main-SNAPSHOT' // Snapshot version – not recommended for production
   }
   ```

3. **Sync your project with Gradle**
   
   In Android Studio, click **"Sync Now"** when prompted, or go to `File > Sync Project with Gradle Files` to apply the changes.

## 🛠️ Developing & Contributing

If you're contributing to Marrow or developing a new feature, you can use the `dev-SNAPSHOT` version:

#### In `dependencies`:

```gradle
implementation("com.github.Skeleton-Army:Marrow:dev-SNAPSHOT") {
    changing = true // This tells Gradle the dependency may change often
}
```

#### And **below** your `dependencies` block:

```gradle
// Tells Gradle not to cache changing modules (e.g., SNAPSHOTs) for any period of time (0 seconds)
configurations.configureEach {
    resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
}
```

This setup ensures you're always using the most recent snapshot version while contributing or testing.

### ⏱ JitPack Build Time

> ⚠️ Heads up: Every time you push a commit to the `main` or `dev` branch, JitPack can take **~2–3 minutes** to build the new version.

You can **check build status** or manually trigger a build at:  
👉 https://jitpack.io/#Skeleton-Army/Marrow

## 💡 Ideas?
If you have suggestions or want to discuss improvements, feel free to open an [issue](https://github.com/Skeleton-Army/Marrow/issues) or start a discussion.
