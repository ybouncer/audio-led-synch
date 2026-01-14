# Audio-LED Synchronization System

A Scala-based reactive system for synchronizing LEDs with audio input, demonstrating advanced concepts from reactive programming.

## 🎯 Project Overview

This project processes audio in real-time and synchronizes LED patterns with musical features (rhythm, bass, mids, highs, beats). It showcases concepts from the course slides on reactive programming, including:

- **Observables** (asynchronous streams of audio data)
- **Actors** (concurrent LED controllers)
- **Futures/Tasks** (async audio processing)
- **Monads** (Try, Option, Future for effect handling)
- **For-comprehensions** (composing async operations)

## 📚 Key Concepts Demonstrated

### 1. Observable Streams (Chapter 5: From Futures to Observables)

```scala
// Audio processing as an Observable stream
val audioSource: Observable[AudioFeatures] = 
  audioProcessor.processAudioFile("song.wav")
    .map(analyzeAudio)           // Transform
    .filter(_.energy > 0.1)      // Filter
    .throttleLast(50.millis)     // Control rate
```

**From slides**: "Observable[T] implements an observable stream where values are pushed to registered observers"

### 2. Actor Model (Chapter 6: Introduction to the Actor Model)

```scala
// LED Controller as an Actor
class LEDControllerActor {
  def receive: Receive = {
    case UpdateFromAudio(features) =>
      // Update LED states
      context.become(newState)
  }
}
```

**From slides**: "Actors are completely encapsulated objects - only interaction through messages"

### 3. Futures for Async Operations (Chapter 4: Dealing with Latency and Failure)

```scala
// Async audio file loading
def loadAudioFile(filePath: String): Future[Array[Double]] = Future {
  // Load audio data asynchronously
}
```

**From slides**: "Future[T] object holds a value that may become available at some point"

### 4. Monadic Composition (Chapter 3: Collections, Monads, Option and Try)

```scala
// Composing operations with for-comprehension
for {
  config <- loadConfig()
  audio <- processAudio(config)
  features <- analyzeAudio(audio)
} yield generateLEDPattern(features)
```

**From slides**: "Monads allow to build computations by composing operations in a useful way"

## 🏗️ Architecture

```
┌─────────────────┐
│  Audio Source   │ (File/Microphone/Test)
└────────┬────────┘
         │
         v
┌─────────────────┐
│ AudioProcessor  │ Observable[AudioFeatures]
│   (FFT, Beat    │ (map, flatMap, filter)
│    Detection)   │
└────────┬────────┘
         │
         v
┌─────────────────┐
│  LED Manager    │ Actor System
│    (Actor)      │ (message passing)
└────────┬────────┘
         │
         v
┌─────────────────┐
│ LED Controller  │ State management
│    (Actor)      │ (behavior switching)
└────────┬────────┘
         │
         v
┌─────────────────┬──────────────────┐
│   Console UI    │     GUI UI       │
│  (Terminal)     │   (Swing)        │
└─────────────────┴──────────────────┘
```

## 🚀 Running the Application

### Prerequisites

```bash
# Scala 2.13.x and sbt must be installed
sbt --version
```

### Quick Start

```bash
cd audio-led-sync

# Run with test audio (synthesized)
sbt "run --test 30"

# Run with audio file
sbt "run --file /path/to/audio.wav"

# Run with microphone input
sbt "run --realtime"
```

### Modes

1. **Test Mode** (Default): Generates synthetic audio with varying frequencies
   ```bash
   sbt "run --test 60"  # 60 seconds
   ```

2. **File Mode**: Process an audio file
   ```bash
   sbt "run --file song.wav"
   ```

3. **Realtime Mode**: Use microphone
   ```bash
   sbt "run --realtime"
   ```

## 🎨 LED Patterns

### Available Patterns

1. **Spectrum**: Maps frequency bands to LED positions
   - Bass (Red) → Low LEDs
   - Mids (Green) → Middle LEDs
   - Highs (Blue) → High LEDs

2. **Energy**: Shows overall audio energy
   - Color transitions based on spectral centroid
   - Intensity based on total energy

3. **Beat**: Flashes on beat detection
   - White flash on beats
   - Color decay between beats

4. **Wave**: Creates wave animation
   - Wave frequency based on audio energy
   - Colors blend across frequency spectrum

5. **Custom**: Define your own pattern!
   ```scala
   LEDPattern.Custom { features =>
     Vector.tabulate(10) { i =>
       // Your custom logic here
       LEDState(i, myColor, myIntensity)
     }
   }
   ```

## 🎵 Audio Analysis Features

The system extracts these features from audio:

- **Energy**: Overall loudness/amplitude
- **Bass Energy**: Low frequency content (20-250 Hz)
- **Mid Energy**: Mid frequency content (250-2000 Hz)
- **High Energy**: High frequency content (2000-8000 Hz)
- **Spectral Centroid**: "Brightness" of sound
- **Beat Detection**: Rhythmic pulses using energy flux
- **Waveform**: Raw audio samples for visualization

## 📝 Code Structure

```
src/main/scala/com/audioled/
├── domain/
│   └── Models.scala              # Data models (Color, LEDState, AudioFeatures)
├── audio/
│   └── AudioProcessor.scala      # Audio analysis with Observables
├── led/
│   └── LEDControllerActor.scala  # Actor-based LED control
├── visualization/
│   ├── ConsoleVisualizer.scala   # Terminal UI
│   └── GUIVisualizer.scala       # Swing GUI
└── AudioLEDSyncApp.scala         # Main application
```

## 🔧 Configuration

Edit `src/main/resources/application.conf`:

```hocon
audio-led-sync {
  audio {
    sample-rate = 44100      # Audio sample rate
    buffer-size = 2048       # Processing buffer size
    fft-size = 2048          # FFT size for frequency analysis
  }
  
  leds {
    count = 10               # Number of LEDs
    update-rate-ms = 50      # LED update rate (20 FPS)
  }
  
  simulation {
    enable-console = true    # Enable terminal visualization
    enable-gui = true        # Enable GUI
    enable-network = false   # Send to real hardware
  }
}
```

## 🔌 Hardware Integration

To connect to real hardware (as mentioned in your project):

### 1. Enable Network Mode

```hocon
simulation {
  enable-network = true
  network {
    raspberry-pi-host = "192.168.1.100"
    raspberry-pi-port = 5683
    protocol = "coap"
  }
}
```

### 2. Implement Hardware Bridge

```scala
// In NetworkLEDController.scala
def sendToHardware(states: Vector[LEDState], host: String, port: Int): Unit = {
  // Implement CoAP protocol
  // Send LED commands to Raspberry Pi
  // Format: LED_ID:R,G,B;LED_ID:R,G,B;...
}
```

### 3. Raspberry Pi Setup

On your Raspberry Pi running the backend:

```scala
// Receive LED commands
def receiveLEDCommand(): Unit = {
  // Parse incoming CoAP messages
  // Update ESP32 via WiFi
  // Control physical LEDs
}
```

## 📊 Visualizations

### Console Mode

```
================================================================================
 Audio-LED Synchronization System 
================================================================================

Audio Features:
  Energy:   ████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ 68%
  Bass:     ███████████████████████████████░░░░░░░░░░░░░░░░░░░ 85%
  Mid:      █████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ 52%
  High:     ████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ 31%
  Beat:     💥 BEAT!
  Centroid: 1234 Hz

LED States:
┌────────────────────────────────────────────────────────────┐
│████████▓▓▓▓▓▓▓▓▒▒▒▒▒▒▒▒░░░░░░░░                          │
└────────────────────────────────────────────────────────────┘

LED Details:
 0:🔴 85%   1:🔴 80%   2:🔴 75%   3:🟢 52%   4:🟢 48%
 5:🟢 45%   6:🔵 31%   7:🔵 28%   8:🔵 25%   9:🔵 20%
```

### GUI Mode

The GUI shows:
- Real-time LED visualization with glow effects
- Frequency band bars (Bass/Mid/High)
- Beat indicator
- Waveform display
- Control panel for pattern selection and brightness

## 🎓 Learning Points

### 1. Reactive Streams (Observable)

**Problem**: Traditional audio processing blocks the main thread.

**Solution**: Use Observable to create push-based asynchronous stream:

```scala
Observable.interval(50.millis)      // Create timed stream
  .map(analyzeAudio)                // Transform
  .filter(_.energy > 0.1)           // Filter
  .subscribe(updateLEDs)            // React
```

### 2. Actor Model

**Problem**: Managing concurrent state updates to LEDs.

**Solution**: Use Actor with message passing:

```scala
// No shared mutable state!
ledActor ! UpdateLEDs(newStates)

// Actor processes messages sequentially
def receive = {
  case UpdateLEDs(states) =>
    this.state = states
    context.become(newBehavior)
}
```

### 3. Monadic Error Handling

**Problem**: Audio processing can fail (file not found, device error).

**Solution**: Use Try/Option/Future monads:

```scala
for {
  config <- Try(loadConfig())           // May fail
  audio <- Future(loadAudio(path))      // Async
  features <- Future(analyze(audio))    // Async
} yield features
```

### 4. Compositional Design

**Problem**: Need flexible LED patterns.

**Solution**: Pattern as first-class function:

```scala
trait LEDPattern
case class Custom(mapper: AudioFeatures => Vector[LEDState])

// User defines custom patterns
val firePattern = Custom { features =>
  // Custom logic
}
```

## 🧪 Testing

```bash
# Run with test audio to verify everything works
sbt "run --test 10"

# Check console output for:
# - Audio features being calculated
# - LED states updating
# - Beat detection working
# - No errors in processing
```

## 🔗 Connecting to Your Project

This code integrates with your "BraceLEDS connectés" project:

1. **Bracelets** = Individual LED actors (one per bracelet)
2. **Raspberry Pi** = LEDManager actor (central coordination)
3. **WiFi/CoAP** = Message passing between actors
4. **Audio Processing** = Microphone + AudioProcessor Observable
5. **Visualization** = The giant screen (projection)

### Integration Example

```scala
// Create one actor per bracelet
val bracelet1 = system.actorOf(LEDControllerActor(16), "bracelet-1")
val bracelet2 = system.actorOf(LEDControllerActor(16), "bracelet-2")

// Manager broadcasts to all bracelets
manager ! RegisterController("bracelet-1", bracelet1)
manager ! RegisterController("bracelet-2", bracelet2)

// Audio stream updates all bracelets
audioStream.subscribe { features =>
  manager ! BroadcastAudio(features)
}
```

## 📖 Further Reading

- Course slides: Chapters 4, 5, 6
- Monix documentation: https://monix.io
- Akka documentation: https://doc.akka.io
- Scala reactive programming: https://www.reactive-streams.org

## 🐛 Troubleshooting

**Audio not loading?**
- Check file format (WAV, 16-bit PCM preferred)
- Try test mode first: `sbt "run --test"`

**GUI not appearing?**
- Set `enable-gui = true` in config
- Check X11/display settings

**Microphone not working?**
- Check microphone permissions
- Try test mode to verify system works

**Actors not responding?**
- Check Akka logs
- Verify message types match

## 💡 Tips for Colleagues

1. **Start Simple**: Run test mode first
2. **Read the Slides**: Each concept maps to specific slides
3. **Experiment**: Try different patterns
4. **Extend**: Add your own custom patterns
5. **Integrate**: Connect to real hardware when ready

## 🎉 Demo Script

For presentation:

```bash
# 1. Show test audio with spectrum pattern
sbt "run --test 30"

# 2. Switch to energy pattern via GUI

# 3. Explain reactive concepts while running

# 4. Show code structure

# 5. Demo custom pattern creation
```

## 📄 License

Educational project for course INFO M453 - UNamur

---

**Questions?** Contact your teammates or course instructors.
**Issues?** Check the troubleshooting section above.
**Want more?** Extend the patterns or add new visualizations!
