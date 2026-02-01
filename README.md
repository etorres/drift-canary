


```shell
sbt Docker/stage
```

```shell
sbt Docker/publishLocal
```


In domain-driven design (DDD), package names should reflect the domain concepts and bounded contexts. Here are some common DDD-inspired package name suggestions:
* model or domain.model: For core domain entities and value objects.
* repository or domain.repository: For repository interfaces and implementations.
* service or domain.service: For domain services containing business logic.
* application: For application services, use cases, and orchestration logic.
* infrastructure: For technical details, adapters, and integrations (e.g., database, HTTP).
* api or interface: For REST controllers, HTTP endpoints, or external interfaces.
* config: For configuration classes.

```
com.yourcompany.yourapp.domain.model
com.yourcompany.yourapp.domain.repository
com.yourcompany.yourapp.domain.service
com.yourcompany.yourapp.application
com.yourcompany.yourapp.infrastructure
com.yourcompany.yourapp.api
com.yourcompany.yourapp.config

```

Choose package names that best match your project's ubiquitous language and bounded contexts.

## Resources

* [MUnit - Scala testing library with actionable errors and extensible APIs](https://scalameta.org/munit/).
* [munit-cats-effect - Integration library for MUnit and cats-effect](https://typelevel.org/munit-cats-effect/).
* [scalacheck-effect - Effectful property testing built on ScalaCheck](https://github.com/typelevel/scalacheck-effect).
