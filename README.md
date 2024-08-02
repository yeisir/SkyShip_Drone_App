# SkyShip Drone App v.1

SkyShip Drone App version 1.0 is an Android application designed to interact with a Bluetooth device to receive location data and display it on a Google Map. The app allows users to connect to the device, input height and air time values, send data, and visualize the drone's location on a map.

## Features

- Bluetooth connectivity to a specified device
- Input fields for height and air time
- Map view showing the drone's location
- Error handling for no GPS signal

## Requirements

-Android SDK 34 or higher

### Required Permissions.
-Bluetooth
-Location Access

These permissions are specified in the `AndroidManifest.xml` file.

## Installation

1. Clone the repository:
    git clone git@github.com:yeisir/SkyShip_Drone_App_v.1.git
2. Open the project in Android Studio.
3. Set up dependencies:
Ensure all required dependencies are included in your build.gradle file for successful project compilation.
4. Add Google Maps API Key:
Insert your Google Maps API key into the AndroidManifest.xml file.

## Usage

1. Launch the app:
  Open the app on an Android device or emulator.
2. Connect to the Bluetooth device:
  Press the button to connect to the Bluetooth device specified in the code.
3. Send data to the device:
  Enter the altitude and flight time in the respective fields and press the "Send" button.
4. View the map:
  The drone's location will be displayed on the map once the coordinates are received.

# Note:

I hope this helps your project. If you have any questions, don't hesitate to contact me :)




