lazy val Version           = "0.0.3"
lazy val ScalaVersion      = "2.13.4"
lazy val CatsEffectVersion = "2.3.0"
lazy val catsRetryVersion  = "2.1.0"
lazy val OdinVersion       = "0.9.1"
lazy val Fs2Version        = "2.4.0"

libraryDependencies += "org.typelevel" %% "cats-effect" % "2.3.0"
libraryDependencies += "co.fs2"        %% "fs2-core"    % "2.4.0"
val GlobalSettingsGroup: Seq[Setting[_]] = Seq(
  version := Version,
  scalaVersion := ScalaVersion,
  homepage := Some(url("https://github.com/alirezameskin/chain4s")),
  organization := "com.github.alirezameskin"
)

lazy val core = (project in file("chain4s-core"))
  .settings(GlobalSettingsGroup)
  .settings(
    name := "chain4s-core",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.1.1"
    )
  )

lazy val effect = (project in file("chain4s-effect"))
  .settings(GlobalSettingsGroup)
  .settings(
    name := "chain4s-effect",
    libraryDependencies ++= Seq(
      "com.github.valskalla" %% "odin-core"   % OdinVersion,
      "org.typelevel"        %% "cats-effect" % CatsEffectVersion,
      "co.fs2"               %% "fs2-core"    % Fs2Version,
      "com.github.cb372"     %% "cats-retry"  % catsRetryVersion
    )
  )
  .dependsOn(core)
  .aggregate(core)

lazy val grpc = (project in file("chain4s-grpc"))
  .settings(GlobalSettingsGroup)
  .settings(
    name := "chain4s-grpc",
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value / "scalapb"
    ),
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime"      % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
      "io.grpc"               % "grpc-netty"           % scalapb.compiler.Version.grpcJavaVersion,
      "io.grpc"               % "grpc-services"        % scalapb.compiler.Version.grpcJavaVersion
    )
  )
  .dependsOn(core, effect)
  .aggregate(core)

lazy val demo = (project in file("chain4s-demo"))
  .dependsOn(core, effect, grpc)
  .aggregate(core, effect, grpc)
  .settings(GlobalSettingsGroup)
  .settings(
    name := "chain4s-demo",
    moduleName := "chain4s-demo",
    publish := {},
    publishLocal := {}
  )

lazy val root = (project in file("."))
  .aggregate(core, effect, grpc, demo)
  .settings(GlobalSettingsGroup)
  .settings(
    name := "chain4s",
    moduleName := "chain4s",
    publish := {},
    publishLocal := {}
  )
