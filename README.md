## Usage

### With PMD Gradle plugin

Add [PMD plugin](https://docs.gradle.org/current/userguide/pmd_plugin.html):

`build.gradle`
```groovy
apply plugin: 'pmd'
pmd {
    // pmd plugin config
}
```

Add this library to the PMD classpath:

`build.gradle`
```groovy
sourceSets {
    pmd
}
dependencies {
    pmdRuntime 'io.github.juliantcook:pmd-rules:0.0.1'
}
// set classpath for appropriate PMD tasks
tasks.pmdMain.doFirst {
    pmdClasspath += sourceSets.pmd.runtimeClasspath
}
```

[These rules](docs/rules.md) can then be used.
