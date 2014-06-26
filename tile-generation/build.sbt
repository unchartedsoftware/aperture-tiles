import AssemblyKeys._

assemblySettings

organization:="com.oculusinfo"

name:="tile-generation"

version:="0.4-SNAPSHOT"

scalaVersion:="2.10.3"



// Testing settings
// Spark tests can only run one at a time, so we have to set all tests to run that way.
// Copied from the Spark build file
// Only allow one test at a time, even across projects, since they run in the same JVM
concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)



resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Global Maven Repository" at "http://repo1.maven.org/maven2/"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

// Needed for older akka libraries; I'm not sure how maven finds it, but SBT
// seems to need it explicitly specified
resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases"

scalacOptions += "-deprecation"

scalacOptions += "-unchecked"

libraryDependencies ++= Seq(
	"org.apache.spark" % "spark-core_2.10" % "0.9.0-incubating",
	"com.oculusinfo" % "binning-utilities" % "0.4-SNAPSHOT",
	"org.apache.spark" % "spark-streaming_2.10" % "0.9.0-incubating",
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

net.virtualvoid.sbt.graph.Plugin.graphSettings
