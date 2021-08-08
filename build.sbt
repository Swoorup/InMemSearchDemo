val scala3Version = "3.0.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "inmem-search-demo",
    version := "0.1.0",
    scalaVersion := scala3Version,
    libraryDependencies ++=
      Seq(
        // functional programming
        "org.typelevel" %% "cats-core" % "2.6.1",
        "org.typelevel" %% "cats-effect" % "3.2.2",

        // command line parser
        "com.monovore" %% "decline" % "2.1.0",
        "com.monovore" %% "decline-effect" % "2.1.0",

        // json utilities
        "io.circe" %% "circe-core" % "0.14.1",
        "io.circe" %% "circe-jawn" % "0.14.1",

        // testing
        "org.scalameta" %% "munit" % "0.7.27" % "test",
        "org.typelevel" %% "munit-cats-effect-3" % "1.0.5" % "test",
      )
  )
