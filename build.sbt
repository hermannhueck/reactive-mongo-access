name := """reactive-mongo-access"""

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.4.8",
  "org.reactivemongo" %% "reactivemongo" % "0.11.14",
  "com.typesafe.play" %% "play-streams" % "2.5.4",
  "org.mongodb.scala" %% "mongo-scala-driver" % "1.1.1",
  "org.mongodb" %% "casbah" % "3.1.1",
  "org.mongodb" % "mongo-java-driver" % "3.3.0",
  "org.mongodb" % "mongodb-driver-async" % "3.3.0",
  "org.mongodb" % "mongodb-driver-rx" % "1.2.0",
  "org.mongodb" % "mongodb-driver-reactivestreams" % "1.2.0",
  "org.mongodb.morphia" % "morphia" % "1.1.1",
  "io.reactivex" % "rxjava-reactive-streams" % "1.1.1",
  "io.reactivex" % "rxscala_2.11" % "0.26.2",
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.8" % "test",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)
