## Usage

`build.gradle`

```groovy
apply plugin: 'pmd'
sourceSets {
    pmd {}
}
pmdRuntime 'io.github.juliantcook:pmd-rules:0.0.1-SNAPSHOT'
pmd {
    // pmd plugin config
}
tasks['pmdMain'].doFirst {
    pmdClasspath += sourceSets.pmd.runtimeClasspath
}
```
