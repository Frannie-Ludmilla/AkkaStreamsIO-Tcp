import sbt._
import sbt.Keys._

object Build extends sbt.Build{

  val commonSettings= Seq(
    version := "1.0",
    scalaVersion := "2.11.8"
  )

  lazy val akkaVersion = "2.4.3"

  val coreDependencies= Seq(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion
    )
  )

scalacOptions in ThisBuild ++= Seq("-deprecation", "-unchecked", "-feature")

  lazy val root = Project(id = "root",
    base = file(".") ,
    settings= commonSettings ++ coreDependencies
  ) settings(
    name := "AkkaFileTransfer"
    )

}


