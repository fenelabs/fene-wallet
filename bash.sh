#!/bin/bash

# Android SDK Install Script for Gitpod / Linux Environment

# Define SDK path
SDK_PATH="/workspace/unstoppable-wallet-android/android-sdk"

# Create directories
mkdir -p $SDK_PATH/cmdline-tools

# Download Command Line Tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip

# Unzip and move
unzip -q cmdline-tools.zip -d $SDK_PATH/cmdline-tools
mv $SDK_PATH/cmdline-tools/cmdline-tools $SDK_PATH/cmdline-tools/latest

# Export environment vars
echo "export ANDROID_HOME=$SDK_PATH" >> ~/.bashrc
echo "export PATH=\$ANDROID_HOME/emulator:\$ANDROID_HOME/tools:\$ANDROID_HOME/tools/bin:\$ANDROID_HOME/platform-tools:\$PATH" >> ~/.bashrc
source ~/.bashrc

# Accept licenses
yes | $SDK_PATH/cmdline-tools/latest/bin/sdkmanager --licenses

# Install essential packages
$SDK_PATH/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-33" "build-tools;33.0.2"

# Create local.properties if inside project directory
if [ -f "gradlew" ]; then
  echo "sdk.dir=$SDK_PATH" > local.properties
  echo "local.properties created."
else
  echo "Don't forget to create local.properties in your project root with sdk.dir=$SDK_PATH"
fi

# Cleanup
rm cmdline-tools.zip
