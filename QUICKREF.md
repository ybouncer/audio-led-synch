# Quick Reference Guide

## Fast Start

```bash
cd audio-led-sync
sbt "run --test 30"
```

## Command Line Options

```bash
# Test mode (synthetic audio)
sbt "run --test 60"

# File mode
sbt "run --file /path/to/audio.wav"

# Realtime (microphone)
sbt "run --realtime"

# Or use the script
./run.sh test 60
./run.sh file song.wav
./run.sh realtime
```

## Key Files

```
audio-led-sync/
├── build.sbt                          # Dependencies
├── src/main/
│   ├── resources/
│   │   └── application.conf           # Configuration
│   └── scala/com/audioled/
│       ├── AudioLEDSyncApp.scala      # Main app
│       ├── domain/Models.scala        # Data models
│       ├── audio/
│       │   └── AudioProcessor.scala   # Observable streams
│       ├── led/
│       │   └── LEDControllerActor.scala  # Actor system
│       └── visualization/
│           ├── ConsoleVisualizer.scala
│           └── GUIVisualizer.scala
└── README.md                          # Full documentation
```

## Code Snippets

### Create Custom LED Pattern

```scala
val customPattern = LEDPattern.Custom { features =>
  Vector.tabulate(10) { i =>
    val color = if (features.isBeat) Color.White else Color.Red
    val intensity = features.energy
    LEDState(i, color, intensity)
  }
}

// Apply pattern
ledController ! LEDControllerActor.SetPattern(customPattern)
```

### Process Audio File

```scala
val audioProcessor = AudioProcessor(config)
val audioStream = audioProcessor.processAudioFile("song.wav")

audioStream
  .map(_.normalized)
  .filter(_.energy > 0.1)
  .subscribe { features =>
    println(s"Energy: ${features.energy}")
  }
```

### Create LED Actor

```scala
val system = ActorSystem(LEDManager(), "system")
val ledActor = system.systemActorOf(
  LEDControllerActor(10),
  "leds"
)

// Send message
ledActor ! LEDControllerActor.UpdateFromAudio(features)
```

## Configuration Quick Edit

Edit `src/main/resources/application.conf`:

```hocon
audio-led-sync {
  leds {
    count = 20              # Change LED count
    update-rate-ms = 33     # ~30 FPS
  }
  
  simulation {
    enable-console = true   # Terminal display
    enable-gui = true       # GUI window
  }
}
```

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "sbt not found" | Install sbt: `brew install sbt` (Mac) or visit scala-sbt.org |
| "Java heap space" | Add to sbt: `-J-Xmx2G` |
| GUI not showing | Set `enable-gui = true` in config |
| Audio file error | Use WAV format, 16-bit PCM |
| No microphone | Use test mode: `sbt "run --test"` |

## Key Concepts

| Concept | Code Location | Purpose |
|---------|--------------|---------|
| Observable | AudioProcessor.scala:47 | Audio stream |
| Actor | LEDControllerActor.scala:31 | LED control |
| Future | AudioProcessor.scala:105 | Async loading |
| Pattern Matching | LEDControllerActor.scala:108 | Pattern selection |
| For-comprehension | AudioLEDSyncApp.scala:215 | Composition |

## Observable Operators

```scala
audioStream
  .map(f)                  // Transform
  .filter(p)               // Select
  .flatMap(f)              // Transform to stream
  .throttleLast(50.millis) // Rate limit
  .take(100)               // Limit count
  .doOnNext(f)             // Side effect
  .subscribe(observer)     // Start
```

## Actor Messages

```scala
// Update from audio
ledController ! UpdateFromAudio(features)

// Change pattern
ledController ! SetPattern(LEDPattern.Spectrum(...))

// Change brightness
ledController ! SetGlobalBrightness(0.8)

// Query state (ask pattern)
val future = ledController.ask(GetState.apply)
```

## LED Patterns

```scala
LEDPattern.Off                    // All LEDs off
LEDPattern.Solid(Color.Red)       // Solid color
LEDPattern.Spectrum(features)     // Frequency spectrum
LEDPattern.Energy(features)       // Overall energy
LEDPattern.Beat(features)         // Beat detection
LEDPattern.Wave(features)         // Wave animation
LEDPattern.Custom(mapper)         // Your function
```

## Visualization

### Console
- Automatic if `enable-console = true`
- Shows: bars, beat indicator, LED states

### GUI
- Automatic if `enable-gui = true`
- Shows: visual LEDs, spectrum, waveform
- Control panel for pattern/brightness

## Integration with Hardware

### Enable Network Mode

```hocon
simulation {
  enable-network = true
  network {
    raspberry-pi-host = "192.168.1.100"
    raspberry-pi-port = 5683
  }
}
```

### Send to Hardware

```scala
NetworkLEDController.sendToHardware(
  ledStates,
  "192.168.1.100",
  5683
)
```

## Testing

```bash
# Compile only
sbt compile

# Run tests (if you add them)
sbt test

# Package
sbt package

# Clean
sbt clean
```

## Common Modifications

### Change LED Count

1. Edit config: `leds.count = 20`
2. Or in code: `AudioLEDConfig(..., ledCount = 20, ...)`

### Change Update Rate

1. Edit config: `leds.update-rate-ms = 33`  # ~30 FPS
2. Or: `throttleLast(33.millis)` in stream

### Add New Pattern

```scala
// In LEDControllerActor.scala
case LEDPattern.MyPattern(audioFeatures) =>
  Vector.tabulate(ledCount) { i =>
    // Your logic here
    LEDState(i, myColor, myIntensity)
  }
```

### Change Audio Analysis

```scala
// In AudioProcessor.scala
private def analyzeAudioBuffer(samples: Array[Double]): AudioFeatures = {
  // Modify FFT parameters
  // Add new feature extraction
  // Change beat detection algorithm
}
```

## Performance Tips

1. **Reduce update rate** if too slow: `throttleLast(100.millis)`
2. **Decrease FFT size** for faster processing: `fft-size = 1024`
3. **Limit LED count** for testing: `count = 5`
4. **Disable GUI** if not needed: `enable-gui = false`

## Demo Script

```bash
# 1. Start with test audio
sbt "run --test 30"

# 2. Observe console output:
#    - Audio features updating
#    - LED states changing
#    - Beat detection working

# 3. If GUI enabled:
#    - Watch visual LEDs
#    - See spectrum bars
#    - Observe waveform

# 4. Try different patterns via GUI control panel

# 5. Show code structure
# 6. Explain reactive concepts
# 7. Demo custom pattern creation
```

## Resources

- Full README: `README.md`
- Concept mapping: `CONCEPTS.md`
- Course slides: `Slides.pdf`
- Configuration: `src/main/resources/application.conf`

## Getting Help

1. Check README.md for detailed explanations
2. Look at CONCEPTS.md for theory
3. Review course slides for reactive concepts
4. Check console output for errors
5. Enable debug logging in config

## Quick Debug

```bash
# Check if sbt works
sbt --version

# Check Scala version
scala -version

# Clean and rebuild
sbt clean compile

# Run with debug
sbt -Dakka.loglevel=DEBUG run
```

---

**Remember**: This is a reactive system! Everything is:
- Non-blocking (Observables, Actors, Futures)
- Composable (map, flatMap, for-comprehensions)
- Resilient (error handling throughout)
- Concurrent (Actors for isolation)

Start simple, experiment, and have fun! 🎵✨
