val scala3Version = "3.0.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "inmem-search-demo",
    version := "0.1.0",
    scalaVersion := scala3Version,
    libraryDependencies ++=
      Seq(
        // "com.monovore" %% "decline" % "2.1.0",
        "com.monovore" %% "decline-effect" % "2.1.0",
        "io.circe" %% "circe-core" % "0.14.1",
        // "io.circe" %% "circe-generic" % "0.14.1",
        "io.circe" %% "circe-jawn" % "0.14.1",
        "org.typelevel" %% "cats-core" % "2.6.1",
        "org.typelevel" %% "cats-effect" % "3.2.1",
        "com.novocode" % "junit-interface" % "0.11" % "test",
      )
  )
