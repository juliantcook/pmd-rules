---
## DirectlyInstantiatedSingleton

**Priority:** High (1)

**Minimum Language Version:** Java 1.6

Classes annotated @Singleton should be instantiated by the dependency injection library.

**This rule is defined by the following Java class:** [io.github.juliantcook.pmdrules.lang.java.rule.injection.DirectlyInstantiatedSingletonRule](https://github.com/juliantcook/pmd-rules/blob/master/src/main/java/io/github/juliantcook/pmdrules/lang/java/rule/injection/DirectlyInstantiatedSingletonRule.java)

**Example(s):**

``` java
@Singleton
public class FooSingleton {
    public void execute() {
        //...
    }
}

public class BarService {
    public void execute() {
        // Naughty. This should be injected so the DI framework can handle ensuring only one instance is created
        new FooSingleton().execute();
    }
}
```

**Use this rule by referencing it:**
``` xml
<rule ref="category/java/injection.xml/DirectlyInstantiatedSingleton" />
```

## UnnecessaryInject

**Priority:** Medium (3)



**This rule is defined by the following XPath expression:**
``` xpath
//MarkerAnnotation/Name[@Image='Inject']
    [./ancestor::ClassOrInterfaceBodyDeclaration/*/FormalParameters[@Size=0]]
```

**Example(s):**

``` java
public class SomeClass {

    @Inject // This is unnecessary.
    public SomeClass() {
    }
}
```

**Use this rule by referencing it:**
``` xml
<rule ref="category/java/injection.xml/UnnecessaryInject" />
```

