name := "account-experiment"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies ++= {
  val akka = "com.typesafe.akka"
  val typeLevel = "org.typelevel"
  val scalaCheck = "org.scalacheck"
  val scalaCheckV = "1.13.4"
  val akkaV = "2.5.3"
  val catsV = "0.9.0"

  Seq(
    // Messaging layer
    akka        %% "akka-actor"       % akkaV,
    akka        %% "akka-persistence" % akkaV,
    akka        %% "akka-slf4j"       % akkaV,
    akka        %% "akka-testkit"     % akkaV % Test,

    // Monads for algebra
    typeLevel   %% "cats"             % catsV,

    // Property based testing
    scalaCheck  %% "scalacheck"       % scalaCheckV % Test,

    // Logging
    "org.codehaus.groovy"             % "groovy"           % "2.4.12",
    "ch.qos.logback"                  % "logback-classic"  % "1.2.3",

    // LevelDB journal
    "org.iq80.leveldb"                % "leveldb"          % "0.7",
    "org.fusesource.leveldbjni"       % "leveldbjni-all"   % "1.8"
  )
}
