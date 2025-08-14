name := "QuantTrading"

version := "0.1.0"

scalaVersion := "2.13.16"

libraryDependencies ++= Seq(
  // scala testing
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,

  // akka streams
  "com.typesafe.akka" %% "akka-stream" % "2.6.21",
  "com.typesafe.akka" %% "akka-actor" % "2.6.21",

  // ssl config
  "com.typesafe" %% "ssl-config-core" % "0.6.1",

  // akka http
  "com.typesafe.akka" %% "akka-http" % "10.5.3",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.3",

  // request-scala
  "com.lihaoyi" %% "requests" % "0.9.0",

  // uJson parsing
  "com.lihaoyi" %% "upickle" % "4.2.1",

  // scalaz (validation)
  "org.scalaz" %% "scalaz-core" % "7.3.8",

  // scalactic (posdouble, poslong, etc)
  "org.scalactic" %% "scalactic" % "3.2.19",

  // apache maths
  "org.apache.commons" % "commons-math3" % "3.6.1",

  // mongoDB scala driver
  "org.reactivemongo" %% "reactivemongo" % "1.0.10",

  // logging
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "org.slf4j" % "slf4j-api" % "2.0.17",

  // plotting
  "org.plotly-scala" %% "plotly-render" % "0.8.5",

  // aws s3
  "software.amazon.awssdk" % "s3" % "2.32.22",
  "software.amazon.awssdk" % "auth" % "2.32.22",
  "software.amazon.awssdk" % "secretsmanager" % "2.32.22",

  // spray json
  "io.spray" %% "spray-json" % "1.3.6",
)

// add IBKR api
Compile / unmanagedJars += baseDirectory.value / "lib" / "TwsApi.jar"
