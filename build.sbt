ThisBuild / scalaVersion := "2.12.6"
ThisBuild / organization := "com.example"

lazy val akkaVersion = "2.5.1"
val opRabbitVersion = "2.1.0"

enablePlugins(FlywayPlugin)

flywayUrl := "jdbc:mysql://mysql:3306/test_provider?user=root"
flywayUser := "root"
flywayLocations+="db/migration"


lazy val hello = (project in file("."))
  .settings(
    name := "Hello",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "0.19",
      "com.spingo" %% "op-rabbit-core"        % opRabbitVersion,
      "com.spingo" %% "op-rabbit-play-json"   % opRabbitVersion,
      "com.spingo" %% "op-rabbit-json4s"      % opRabbitVersion,
      "com.spingo" %% "op-rabbit-airbrake"    % opRabbitVersion,
      "com.spingo" %% "op-rabbit-akka-stream" % opRabbitVersion,
      "com.typesafe.slick" %% "slick" % "3.2.3",
      "mysql" % "mysql-connector-java" % "5.1.34",
      "org.slf4j" % "slf4j-nop" % "1.6.4",
      "org.scalatest" %% "scalatest" % "3.0.1" % "test"
    )
  )
