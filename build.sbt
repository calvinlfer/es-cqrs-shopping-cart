import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport.scalafmtOnCompile

organization in ThisBuild := "com.experiments"
version in ThisBuild := "0.1"

scalaVersion in ThisBuild := "2.12.4"

lazy val `shopping-cart` = (project in file(".")).aggregate(`common`, `shopping-cart-command`)

// The common project contains the protocol buffers responsible for communication between the command and
// query-processor sides. It is responsible for creating the generated files and making it available to
// any project that depends on common, if a project needs to compile proto files, then add ScalaPB to it
lazy val `common` =
  (project in file("common"))
    .settings(PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value))

lazy val `shopping-cart-command` =
  (project in file("shopping-cart-command"))
    .dependsOn(`common`)
    .settings(
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
      },
      scalafmtOnCompile in ThisBuild := true
    )

// Won't work properly until SBT 1.1.1, use IntelliJ until then
addCommandAlias("command-side", "shopping-cart-command/runMain com.experiments.shopping.cart.Main")
