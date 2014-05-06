package com.mixpanel.example.advanced;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.mixpanel.example.hello.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * This is a sample application that demonstrates some advanced usages of Mixpanel.
 * If you are just beginning to implement Mixpanel in your application,
 * we recommend looking at the simple Mixpanel example first, and then use this application as
 * a reference once you need to have a more advanced integration.
 *
 * For more information about integrating Mixpanel with your Android application,
 * please check out:
 *
 *     https://mixpanel.com/docs/integration-libraries/android
 *
 * For instructions on enabling push notifications from Mixpanel, please see
 *
 *     https://mixpanel.com/docs/people-analytics/android-push
 *
 * @author mixpanel
 *
 */
public class MainActivity extends Activity {

    /*
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

    public static final String ANDROID_PUSH_SENDER_ID = "YOUR SENDER ID";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String trackingDistinctId = getTrackingDistinctId();

        setContentView(R.layout.activity_main);

        // Initialize the Mixpanel library for tracking and push notifications.
        mMixpanel = MixpanelAPI.getInstance(this, MIXPANEL_API_TOKEN);

        // We also identify the current user with a distinct ID, and
        // register ourselves for push notifications from Mixpanel.

        mMixpanel.identify(trackingDistinctId); //this is the distinct_id value that
        // will be sent with events. If you choose not to set this,
        // the SDK will generate one for you

        mMixpanel.getPeople().identify(trackingDistinctId); //this is the distinct_id
        // that will be used for people analytics. You must set this explicitly in order
        // to dispatch people data.

        // The length of a session is defined as the time between a call to startSession() and a
        // call to endSession() after which there is not another call to startSession() for at
        // least 15 seconds. If a session has been started and another startSession() function is
        // called, it is a no op. getSessionLength() returns the session time in milliseconds
        this._sessionManager = SessionManager.getInstance(this, new SessionManager.SessionCompleteCallback() {
            @Override
            public void onSessionComplete(SessionManager.Session session) {
                try {
                    JSONObject j = new JSONObject();
                    j.put("ms length", session.getSessionLength());
                    mMixpanel.track("Session Ended", j);
                } catch (JSONException j) {
                    Log.e(LOGTAG, "Failed to track session time", j);
                }
            }
        });

        // Begin the registration process for GCM. In this advanced implementation, we do not call
        // the Mixpanel helper function mMixpanel.initPushHandling(). Instead, we will use a custom
        // CustomGCMReceiver which gives you greater control over what you can do with the push
        // notification data.
        registerInBackground();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // By placing startSession() in onResume and endSession in onPause() for all your activities,
        // we allow _sessionManager to properly accumulate time for a session.
        this._sessionManager.startSession();

        // If you have surveys or notifications, and you have set AutoShowMixpanelUpdates set to false,
        // the onResume function is a good place to call the functions to display surveys or
        // in app notifications. If you would like to control exactly which notification or survey
        // you would like to show at a given time, you may use the showNotificationById
        // or the showSurveyById methods. The ID for a notification may be found in the URL in the
        // notification builder. The ID for a survey may be found in the URL after clicking on a survey
        // in the Surveys tab.
        mMixpanel.getPeople().showSurveyById(1234, this);
        mMixpanel.getPeople().showNotificationById(5678, this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // By placing startSession() in onResume and endSession in onPause() for all your activities,
        // we allow _sessionManager to properly accumulate time for a session.
        this._sessionManager.endSession();
    }

    // Associated with the "Send to Mixpanel" button in activity_main.xml
    // In this method, we update a Mixpanel people profile using MixpanelAPI.People.set()
    // and set some persistent properties that will be sent with
    // all future track() calls using MixpanelAPI.registerSuperProperties()
    public void sendToMixpanel(final View view) {

        final EditText firstNameEdit = (EditText) findViewById(R.id.edit_first_name);
        final EditText lastNameEdit = (EditText) findViewById(R.id.edit_last_name);
        final EditText emailEdit = (EditText) findViewById(R.id.edit_email_address);

        final String firstName = firstNameEdit.getText().toString();
        final String lastName = lastNameEdit.getText().toString();
        final String email = emailEdit.getText().toString();

        final MixpanelAPI.People people = mMixpanel.getPeople();

        // Update the basic data in the user's People Analytics record.
        // Unlike events, People Analytics always stores the most recent value
        // provided.
        people.set("$first_name", firstName);
        people.set("$last_name", lastName);
        people.set("$email", email);

        // We also want to keep track of how many times the user
        // has updated their info.
        people.increment("Update Count", 1L);

        // Mixpanel events are separate from Mixpanel people records,
        // but it might be valuable to be able to query events by
        // user domain (for example, if they represent customer organizations).
        //
        // We use the user domain as a superProperty here, but we call registerSuperProperties
        // instead of registerSuperPropertiesOnce so we can overwrite old values
        // as we get new information.
        try {
            final JSONObject domainProperty = new JSONObject();
            domainProperty.put("user domain", domainFromEmailAddress(email));
            mMixpanel.registerSuperProperties(domainProperty);
        } catch (final JSONException e) {
            throw new RuntimeException("Cannot write user email address domain as a super property");
        }

        // In addition to viewing the updated record in mixpanel's UI, it might
        // be interesting to see when and how many and what types of users
        // are updating their information, so we'll send an event as well.
        // You can call track with null if you don't have any properties to add
        // to an event (remember all the established superProperties will be added
        // before the event is dispatched to Mixpanel)
        mMixpanel.track("update info button clicked", null);
    }

    // This is an example of how you can use Mixpanel's revenue tracking features from Android.
    public void recordRevenue(final View view) {
        final MixpanelAPI.People people = mMixpanel.getPeople();
        // Call trackCharge() with a floating point amount
        // (for example, the amount of money the user has just spent on a purchase)
        // and an optional set of properties describing the purchase.
        people.trackCharge(1.50, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // To preserve battery life, the Mixpanel library will store
        // events rather than send them immediately. This means it
        // is important to call flush() to send any unsent events
        // before your application is taken out of memory.
        mMixpanel.flush();
    }

    ////////////////////////////////////////////////////

    public void setBackgroundImage(final View view) {
        final Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, PHOTO_WAS_PICKED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (PHOTO_WAS_PICKED == requestCode && null != data) {
            final Uri imageUri = data.getData();
            if (null != imageUri) {
                // AsyncTask, please...
                final ContentResolver contentResolver = getContentResolver();
                try {
                    final InputStream imageStream = contentResolver.openInputStream(imageUri);
                    System.out.println("DRAWING IMAGE FROM URI " + imageUri);
                    final Bitmap background = BitmapFactory.decodeStream(imageStream);
                    getWindow().setBackgroundDrawable(new BitmapDrawable(getResources(), background));
                } catch (final FileNotFoundException e) {
                    Log.e(LOGTAG, "Image apparently has gone away", e);
                }
            }
        }
    }

    private void registerInBackground() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(MainActivity.this);
                    String regid = gcm.register(ANDROID_PUSH_SENDER_ID);

                    // Once you have the registration ID for this device, you need to send it to
                    // Mixpanel so that we can send push notifications to this device on your behalf.
                    mMixpanel.getPeople().setPushRegistrationId(regid);

                    // In your app, you may continue to send the send the registration ID to your own
                    // backend and persist it on the device somehow.
                    // sendRegistrationIdToBackend();
                    // storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    Log.e(LOGTAG, "Error registering for GCM", ex);
                }

                return null;
            }
        }.execute(null, null, null);
    }

    private String getTrackingDistinctId() {
        final SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        String ret = prefs.getString(MIXPANEL_DISTINCT_ID_NAME, null);
        if (ret == null) {
            ret = generateDistinctId();
            final SharedPreferences.Editor prefsEditor = prefs.edit();
            prefsEditor.putString(MIXPANEL_DISTINCT_ID_NAME, ret);
            prefsEditor.commit();
        }

        return ret;
    }

    // These disinct ids are here for the purposes of illustration.
    // In practice, there are great advantages to using distinct ids that
    // are easily associated with user identity, either from server-side
    // sources, or user logins. A common best practice is to maintain a field
    // in your users table to store mixpanel distinct_id, so it is easily
    // accesible for use in attributing cross platform or server side events.
    private String generateDistinctId() {
        final Random random = new Random();
        final byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        return Base64.encodeToString(randomBytes, Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private String domainFromEmailAddress(String email) {
        String ret = "";
        final int atSymbolIndex = email.indexOf('@');
        if ((atSymbolIndex > -1) && (email.length() > atSymbolIndex)) {
            ret = email.substring(atSymbolIndex + 1);
        }

        return ret;
    }

    private MixpanelAPI mMixpanel;
    private SessionManager _sessionManager;
    private static final String MIXPANEL_DISTINCT_ID_NAME = "Mixpanel Example $distinctid";
    private static final int PHOTO_WAS_PICKED = 2;
    private static final String LOGTAG = "Mixpanel Example Application";
}
