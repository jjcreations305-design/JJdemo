JJ FREQUENCY CHAT — Android starter project
===========================================

What this prototype does
- Android app source for nearby, offline Bluetooth text chat.
- Users type the same channel code on both phones.
- One phone taps Create / Listen.
- On the other phone, first pair the devices in Android Bluetooth Settings, then open Find Devices and tap the paired phone.
- Both phones must be running the app and use the same code.

Important limitations
- The channel code is a room label/handshake, NOT an actual FM radio frequency.
- Bluetooth is short range; this cannot chat across 30 km or across cities directly.
- No internet or SIM recharge is needed for Bluetooth chat.
- This prototype does not provide end-to-end encryption. Do not use it for sensitive messages.
- The app source is provided; this ZIP is not a prebuilt APK.

Build on a computer
1. Install Android Studio and a compatible JDK (17).
2. Open this folder in Android Studio.
3. Let Gradle sync and install/build the app.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).

To test
1. Install the built APK on two Android phones.
2. Pair the phones through Android Bluetooth settings.
3. Open app on phone A, enter a channel code, tap Create / Listen.
4. On phone B, enter the exact same code, tap Find Devices, and tap phone A.
5. Keep both apps open during testing.

30 km goal
For actual long-distance no-internet messaging, this would need compatible external radio hardware (for example, appropriate LoRa modules), a supported phone-to-radio connection, and compliance with Indian radio regulations. This project does not transmit FM or LoRa.
