import sbt._
import sbt.Keys._


name := "SimpleAKKAFileTransfer"
version := "1.0"
scalaVersion := "2.11.8"

//enablePlugins(JavaServerAppPackaging)
//fork in run := true

mainClass in Compile := Some("akkaio.MainApp2")
scalacOptions in ThisBuild ++= Seq("-deprecation", "-unchecked", "-feature")
//See if required
//lazy val root = (project in file("."))

//mappings in Universal ++= {
// optional example illustrating how to copy additional directory
//directory("scripts") ++
// copy configuration files to config directory
//  contentOf("src/main/resources").toMap.mapValues("config/" + _)
//}


// add ’config’ directory first in the classpath of the start script,
// an alternative is to set the config file locations via CLI parameters
// when starting the application
//scriptClasspath := Seq("../config/") ++ scriptClasspath.value

lazy val root = Project(id = "root",
  base = file(".")).settings(
  name := "AkkaProject",

  libraryDependencies ++= {
    lazy val akkaVersion = "2.4.2"

    Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-remote" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
      "com.typesafe.akka" %% "akka-contrib" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      //"com.github.romix.akka" %% "akka-kryo-serialization" % "0.4.0",
      "com.typesafe" % "config" % "1.3.0"
    )
  }
)


