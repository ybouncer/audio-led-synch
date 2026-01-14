#!/bin/bash

# Audio-LED Sync Run Script
# Simple wrapper for running the application

echo "======================================"
echo " Audio-LED Synchronization System"
echo "======================================"
echo ""

# Check if sbt is installed
if ! command -v sbt &> /dev/null; then
    echo "Error: sbt is not installed"
    echo "Please install sbt: https://www.scala-sbt.org/download.html"
    exit 1
fi

# Function to show usage
show_usage() {
    echo "Usage: $0 [MODE] [OPTIONS]"
    echo ""
    echo "Modes:"
    echo "  test [duration]     Generate test audio (default: 30 seconds)"
    echo "  file <path>         Process audio file"
    echo "  realtime            Use microphone input"
    echo "  help                Show this help"
    echo ""
    echo "Examples:"
    echo "  $0 test 60                 # Test mode for 60 seconds"
    echo "  $0 file song.wav           # Process audio file"
    echo "  $0 realtime                # Realtime microphone"
    echo ""
}

# Parse arguments
case "$1" in
    test)
        DURATION=${2:-30}
        echo "Running in TEST mode ($DURATION seconds)"
        echo "Generating synthetic audio..."
        echo ""
        sbt "run --test $DURATION"
        ;;
    
    file)
        if [ -z "$2" ]; then
            echo "Error: Audio file path required"
            show_usage
            exit 1
        fi
        
        if [ ! -f "$2" ]; then
            echo "Error: File not found: $2"
            exit 1
        fi
        
        echo "Running in FILE mode"
        echo "Processing: $2"
        echo ""
        sbt "run --file $2"
        ;;
    
    realtime)
        echo "Running in REALTIME mode"
        echo "Using microphone input..."
        echo "Make some noise! 🎵"
        echo ""
        sbt "run --realtime"
        ;;
    
    help|--help|-h)
        show_usage
        exit 0
        ;;
    
    "")
        echo "No mode specified, using TEST mode (30 seconds)"
        echo ""
        sbt "run --test 30"
        ;;
    
    *)
        echo "Error: Unknown mode: $1"
        show_usage
        exit 1
        ;;
esac
