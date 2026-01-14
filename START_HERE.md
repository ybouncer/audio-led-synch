# Audio-LED Synchronization System - START HERE

Welcome! This is your complete Scala reactive programming project for audio-LED synchronization.

## 🚀 Quick Start (5 minutes)

1. **Navigate to project:**
   ```bash
   cd audio-led-sync
   ```

2. **Run the demo:**
   ```bash
   sbt "run --test 30"
   ```

3. **Watch the magic happen!** ✨

## 📚 Documentation Guide

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

## 📂 File Structure

```
audio-led-sync/
│
├── 📖 Documentation/
│   ├── README.md              ← Start here!
│   ├── CONCEPTS.md            ← Course concept mapping
│   ├── QUICKREF.md            ← Quick reference
│   ├── PRESENTATION.md        ← Demo guide
│   └── PROJECT_SUMMARY.md     ← This file
│
├── 🔧 Configuration/
│   ├── build.sbt              ← Dependencies
│   ├── run.sh                 ← Convenience script
│   └── src/main/resources/
│       └── application.conf   ← System configuration
│
└── 💻 Source Code/
    └── src/main/scala/com/audioled/
        ├── AudioLEDSyncApp.scala              ← Main application
        ├── domain/Models.scala                ← Data models
        ├── audio/AudioProcessor.scala         ← Observable streams
        ├── led/LEDControllerActor.scala       ← Actor system
        └── visualization/
            ├── ConsoleVisualizer.scala        ← Terminal UI
            └── GUIVisualizer.scala            ← Swing GUI
```

## 🎯 Common Tasks

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

### Prepare for Demo

1. Read `PRESENTATION.md`
2. Test the system: `sbt "run --test 30"`
3. Review key code sections
4. Practice explaining concepts

### Modify the System

1. Check `QUICKREF.md` for common modifications
2. Look at relevant source file
3. Follow patterns in existing code
4. Test your changes

## 🎓 Learning Path

### Level 1: User
**Goal:** Run and understand the system  
**Read:** README.md  
**Do:** Run in test mode, observe output  
**Time:** 30 minutes

### Level 2: Student
**Goal:** Understand reactive concepts  
**Read:** CONCEPTS.md  
**Do:** Map code to slides, identify patterns  
**Time:** 2 hours

### Level 3: Developer
**Goal:** Modify and extend  
**Read:** QUICKREF.md + Source code  
**Do:** Add custom pattern, change configuration  
**Time:** 4 hours

### Level 4: Integrator
**Goal:** Connect to hardware  
**Read:** README.md hardware section  
**Do:** Add network layer, deploy to Pi  
**Time:** 8 hours

## ✅ System Check

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

## 🆘 Troubleshooting

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

## 🎨 What You'll See

### Console Mode
```
================================================================================
 Audio-LED Synchronization System 
================================================================================

Audio Features:
  Energy:   ████████████████████░░░░░░░░░░░░░░░ 68%
  Bass:     ███████████████████████████████░░░░ 85%
  Mid:      █████████████████░░░░░░░░░░░░░░░░░ 52%
  High:     ████████░░░░░░░░░░░░░░░░░░░░░░░░░░ 31%
  Beat:     💥 BEAT!

LED States:
┌────────────────────────────────────────┐
│████████▓▓▓▓▓▓▓▓▒▒▒▒░░░░░░░░          │
└────────────────────────────────────────┘
```

### GUI Mode
- Visual LED circles with glow effects
- Real-time spectrum bars (Bass/Mid/High)
- Waveform display
- Control panel for patterns and brightness

## 🔗 Integration with Your Project

This system is designed to integrate with your "BraceLEDS connectés" project:

**Current:** Simulation with visual feedback  
**Next:** Connect to Raspberry Pi + ESP32  
**Hardware:** Physical LEDs on bracelets  

See README.md "Hardware Integration" section for details.

## 💡 Key Features

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

## 🎯 Your Next Steps

### Today
1. ✅ Read this file (you're doing it!)
2. ✅ Run the system: `sbt "run --test 30"`
3. ✅ Observe the output
4. ✅ Read README.md for details

### This Week
1. ✅ Review CONCEPTS.md
2. ✅ Map code to course slides
3. ✅ Try different patterns
4. ✅ Show to team members

### This Month
1. ✅ Prepare presentation using PRESENTATION.md
2. ✅ Demo to colleagues
3. ✅ Plan hardware integration
4. ✅ Extend with custom features

## 🎉 You're Ready!

You now have:
- ✅ Complete working system
- ✅ Comprehensive documentation
- ✅ Course concepts implemented
- ✅ Demo-ready code
- ✅ Integration path to hardware

**Everything you need is in this directory.**

## 📞 Getting Help

1. **First:** Check relevant documentation file
2. **Then:** Look at code comments
3. **Next:** Review course slides
4. **Finally:** Ask team members

## 🚀 Let's Go!

```bash
cd audio-led-sync
sbt "run --test 30"
```

Watch your terminal come alive with audio-reactive LEDs! ✨🎵

---

**Questions?** Start with README.md  
**Want theory?** Read CONCEPTS.md  
**Need quick tips?** Check QUICKREF.md  
**Preparing demo?** Follow PRESENTATION.md  

**Have fun and good luck with your project! 🎵💡🚀**
