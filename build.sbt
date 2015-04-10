name := "wesqc"

version := "1.0"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= Seq(
	"org.apache.spark" %% "spark-core" % "1.2.1" % "provided",
	"org.ini4j" % "ini4j" % "0.5.4"
)

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
assemblyJarName in assembly := "wesqc-1.0.jar"