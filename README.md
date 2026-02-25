# Drift canary

```shell
sbt "attributionService / Docker / stage"
```

```shell
sbt "attributionService / Docker / publishLocal"
```

```shell
docker compose up
```

```shell
curl http://localhost:8080/healthz
```

```terminaloutput
"Attribution Service is live"
```

```shell
curl http://localhost:8080/ready
```

```terminaloutput
"Attribution Service is ready
```

```shell
curl -s http://localhost:8080/api/v1/meta/model | jq .
```

```json
{
  "current_version": "v1"
}
```

```shell
act -l push
```

```shell
act push -j 'build'
```

## Resources

* [MUnit - Scala testing library with actionable errors and extensible APIs](https://scalameta.org/munit/).
* [munit-cats-effect - Integration library for MUnit and cats-effect](https://typelevel.org/munit-cats-effect/).
* [scalacheck-effect - Effectful property testing built on ScalaCheck](https://github.com/typelevel/scalacheck-effect).
* [act - Usage guide](https://nektosact.com/usage/index.html).
