addSbtPlugin("com.lucidchart"   % "sbt-scalafmt"        % "1.14")
addSbtPlugin("com.thesamet"     % "sbt-protoc"          % "0.99.12")
addSbtPlugin("io.get-coursier"  % "sbt-coursier"        % "1.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.2")

libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.6"