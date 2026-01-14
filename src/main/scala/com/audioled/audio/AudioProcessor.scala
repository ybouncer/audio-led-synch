package com.audioled.audio

import com.audioled.domain._
import monix.eval.Task
import monix.reactive.{Observable, Observer}
import org.jtransforms.fft.DoubleFFT_1D
import scala.concurrent.duration._
import javax.sound.sampled._
import java.io.{File, ByteArrayInputStream}
import scala.util.{Try, Success, Failure}
import com.typesafe.scalalogging.LazyLogging

/**
 * Audio processor that demonstrates reactive programming concepts:
 * - Observable streams (from slides: "asynchronous, many values")
 * - map, filter, flatMap operations
 * - Effect handling with Try monad
 */
class AudioProcessor(config: AudioLEDConfig) extends LazyLogging {
  
  private val fft = new DoubleFFT_1D(config.fftSize)
  private var energyHistory: List[Double] = List.fill(43)(0.0)
  
  /**
   * Creates an Observable stream from an audio file
   * Demonstrates: Observable[T] pattern from slides
   */
  def processAudioFile(filePath: String): Observable[AudioFeatures] = {
    Observable.fromTask(loadAudioFile(filePath))
      .flatMap(audioData => processAudioData(audioData))
      .onErrorHandle { ex =>
        logger.error(s"Error processing audio: ${ex.getMessage}")
        AudioFeatures(
          timestamp = System.currentTimeMillis(),
          energy = 0.0,
          bassEnergy = 0.0,
          midEnergy = 0.0,
          highEnergy = 0.0,
          spectralCentroid = 0.0,
          isBeat = false,
          tempo = None,
          samples = Array.empty
        )
      }
  }
  
  /**
   * Creates an Observable stream from real-time microphone input
   * Demonstrates: push-based reactive stream
   */
  def processRealtimeAudio(): Observable[AudioFeatures] = {
    Observable.unsafeCreate[Array[Byte]] { subscriber =>
      import monix.execution.Scheduler.Implicits.global
      import monix.execution.Cancelable
      
      Try {
        val format = new AudioFormat(
          config.sampleRate.toFloat,
          16, // 16-bit
          1,  // mono
          true, // signed
          false // little endian
        )
        
        val targetLine = AudioSystem.getTargetDataLine(format)
        targetLine.open(format, config.bufferSize * 2)
        targetLine.start()
        
        val buffer = new Array[Byte](config.bufferSize * 2)
        var running = true
        
        // Background thread that reads from microphone
        val readThread = new Thread(() => {
          while (running) {
            val bytesRead = targetLine.read(buffer, 0, buffer.length)
            if (bytesRead > 0) {
              subscriber.onNext(buffer.clone())
            }
          }
          targetLine.close()
          subscriber.onComplete()
        })
        readThread.start()
        
        // Return cancelable
        Cancelable { () =>
          running = false
          targetLine.stop()
          targetLine.close()
          readThread.interrupt()
        }
      } match {
        case Success(cancelable) => cancelable
        case Failure(ex) =>
          subscriber.onError(ex)
          Cancelable.empty
      }
    }
    .map(bytesToDoubles(_))
    .bufferTimedAndCounted(50.millis, config.bufferSize / 2)
    .map(_.flatten.toArray)
    .filter(_.length >= config.bufferSize)
    .map(samples => analyzeAudioBuffer(samples.take(config.bufferSize)))
  }
  
  /**
   * Generate synthetic audio for testing
   * Demonstrates: Observable.interval and map transformations
   */
  def generateTestAudio(durationSeconds: Int): Observable[AudioFeatures] = {
    val samplesPerUpdate = config.bufferSize
    val updatesPerSecond = config.sampleRate / samplesPerUpdate
    val totalUpdates = durationSeconds * updatesPerSecond
    
    Observable.interval(1000.millis / updatesPerSecond)
      .take(totalUpdates)
      .map { tick =>
        // Generate synthetic audio with varying frequencies and amplitude
        val t = tick.toDouble / updatesPerSecond
        val bassFreq = 60.0 + 20.0 * math.sin(t * 0.5)
        val midFreq = 440.0 + 100.0 * math.sin(t * 0.8)
        val highFreq = 2000.0 + 500.0 * math.sin(t * 1.2)
        
        val samples = Array.tabulate(samplesPerUpdate) { i =>
          val time = (tick * samplesPerUpdate + i).toDouble / config.sampleRate
          val bass = 0.5 * math.sin(2 * math.Pi * bassFreq * time)
          val mid = 0.3 * math.sin(2 * math.Pi * midFreq * time)
          val high = 0.2 * math.sin(2 * math.Pi * highFreq * time)
          
          // Add periodic "beats"
          val beatAmp = if (time % 0.5 < 0.1) 1.5 else 1.0
          
          (bass + mid + high) * beatAmp
        }
        
        analyzeAudioBuffer(samples)
      }
  }
  
  /**
   * Main audio analysis function
   * Demonstrates: functional composition and Try monad for error handling
   */
  private def analyzeAudioBuffer(samples: Array[Double]): AudioFeatures = {
    require(samples.length >= config.bufferSize, s"Need at least ${config.bufferSize} samples")
    
    // Apply windowing to reduce spectral leakage
    val windowedSamples = applyHannWindow(samples.take(config.fftSize))
    
    // Perform FFT
    val fftData = performFFT(windowedSamples)
    
    // Calculate frequency band energies
    val bassEnergy = calculateBandEnergy(fftData, 20, 250)
    val midEnergy = calculateBandEnergy(fftData, 250, 2000)
    val highEnergy = calculateBandEnergy(fftData, 2000, 8000)
    val totalEnergy = bassEnergy + midEnergy + highEnergy
    
    // Calculate spectral centroid (perceived brightness)
    val centroid = calculateSpectralCentroid(fftData)
    
    // Beat detection using energy flux
    val isBeat = detectBeat(totalEnergy)
    
    AudioFeatures(
      timestamp = System.currentTimeMillis(),
      energy = totalEnergy,
      bassEnergy = bassEnergy,
      midEnergy = midEnergy,
      highEnergy = highEnergy,
      spectralCentroid = centroid,
      isBeat = isBeat,
      tempo = None, // Tempo detection requires more sophisticated analysis
      samples = samples.take(100) // Keep some samples for waveform display
    )
  }
  
  /**
   * Load audio file using Task (Future-like, from slides)
   * Demonstrates: asynchronous computation with Task monad
   */
  private def loadAudioFile(filePath: String): Task[Array[Double]] = Task {
    logger.info(s"Loading audio file: $filePath")
    
    val file = new File(filePath)
    val audioInputStream = AudioSystem.getAudioInputStream(file)
    val format = audioInputStream.getFormat
    
    // Convert to mono PCM if necessary
    val monoFormat = new AudioFormat(
      AudioFormat.Encoding.PCM_SIGNED,
      config.sampleRate.toFloat,
      16,
      1, // mono
      2, // frame size
      config.sampleRate.toFloat,
      false // little endian
    )
    
    val convertedStream = if (format.matches(monoFormat)) {
      audioInputStream
    } else {
      AudioSystem.getAudioInputStream(monoFormat, audioInputStream)
    }
    
    // Read all bytes
    val allBytes = Iterator.continually(convertedStream.read())
      .takeWhile(_ != -1)
      .map(_.toByte)
      .toArray
    
    convertedStream.close()
    
    bytesToDoubles(allBytes)
  }
  
  /**
   * Process loaded audio data into Observable stream
   * Demonstrates: flatMap and chunking operations
   */
  private def processAudioData(audioData: Array[Double]): Observable[AudioFeatures] = {
    Observable.fromIterable(audioData.grouped(config.bufferSize).toSeq)
      .filter(_.length == config.bufferSize)
      .map(analyzeAudioBuffer)
  }
  
  // === Audio Processing Utilities ===
  
  private def bytesToDoubles(bytes: Array[Byte]): Array[Double] = {
    bytes.grouped(2).map { pair =>
      if (pair.length == 2) {
        val sample = (pair(1) << 8) | (pair(0) & 0xFF)
        sample.toShort.toDouble / 32768.0 // Normalize to -1.0 to 1.0
      } else {
        0.0
      }
    }.toArray
  }
  
  private def applyHannWindow(samples: Array[Double]): Array[Double] = {
    samples.zipWithIndex.map { case (sample, i) =>
      val window = 0.5 * (1 - math.cos(2 * math.Pi * i / (samples.length - 1)))
      sample * window
    }
  }
  
  private def performFFT(samples: Array[Double]): Array[Double] = {
    // JTransforms requires input array of size 2*n for real FFT
    val fftInput = new Array[Double](config.fftSize * 2)
    Array.copy(samples, 0, fftInput, 0, math.min(samples.length, config.fftSize))
    
    fft.realForwardFull(fftInput)
    
    // Calculate magnitude spectrum
    val magnitudes = new Array[Double](config.fftSize)
    for (i <- 0 until config.fftSize) {
      val real = fftInput(2 * i)
      val imag = fftInput(2 * i + 1)
      magnitudes(i) = math.sqrt(real * real + imag * imag)
    }
    
    magnitudes
  }
  
  private def calculateBandEnergy(fftMagnitudes: Array[Double], 
                                   lowFreq: Double, 
                                   highFreq: Double): Double = {
    val binWidth = config.sampleRate.toDouble / config.fftSize
    val lowBin = (lowFreq / binWidth).toInt
    val highBin = math.min((highFreq / binWidth).toInt, fftMagnitudes.length - 1)
    
    if (lowBin >= highBin) return 0.0
    
    val bandEnergy = fftMagnitudes.slice(lowBin, highBin).map(m => m * m).sum
    bandEnergy / (highBin - lowBin) // Normalize by number of bins
  }
  
  private def calculateSpectralCentroid(fftMagnitudes: Array[Double]): Double = {
    val binWidth = config.sampleRate.toDouble / config.fftSize
    val weightedSum = fftMagnitudes.zipWithIndex.map { case (mag, i) =>
      mag * (i * binWidth)
    }.sum
    val totalMagnitude = fftMagnitudes.sum
    
    if (totalMagnitude > 0) weightedSum / totalMagnitude else 0.0
  }
  
  private def detectBeat(energy: Double): Boolean = {
    if (energyHistory.isEmpty) {
      energyHistory = List(energy)
      return false
    }
    
    val avgEnergy = energyHistory.sum / energyHistory.size
    val threshold = 1.3 * avgEnergy
    
    energyHistory = (energy :: energyHistory).take(43)
    
    energy > threshold && energy > 0.1
  }
}

object AudioProcessor {
  def apply(config: AudioLEDConfig): AudioProcessor = 
    new AudioProcessor(config)
}
