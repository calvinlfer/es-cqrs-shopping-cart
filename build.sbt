import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport.scalafmtOnCompile

organization in ThisBuild := "com.experiments"
version in ThisBuild := "0.1"

scalaVersion in ThisBuild := "2.12.4"

lazy val `shopping-cart` = (project in file(".")).aggregate(`common`, `shopping-cart-command`)

val akka = "com.typesafe.akka"
val akkaV = "2.5.8"

// The common project contains the protocol buffers responsible for communication between the command and
// query-processor sides and also contains reusable code, it also generates the source for its protos
lazy val `common` =
  (project in file("common"))
    .settings(
      libraryDependencies ++= Seq(akka %% "akka-actor" % akkaV, akka %% "akka-cluster" % akkaV),
      scalafmtOnCompile in ThisBuild := true,
      PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)
    )

lazy val `shopping-cart-command` =
  (project in file("shopping-cart-command"))
    .dependsOn(`common`)
    .settings(
      libraryDependencies ++= {
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
      scalafmtOnCompile in ThisBuild := true,
      // Import proto files of the depending project since the command protos reference the depending protos
      PB.includePaths in Compile += file("common/src/main/protobuf"),
      PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value)
    )

// Won't work properly until SBT 1.1.1, use IntelliJ until then
addCommandAlias("command-side", "shopping-cart-command/runMain com.experiments.shopping.cart.Main")
