name := "shopping-cart"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= {
  val akka = "com.typesafe.akka"
  val akkaV = "2.5.8"
  Seq(
    akka             %% "akka-actor"       % akkaV,
    akka             %% "akka-testkit"     % akkaV % Test,
    akka             %% "akka-cluster"     % akkaV,
    akka             %% "akka-persistence" % akkaV,
    akka             %% "akka-slf4j"       % akkaV,
    "ch.qos.logback" % "logback-classic"   % "1.2.3"
  )
}

scalafmtOnCompile in ThisBuild := true
