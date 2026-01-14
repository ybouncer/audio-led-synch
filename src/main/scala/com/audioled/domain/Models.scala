package com.audioled.domain

/**
 * Represents RGB color for an LED
 * Demonstrates functional programming with immutable case class
 */
case class Color(red: Int, green: Int, blue: Int) {
  require(red >= 0 && red <= 255, "Red must be 0-255")
  require(green >= 0 && green <= 255, "Green must be 0-255")
  require(blue >= 0 && blue <= 255, "Blue must be 0-255")
  
  def brightness(factor: Double): Color = {
    require(factor >= 0.0 && factor <= 1.0, "Brightness factor must be 0.0-1.0")
    Color(
      (red * factor).toInt,
      (green * factor).toInt,
      (blue * factor).toInt
    )
  }
  
  def blend(other: Color, ratio: Double): Color = {
    Color(
      (red * (1 - ratio) + other.red * ratio).toInt,
      (green * (1 - ratio) + other.green * ratio).toInt,
      (blue * (1 - ratio) + other.blue * ratio).toInt
    )
  }
  
  override def toString: String = f"RGB($red%3d, $green%3d, $blue%3d)"
}

object Color {
  val Red = Color(255, 0, 0)
  val Green = Color(0, 255, 0)
  val Blue = Color(0, 0, 255)
  val Yellow = Color(255, 255, 0)
  val Cyan = Color(0, 255, 255)
  val Magenta = Color(255, 0, 255)
  val White = Color(255, 255, 255)
  val Black = Color(0, 0, 0)
}

/**
 * Represents the state of a single LED
 */
case class LEDState(id: Int, color: Color, intensity: Double) {
  require(intensity >= 0.0 && intensity <= 1.0, "Intensity must be 0.0-1.0")
  
  def actualColor: Color = color.brightness(intensity)
  
  override def toString: String = 
    f"LED[$id%2d]: ${actualColor.toString} (${intensity * 100}%3.0f%%)"
}

/**
 * Represents audio features extracted from analysis
 * This is the monadic value that flows through Observables
 */
case class AudioFeatures(
  timestamp: Long,
  energy: Double,              // Overall energy/loudness
  bassEnergy: Double,          // Low frequency energy
  midEnergy: Double,           // Mid frequency energy
  highEnergy: Double,          // High frequency energy
  spectralCentroid: Double,    // "Brightness" of sound
  isBeat: Boolean,             // Beat detected
  tempo: Option[Double],       // BPM if detected
  samples: Array[Double]       // Raw audio samples for waveform
) {
  def normalized: AudioFeatures = {
    val maxEnergy = math.max(math.max(bassEnergy, midEnergy), highEnergy)
    val normFactor = if (maxEnergy > 0) 1.0 / maxEnergy else 1.0
    
    copy(
      energy = math.min(1.0, energy * normFactor),
      bassEnergy = bassEnergy * normFactor,
      midEnergy = midEnergy * normFactor,
      highEnergy = highEnergy * normFactor
    )
  }
  
  override def toString: String = 
    f"AudioFeatures(E:$energy%.2f B:$bassEnergy%.2f M:$midEnergy%.2f " +
    f"H:$highEnergy%.2f Beat:$isBeat SC:$spectralCentroid%.2f)"
}

/**
 * Commands for LED Actor system
 */
sealed trait LEDCommand

object LEDCommand {
  case class UpdateLEDs(states: Vector[LEDState]) extends LEDCommand
  case class SetPattern(pattern: LEDPattern) extends LEDCommand
  case class SetBrightness(brightness: Double) extends LEDCommand
  case object GetCurrentState extends LEDCommand
  case class CurrentState(states: Vector[LEDState]) extends LEDCommand
}

/**
 * LED patterns that can be applied
 */
sealed trait LEDPattern

object LEDPattern {
  case object Off extends LEDPattern
  case class Solid(color: Color) extends LEDPattern
  case class Spectrum(features: AudioFeatures) extends LEDPattern
  case class Energy(features: AudioFeatures) extends LEDPattern
  case class Beat(features: AudioFeatures) extends LEDPattern
  case class Wave(features: AudioFeatures) extends LEDPattern
  case class Custom(mapper: AudioFeatures => Vector[LEDState]) extends LEDPattern
}

/**
 * Configuration for the system
 */
case class AudioLEDConfig(
  sampleRate: Int,
  bufferSize: Int,
  fftSize: Int,
  ledCount: Int,
  updateRateMs: Int
)

/**
 * Events that can occur in the system (for logging/monitoring)
 */
sealed trait SystemEvent

object SystemEvent {
  case class AudioProcessed(features: AudioFeatures) extends SystemEvent
  case class LEDsUpdated(states: Vector[LEDState]) extends SystemEvent
  case class BeatDetected(timestamp: Long, energy: Double) extends SystemEvent
  case class Error(message: String, cause: Option[Throwable] = None) extends SystemEvent
}
