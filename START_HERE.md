# Audio-LED Synchronization System - START HERE

Audio-LED synchronization.

## Quick Start

1. **Navigate to project:**
   ```bash
   cd audio-led-sync
   ```

2. **Run the demo:**
   ```bash
   sbt "run --test 30"
   ```

3. **Watch the magic happen!** 

## Documentation Guide

### For First-Time Users
**Start here:** `README.md`
- What the system does
- How to run it
- Architecture overview
- Configuration options

### For Understanding Concepts
**Read:** `CONCEPTS.md`
- Maps code to course slides
- Explains every reactive concept
- Shows implementation details
- Perfect for learning

### For Quick Reference
**Use:** `QUICKREF.md`
- Common commands
- Code snippets
- Configuration tips
- Troubleshooting

### For Presentation/Demo
**Follow:** `PRESENTATION.md`
- Complete demo script
- Talking points
- Q&A preparation
- Success criteria

### For Project Overview
**See:** `PROJECT_SUMMARY.md`
- High-level overview
- What's been built
- How it integrates
- Next steps

## File Structure

```
audio-led-sync/
│
├── Documentation/
│   ├── README.md              ← Start here!
│   ├── CONCEPTS.md            ← Course concept mapping
│   ├── QUICKREF.md            ← Quick reference
│   ├── PRESENTATION.md        ← Demo guide
│   └── PROJECT_SUMMARY.md     ← This file
│
├── Configuration/
│   ├── build.sbt              ← Dependencies
│   ├── run.sh                 ← Convenience script
│   └── src/main/resources/
│       └── application.conf   ← System configuration
│
└── Source Code/
    └── src/main/scala/com/audioled/
        ├── AudioLEDSyncApp.scala              ← Main application
        ├── domain/Models.scala                ← Data models
        ├── audio/AudioProcessor.scala         ← Observable streams
        ├── led/LEDControllerActor.scala       ← Actor system
        └── visualization/
            ├── ConsoleVisualizer.scala        ← Terminal UI
            └── GUIVisualizer.scala            ← Swing GUI
```

## Common Tasks

### Run the System

```bash
# Test mode (recommended first)
sbt "run --test 30"

# With audio file
sbt "run --file song.wav"

# With microphone
sbt "run --realtime"

# Using convenience script
./run.sh test 30
```

### Understand a Concept

1. Look up concept in `CONCEPTS.md` table of contents
2. Find relevant code section
3. See explanation with slide references
4. Look at actual code file


### Modify the System

1. Check `QUICKREF.md` for common modifications
2. Look at relevant source file
3. Follow patterns in existing code
4. Test your changes


## System Check

Before doing anything, verify your environment:

```bash
# Check Scala/sbt
sbt --version
# Should show: sbt 1.9+ and Scala 2.13+

# Check Java
java -version
# Should show: Java 8 or higher

# Compile project
cd audio-led-sync
sbt compile
# Should complete without errors
```

If any checks fail, see README.md installation section.

## Troubleshooting

### "sbt not found"
- Install sbt: https://www.scala-sbt.org/download.html
- Mac: `brew install sbt`
- Linux: Use package manager

### "Java version error"
- Install Java 8 or higher
- Mac: `brew install openjdk@11`
- Set JAVA_HOME if needed

### "Compilation errors"
- Run `sbt clean compile`
- Check internet connection (downloads dependencies)
- See README.md troubleshooting section

### Demo not working
- Try test mode first: `sbt "run --test 30"`
- Check console for error messages
- Increase memory: `sbt -J-Xmx2G run`


## Key Features

✅ **Real-time audio processing** with FFT analysis  
✅ **Beat detection** using energy flux algorithm  
✅ **Actor-based** LED control (scalable to 100s of bracelets)  
✅ **Observable streams** for audio pipeline  
✅ **Multiple patterns** (Spectrum, Energy, Beat, Wave, Custom)  
✅ **Interactive GUI** with control panel  
✅ **Console visualization** for debugging  
✅ **Extensible** pattern system  
✅ **Configurable** via HOCON file  
✅ **Production-ready** architecture  

## 📖 Course Concepts Covered

All major concepts from INFO M451:

- **Chapter 5:** Observable[T] - Audio streams
- **Chapter 6:** Actor model - LED controllers
- **Chapter 4:** Future[T] - Async operations
- **Chapter 3:** Monads - Option, Try, composition
- **Chapter 2:** Functional - Immutability, patterns
- **Chapter 1:** Reactive - Event-driven design

See `CONCEPTS.md` for detailed mapping.

