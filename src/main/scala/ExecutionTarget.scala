

/** A target for an adb command */
sealed abstract class ExecutionTarget

/** The emulator as targeted with adb -e */
case object Emulator extends ExecutionTarget

/** The first USB device as targeted with adb -d */
case object USBDevice extends ExecutionTarget

/** The device as targeted with adb -s <id> */
case class IdentifiedDevice(id: String) extends ExecutionTarget