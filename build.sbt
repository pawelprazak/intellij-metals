import org.jetbrains.sbtidea.IntelliJPlatform.IdeaUltimate

lazy val scala213           = "2.13.12"
lazy val scalaPluginVersion = "2024.1.15"
lazy val minorVersion       = "0"
lazy val buildVersion       = sys.env.getOrElse("ZIO_INTELLIJ_BUILD_NUMBER", minorVersion)
lazy val pluginVersion      = s"2024.1.0.$buildVersion"

ThisBuild / intellijPluginName := "intellij-metals"
ThisBuild / intellijPlatform := IdeaUltimate
ThisBuild / intellijBuild := "241.14494.240"

Global / intellijAttachSources := true

addCommandAlias("fmt", "scalafmtAll")
addCommandAlias("check", "scalafmtCheckAll")

(Global / javacOptions) := Seq("--release", "17")

lazy val root =
  newProject("intellij-metals", file("."))
    .enablePlugins(SbtIdeaPlugin)
    .settings(
      patchPluginXml := pluginXmlOptions { xml =>
        xml.version = version.value
        xml.changeNotes = sys.env.getOrElse(
          "PLUGIN_CHANGE_NOTES",
          s"""<![CDATA[
        <b>Initial release of Metals support for IntelliJ IDEA!</b>
        <ul>
        </ul>
        ]]>"""
        )
      }
    )

def newProject(projectName: String, base: File): Project =
  Project(projectName, base).settings(
    name := projectName,
    scalaVersion := scala213,
    version := pluginVersion,
    libraryDependencies ++= Seq(
      "com.novocode" % "junit-interface" % "0.11" % Test
    ),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-s", "-a", "+c", "+q"),
    intellijPlugins := Seq(
      "com.intellij.java".toPlugin,
      s"org.intellij.scala:$scalaPluginVersion".toPlugin
    )
  )
