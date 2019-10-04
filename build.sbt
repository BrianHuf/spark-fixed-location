name := "spark-fixed-location"
version := "0.0.1"
organization := "example"

scalaVersion := "2.11.7"

spName := "example/spark-fixed-location"
sparkVersion := "2.4.4"

sparkComponents := Seq("core", "sql")

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.5" % "provided",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "com.novocode" % "junit-interface" % "0.9" % "test"
)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion.value % "test",
  "org.apache.spark" %% "spark-sql" % sparkVersion.value % "test",
  "org.scala-lang" % "scala-library" % scalaVersion.value % "compile"
)
