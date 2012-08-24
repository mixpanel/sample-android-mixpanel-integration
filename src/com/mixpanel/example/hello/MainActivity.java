package com.mixpanel.example.hello;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
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

public class MainActivity extends Activity {

	public static final String MIXPANEL_API_TOKEN = "YOUR API TOKEN HERE";
	public static final String MIXPANEL_DISTINCT_ID_NAME = "Mixpanel $distinctid";
	
    // Called when the user clicks the "OK" button
    public void okClicked(View view) {
    	try {
    		JSONObject properties = new JSONObject();
    		properties.put("hour of the day", hourOfTheDay());
    		mMPMetrics.track("Information Updated", properties);
    	} catch(JSONException e) {
    		throw new RuntimeException("Could not encode form values as JSON");
    	}
    		
    	EditText firstNameEdit = (EditText) findViewById(R.id.edit_first_name);
    	EditText lastNameEdit = (EditText) findViewById(R.id.edit_last_name);
    	EditText emailEdit = (EditText) findViewById(R.id.edit_email_address);
    	
    	String firstName = firstNameEdit.getText().toString();
    	String lastName = lastNameEdit.getText().toString();
    	String email = emailEdit.getText().toString();
    	
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
    }  
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        String trackingDistinctId = getTrackingDistinctId();
        
        mMPMetrics = MPMetrics.getInstance(this, MIXPANEL_API_TOKEN);
        mMPMetrics.identify(trackingDistinctId);
        
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
    	
    	try {
    		JSONObject properties = new JSONObject();
    		properties.put("hour of the day", hourOfTheDay());
    		mMPMetrics.track("App Resumed", properties);
    	} catch(JSONException e) {
    		throw new RuntimeException("Could not encode form values as JSON");
    	}
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	mMPMetrics.flushAll();
    }

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
    
    // This is just an example of how we might generate disinct ids for users.
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
    
    MPMetrics mMPMetrics;
}
