import java.util.Map
import javax.management.remote.rmi._RMIConnection_Stub
import sbt._

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.android.ddmlib.Log
import com.android.ddmlib.RawImage

import java.io.{File, OutputStream, IOException}
import java.awt.image.{BufferedImage, RenderedImage}
import javax.imageio.ImageIO

trait DdmSupport extends BaseAndroidProject {

  lazy val bridge = {
    AndroidDebugBridge.init(false)
    Runtime.getRuntime().addShutdownHook(new Thread() { override def run() { AndroidDebugBridge.terminate() }})
    AndroidDebugBridge.createBridge(adbPath.absolutePath, false)
  }

  lazy val screenshotEmulator = screenshotEmulatorAction
  def screenshotEmulatorAction = task {
    screenshot(Emulator, false).getOrElse(error("could not get screenshot")).toFile("png", "emulator.png")
    None
  } describedAs("take a screenshot from the emulator")

  lazy val screenshotDevice = screenshotDeviceAction
  def screenshotDeviceAction = task {
    screenshot(device, false).getOrElse(error("could not get screenshot")).toFile("png", "device.png")
    None
  } describedAs("take a screenshot from the device")

  // ported from http://dustingram.com/wiki/Device_Screenshots_with_the_Android_Debug_Bridge

  private[this] def initializedBridgeOption: Option[AndroidDebugBridge] = {
    var count = 0
    while (!bridge.hasInitialDeviceList() && count < 50) {
      Thread.sleep(100)
      count += 1
    }
    if (!bridge.hasInitialDeviceList()) {
      System.err.println("Timeout getting device list")
      None
    } else {
      Some(bridge)
    }
  }

  private[this] def withDevice[F](executionTarget: ExecutionTarget)(action: IDevice => F):Option[F] = {
    initializedBridgeOption flatMap { initializedBridge =>
      val candidateFilter: (IDevice=>Boolean) = executionTarget match {
        case Emulator => _.isEmulator
        case IdentifiedDevice(serial) => serial == _.getSerialNumber
        case USBDevice => ! _.isEmulator
      }
      val candidates = initializedBridge.getDevices() filter candidateFilter
      candidates.firstOption map action
    }
  }

  private[this] def screenshot(executionTarget: ExecutionTarget, landscape: Boolean):Option[Screenshot] = {
    withDevice(executionTarget) { device =>
      val raw = device.getScreenshot()
      val (width2, height2) = if (landscape) (raw.height, raw.width) else (raw.width, raw.height)
      val image = new BufferedImage(width2, height2, BufferedImage.TYPE_INT_RGB)
      var index = 0
      val indexInc = raw.bpp >> 3
      for (y <- 0 until raw.height; x <- 0 until raw.width) {
        val value = raw.getARGB(index)
        if (landscape)
          image.setRGB(y, raw.width - x - 1, value)
        else
          image.setRGB(x, y, value)
        index += indexInc
      }
      new Screenshot(image)
    }
  }

  override lazy val device: ExecutionTarget = {
    initializedBridgeOption match {
      case Some(initializedBridge) =>
        val realDevices = initializedBridge.getDevices() filter { ! _.isEmulator() }
        realDevices.firstOption map { d => IdentifiedDevice(d.getSerialNumber) } getOrElse super.device
      case None => super.device
    }
  }

  class Screenshot(val r: RenderedImage) {
    def toFile(format: String, f: File):Boolean = ImageIO.write(r, format, f)
    def toFile(format: String, s: String):Boolean = toFile(format, new File(s))
    def toOutputStream(format: String, o: OutputStream) = ImageIO.write(r, format, o)
  }
}
