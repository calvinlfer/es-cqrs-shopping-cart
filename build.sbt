name := "shopping-cart"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= {
  val akka = "com.typesafe.akka"
  val akkaV = "2.5.8"
  Seq(
    akka                       %% "akka-actor"                        % akkaV,
    akka                       %% "akka-testkit"                      % akkaV % Test,
    akka                       %% "akka-cluster-sharding"             % akkaV,
    akka                       %% "akka-persistence"                  % akkaV,
    akka                       %% "akka-slf4j"                        % akkaV,
    akka                       %% "akka-persistence-cassandra"        % "0.80-RC2",
    "org.typelevel"            %% "cats-core"                         % "1.0.0-RC1",
    "de.heikoseeberger"        %% "constructr"                        % "0.18.1",
    "com.lightbend.constructr" %% "constructr-coordination-zookeeper" % "0.4.0",
    "ch.qos.logback"           % "logback-classic"                    % "1.2.3",
    "org.codehaus.groovy"      % "groovy"                             % "2.4.13"
  )
}

scalafmtOnCompile in ThisBuild := true
