<h1 align="center">
  Remote Volume Limiter
</h1>
<p align="center">
 <b>An app that allows you to adjust maximum sound volume of your Android device from any device in same local network, that has a web browser.</b>
</p>

This project is a fork of [Chgv99's Web Remote Volume Control](https://github.com/Chgv99/web-remote-volume-control) that implements a couple of features such as:
- Maximum volume slider
- Maximum volume indicator and slider updated dynamically 
- Allow mute (zero volume)
- Auto restart service after device boot up
- The limiting value remains after the device is rebooted
- Local network connectivity events management

The application implemented as AccesibilityService and intended for users, who has various reasons to limit maximum sound volume of own devices.

## TODO

<details>

- [ ] Make ***current*** volume indicator and slider  
</details>

How to use it :
===============

1. First, install "apk" of this application on the Android device of which you want to control sound volume remotely. 
2. Go to Settings->Accessibility->Accessibility->Remote Volume Limiter, than turn slider on. 
3. Goto Settings->Battery->Applications startup->Remote Volume Limiter, than turn off automatic control.
4. Now Notification Bar should display the internet address (URL, example : http://192.168.1.35:9000/) you will have to connect to in order to control sound volume remotely. (Or, if no local network  available, - "Waiting for local network...")
5. Then, on any other device connected on the same local network (Wifi) as your Android device, open a web browser (Chrome, Safari, Firefox, whatever, any web browser should work), and navigate to address obtained it step 4.
6. Finally, on the page that appears, press buttons to remotely adjust your Android device's maximum sound volume.



How it works:
==============

On your Android device, this app will start a lightweight minimalistic and app-specific web server, as a foreground AccessibilityService.  
This web server will listen on port 9000 and serve a static html page (single page application).  
That page will display only two buttons, Raise Volume and Lower Volume, which when clicked will asynchronously tell the web server / Android device to adjust maximum sound volume. Service is intended to control only one OS audio stream: "STREAM_MUSIC".

The web server isn't really one : it does not list directories or serve any requested file from filesystem.  
Also it will listen only on local IP addresses : when it determines the IP address of your Android device to listen on, it aborts if obtained IP address is not a local IP address.
 


