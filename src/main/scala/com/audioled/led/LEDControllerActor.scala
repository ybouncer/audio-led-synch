package com.audioled.led

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import com.audioled.domain._
import scala.concurrent.duration._
import com.typesafe.scalalogging.LazyLogging

/**
 * LED Controller Actor
 * Demonstrates: Actor model from slides
 * - Message passing (!)
 * - State management through behaviors
 * - Actor context and lifecycle
 */
object LEDControllerActor extends LazyLogging {
  
  // Actor protocol (messages the actor can receive)
  sealed trait Command
  case class UpdateFromAudio(features: AudioFeatures) extends Command
  case class SetPattern(pattern: LEDPattern) extends Command
  case class SetGlobalBrightness(brightness: Double) extends Command
  case class GetState(replyTo: ActorRef[StateResponse]) extends Command
  case class StateResponse(states: Vector[LEDState]) extends Command
  private case object Tick extends Command
  
  // Actor state
  private case class State(
    ledStates: Vector[LEDState],
    currentPattern: LEDPattern,
    globalBrightness: Double,
    lastUpdate: Long
  )
  
  def apply(ledCount: Int): Behavior[Command] = {
    Behaviors.setup { context =>
      // Initialize LEDs to off state
      val initialStates = Vector.tabulate(ledCount) { i =>
        LEDState(i, Color.Black, 0.0)
      }
      
      val initialState = State(
        ledStates = initialStates,
        currentPattern = LEDPattern.Off,
        globalBrightness = 1.0,
        lastUpdate = System.currentTimeMillis()
      )
      
      // Start a periodic tick for smooth animations
      context.scheduleOnce(20.millis, context.self, Tick)
      
      logger.info(s"LED Controller initialized with $ledCount LEDs")
      
      active(initialState)(context)
    }
  }
  
  /**
   * Active behavior - handles all commands
   * Demonstrates: behavior switching with context.become (from slides)
   */
  private def active(state: State)(implicit context: ActorContext[Command]): Behavior[Command] = {
    Behaviors.receiveMessage {
      case UpdateFromAudio(features) =>
        // Apply current pattern to generate new LED states
        val newStates = applyPattern(state.currentPattern, features, state.ledStates.size)
        val adjustedStates = newStates.map(led => 
          led.copy(intensity = led.intensity * state.globalBrightness)
        )
        
        val newState = state.copy(
          ledStates = adjustedStates,
          lastUpdate = System.currentTimeMillis()
        )
        
        // Could notify observers here
        logger.debug(s"Updated LEDs from audio: ${features.toString.take(50)}...")
        
        active(newState)
      
      case SetPattern(pattern) =>
        logger.info(s"Setting LED pattern to: ${pattern.getClass.getSimpleName}")
        active(state.copy(currentPattern = pattern))
      
      case SetGlobalBrightness(brightness) =>
        val clampedBrightness = math.max(0.0, math.min(1.0, brightness))
        logger.info(f"Setting global brightness to: $clampedBrightness%.2f")
        active(state.copy(globalBrightness = clampedBrightness))
      
      case GetState(replyTo) =>
        replyTo ! StateResponse(state.ledStates)
        Behaviors.same
      
      case Tick =>
        // Schedule next tick for smooth animations
        context.scheduleOnce(20.millis, context.self, Tick)
        
        // Could add animation interpolation here
        Behaviors.same
      
      case StateResponse(_) =>
        // Shouldn't receive this, but handle gracefully
        Behaviors.same
    }
  }
  
  /**
   * Apply a pattern to generate LED states from audio features
   * Demonstrates: pattern matching and functional composition
   */
  private def applyPattern(pattern: LEDPattern, 
                          features: AudioFeatures, 
                          ledCount: Int): Vector[LEDState] = {
    pattern match {
      case LEDPattern.Off =>
        Vector.fill(ledCount)(LEDState(0, Color.Black, 0.0)).zipWithIndex.map {
          case (led, i) => led.copy(id = i)
        }
      
      case LEDPattern.Solid(color) =>
        Vector.fill(ledCount)(LEDState(0, color, 1.0)).zipWithIndex.map {
          case (led, i) => led.copy(id = i)
        }
      
      case LEDPattern.Spectrum(audioFeatures) =>
        // Map frequency bands to LED positions
        val normalized = audioFeatures.normalized
        
        Vector.tabulate(ledCount) { i =>
          val position = i.toDouble / ledCount
          
          val (color, intensity) = if (position < 0.33) {
            // Bass region - Red
            (Color.Red, normalized.bassEnergy)
          } else if (position < 0.66) {
            // Mid region - Green
            (Color.Green, normalized.midEnergy)
          } else {
            // High region - Blue
            (Color.Blue, normalized.highEnergy)
          }
          
          LEDState(i, color, intensity)
        }
      
      case LEDPattern.Energy(audioFeatures) =>
        // All LEDs show overall energy
        val normalized = audioFeatures.normalized
        val hue = (normalized.spectralCentroid / 4000.0).min(1.0)
        val color = Color.Red.blend(Color.Blue, hue)
        
        Vector.tabulate(ledCount) { i =>
          LEDState(i, color, normalized.energy)
        }
      
      case LEDPattern.Beat(audioFeatures) =>
        // Flash on beats
        val normalized = audioFeatures.normalized
        if (audioFeatures.isBeat) {
          Vector.tabulate(ledCount) { i =>
            LEDState(i, Color.White, 1.0)
          }
        } else {
          // Decay based on energy
          val intensity = normalized.energy * 0.3
          Vector.tabulate(ledCount) { i =>
            val hue = i.toDouble / ledCount
            val color = Color.Red.blend(Color.Blue, hue)
            LEDState(i, color, intensity)
          }
        }
      
      case LEDPattern.Wave(audioFeatures) =>
        // Create wave pattern based on audio
        val normalized = audioFeatures.normalized
        val time = System.currentTimeMillis() / 1000.0
        
        Vector.tabulate(ledCount) { i =>
          val position = i.toDouble / ledCount
          val wave = math.sin(position * 2 * math.Pi + time * normalized.energy * 5)
          val intensity = ((wave + 1) / 2) * normalized.energy
          
          val bassColor = Color.Red.brightness(normalized.bassEnergy)
          val midColor = Color.Green.brightness(normalized.midEnergy)
          val highColor = Color.Blue.brightness(normalized.highEnergy)
          
          val blendedColor = if (position < 0.5) {
            bassColor.blend(midColor, position * 2)
          } else {
            midColor.blend(highColor, (position - 0.5) * 2)
          }
          
          LEDState(i, blendedColor, intensity)
        }
      
      case LEDPattern.Custom(mapper) =>
        mapper(features)
    }
  }
}

/**
 * LED Manager - coordinates multiple LED controllers
 * Demonstrates: actor hierarchy and supervision
 */
object LEDManager {
  
  sealed trait Command
  case class RegisterController(name: String, controller: ActorRef[LEDControllerActor.Command]) extends Command
  case class BroadcastAudio(features: AudioFeatures) extends Command
  case class SetPatternAll(pattern: LEDPattern) extends Command
  case class SetBrightnessAll(brightness: Double) extends Command
  
  def apply(): Behavior[Command] = {
    Behaviors.setup { context =>
      active(Map.empty)
    }
  }
  
  private def active(controllers: Map[String, ActorRef[LEDControllerActor.Command]]): Behavior[Command] = {
    Behaviors.receiveMessage {
      case RegisterController(name, controller) =>
        active(controllers + (name -> controller))
      
      case BroadcastAudio(features) =>
        controllers.values.foreach { controller =>
          controller ! LEDControllerActor.UpdateFromAudio(features)
        }
        Behaviors.same
      
      case SetPatternAll(pattern) =>
        controllers.values.foreach { controller =>
          controller ! LEDControllerActor.SetPattern(pattern)
        }
        Behaviors.same
      
      case SetBrightnessAll(brightness) =>
        controllers.values.foreach { controller =>
          controller ! LEDControllerActor.SetGlobalBrightness(brightness)
        }
        Behaviors.same
    }
  }
}
