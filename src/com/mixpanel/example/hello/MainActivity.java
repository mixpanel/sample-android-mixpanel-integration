package com.mixpanel.example.hello;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Base64;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import com.mixpanel.android.mpmetrics.MPMetrics;

/**
 * A little application that allows people to update their Mixpanel information,
 * and receive push notifications from a Mixpanel project.
 *
 * For more information about integrating Mixpanel with your Android application,
 * please check out:
 *
 *     https://mixpanel.com/docs/integration-libraries
 *
 * For instructions on enabling push notifications from Mixpanel, please see
 *
 *     https://mixpanel.com/docs/people-analytics/android-push
 *
 * @author mixpanel
 *
 */
public class MainActivity extends Activity {

    /**
     * You will use a Mixpanel API token to allow your app to send data to Mixpanel. To get your token
     * - Log in to Mixpanel, and select the project you want to use for this application
     * - Click the gear icon in the lower left corner of the screen to view the settings dialog
     * - In the settings dialog, you will see the label "Token", and a string that looks something like this:
     *
     *        2ef3c08e8466df98e67ea0cfa1512e9f
     *
     *   Paste it below (where you see "YOUR API TOKEN")
     */
    public static final String MIXPANEL_API_TOKEN = "YOUR API TOKEN";

    /**
     * In order for your app to receive push notifications, you will need to enable
     * the Google Cloud Messaging for Android service in your Google APIs console. To do this:
     *
     * - Navigate to https://code.google.com/apis/console
     * - Select "Services" from the menu on the left side of the screen
     * - Scroll down until you see the row labeled "Google Cloud Messaging for Android"
     * - Make sure the switch next to the service name says "On"
     *
     * To identify this application with your Google API account, you'll also need your sender id from Google.
     * You can get yours by logging in to the Google APIs Console at https://code.google.com/apis/console
     * Once you have logged in, your sender id will appear as part of the URL in your browser's address bar.
     * The URL will look something like this:
     *
     *     https://code.google.com/apis/console/b/0/#project:256660625236
     *                                                       ^^^^^^^^^^^^
     *
     * The twelve-digit number after 'project:' is your sender id. Paste it below (where you see "YOUR SENDER ID")
     *
     * There are also some changes you will need to make to your AndroidManifest.xml file to
     * declare the permissions and receiver capabilities you'll need to get your push notifications working.
     * You can take a look at this application's AndroidManifest.xml file for an example of what is needed.
     */
    public static final String ANDROID_PUSH_SENDER_ID = "YOUR SENDER ID";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String trackingDistinctId = getTrackingDistinctId();

        // Initialize the Mixpanel library for tracking and push notifications.
        // We also identify the current user with a distinct ID, and
        // register ourselves for push notifications from Mixpanel
        mMPMetrics = MPMetrics.getInstance(this, MIXPANEL_API_TOKEN);
        mMPMetrics.identify(trackingDistinctId);
        mMPMetrics.registerForPush(ANDROID_PUSH_SENDER_ID);

        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // For our simple test app, we're interested in a single
        // tracking a single Mixpanel event- when do users view our
        // application in the foreground?
        try {
            JSONObject properties = new JSONObject();
            properties.put("hour of the day", hourOfTheDay());
            mMPMetrics.track("App Resumed", properties);
        } catch(JSONException e) {
            throw new RuntimeException("Could not encode form values as JSON");
        }
    }

    // Associated with the "Send to Mixpanel" button in activity_main.xml
    public void sendToMixpanel(View view) {

        EditText firstNameEdit = (EditText) findViewById(R.id.edit_first_name);
        EditText lastNameEdit = (EditText) findViewById(R.id.edit_last_name);
        EditText emailEdit = (EditText) findViewById(R.id.edit_email_address);

        String firstName = firstNameEdit.getText().toString();
        String lastName = lastNameEdit.getText().toString();
        String email = emailEdit.getText().toString();


        // When the user clicks the "Send to Mixpanel" button in the UI,
        // we want to use the information they have given us to update
        // their Mixpanel people record. We do this with MPMetrics.set()
        try {
            JSONObject properties = new JSONObject();
            properties.put("$first_name", firstName);
            properties.put("$last_name", lastName);
            properties.put("$email", email);
            mMPMetrics.set(properties);
        } catch(JSONException e) {
            // This will only happen if the values are non-finite numbers
            // or the keys are null, which should be impossible.
            throw new RuntimeException("Could not encode form values as JSON");
        }

        // We also want to keep track of how many times the user
        // has updated their info.
        mMPMetrics.increment("Update Count", 1L);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // To preserve battery life, the Mixpanel library will store
        // events rather than send them immediately. This means it
        // is important to call flushAll() to get any unsent events
        // before your application is taken out of memory.
        mMPMetrics.flushAll();
    }



    ////////////////////////////////////////////////////


    private String getTrackingDistinctId() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        String ret = prefs.getString(MIXPANEL_DISTINCT_ID_NAME, null);
        if (ret == null) {
            ret = generateDistinctId();
            SharedPreferences.Editor prefsEditor = prefs.edit();
            prefsEditor.putString(MIXPANEL_DISTINCT_ID_NAME, ret);
            prefsEditor.commit();
        }

        return ret;
    }

    private int hourOfTheDay() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    // This is just an example of how we might generate distinct ids for users.
    // In practice, there are serious advantages to associating these ids to your
    // internal systems, or basing them on user provided login or identity information.
    private String generateDistinctId() {
        String androidId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
        if (androidId == null) {
            androidId = "";
        }

        Random random = new Random();
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);

        Date now = new Date();
        Long nowMs = Long.valueOf(now.getTime());

        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance("SHA-1");
        }
        catch(NoSuchAlgorithmException e) {
            // This should never happen, "SHA-1" is one of the standard algorithm names
            throw new RuntimeException("Cannot generate distinct user id");
        }

        digest.update(androidId.getBytes());
        digest.update(randomBytes);
        digest.update(nowMs.byteValue());

        return Base64.encodeToString(digest.digest(), Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private MPMetrics mMPMetrics;

    private static final String MIXPANEL_DISTINCT_ID_NAME = "Mixpanel $distinctid";
}
