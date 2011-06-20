import sbt._

abstract class BaseAndroidTestProject(info: ProjectInfo) extends BaseAndroidProject(info) with Installable {

  lazy val testEmulator = instrumentationTestAction(Emulator) describedAs("runs tests in emulator") dependsOn reinstallEmulator
  lazy val testDevice = instrumentationTestAction(USBDevice) describedAs("runs tests on device") dependsOn reinstallDevice

  def instrumentationTestAction(executionTarget: ExecutionTarget) = adbTask(executionTarget, "shell am instrument -w "+manifestPackage+"/android.test.InstrumentationTestRunner") describedAs("runs instrumentation tests")
}
