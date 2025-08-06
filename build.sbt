import sbtcrossproject.CrossProject

val Version = new {
  val Cats = "2.13.0"
  val CatsEffect = "3.6.1"
  val CatsTime = "0.5.1"
  val Cron4s = "0.8.2"
  val CronUtils = "9.2.1"
  val Fs2 = "3.12.0"
  val Kittens = "3.5.0"
  val MUnit = "1.1.0"
  val MUnitCatsEffect = "2.1.0"
  val Scala = "3.3.6"
  val ScalaJavaTime = "2.6.0"
  val Slf4j = "2.0.17"
  val Skunk = "1.0.0-M11"
  val Testcontainers = "1.20.6"
}

def module(identifier: Option[String], jvmOnly: Boolean = false): CrossProject = {
  val platforms = JVMPlatform :: (if (jvmOnly) Nil else JSPlatform :: Nil)

  CrossProject(identifier.getOrElse("root"), file(identifier.fold(".")("modules/" + _)))(platforms: _*)
    .crossType(CrossType.Pure)
    .withoutSuffixFor(JVMPlatform)
    .build()
    .settings(
      Compile / scalacOptions ++= "-source:future" :: "-rewrite" :: "-new-syntax" :: "-Wunused:all" :: Nil,
      name := "unimog" + identifier.fold("")("-" + _)
    )
}

inThisBuild(
  Def.settings(
    developers := List(Developer("taig", "Niklas Klein", "mail@taig.io", url("https://taig.io/"))),
    dynverVTagPrefix := false,
    homepage := Some(url("https://github.com/taig/unimog/")),
    licenses := List("MIT" -> url("https://raw.githubusercontent.com/taig/unimog/main/LICENSE")),
    scalaVersion := Version.Scala,
    versionScheme := Some("early-semver")
  )
)

noPublishSettings

lazy val root = module(identifier = None)
  .enablePlugins(BlowoutYamlPlugin)
  .settings(noPublishSettings)
  .settings(
    blowoutGenerators ++= {
      val workflows = file(".github") / "workflows"
      BlowoutYamlGenerator.lzy(workflows / "main.yml", GitHubActionsGenerator.main) ::
        BlowoutYamlGenerator.lzy(workflows / "pull-request.yml", GitHubActionsGenerator.pullRequest) ::
        BlowoutYamlGenerator.lzy(workflows / "tag.yml", GitHubActionsGenerator.tag) ::
        Nil
    }
  )
  .aggregate(core, api, skunk)

lazy val core = module(identifier = Some("core"))
  .settings(
    libraryDependencies ++=
      "org.typelevel" %%% "cats-core" % Version.Cats ::
        Nil
  )

lazy val api = module(identifier = Some("api"))
  .settings(
    libraryDependencies ++=
      "co.fs2" %%% "fs2-core" % Version.Fs2 ::
        "org.typelevel" %%% "cats-effect" % Version.CatsEffect ::
        Nil
  )
  .jsSettings(
    libraryDependencies ++=
      "io.github.cquiroz" %%% "scala-java-time" % Version.ScalaJavaTime ::
        Nil
  )
  .dependsOn(core)

lazy val skunk = module(identifier = Some("skunk"))
  .settings(
    libraryDependencies ++=
      "org.tpolecat" %%% "skunk-core" % Version.Skunk ::
        Nil
  )
  .dependsOn(api)
