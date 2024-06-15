## Wallpaper Generator
This repository contains an Android application that generates custom wallpapers using the OpenAI API. The application also allows setting the generated images as wallpapers and saving them to the device gallery. Additionally, it includes PayPal integration for donations and displays interstitial ads.

## Features
* Image generation using the OpenAI API.
* Set the generated images as wallpapers.
* Save the generated images to the device gallery.
* PayPal integration for donations.
* Display interstitial ads using Google AdMob.
* Requirements
* Android Studio.
* OpenAI API Key.
* PayPal Client ID.
* Google AdMob configuration.
* Installation
* Clone this repository:

## bash
```
git clone https://github.com/ByStarki22/wallpaper-generator.git
```
Open the project in Android Studio.

Add your OpenAI API Key and PayPal Client ID to the corresponding variables in the code:

## kotlin
```
private val stringAPIKey = "Your OpenAI API Key"
payPalManager = PayPalManager(this, "Your PayPal Client ID")
```
Configure your AdMob account to obtain an interstitial ad ID and replace it in the code:

## kotlin
```
InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
```
Build and run the application on your device or emulator.

## Usage
* Enter the text for image generation in the text field.
* Click the "Generate Image" button to obtain the image from the OpenAI API.
* The generated image will be displayed in the application.
* You can set the image as wallpaper, save it to the gallery, or make a donation.
* Screenshots
* Add screenshots of the application here.

## Contributions
Contributions are welcome. Please open an issue or a pull request to discuss any changes you would like to make.
