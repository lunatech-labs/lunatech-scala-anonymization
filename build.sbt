name := "anonymization_scala_tensorflow"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
    "com.github.haifengl" %% "smile-scala" % "1.5.3",
    "org.platanios" %% "tensorflow" % "0.4.1" classifier "darwin-cpu-x86_64",
    "com.github.scopt" %% "scopt" % "4.0.0-RC2")

assemblyMergeStrategy in assembly := {
    case PathList("META-INF", _*) => MergeStrategy.discard
    case _ => MergeStrategy.first
}