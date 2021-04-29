## Usage

Add PMD plugin:

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
pmdRuntime 'io.github.juliantcook:pmd-rules:0.0.1-SNAPSHOT'
// set classpath for appropriate PMD tasks
tasks['pmdMain'].doFirst {
    pmdClasspath += sourceSets.pmd.runtimeClasspath
}
```

[These rules](docs/rules.md) can then be used.
