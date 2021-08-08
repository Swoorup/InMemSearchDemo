# In-Memory Document Database Demo

A demonstration of in-memory document database, powered by [cats-effect](https://typelevel.org/cats-effect/).

## Running

The repository provides sample json in the directory `./sample-data/`. You can launch the demo app using the following sbt command.

```shell
sbt "run --users sample-data/users.json --tickets sample-data/tickets.json"
```

To view supported command line options, you can run:

```shell
sbt "run --help"
```

## sbt project compiled with Scala 3

### Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

For more information on the sbt-dotty plugin, see the
[scala3-example-project](https://github.com/scala/scala3-example-project/blob/main/README.md).
