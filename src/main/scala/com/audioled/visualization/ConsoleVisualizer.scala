package com.audioled.visualization

import com.audioled.domain._
import scala.io.AnsiColor._

/**
 * Console-based LED visualization
 * Simple but effective way to see LED states in terminal
 */
class ConsoleVisualizer(ledCount: Int, width: Int = 80) {
  
  private val ledWidth = width / ledCount
  
  def visualize(states: Vector[LEDState], features: AudioFeatures): Unit = {
    // Clear screen (works in most terminals)
    print("\u001b[2J\u001b[H")
    
    println("=" * width)
    println(" Audio-LED Synchronization System ")
    println("=" * width)
    println()
    
    // Show audio features
    println("Audio Features:")
    println(f"  Energy:   ${createBar(features.energy, 50)}")
    println(f"  Bass:     ${createBar(features.bassEnergy, 50)}")
    println(f"  Mid:      ${createBar(features.midEnergy, 50)}")
    println(f"  High:     ${createBar(features.highEnergy, 50)}")
    println(f"  Beat:     ${if (features.isBeat) "💥 BEAT!" else "       "}")
    println(f"  Centroid: ${features.spectralCentroid}%.0f Hz")
    println()
    
    // Show LEDs
    println("LED States:")
    println()
    
    // LED visual representation
    val ledLine = states.map { led =>
      val char = if (led.intensity > 0.8) "█"
                 else if (led.intensity > 0.6) "▓"
                 else if (led.intensity > 0.4) "▒"
                 else if (led.intensity > 0.2) "░"
                 else " "
      
      val colorCode = getAnsiColor(led.actualColor)
      s"$colorCode${char * ledWidth}$RESET"
    }.mkString("")
    
    println("┌" + "─" * width + "┐")
    println("│" + ledLine + "│")
    println("└" + "─" * width + "┘")
    println()
    
    // LED details
    println("LED Details:")
    states.grouped(5).foreach { group =>
      println(group.map { led =>
        f"${led.id}%2d:${getColorEmoji(led.actualColor)}${led.intensity * 100}%3.0f%%"
      }.mkString("  "))
    }
    
    println()
    println("─" * width)
    println(s"Timestamp: ${features.timestamp}")
  }
  
  def visualizeWaveform(samples: Array[Double], width: Int = 80, height: Int = 20): Unit = {
    if (samples.isEmpty) return
    
    println("\nWaveform:")
    
    val normalized = samples.map(_ / samples.map(math.abs).max.max(0.001))
    val samplesPerColumn = math.max(1, samples.length / width)
    
    for (row <- (height - 1) to 0 by -1) {
      val threshold = (row.toDouble / height) * 2 - 1
      val line = (0 until width).map { col =>
        val sampleIdx = col * samplesPerColumn
        if (sampleIdx < normalized.length) {
          val sample = normalized(sampleIdx)
          if (math.abs(sample - threshold) < (1.0 / height)) "█"
          else if (math.abs(threshold) < 0.05) "─"
          else " "
        } else " "
      }.mkString("")
      
      println("│" + line + "│")
    }
    println("└" + "─" * width + "┘")
  }
  
  private def createBar(value: Double, width: Int): String = {
    val filled = (value * width).toInt.min(width)
    val empty = width - filled
    
    val color = if (value > 0.8) RED
                else if (value > 0.5) YELLOW
                else GREEN
    
    s"$color${"█" * filled}$RESET${"░" * empty} ${(value * 100).toInt}%"
  }
  
  private def getAnsiColor(color: Color): String = {
    // Simple color mapping to ANSI colors
    if (color.red > 200 && color.green < 100 && color.blue < 100) RED
    else if (color.green > 200 && color.red < 100 && color.blue < 100) GREEN
    else if (color.blue > 200 && color.red < 100 && color.green < 100) BLUE
    else if (color.red > 200 && color.green > 200) YELLOW
    else if (color.red > 200 && color.blue > 200) MAGENTA
    else if (color.green > 200 && color.blue > 200) CYAN
    else if (color.red > 200 && color.green > 200 && color.blue > 200) WHITE
    else RESET
  }
  
  private def getColorEmoji(color: Color): String = {
    if (color.red > 200 && color.green < 100 && color.blue < 100) "🔴"
    else if (color.green > 200 && color.red < 100 && color.blue < 100) "🟢"
    else if (color.blue > 200 && color.red < 100 && color.green < 100) "🔵"
    else if (color.red > 200 && color.green > 200) "🟡"
    else if (color.red < 50 && color.green < 50 && color.blue < 50) "⚫"
    else "⚪"
  }
}

/**
 * Simple spectrum analyzer visualization
 */
class SpectrumVisualizer(width: Int = 80, height: Int = 20) {
  
  def visualize(features: AudioFeatures): Unit = {
    println("\nSpectrum Analysis:")
    
    // Create a simple 3-band spectrum display
    val bands = Vector(
      ("Bass", features.bassEnergy, RED),
      ("Mid", features.midEnergy, GREEN),
      ("High", features.highEnergy, BLUE)
    )
    
    for (row <- (height - 1) to 0 by -1) {
      val threshold = row.toDouble / height
      val line = bands.map { case (name, energy, color) =>
        if (energy > threshold) s"$color█$RESET"
        else "░"
      }.mkString("  ")
      
      val label = if (row == height - 1) "100%"
                  else if (row == height / 2) " 50%"
                  else if (row == 0) "  0%"
                  else "    "
      
      println(f"$label │ $line")
    }
    
    println("     └─" + "─" * 15)
    println("       " + bands.map(_._1).mkString("  "))
  }
}

/**
 * Beat visualizer - shows beat detection history
 */
class BeatVisualizer(historySize: Int = 50) {
  private var beatHistory: Vector[Boolean] = Vector.empty
  
  def visualize(isBeat: Boolean): Unit = {
    beatHistory = (beatHistory :+ isBeat).takeRight(historySize)
    
    println("\nBeat Detection:")
    val visual = beatHistory.map { beat =>
      if (beat) s"${RED}█${RESET}" else "░"
    }.mkString("")
    
    println("│" + visual + "│")
    println("└" + "─" * historySize + "┘")
    println("  " + "←" + " " * (historySize - 10) + "Now")
  }
}
