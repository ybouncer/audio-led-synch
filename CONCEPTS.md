# Mapping to Course Concepts (INFO M451)

This document explains how each part of the Audio-LED Sync system maps to specific concepts from the reactive programming course slides.

## Table of Contents

1. [Observables (Chapter 5)](#observables)
2. [Actors (Chapter 6)](#actors)
3. [Futures and Async (Chapter 4)](#futures)
4. [Monads (Chapter 3)](#monads)
5. [Functional Programming (Chapter 2)](#functional-programming)
6. [Reactive Principles (Chapter 1)](#reactive-principles)

---

## Observables (Chapter 5: From Futures to Observables)

### Concept from Slides

> **Observable[T]**: Asynchronous, push-based stream of multiple values
> - Demonstrates: map, flatMap, filter operations
> - Cold vs Hot observables
> - Subscription management

### Implementation in Our Code

#### Location: `AudioProcessor.scala`

```scala
def processAudioFile(filePath: String): Observable[AudioFeatures] = {
  Observable.fromTask(loadAudioFile(filePath))  // Convert Task to Observable
    .flatMap(audioData => processAudioData(audioData))  // Transform stream
    .onErrorHandle { ex =>  // Error handling
      AudioFeatures(/* ... */)
    }
}
```

**What's happening:**
1. **Observable.fromTask**: Creates Observable from async Task (Future-like)
2. **flatMap**: Transforms each audio buffer into a stream of features
3. **onErrorHandle**: Handles errors without breaking the stream

#### Another Example: Real-time Audio

```scala
def processRealtimeAudio(): Observable[AudioFeatures] = {
  Observable.create[Array[Byte]] { subscriber =>
    // Creates push-based stream from microphone
    // Demonstrates: Custom observable creation
  }
  .map(bytesToDoubles)           // Transform bytes to doubles
  .bufferTimedAndCounted(...)    // Buffer operator
  .filter(_.length >= bufferSize) // Filter operator
  .map(analyzeAudioBuffer)       // Final transformation
}
```

**Key Observable Operators Used:**

| Operator | Purpose | Slide Reference |
|----------|---------|----------------|
| `map` | Transform each value | Slide 81: map definition |
| `filter` | Select values based on predicate | Slide 82: filter definition |
| `flatMap` | Transform to stream and flatten | Slide 83: flatMap definition |
| `throttleLast` | Rate limiting | Slide 182: controlling emission rate |
| `interval` | Periodic emission | Slide 164: Observable.interval |
| `create` | Custom observable | Slide 162: Observable.create |

#### Test Audio Generator

```scala
def generateTestAudio(durationSeconds: Int): Observable[AudioFeatures] = {
  Observable.interval(1000.millis / updatesPerSecond)  // Timed stream
    .take(totalUpdates)                                // Limit count
    .map { tick =>                                     // Transform
      val samples = Array.tabulate(samplesPerUpdate) { i =>
        // Generate synthetic audio
      }
      analyzeAudioBuffer(samples)
    }
}
```

**Demonstrates:**
- Observable.interval (slide 164)
- take operator (slide 182)
- map transformation (slide 81)

---

## Actors (Chapter 6: Introduction to the Actor Model)

### Concept from Slides

> **Actor**: Encapsulated object with message-based communication
> - No shared mutable state
> - Asynchronous message passing with `!`
> - Behavior switching with `context.become`
> - Actor hierarchy and supervision

### Implementation in Our Code

#### Location: `LEDControllerActor.scala`

```scala
object LEDControllerActor {
  sealed trait Command
  case class UpdateFromAudio(features: AudioFeatures) extends Command
  case class SetPattern(pattern: LEDPattern) extends Command
  
  def apply(ledCount: Int): Behavior[Command] = {
    Behaviors.setup { context =>
      active(initialState)(context)  // Initial behavior
    }
  }
  
  private def active(state: State)(implicit context: ActorContext[Command]): Behavior[Command] = {
    Behaviors.receiveMessage {
      case UpdateFromAudio(features) =>
        val newStates = applyPattern(state.currentPattern, features, state.ledStates.size)
        active(newState)  // Behavior switching!
      
      case SetPattern(pattern) =>
        active(state.copy(currentPattern = pattern))
    }
  }
}
```

**Key Actor Concepts:**

| Concept | Implementation | Slide Reference |
|---------|---------------|----------------|
| Message Protocol | `sealed trait Command` | Slide 215: Actor messages |
| Message Passing | `actor ! message` | Slide 206: `!` operator |
| State Management | `active(state)` | Slide 210: behavior switching |
| Behavior Switching | `context.become(newBehavior)` | Slide 209: context.become |
| Actor Creation | `context.actorOf(...)` | Slide 211: actorOf |

#### Actor Hierarchy

```scala
object LEDManager {
  def apply(): Behavior[Command] = {
    active(Map.empty)  // Manages multiple child actors
  }
  
  private def active(controllers: Map[String, ActorRef[...]]): Behavior[Command] = {
    Behaviors.receiveMessage {
      case BroadcastAudio(features) =>
        controllers.values.foreach { controller =>
          controller ! LEDControllerActor.UpdateFromAudio(features)
        }
        Behaviors.same
    }
  }
}
```

**Demonstrates:**
- Actor hierarchy (parent manages children)
- Broadcasting messages
- Supervision pattern

#### Integration with Observables

```scala
// In main application
val subscription = audioSource
  .doOnNext { features =>
    system ! LEDManager.BroadcastAudio(features)  // Observable → Actor
  }
  .flatMap { features =>
    val futureState: Future[StateResponse] = 
      ledController.ask(LEDControllerActor.GetState.apply)  // Actor → Future
    Observable.fromFuture(futureState)  // Future → Observable
  }
```

**Demonstrates:**
- Bridging Observables and Actors (slide 228)
- Ask pattern (?) vs Tell (!)
- Converting Future to Observable

---

## Futures and Async (Chapter 4: Dealing with Latency and Failure)

### Concept from Slides

> **Future[T]**: Value that may become available at some point
> - Non-blocking asynchronous computation
> - Composable with map, flatMap
> - Error handling with recover, recoverWith

### Implementation in Our Code

#### Location: `AudioProcessor.scala`

```scala
private def loadAudioFile(filePath: String): Task[Array[Double]] = Task {
  // Async audio loading
  val audioInputStream = AudioSystem.getAudioInputStream(file)
  // ... processing ...
  bytesToDoubles(allBytes)
}
```

**Task is similar to Future:**
- Both represent async computation
- Both support map/flatMap
- Task is "lazy" (doesn't start until run)

#### Composition Example

```scala
// From MonadicCompositionExample.scala
def processAudioPipeline(filePath: String): Future[Vector[LEDState]] = {
  for {
    config <- Future.fromTry(loadConfig())      // Try → Future
    audioProcessor = AudioProcessor(config)     // Pure computation
    audioFeatures <- audioProcessor
      .processAudioFile(filePath)
      .headL                                    // Observable → Future
      .runToFuture
    ledStates = generateLEDStates(audioFeatures, config)
  } yield ledStates
}
```

**Demonstrates:**
- for-comprehension with Future (slide 134)
- Composing async operations
- Converting between monadic types

#### Error Handling

```scala
def processWithErrorHandling(): Future[AudioFeatures] = {
  audioProcessor.processFile(path)
    .recover {  // Handle errors
      case ex: FileNotFoundException =>
        AudioFeatures(/* default values */)
    }
    .recoverWith {  // Alternative computation
      case ex: IOException =>
        audioProcessor.generateTestAudio(10).headL.runToFuture
    }
}
```

**Maps to slides:**
- recover (slide 137-138)
- recoverWith (slide 139)
- fallbackTo pattern

---

## Monads (Chapter 3: Collections, Monads, Option and Try)

### Concept from Slides

> **Monad**: Parametric type M[T] with flatMap and unit
> - Enables for-comprehension
> - Composes computations
> - Examples: List, Option, Try, Future, Observable

### Implementation in Our Code

#### Option Monad

```scala
// In domain models
case class AudioFeatures(
  // ...
  tempo: Option[Double],  // May or may not have tempo
  // ...
)

// Usage with flatMap
def getTempoString(features: AudioFeatures): String = {
  features.tempo
    .map(t => f"$t%.1f BPM")  // Transform if present
    .getOrElse("No tempo detected")
}
```

**Demonstrates:**
- Option[T] monad (slide 104-106)
- Handling absence of value
- map on Option

#### Try Monad

```scala
// Network sending with error handling
def sendToHardware(states: Vector[LEDState]): Try[Unit] = Try {
  val socket = new DatagramSocket()
  // ... may throw exception ...
  socket.send(packet)
}

// Chaining operations
for {
  config <- Try(loadConfig())
  connection <- Try(openConnection(config))
  result <- Try(sendData(connection))
} yield result
```

**Demonstrates:**
- Try[T] monad (slide 110-113)
- Exception handling as value
- Chaining operations that may fail

#### List Monad

```scala
// Functional collection operations
val ledStates = Vector.tabulate(ledCount) { i =>
  LEDState(i, color, intensity)
}

// Composition
for {
  state <- ledStates         // Iterate
  if state.intensity > 0.5   // Filter
  adjusted = state.copy(intensity = state.intensity * 0.8)  // Transform
} yield adjusted
```

**Demonstrates:**
- List/Vector as monad (slide 79-83)
- for-comprehension translation
- map/flatMap/filter

#### Custom Monad Pattern

```scala
// LEDPattern as a monad-like structure
sealed trait LEDPattern
case class Custom(mapper: AudioFeatures => Vector[LEDState]) extends LEDPattern

// Composable patterns
def combinePatterns(p1: LEDPattern, p2: LEDPattern): LEDPattern = {
  Custom { features =>
    val states1 = applyPattern(p1, features, 10)
    val states2 = applyPattern(p2, features, 10)
    states1.zip(states2).map { case (s1, s2) =>
      LEDState(s1.id, s1.color.blend(s2.color, 0.5), 
               (s1.intensity + s2.intensity) / 2)
    }
  }
}
```

**Demonstrates:**
- Custom monadic structures
- Higher-order functions
- Composition patterns

---

## Functional Programming (Chapter 2: A Crash Course on Scala)

### Concept from Slides

> **Functional Programming**: Immutable data, pure functions, composition
> - val over var
> - Immutable collections
> - Pattern matching
> - Higher-order functions

### Implementation in Our Code

#### Immutability

```scala
// All case classes are immutable
case class Color(red: Int, green: Int, blue: Int) {
  // Operations return new instances
  def brightness(factor: Double): Color = {
    Color(
      (red * factor).toInt,
      (green * factor).toInt,
      (blue * factor).toInt
    )
  }
  
  def blend(other: Color, ratio: Double): Color = {
    Color(/* ... */)  // Returns new Color
  }
}
```

**No mutable state!**
- All fields are `val` (slide 27)
- Operations return new instances
- Thread-safe by design

#### Pattern Matching

```scala
def applyPattern(pattern: LEDPattern, features: AudioFeatures, ledCount: Int): Vector[LEDState] = {
  pattern match {  // Pattern matching (slide 69)
    case LEDPattern.Off =>
      Vector.fill(ledCount)(LEDState(0, Color.Black, 0.0))
    
    case LEDPattern.Spectrum(audioFeatures) =>
      Vector.tabulate(ledCount) { i =>
        val position = i.toDouble / ledCount
        val (color, intensity) = if (position < 0.33) {
          (Color.Red, audioFeatures.bassEnergy)
        } else if (position < 0.66) {
          (Color.Green, audioFeatures.midEnergy)
        } else {
          (Color.Blue, audioFeatures.highEnergy)
        }
        LEDState(i, color, intensity)
      }
    
    case LEDPattern.Custom(mapper) =>
      mapper(features)  // Higher-order function
  }
}
```

**Demonstrates:**
- Pattern matching on case classes (slide 67-69)
- Exhaustive matching
- Higher-order functions (slide 45-48)

#### Higher-Order Functions

```scala
// Function as parameter
def processWithTransform(data: Array[Double], 
                        transform: Double => Double): Array[Double] = {
  data.map(transform)
}

// Function as return value
def createColorMapper(style: String): (AudioFeatures => Color) = {
  style match {
    case "warm" => features => Color.Red.blend(Color.Yellow, features.energy)
    case "cool" => features => Color.Blue.blend(Color.Cyan, features.energy)
    case _ => features => Color.White
  }
}
```

**Demonstrates:**
- Functions as first-class values (slide 45)
- Closures (slide 49)
- Function composition

#### For-Comprehensions

```scala
// Translated to map/flatMap by compiler (slides 85-92)
for {
  i <- 1 until ledCount         // flatMap
  if i % 2 == 0                 // filter  
  state = LEDState(i, Color.Red, 1.0)  // map
} yield state
```

**Translates to:**
```scala
(1 until ledCount)
  .flatMap(i => if (i % 2 == 0) Some(i) else None)
  .map(i => LEDState(i, Color.Red, 1.0))
```

---

## Reactive Principles (Chapter 1: Introduction)

### Concept from Slides

> **Reactive Applications** are:
> 1. Event-driven (react to events)
> 2. Scalable (react to load)
> 3. Resilient (react to failure)
> 4. Responsive (react to users)

### Our Implementation

#### 1. Event-Driven

```scala
// Audio events drive LED updates
audioSource                           // Event stream
  .map(analyzeAudio)                 // Transform events
  .subscribe { features =>           // React to events
    ledController ! UpdateFromAudio(features)
  }
```

**Demonstrates:**
- Non-blocking event processing
- Push-based architecture
- Loose coupling

#### 2. Scalable

```scala
// Can easily add more LED controllers
for (i <- 1 to 100) {
  val controller = system.actorOf(
    LEDControllerActor(16),
    s"bracelet-$i"
  )
  manager ! RegisterController(s"bracelet-$i", controller)
}

// All controlled by same audio stream
audioSource.subscribe { features =>
  manager ! BroadcastAudio(features)  // Scales to all controllers
}
```

**Demonstrates:**
- Location transparency
- Elastic scaling
- Concurrent processing

#### 3. Resilient

```scala
// Error handling at multiple levels
audioSource
  .onErrorRestartUnlimited              // Restart on error
  .onErrorHandle { ex =>                // Handle errors gracefully
    logger.error("Audio error", ex)
    AudioFeatures(/* default */)
  }

// Actor supervision
val supervisor = Behaviors.supervise(ledController)
  .onFailure[Exception](SupervisorStrategy.restart)
```

**Demonstrates:**
- Failure as first-class concept
- Bulkhead pattern (isolated failures)
- Automatic recovery

#### 4. Responsive

```scala
// Fast response times
audioSource
  .throttleLast(50.millis)  // Max 20 updates/second
  .map(analyzeAudio)         // O(1) or O(log n) operations
  .subscribe(updateLEDs)     // Non-blocking updates
```

**Demonstrates:**
- Predictable latency
- Rate limiting
- Non-blocking design

---

## Complete Flow Example

Let's trace a single audio frame through the entire system:

### 1. Audio Acquisition (Observable)

```scala
// Microphone produces bytes
Observable.create[Array[Byte]] { subscriber =>
  // Async read from microphone
  subscriber.onNext(audioBytes)
}
```

**Concept**: Observable creation (slide 162)

### 2. Transformation (Observable + Functional)

```scala
.map(bytesToDoubles)              // Transform bytes → doubles
.map(applyHannWindow)             // Apply window function
.map(performFFT)                  // FFT transform
.map(calculateFeatures)           // Extract features
```

**Concept**: map operator (slide 81), functional composition

### 3. Processing (Observable + Monad)

```scala
.flatMap { features =>
  // Get LED state (Actor interaction)
  val futureState = ledController.ask(GetState.apply)
  Observable.fromFuture(futureState)
    .map(state => (features, state))
}
```

**Concept**: flatMap (slide 83), Future integration (slide 228)

### 4. Actor Update (Actor Model)

```scala
.doOnNext { case (features, state) =>
  // Send message to actor
  ledController ! UpdateFromAudio(features)
}
```

**Concept**: Message passing (slide 206)

### 5. State Update (Actor + Functional)

```scala
// Inside actor
case UpdateFromAudio(features) =>
  val newStates = applyPattern(currentPattern, features, ledCount)
  context.become(active(state.copy(ledStates = newStates)))
```

**Concept**: Behavior switching (slide 209), immutable updates

### 6. Visualization (Side Effect)

```scala
.doOnNext { case (features, states) =>
  visualizer.update(states, features)
}
```

**Concept**: Side effects in reactive streams

---

## Summary Table

| Course Chapter | Concept | Our Implementation | File Location |
|---------------|---------|-------------------|---------------|
| Chapter 1 | Reactive Principles | Event-driven audio processing | AudioLEDSyncApp.scala |
| Chapter 2 | Functional Programming | Immutable data, pattern matching | Models.scala |
| Chapter 2 | Higher-order functions | Custom pattern mappers | LEDControllerActor.scala |
| Chapter 3 | Monads (Option/Try) | Error handling, optional values | Throughout |
| Chapter 3 | For-comprehensions | Async composition | MonadicCompositionExample |
| Chapter 4 | Future[T] | Async audio loading | AudioProcessor.scala |
| Chapter 4 | Error handling | recover/recoverWith | AudioProcessor.scala |
| Chapter 5 | Observable[T] | Audio stream processing | AudioProcessor.scala |
| Chapter 5 | Operators (map/filter) | Stream transformations | AudioProcessor.scala |
| Chapter 6 | Actor model | LED controllers | LEDControllerActor.scala |
| Chapter 6 | Message passing | LED updates | AudioLEDSyncApp.scala |
| Chapter 6 | Behavior switching | Pattern changes | LEDControllerActor.scala |

---

## Key Takeaways

1. **Observables** handle continuous audio stream with backpressure
2. **Actors** manage concurrent LED state without locks
3. **Futures** enable async file operations
4. **Monads** provide compositional error handling
5. **Functional** design ensures thread-safety
6. **Reactive** principles enable scalability

Every major concept from the course is demonstrated in a practical, working system!
