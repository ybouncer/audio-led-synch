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
      .flatMap { audioData =>
        if (audioData.isEmpty) {
          logger.error(s"No audio data loaded from file: $filePath")
          Observable.raiseError(new RuntimeException(s"Failed to load audio data from: $filePath"))
        } else {
          logger.info(s"Processing ${audioData.length} samples from file")
          processAudioData(audioData)
        }
      }
      .doOnError(ex => monix.eval.Task.eval {
        logger.error(s"Error processing audio: ${ex.getMessage}", ex)
      })
  }
  
  /**
   * Creates an Observable stream from real-time microphone input
   * Demonstrates: push-based reactive streamc
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

    logger.info(s"Original format: $format")
    logger.info(s"  Channels: ${format.getChannels}, Sample rate: ${format.getSampleRate}, Bits: ${format.getSampleSizeInBits}")

    // Target format - match the file's sample rate initially, then we work with it
    val targetSampleRate = if (format.getSampleRate > 0) format.getSampleRate else config.sampleRate.toFloat

    // Step 1: Convert to PCM if necessary (handles MP3, etc.)
    val pcmFormat = new AudioFormat(
      AudioFormat.Encoding.PCM_SIGNED,
      targetSampleRate,
      16,
      format.getChannels, // Keep original channels first
      format.getChannels * 2, // frame size = channels * 2 bytes
      targetSampleRate,
      false // little endian
    )

    val pcmStream = if (format.getEncoding == AudioFormat.Encoding.PCM_SIGNED &&
                        format.getSampleSizeInBits == 16) {
      audioInputStream
    } else {
      try {
        AudioSystem.getAudioInputStream(pcmFormat, audioInputStream)
      } catch {
        case e: Exception =>
          logger.warn(s"Could not convert to PCM: ${e.getMessage}, using original stream")
          audioInputStream
      }
    }

    // Read all bytes using buffer (required for frame size > 1)
    val actualFormat = pcmStream.getFormat
    val channels = actualFormat.getChannels
    val bytesPerSample = actualFormat.getSampleSizeInBits / 8
    val frameSize = channels * bytesPerSample

    logger.info(s"Working format: $actualFormat")
    logger.info(s"Frame size: $frameSize bytes (channels=$channels, bytesPerSample=$bytesPerSample)")

    // Read in chunks matching frame size
    val buffer = new Array[Byte](frameSize * 1024) // Read 1024 frames at a time
    val allBytesBuilder = Array.newBuilder[Byte]

    var bytesRead = pcmStream.read(buffer)
    while (bytesRead > 0) {
      allBytesBuilder ++= buffer.take(bytesRead)
      bytesRead = pcmStream.read(buffer)
    }

    pcmStream.close()
    val allBytes = allBytesBuilder.result()
    logger.info(s"Read ${allBytes.length} bytes from audio file")

    // Convert bytes to doubles, handling stereo by averaging channels
    val samples = allBytes.grouped(frameSize).flatMap { frame =>
      if (frame.length >= frameSize) {
        // Average all channels to mono
        val channelSamples = (0 until channels).map { ch =>
          val offset = ch * bytesPerSample
          val sample = (frame(offset + 1) << 8) | (frame(offset) & 0xFF)
          sample.toShort.toDouble / 32768.0
        }
        Some(channelSamples.sum / channels) // Average to mono
      } else {
        None
      }
    }.toArray

    logger.info(s"Converted to ${samples.length} mono samples")
    samples
  }
  
  /**
   * Process loaded audio data into Observable stream
   * Demonstrates: flatMap and chunking operations
   */
  private def processAudioData(audioData: Array[Double]): Observable[AudioFeatures] = {
    // Calculate delay to match real-time playback: each chunk represents bufferSize/sampleRate seconds
    val chunkDurationMs = (config.bufferSize.toDouble / config.sampleRate * 1000).toLong

    Observable.fromIterable(audioData.grouped(config.bufferSize).toSeq)
      .filter(_.length == config.bufferSize)
      .map(analyzeAudioBuffer)
      .delayOnNext(chunkDurationMs.millis)  // Emit at real-time pace
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
