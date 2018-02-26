import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport.scalafmtOnCompile

organization in ThisBuild := "com.experiments"
version in ThisBuild := "0.1"

scalaVersion in ThisBuild := "2.12.4"

lazy val `shopping-cart` = (project in file(".")).aggregate(`common`, `shopping-cart-command`)

val akka = "com.typesafe.akka"
val akkaV = "2.5.10"

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
    .enablePlugins(JavaAppPackaging)
    .settings(
      libraryDependencies ++= {
        val circe = "io.circe"
        val circeV = "0.9.0"

        Seq(
          akka                       %% "akka-actor"                        % akkaV,
          akka                       %% "akka-http"                         % "10.1.0-RC1",
          "de.heikoseeberger"        %% "akka-http-circe"                   % "1.20.0-RC1",
          akka                       %% "akka-stream"                       % akkaV,
          akka                       %% "akka-testkit"                      % akkaV % Test,
          akka                       %% "akka-cluster-sharding"             % akkaV,
          akka                       %% "akka-cluster-tools"                % akkaV,
          akka                       %% "akka-persistence"                  % akkaV,
          akka                       %% "akka-slf4j"                        % akkaV,
          akka                       %% "akka-persistence-cassandra"        % "0.80",
          circe                      %% "circe-core"                        % circeV,
          circe                      %% "circe-generic"                     % circeV,
          circe                      %% "circe-parser"                      % circeV,
          circe                      %% "circe-java8"                       % circeV,
          "com.lightbend.akka"       %% "akka-management-cluster-http"      % "0.6",
          "org.typelevel"            %% "cats-core"                         % "1.0.1",
          "de.heikoseeberger"        %% "constructr"                        % "0.18.1",
          "com.lightbend.constructr" %% "constructr-coordination-zookeeper" % "0.4.0",
          "ch.qos.logback"           % "logback-classic"                    % "1.2.3",
          "org.codehaus.groovy"      % "groovy"                             % "2.4.13"
        )
      },
      scalafmtOnCompile in ThisBuild := true,
      // Import proto files of the depending project since the command protos reference the depending protos
      PB.includePaths in Compile += file("common/src/main/protobuf"),
      PB.targets in Compile := Seq(scalapb.gen() -> (sourceManaged in Compile).value),
      dockerBaseImage := "anapsix/alpine-java:8"
    )

lazy val `shopping-cart-query-vendor-billing` =
  (project in file("shopping-cart-query-vendor-billing"))
    .dependsOn(`common`)
    .enablePlugins(JavaAppPackaging)
    .settings(
      libraryDependencies ++= {
        val phantom = "com.outworkers"
        val phantomV = "2.20.2"

        Seq(
          akka                       %% "akka-actor"                        % akkaV,
          akka                       %% "akka-stream"                       % akkaV,
          akka                       %% "akka-cluster-sharding"             % akkaV,
          akka                       %% "akka-cluster-tools"                % akkaV,
          akka                       %% "akka-persistence-query"            % akkaV,
          akka                       %% "akka-slf4j"                        % akkaV,
          akka                       %% "akka-persistence-cassandra"        % "0.80",
          "de.heikoseeberger"        %% "constructr"                        % "0.18.1",
          "com.lightbend.constructr" %% "constructr-coordination-zookeeper" % "0.4.0",
          phantom                    %% "phantom-dsl"                       % phantomV,
          phantom                    %% "phantom-jdk8"                      % phantomV,
          "org.scala-lang"           % "scala-reflect"                      % scalaVersion.value,
          "ch.qos.logback"           % "logback-classic"                    % "1.2.3",
          "org.codehaus.groovy"      % "groovy"                             % "2.4.13"
        )
      },
      scalafmtOnCompile in ThisBuild := true,
      dockerBaseImage := "anapsix/alpine-java:8"
    )

lazy val `shopping-cart-query-vendor-billing-jdbc` =
  (project in file("shopping-cart-query-vendor-billing-jdbc"))
    .dependsOn(`common`)
    .enablePlugins(JavaAppPackaging)
    .settings(
      libraryDependencies ++= {
        val slick = "com.typesafe.slick"
        val slickV = "3.2.1"

        Seq(
          akka                       %% "akka-actor"                        % akkaV,
          akka                       %% "akka-stream"                       % akkaV,
          akka                       %% "akka-cluster-sharding"             % akkaV,
          akka                       %% "akka-cluster-tools"                % akkaV,
          akka                       %% "akka-persistence-query"            % akkaV,
          akka                       %% "akka-slf4j"                        % akkaV,
          slick                      %% "slick"                             % slickV,
          slick                      %% "slick-hikaricp"                    % slickV,
          akka                       %% "akka-persistence-cassandra"        % "0.80",
          "de.heikoseeberger"        %% "constructr"                        % "0.18.1",
          "com.lightbend.constructr" %% "constructr-coordination-zookeeper" % "0.4.0",
          "org.postgresql"           % "postgresql"                         % "42.1.4",
          "org.scala-lang"           % "scala-reflect"                      % scalaVersion.value,
          "ch.qos.logback"           % "logback-classic"                    % "1.2.3",
          "org.codehaus.groovy"      % "groovy"                             % "2.4.13"
        )
      },
      scalafmtOnCompile in ThisBuild := true,
      dockerBaseImage := "anapsix/alpine-java:8"
    )

lazy val `shopping-cart-query-popular-items` =
  (project in file("shopping-cart-query-popular-items"))
    .dependsOn(`common`)
    .enablePlugins(JavaAppPackaging)
    .settings(
      libraryDependencies ++= {
        val phantom = "com.outworkers"
        val phantomV = "2.20.2"

        Seq(
          akka                       %% "akka-actor"                        % akkaV,
          akka                       %% "akka-stream"                       % akkaV,
          akka                       %% "akka-cluster-sharding"             % akkaV,
          akka                       %% "akka-cluster-tools"                % akkaV,
          akka                       %% "akka-persistence-query"            % akkaV,
          akka                       %% "akka-slf4j"                        % akkaV,
          akka                       %% "akka-persistence-cassandra"        % "0.80",
          "de.heikoseeberger"        %% "constructr"                        % "0.18.1",
          "com.lightbend.constructr" %% "constructr-coordination-zookeeper" % "0.4.0",
          phantom                    %% "phantom-dsl"                       % phantomV,
          phantom                    %% "phantom-jdk8"                      % phantomV,
          "org.scala-lang"           % "scala-reflect"                      % scalaVersion.value,
          "ch.qos.logback"           % "logback-classic"                    % "1.2.3",
          "org.codehaus.groovy"      % "groovy"                             % "2.4.13"
        )
      },
      scalafmtOnCompile in ThisBuild := true,
      dockerBaseImage := "anapsix/alpine-java:8"
    )

lazy val `shopping-cart-query-items-purchased-events` =
  (project in file("shopping-cart-query-item-purchased-events"))
    .dependsOn(`common`)
    .enablePlugins(JavaAppPackaging)
    .settings(libraryDependencies ++= {
      val phantom = "com.outworkers"
      val phantomV = "2.20.2"
      val circe = "io.circe"
      val circeV = "0.9.0"

      Seq(
        akka                       %% "akka-actor"                        % akkaV,
        akka                       %% "akka-stream"                       % akkaV,
        akka                       %% "akka-cluster-sharding"             % akkaV,
        akka                       %% "akka-cluster-tools"                % akkaV,
        akka                       %% "akka-persistence-query"            % akkaV,
        akka                       %% "akka-slf4j"                        % akkaV,
        "de.heikoseeberger"        %% "constructr"                        % "0.18.1",
        "com.lightbend.constructr" %% "constructr-coordination-zookeeper" % "0.4.0",
        akka                       %% "akka-persistence-cassandra"        % "0.80",
        akka                       %% "akka-stream-kafka"                 % "0.18",
        circe                      %% "circe-core"                        % circeV,
        circe                      %% "circe-generic"                     % circeV,
        circe                      %% "circe-parser"                      % circeV,
        circe                      %% "circe-java8"                       % circeV,
        phantom                    %% "phantom-dsl"                       % phantomV,
        phantom                    %% "phantom-jdk8"                      % phantomV,
        "org.scala-lang"           % "scala-reflect"                      % scalaVersion.value,
        "ch.qos.logback"           % "logback-classic"                    % "1.2.3",
        "org.codehaus.groovy"      % "groovy"                             % "2.4.13"
      )
    })

addCommandAlias("command-side", "shopping-cart-command/runMain com.experiments.shopping.cart.Main")
addCommandAlias("query-vendor-billing", "shopping-cart-query-vendor-billing/runMain com.experiments.shopping.cart.Main")
addCommandAlias("query-popular-items", "shopping-cart-query-popular-items/runMain com.experiments.shopping.cart.Main")
