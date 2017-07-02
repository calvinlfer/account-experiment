name := "account-experiment"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies ++= {
  val akka = "com.typesafe.akka"
  val typeLevel = "org.typelevel"
  val akkaV = "2.5.3"
  val catsV = "0.9.0"

  Seq(
    akka      %% "akka-actor"       % akkaV,
    akka      %% "akka-persistence" % akkaV,
    akka      %% "akka-slf4j"       % akkaV,
    akka      %% "akka-testkit"     % akkaV % Test,
    typeLevel %% "cats"             % catsV,

    // Logging
    "org.codehaus.groovy"         % "groovy"           % "2.4.12",
    "ch.qos.logback"              % "logback-classic"  % "1.2.3",

    // LevelDB journal
    "org.iq80.leveldb"            % "leveldb"          % "0.7",
    "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8"
  )
}
