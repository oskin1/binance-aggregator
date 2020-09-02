import sbt.compilerPlugin

name := "binance-aggregator"

version := "0.1"

scalaVersion := "2.13.3"

scalacOptions ++= commonScalacOptions

resolvers += Resolver.sonatypeRepo("public")
resolvers += Resolver.sonatypeRepo("snapshots")

lazy val TofuVer   = "0.7.9-streams-SNAPSHOT"
lazy val SttpVer   = "2.2.6"
lazy val CirceVer  = "0.14.0-M1"
lazy val DoobieVer = "0.9.0"

libraryDependencies ++= Seq(
  "ru.tinkoff"                   %% "tofu-core"                     % TofuVer,
  "ru.tinkoff"                   %% "tofu-streams"                  % TofuVer,
  "ru.tinkoff"                   %% "tofu-fs2-interop"              % TofuVer,
  "ru.tinkoff"                   %% "tofu-logging"                  % TofuVer,
  "ru.tinkoff"                   %% "tofu-env"                      % TofuVer,
  "ru.tinkoff"                   %% "tofu-derivation"               % TofuVer,
  "ru.tinkoff"                   %% "tofu-optics-core"              % TofuVer,
  "ru.tinkoff"                   %% "tofu-optics-macro"             % TofuVer,
  "ru.tinkoff"                   %% "tofu-doobie"                   % TofuVer,
  "io.circe"                     %% "circe-core"                    % CirceVer,
  "io.circe"                     %% "circe-parser"                  % CirceVer,
  "org.tpolecat"                 %% "doobie-h2"                     % DoobieVer,
  "org.tpolecat"                 %% "doobie-hikari"                 % DoobieVer,
  "org.tpolecat"                 %% "doobie-postgres"               % DoobieVer,
  "com.softwaremill.sttp.client" %% "core"                          % SttpVer,
  "com.softwaremill.sttp.client" %% "async-http-client-backend-fs2" % SttpVer,
  compilerPlugin("org.typelevel" %% "kind-projector"     % "0.11.0" cross CrossVersion.full),
  compilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
)

lazy val commonScalacOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-feature",
  "-unchecked",
  "-Ywarn-numeric-widen",
  "-Ymacro-annotations",
  "-Xlog-implicits"
)
