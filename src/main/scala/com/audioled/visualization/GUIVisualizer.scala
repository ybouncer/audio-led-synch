package com.audioled.visualization

import com.audioled.domain._
import scala.swing._
import scala.swing.event._
import java.awt.{Color => AWTColor, Graphics2D, RenderingHints, BasicStroke, Font}
import java.awt.geom._
import javax.swing.Timer
import java.util.concurrent.atomic.AtomicReference

/**
 * GUI-based LED visualization
 * Demonstrates: Integration with UI framework
 */
class GUIVisualizer(ledCount: Int, width: Int = 800, height: Int = 600) extends Frame {
  
  title = "Audio-LED Synchronization"
  preferredSize = new Dimension(width, height)
  
  private val currentState = new AtomicReference[VisualizationState](
    VisualizationState(
      Vector.fill(ledCount)(LEDState(0, Color.Black, 0.0)),
      AudioFeatures(0, 0, 0, 0, 0, 0, isBeat = false, None, Array.empty)
    )
  )
  
  private val canvas = new LEDCanvas()
  contents = new BorderPanel {
    layout(canvas) = BorderPanel.Position.Center
  }
  
  // Update timer
  private val updateTimer = new Timer(16, _ => canvas.repaint()) // ~60 FPS
  updateTimer.start()
  
  def update(states: Vector[LEDState], features: AudioFeatures): Unit = {
    currentState.set(VisualizationState(states, features))
  }
  
  override def closeOperation(): Unit = {
    updateTimer.stop()
    super.closeOperation()
  }
  
  private case class VisualizationState(
    ledStates: Vector[LEDState],
    audioFeatures: AudioFeatures
  )
  
  private class LEDCanvas extends Panel {
    background = AWTColor.BLACK
    
    override def paintComponent(g: Graphics2D): Unit = {
      super.paintComponent(g)
      
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                         RenderingHints.VALUE_ANTIALIAS_ON)
      g.setRenderingHint(RenderingHints.KEY_RENDERING,
                         RenderingHints.VALUE_RENDER_QUALITY)
      
      val state = currentState.get()
      val w = size.width
      val h = size.height
      
      // Draw background
      g.setColor(AWTColor.BLACK)
      g.fillRect(0, 0, w, h)
      
      // Draw LEDs
      drawLEDs(g, state.ledStates, w, h)
      
      // Draw audio features
      drawAudioFeatures(g, state.audioFeatures, w, h)
      
      // Draw waveform
      if (state.audioFeatures.samples.nonEmpty) {
        drawWaveform(g, state.audioFeatures.samples, w, h)
      }
      
      // Draw info text
      drawInfo(g, state, w, h)
    }
    
    private def drawLEDs(g: Graphics2D, states: Vector[LEDState], w: Int, h: Int): Unit = {
      val ledSize = math.min(w, h) * 0.08
      val spacing = (w - ledSize * ledCount) / (ledCount + 1)
      val yPos = h * 0.7
      
      states.foreach { led =>
        val xPos = spacing + (spacing + ledSize) * led.id
        
        // Draw LED glow effect
        val glowSize = ledSize * (1 + led.intensity * 0.5)
        val glowAlpha = (led.intensity * 50).toInt.min(255)
        
        g.setColor(new AWTColor(
          led.actualColor.red,
          led.actualColor.green,
          led.actualColor.blue,
          glowAlpha
        ))
        g.fill(new Ellipse2D.Double(
          xPos - (glowSize - ledSize) / 2,
          yPos - (glowSize - ledSize) / 2,
          glowSize,
          glowSize
        ))
        
        // Draw LED body
        g.setColor(toAWTColor(led.actualColor))
        g.fill(new Ellipse2D.Double(xPos, yPos, ledSize, ledSize))
        
        // Draw LED border
        g.setColor(AWTColor.DARK_GRAY)
        g.setStroke(new BasicStroke(2))
        g.draw(new Ellipse2D.Double(xPos, yPos, ledSize, ledSize))
        
        // Draw LED number
        g.setColor(AWTColor.WHITE)
        g.setFont(new Font("Arial", Font.BOLD, 12))
        val fm = g.getFontMetrics
        val text = led.id.toString
        val textX = xPos + (ledSize - fm.stringWidth(text)) / 2
        val textY = yPos + ledSize + 15
        g.drawString(text, textX.toFloat, textY.toFloat)
      }
    }
    
    private def drawAudioFeatures(g: Graphics2D, features: AudioFeatures, w: Int, h: Int): Unit = {
      val barWidth = w * 0.15
      val barHeight = h * 0.4
      val yStart = h * 0.1
      val spacing = (w - barWidth * 3) / 4
      
      // Bass bar
      drawBar(g, spacing, yStart, barWidth, barHeight, 
              features.bassEnergy, "Bass", AWTColor.RED)
      
      // Mid bar
      drawBar(g, spacing * 2 + barWidth, yStart, barWidth, barHeight,
              features.midEnergy, "Mid", AWTColor.GREEN)
      
      // High bar
      drawBar(g, spacing * 3 + barWidth * 2, yStart, barWidth, barHeight,
              features.highEnergy, "High", AWTColor.BLUE)
      
      // Beat indicator
      if (features.isBeat) {
        g.setColor(new AWTColor(255, 255, 255, 200))
        g.setFont(new Font("Arial", Font.BOLD, 36))
        val text = "BEAT!"
        val fm = g.getFontMetrics
        g.drawString(text,
                    (w - fm.stringWidth(text)) / 2,
                    h * 0.55f)
      }
    }
    
    private def drawBar(g: Graphics2D, x: Double, y: Double,
                       width: Double, maxHeight: Double,
                       value: Double, label: String, color: AWTColor): Unit = {
      val height = value * maxHeight
      
      // Draw bar background
      g.setColor(new AWTColor(50, 50, 50))
      g.fill(new Rectangle2D.Double(x, y, width, maxHeight))
      
      // Draw bar fill
      g.setColor(new AWTColor(
        color.getRed,
        color.getGreen,
        color.getBlue,
        (value * 255).toInt.min(255)
      ))
      g.fill(new Rectangle2D.Double(x, y + maxHeight - height, width, height))
      
      // Draw bar border
      g.setColor(AWTColor.GRAY)
      g.setStroke(new BasicStroke(2))
      g.draw(new Rectangle2D.Double(x, y, width, maxHeight))
      
      // Draw label
      g.setColor(AWTColor.WHITE)
      g.setFont(new Font("Arial", Font.BOLD, 14))
      val fm = g.getFontMetrics
      g.drawString(label,
                  (x + (width - fm.stringWidth(label)) / 2).toFloat,
                  (y - 5).toFloat)
      
      // Draw value
      val valueText = f"${value * 100}%.0f%%"
      g.setFont(new Font("Arial", Font.PLAIN, 12))
      val fm2 = g.getFontMetrics
      g.drawString(valueText,
                  (x + (width - fm2.stringWidth(valueText)) / 2).toFloat,
                  (y + maxHeight + 15).toFloat)
    }
    
    private def drawWaveform(g: Graphics2D, samples: Array[Double], w: Int, h: Int): Unit = {
      if (samples.length < 2) return
      
      val waveformHeight = h * 0.08
      val waveformY = h * 0.6
      val waveformWidth = w * 0.8
      val waveformX = w * 0.1
      
      g.setColor(new AWTColor(100, 100, 100, 100))
      g.fill(new Rectangle2D.Double(waveformX, waveformY - waveformHeight / 2,
                                    waveformWidth, waveformHeight))
      
      val path = new GeneralPath()
      val xStep = waveformWidth / samples.length
      
      samples.zipWithIndex.foreach { case (sample, i) =>
        val x = waveformX + i * xStep
        val y = waveformY + sample * waveformHeight * 0.4
        
        if (i == 0) path.moveTo(x, y)
        else path.lineTo(x, y)
      }
      
      g.setColor(AWTColor.CYAN)
      g.setStroke(new BasicStroke(2))
      g.draw(path)
    }
    
    private def drawInfo(g: Graphics2D, state: VisualizationState, w: Int, h: Int): Unit = {
      g.setColor(AWTColor.WHITE)
      g.setFont(new Font("Monospaced", Font.PLAIN, 11))
      
      val lines = Vector(
        f"Energy: ${state.audioFeatures.energy}%.3f",
        f"Spectral Centroid: ${state.audioFeatures.spectralCentroid}%.0f Hz",
        f"Timestamp: ${state.audioFeatures.timestamp}",
        f"LEDs: ${state.ledStates.count(_.intensity > 0.1)}/${ledCount} active"
      )
      
      lines.zipWithIndex.foreach { case (line, i) =>
        g.drawString(line, 10, h - 80 + i * 15)
      }
    }
    
    private def toAWTColor(color: com.audioled.domain.Color): AWTColor = {
      new AWTColor(color.red, color.green, color.blue)
    }
  }
}

/**
 * Interactive control panel for the LED system
 */
class ControlPanel extends Frame {
  title = "LED Control Panel"
  preferredSize = new Dimension(400, 500)
  
  private var patternCallback: LEDPattern => Unit = _ => ()
  private var brightnessCallback: Double => Unit = _ => ()
  
  def setPatternCallback(callback: LEDPattern => Unit): Unit = {
    patternCallback = callback
  }
  
  def setBrightnessCallback(callback: Double => Unit): Unit = {
    brightnessCallback = callback
  }
  
  contents = new BorderPanel {
    val patternPanel = new BoxPanel(Orientation.Vertical) {
      border = Swing.TitledBorder(Swing.LineBorder(java.awt.Color.GRAY), "Pattern Selection")
      
      val offButton = new Button("Off")
      val spectrumButton = new Button("Spectrum")
      val energyButton = new Button("Energy")
      val beatButton = new Button("Beat")
      val waveButton = new Button("Wave")
      
      contents ++= Seq(offButton, spectrumButton, energyButton, beatButton, waveButton)
      
      listenTo(offButton, spectrumButton, energyButton, beatButton, waveButton)
      
      reactions += {
        case ButtonClicked(`offButton`) =>
          patternCallback(LEDPattern.Off)
        case ButtonClicked(`spectrumButton`) =>
          patternCallback(LEDPattern.Spectrum(AudioFeatures(0, 0, 0, 0, 0, 0, 
                                                            isBeat = false, None, Array.empty)))
        case ButtonClicked(`energyButton`) =>
          patternCallback(LEDPattern.Energy(AudioFeatures(0, 0, 0, 0, 0, 0,
                                                          isBeat = false, None, Array.empty)))
        case ButtonClicked(`beatButton`) =>
          patternCallback(LEDPattern.Beat(AudioFeatures(0, 0, 0, 0, 0, 0,
                                                        isBeat = false, None, Array.empty)))
        case ButtonClicked(`waveButton`) =>
          patternCallback(LEDPattern.Wave(AudioFeatures(0, 0, 0, 0, 0, 0,
                                                        isBeat = false, None, Array.empty)))
      }
    }
    
    val brightnessPanel = new BorderPanel {
      border = Swing.TitledBorder(Swing.LineBorder(java.awt.Color.GRAY), "Brightness")
      
      val slider = new Slider {
        min = 0
        max = 100
        value = 100
        majorTickSpacing = 25
        minorTickSpacing = 5
        paintTicks = true
        paintLabels = true
      }
      
      val label = new Label("100%")
      
      layout(slider) = BorderPanel.Position.Center
      layout(label) = BorderPanel.Position.South
      
      listenTo(slider)
      reactions += {
        case ValueChanged(`slider`) =>
          val brightness = slider.value / 100.0
          label.text = f"${slider.value}%%"
          brightnessCallback(brightness)
      }
    }
    
    layout(patternPanel) = BorderPanel.Position.North
    layout(brightnessPanel) = BorderPanel.Position.Center
  }
}
