# Sample Android Application for Mixpanel Integration

This repository contains a sample application demonstrating how you
can use Mixpanel in your Android apps.

## Getting Started

The sample application can be build using Android Studio.

### Using Android Studio
Integration in Android Studio is very simple.

- Clone this git repository (the instructions are at the top of the page)
- Within Android Studio, go to "File > Import Project"
- Select the directory you just cloned.
- When the gradle popup shows up, choose "use gradle default wrapper (recommended)" and click OK

You should now be able to run the sample application

You will also need to add your Mixpanel API token and your Android
Push Sender id to the source code, and enable Mixpanel to send Google
Cloud Messages on your behalf.

## Add your Mixpanel Token to your code and google-services.json file to your project
There is one value in MainActivity.java that you'll need to update
before you can send data to Mixpanel. You'll need to update the source code with
your Mixpanel API Token to send data.

### For Your Mixpanel Token

- Log in to your account at https://www.mixpanel.com
- Select the project you'll be working with from the drop-down at the top left
- Click the gear link at the top right to show the project settings dialog
- Copy the "Token" string from the dialog

Change the value of MainActivity.MIXPANEL_API_TOKEN to the value you
copied from the web page.

### For enabling push notifications using FCM

- Log in to your Firebase Console at https://console.firebase.google.com
- Select your project and click on the gear (top left corner) to access your Project settings
- Go to "Cloud Messaging" tab
- Scroll down and click on "google-services.json" to download your config file.
- Place your file on your app module directory.

## Set up Firebase Cloud Messaging services in Mixpanel

To send Firebase Cloud Messages, you will also have to connect Mixpanel
to your Google APIs account. To do this you'll need to make sure Cloud
Messaging is enabled, and provide an FCM Server key to Mixpanel

This process is documented in more detail, including screenshots, at

https://mixpanel.com/docs/people-analytics/android-push

### To Provide your Firebase Server Key to Mixpanel

- Log in to your Firebase Console Console at https://console.firebase.google.com
- Select your project and click on the gear (top left corner) to access your Project settings
- Go to "Cloud Messaging" tab
- Copy your "Server key"
- Log in to Mixpanel at http://www.mixpanel.com, and select the project associated with this application
- Click the gear icon in the upper right corner of the screen to show the project settings dialog
- Click on the Messages tab of the project settings dialog
- Paste your Firebase Server Key into the "Android FCM Server Key" field

Once you've added your keys to the source code and set up Mixpanel to
send Firebase Cloud Messages, you're ready to build and deploy your application.

## Getting More Information

The Mixpanel Android integration API documentation is available on the Mixpanel website.

For an overview of Mixpanel Android library features
: https://mixpanel.com/help/reference/android

For details about setting up and implementing Google Cloud Messaging Notifications
: https://mixpanel.com/help/reference/android-push-notifications

For a detailed Android API reference
: http://mixpanel.github.io/mixpanel-android/index.html
