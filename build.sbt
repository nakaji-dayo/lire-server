name := "lire-server"

version := "1.0"

scalaVersion := "2.11.7"


resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-io" % "1.3.2",
  "io.circe" %% "circe-generic" % "0.3.0",
  "com.github.finagle" %% "finch-circe" % "0.10.0",
  "com.github.finagle"  %% "finch-core"      % "0.9.4-SNAPSHOT" changing(),
  "com.github.finagle"  %% "finch-argonaut"  % "0.9.4-SNAPSHOT" changing(),
  "com.twitter" %% "util-eval" % "6.24.0"
)