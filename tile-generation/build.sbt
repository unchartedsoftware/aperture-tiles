import AssemblyKeys._

assemblySettings

organization:="com.oculusinfo"

name:="tile-generation"

version:="0.1-SNAPSHOT"

scalaVersion:="2.9.3"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Global Maven Repository" at "http://repo1.maven.org/maven2/"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

scalacOptions += "-deprecation"

scalacOptions += "-unchecked"

libraryDependencies ++= Seq(
	"org.spark-project" % "spark-core_2.9.3" % "0.7.2",
	"com.oculusinfo" % "geometric-utilities" % "0.1-SNAPSHOT",
        "com.oculusinfo" % "binning-utilities" % "0.1-SNAPSHOT",
	"org.scalatest" %% "scalatest" % "1.9.1" % "test"
)

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.m2/repository")))


test in assembly := {}

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  cp filter {j => j.data.getName == "mesos-0.9.0-incubating.jar" ||
                  j.data.getName == "mockito-all-1.8.5.jar" ||
                  j.data.getName == "spark-core_2.9.3.jar" ||
                  j.data.getName == "scala-library.jar" ||
                  j.data.getName == "servlet-api-2.5-20081211.jar" ||
                  j.data.getName == "javax.servlet-2.5.0.v201103041518.jar"
            }
}

mergeStrategy in assembly := {
  case m if m.toLowerCase.endsWith("manifest.mf") => MergeStrategy.discard
  case m if m.toLowerCase.endsWith(".rsa") => MergeStrategy.discard
  case m if m.toLowerCase.endsWith(".dsa") => MergeStrategy.discard
  case m if m.toLowerCase.endsWith(".sf") => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}
