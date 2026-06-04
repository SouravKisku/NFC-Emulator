# NFC Emulator for Android

A simple Android application that allows you to read, store, and emulate NFC cards using Host Card Emulation (HCE).

## Features

*   **Read NFC Cards:** Supports various NFC technologies including IsoDep, Mifare Classic, Mifare Ultralight, and NDEF.
*   **Store Cards:** Save scanned card data (UID and a snippet of content) locally on your device with custom names.
*   **Emulate Cards:** Emulate stored card data using Android's Host Card Emulation (HCE) capabilities.
*   **Modern UI:** Simple interface built with Material Components.

## Prerequisites

*   An Android device with **NFC hardware**.
*   Android 5.0 (API level 21) or higher.
*   For Emulation: The device must support **Host Card Emulation (HCE)**.

## Getting Started

### Installation

1.  Clone the repository:
    ```bash
    git clone https://github.com/yourusername/NFCEmulator.git
    ```
2.  Open the project in **Android Studio**.
3.  Build and run the app on your physical NFC-enabled Android device.

### How to Use

#### 1. Reading a Card
1.  Open the app.
2.  Tap the **Read Card** button. The status will change to "Tap a card to read...".
3.  Hold your NFC card against the back of your phone.
4.  Once read, the app will display the card's UID and some technology details.

#### 2. Saving a Card
1.  After successfully reading a card, tap the **Save** button.
2.  Enter a name for the card in the dialog.
3.  The card will be added to your saved list.

#### 3. Emulating a Card
1.  Select a card from the dropdown menu (Spinner).
2.  Tap the **Emulate** button.
3.  The status will show "Emulating: [Card Name]".
4.  Hold your phone near an NFC reader. The phone will act as the selected card.
    *   *Note: Emulation works via HCE and currently uses a sample AID (`F0010203040506`) as defined in `apduservice.xml`.*

## Project Structure

*   `MainActivity.java`: Handles the UI, NFC foreground dispatch for reading tags, and managing the card store.
*   `HceService.java`: Extends `HostApduService` to handle NFC reader requests while the app is in emulation mode.
*   `CardStore.java`: A helper class that manages saving and retrieving card data using `SharedPreferences` and JSON.
*   `AndroidManifest.xml`: Defines necessary NFC permissions and registers the HCE service.
*   `res/xml/apduservice.xml`: Configuration for the HCE service, including the Application Identifier (AID) filters.

## Technical Details

### NFC Technologies Supported
The app attempts to read data using several tech-specific methods:
*   **IsoDep:** Tries to select the PPSE (Proximity Payment System Environment) for payment cards.
*   **MifareClassic / MifareUltralight:** Reads the first block or pages using default keys.
*   **NDEF:** Reads the standard NDEF message if present.

### Security Note
This app is for **educational purposes only**. Emulating encrypted or secure cards (like credit cards or secure access badges) usually requires cryptographic keys and secure elements which are not accessible via standard HCE without specific authorization and hardware support.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
