name := """reactive-mongo-access"""

version := "1.0"

scalaVersion := "2.11.7"

// resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.4.4",
  "org.reactivemongo" %% "reactivemongo" % "0.11.11",
  "org.mongodb.scala" %% "mongo-scala-driver" % "1.1.1",
  "org.mongodb" % "mongo-java-driver" % "3.2.2",
  "org.mongodb" % "mongodb-driver-async" % "3.2.2",
  "org.mongodb" % "mongodb-driver-rx" % "1.2.0",
  "org.mongodb" % "mongodb-driver-reactivestreams" % "1.2.0",
  "org.mongodb.morphia" % "morphia" % "1.1.1",
  "io.reactivex" % "rxjava-reactive-streams" % "1.0.1",
  "io.reactivex" % "rxscala_2.11" % "0.26.1",
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.4" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)
