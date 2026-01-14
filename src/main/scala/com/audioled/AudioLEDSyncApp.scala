package com.audioled

import akka.actor.typed.ActorSystem
import com.audioled.audio.AudioProcessor
import com.audioled.domain._
import com.audioled.led.{LEDControllerActor, LEDManager}
import com.audioled.visualization._
import com.typesafe.config.ConfigFactory
import monix.reactive.Observable
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Try, Success, Failure}
import com.typesafe.scalalogging.LazyLogging

/**
 * Main Application
 * Demonstrates: Integration of all reactive programming concepts
 * - Observables for audio stream
 * - Actors for LED control
 * - Futures for async operations
 * - Monadic composition with for-comprehensions
 */
object AudioLEDSyncApp extends App with LazyLogging {
  
  logger.info("Starting Audio-LED Synchronization System")
  
  // Load configuration
  val config = ConfigFactory.load()
  val audioLEDConfig = AudioLEDConfig(
    sampleRate = config.getInt("audio-led-sync.audio.sample-rate"),
    bufferSize = config.getInt("audio-led-sync.audio.buffer-size"),
    fftSize = config.getInt("audio-led-sync.audio.fft-size"),
    ledCount = config.getInt("audio-led-sync.leds.count"),
    updateRateMs = config.getInt("audio-led-sync.leds.update-rate-ms")
  )
  
  // Initialize components
  val audioProcessor = AudioProcessor(audioLEDConfig)
  val enableConsole = config.getBoolean("audio-led-sync.simulation.enable-console")
  val enableGUI = config.getBoolean("audio-led-sync.simulation.enable-gui")
  
  // Console visualizer
  val consoleVisualizer = if (enableConsole) {
    Some(new ConsoleVisualizer(audioLEDConfig.ledCount))
  } else None
  
  // GUI visualizer
  val guiVisualizer: Option[(GUIVisualizer, ControlPanel)] = if (enableGUI) {
    Try {
      val gui = new GUIVisualizer(audioLEDConfig.ledCount)
      val controlPanel = new ControlPanel()
      
      // Wire up control panel callbacks (will be connected to actor system)
      gui.visible = true
      controlPanel.visible = true
      
      (gui, controlPanel)
    }.toOption
  } else None
  
  // Initialize Actor System
  val system: ActorSystem[LEDManager.Command] = ActorSystem(
    LEDManager(),
    "led-manager-system"
  )
  
  // Create LED controller actor
  val ledController = system.systemActorOf(
    LEDControllerActor(audioLEDConfig.ledCount),
    "led-controller"
  )
  
  // Register controller with manager
  system ! LEDManager.RegisterController("main", ledController)
  
  // Set initial pattern
  ledController ! LEDControllerActor.SetPattern(LEDPattern.Spectrum(
    AudioFeatures(0, 0, 0, 0, 0, 0, isBeat = false, None, Array.empty)
  ))
  
  // Wire up GUI control panel to actor system
  guiVisualizer.foreach { case (gui, controlPanel) =>
    controlPanel.setPatternCallback { pattern =>
      ledController ! LEDControllerActor.SetPattern(pattern)
    }
    
    controlPanel.setBrightnessCallback { brightness =>
      ledController ! LEDControllerActor.SetGlobalBrightness(brightness)
    }
  }
  
  // Choose audio source based on command line args
  val audioSource: Observable[AudioFeatures] = args.headOption match {
    case Some("--file") if args.length > 1 =>
      logger.info(s"Processing audio file: ${args(1)}")
      audioProcessor.processAudioFile(args(1))
    
    case Some("--realtime") =>
      logger.info("Processing realtime microphone input")
      audioProcessor.processRealtimeAudio()
    
    case Some("--test") =>
      val duration = args.lift(1).flatMap(s => Try(s.toInt).toOption).getOrElse(30)
      logger.info(s"Generating test audio for $duration seconds")
      audioProcessor.generateTestAudio(duration)
    
    case _ =>
      logger.info("No audio source specified, generating test audio (30 seconds)")
      logger.info("Usage:")
      logger.info("  --file <path>     Process audio file")
      logger.info("  --realtime        Use microphone")
      logger.info("  --test [duration] Generate test audio")
      audioProcessor.generateTestAudio(30)
  }
  
  // Main reactive pipeline
  // Demonstrates: Observable composition with map, flatMap, and effects
  val subscription = audioSource
    .throttleLast(audioLEDConfig.updateRateMs.millis) // Limit update rate
    .map { features =>
      // Update actor system
      system ! LEDManager.BroadcastAudio(features)
      features
    }
    .delayExecution(100.millis) // Give actors time to process
    .mapEval { features =>
      // Query actor for current LED state
      // Demonstrates: Bridging between Observables and Actors using Future
      import akka.actor.typed.scaladsl.AskPattern._
      import akka.util.Timeout
      import scala.concurrent.ExecutionContext.Implicits.global
      implicit val timeout: Timeout = 100.millis
      implicit val scheduler = system.scheduler
      
      val futureState: Future[LEDControllerActor.StateResponse] = 
        ledController.ask(LEDControllerActor.GetState.apply)
      
      // Convert Future to Task
      import monix.eval.Task
      Task.fromFuture(futureState).map { response =>
        (features, response.states)
      }.onErrorHandle { ex =>
        logger.error(s"Error getting LED state: ${ex.getMessage}")
        (features, Vector.empty[LEDState])
      }
    }
    .map { case (features, states) =>
      // Update visualizations
      consoleVisualizer.foreach(_.visualize(states, features))
      guiVisualizer.foreach { case (gui, _) => 
        gui.update(states, features)
      }
      (features, states)
    }
    .doOnComplete(monix.eval.Task.eval(logger.info("Audio stream completed")))
    .doOnError { ex =>
      monix.eval.Task.eval(logger.error(s"Error in audio stream: ${ex.getMessage}", ex))
    }
    .subscribe()
  
  // Shutdown hook
  sys.addShutdownHook {
    logger.info("Shutting down...")
    subscription.cancel()
    system.terminate()
    Await.result(system.whenTerminated, 5.seconds)
    logger.info("Shutdown complete")
  }
  
  // Keep application running
  if (guiVisualizer.isEmpty && consoleVisualizer.isEmpty) {
    logger.info("Press Ctrl+C to stop")
    Thread.currentThread().join()
  } else if (guiVisualizer.nonEmpty) {
    // GUI will keep app running
    logger.info("GUI mode - close windows to exit")
  } else {
    // Console mode
    logger.info("Console mode - press Ctrl+C to exit")
    Thread.currentThread().join()
  }
}

/**
 * Advanced example showing custom pattern creation
 * Demonstrates: Higher-order functions and composability
 */
object CustomPatternExample extends App {
  
  /**
   * Creates a custom LED pattern using a user-defined function
   * This shows how the system is extensible
   */
  def createReactivePattern(): LEDPattern = {
    LEDPattern.Custom { features =>
      val normalized = features.normalized
      
      // Custom logic: Create a "fire" effect based on bass energy
      Vector.tabulate(10) { i =>
        val position = i / 10.0
        val flicker = math.random() * 0.3
        val intensity = (normalized.bassEnergy + flicker).min(1.0)
        
        // Color transitions from red to yellow based on intensity
        val color = if (intensity > 0.7) {
          Color.Red.blend(Color.Yellow, (intensity - 0.7) / 0.3)
        } else {
          Color.Red
        }
        
        LEDState(i, color, intensity * (1.0 - position * 0.3))
      }
    }
  }
  
  println("Custom pattern created!")
  println("This pattern creates a 'fire' effect that reacts to bass frequencies")
}

/**
 * Example showing how to create a network-enabled LED controller
 * For integration with real hardware (Raspberry Pi, ESP32)
 */
object NetworkLEDController extends LazyLogging {
  
  import java.net._
  import java.nio.charset.StandardCharsets
  
  /**
   * Sends LED commands over network (UDP example)
   * In real implementation, use CoAP as mentioned in project docs
   */
  def sendToHardware(states: Vector[LEDState], host: String, port: Int): Try[Unit] = Try {
    val socket = new DatagramSocket()
    
    // Create simple protocol: JSON-like format
    val message = states.map { led =>
      s"${led.id}:${led.actualColor.red},${led.actualColor.green},${led.actualColor.blue}"
    }.mkString(";")
    
    val data = message.getBytes(StandardCharsets.UTF_8)
    val packet = new DatagramPacket(data, data.length, InetAddress.getByName(host), port)
    
    socket.send(packet)
    socket.close()
    
    logger.debug(s"Sent LED data to $host:$port")
  }
  
  /**
   * Creates an Observable effect that sends LED states to hardware
   * Demonstrates: side effects in reactive streams
   */
  def createHardwareSync(host: String, port: Int): ((Vector[LEDState]) => Unit) = {
    states =>
      sendToHardware(states, host, port) match {
        case Success(_) => ()
        case Failure(ex) => logger.error(s"Failed to send to hardware: ${ex.getMessage}")
      }
  }
}

/**
 * Example showing monadic composition patterns
 * Demonstrates: for-comprehensions with Try, Option, Future
 */
object MonadicCompositionExample {
  
  import scala.concurrent.Future
  import scala.concurrent.ExecutionContext.Implicits.global
  
  /**
   * Combines multiple async operations using for-comprehension
   * Demonstrates: monadic composition from slides
   */
  def processAudioPipeline(filePath: String): Future[Vector[LEDState]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    import monix.execution.Scheduler.Implicits.{global => monixGlobal}
    
    for {
      // Step 1: Load configuration (Try monad)
      config <- Future.fromTry(loadConfig())
      
      // Step 2: Process audio (Future monad)
      audioProcessor = AudioProcessor(config)
      
      // Step 3: Get first audio frame
      audioFeatures <- audioProcessor
        .processAudioFile(filePath)
        .headL  // Get first element
        .runToFuture(monixGlobal)
      
      // Step 4: Generate LED states
      ledStates = generateLEDStates(audioFeatures, config)
      
    } yield ledStates
  }
  
  private def loadConfig(): Try[AudioLEDConfig] = Try {
    AudioLEDConfig(
      sampleRate = 44100,
      bufferSize = 2048,
      fftSize = 2048,
      ledCount = 10,
      updateRateMs = 50
    )
  }
  
  private def generateLEDStates(features: AudioFeatures, 
                                config: AudioLEDConfig): Vector[LEDState] = {
    Vector.tabulate(config.ledCount) { i =>
      LEDState(i, Color.Red, features.energy)
    }
  }
}
