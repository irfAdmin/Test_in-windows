package de.kai_morich.simple_usb_terminal;

import static java.security.AccessController.getContext;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.Manifest;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {
    //step 4 begin
    //private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private LocationManager locationManager;
    private Location currentLocation;

    public static String vtuId ="TL6RkXV1hN4";
    //TL6RkXV1hN4 PL:4NR24jR60fR85aG20 Off: 17
    public static String dataPayload = "4nR24jR60fR85aG20";
    public static int offset =10;
    private String pushCode;
    private double previousDistance;

    private TextView locationText;
    private TextView genText;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long MIN_TIME_BW_UPDATES = 2000; // 2 seconds
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 0 meters

    //stage 3 for pop up
    private View popupView;
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    private CountDownTimer leftTimer, middleTimer, rightTimer;
    private TextView leftTimerTextView, middleTimerTextView, rightTimerTextView;
    private View leftView, middleView, rightView;
    char colorcode = 'x';
    char flagD;
    //methods in MainActivity
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            currentLocation = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
        }
    };
    //private TextView locationText;

    //step 4 end
    @SuppressLint("SetTextI18n")
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        //import place
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        //locationText.setText("on Create");


        setSupportActionBar(toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        if (savedInstanceState == null)
            getSupportFragmentManager().beginTransaction().add(R.id.fragment, new DevicesFragment(), "devices").commit();
        else
            onBackStackChanged();

        //st4
        locationText = findViewById(R.id.myTextView);
        genText = findViewById(R.id.myLocView);
        Button clickButton = findViewById(R.id.myButton);  //temp for test

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            requestLocationUpdates();
        }


        clickButton.setOnClickListener(v -> {

            // click 🔎 ⭕

            showPopup("G90");
            //getPopupCode();
            /*
            if(pushCode == null) {
                showPopup("G90");
                Toast.makeText(MainActivity.this, "VTU issue", Toast.LENGTH_SHORT).show();
            }
            else
                showPopup(pushCode);
            */


        });

        // Start polling for pushCode updates
        /*
        Handler handler = new Handler();
        Runnable checkPushCode = new Runnable() {
            @Overridep
            public void run() {
                if (pushCode != null) {
                    //myButton.performClick(); // simulate button click
                    //pushCode = null; // reset if needed
                    Toast.makeText(MainActivity.this, "do .popUp", Toast.LENGTH_SHORT).show();
                    //call popup
                    showPopup(pushCode);
                    //pushCode = null;
                }
                handler.postDelayed(this, 500); // repeat every 500ms
            }
        };
        handler.post(checkPushCode);
        */
    } //on create

    // important one
    private void getPopupCode() {
        //121-182
        locationText.setText("VTU :" + vtuId);
        locationText.append("\nData: " + dataPayload);
        locationText.append("\nOffset: " + offset);
        //locationText.append("\nPassing Code " + pushCode);
        double curLat =0;
        double curLon = 0;
        double curAlt = 0;

        //find cur location
        if (currentLocation != null) {
            curLat = roundTo5(currentLocation.getLatitude());
            curLon = roundTo5(currentLocation.getLongitude());
            curAlt = roundTo5(currentLocation.getAltitude());
            genText.setText("Cur Loc "+ curLat + ", " + curLon);
        } else {
            Toast.makeText(MainActivity.this, "Location not available", Toast.LENGTH_SHORT).show();
        }

        double fixLat = decodeBase62(vtuId.substring(2,6));
        double fixLon = decodeBase62(vtuId.substring(6,10));
        genText.append(" Fix "+ vtuId.substring(2,6) + ", " + vtuId.substring(6,10));


        // Calculate distance ✏️
        float[] result = new float[2];
        Location.distanceBetween(fixLat, fixLon, curLat, curLon, result);
        //distance = roundTo5(result[0]);
        double distance = Math.round(result[0] * 10.0) / 10.0;
        double bearing = roundTo5(result[1]);

        //Toast.makeText(MainActivity.this, "Dist : " + distance + "Bearing : " + bearing, Toast.LENGTH_SHORT).show();
        // toasted dist and bearing dist is too gib may be due to geo location  ✅

        // Calculate heading ✏️
        int headingIndex = (int) (((bearing + 11.25 + 360) % 360) / 22.5);
        headingIndex = Math.min(Math.max(headingIndex, 0), 15); // Clamp to 0–15
        char headingChar = (char) ('a' + headingIndex);

        //Toast.makeText(MainActivity.this, "HeadIndex : " + headingChar , Toast.LENGTH_SHORT).show();
        //toasted e it is correct

        // Check if distance increased or decreased
        String sign = "P";
        if (previousDistance > 0) {
            if (distance < previousDistance) {
                sign = "+"; // approaching
            } else
            if (distance > previousDistance) {
                sign = "-"; // moving away
            }
        }

        String heading = sign + headingChar;
        previousDistance = distance;

        //Toast.makeText(MainActivity.this, "Heading CodeDist : " + heading , Toast.LENGTH_SHORT).show();
        // toasted -e correct  ✅

        findPushCode(heading);  //set pushCode
        locationText.append("\nPush Code: " + pushCode);
        //showPopup("G90");

    }

    private double decodeBase62(String base62Str) {
        // Convert Base-62 to decimal

            String base62Chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
            int result = 0;
            for (char c : base62Str.toCharArray()) {
                result = result * 62 + base62Chars.indexOf(c);
            }
            return result/100000.0;

    }

    private void findPushCode(String heading) {
        //find G50 like code from datapayload ⭕⭕
        char char1 = heading.charAt(0);
        char char2 = heading.charAt(1);
        int index = dataPayload.indexOf(char2);
        char colorCode = dataPayload.charAt(index + 1);
        int time = 0;

        if (index > 0) {
            //pushCode = null;

            StringBuilder numberBuilder = new StringBuilder();

            for (int i = index + 2; i < dataPayload.length(); i++) {
                char c = dataPayload.charAt(i);
                if (Character.isDigit(c)) {
                    numberBuilder.append(c);
                } else {
                    break;
                }
            }

            //toast check for char 2 and colorCode and time
            // handle last number if string ends with digits
            if (numberBuilder.length() > 0) {
                int num = Integer.parseInt(numberBuilder.toString());
                time = num - offset;
            }

            //Toast.makeText(MainActivity.this, "Char2: " + char2 + " Color: " + colorCode + " Time: "+ time  + " Off: " + offset, Toast.LENGTH_SHORT).show();
            // toasted -e correct  ✅


            pushCode = colorCode + String.valueOf(time);
        } else
            pushCode = null;
        locationText.append("\nHead Index code: " + heading + " "+ index);
        Toast.makeText(MainActivity.this, "PushCode_250 " + pushCode , Toast.LENGTH_SHORT).show();
        // toasted -e correct  ✅
        //return colorCode +  String.valueOf(time);
    }

    @Override
    public void onBackStackChanged() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount()>0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(intent.getAction())) {
            TerminalFragment terminal = (TerminalFragment)getSupportFragmentManager().findFragmentByTag("terminal");
            if (terminal != null)
                terminal.status("USB device detected");
        }
        super.onNewIntent(intent);
    }

    //step 5
    @SuppressLint("SetTextI18n")
    private void startGps() {
        // Check if GPS is enabled
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                locationText.setText("GPS is ON");

                return;
            }
            locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 1000 , 1, locationListener);
            locationText.setText("GPS is ON1");
        } else {
            locationText.setText("GPS is disabled.");
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //st6
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length >= 2 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                locationText.setText("Loc permission Granted.");
            startGps();
        } else {
            locationText.setText("Loc permission denied.");
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // block from GPS geo test GT app
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
            Toast.makeText(MainActivity.this, "GPS issue", Toast.LENGTH_SHORT).show();
        }
        // block from VTU
        if (pushCode == null) {
            Toast.makeText(MainActivity.this, "VTU issue", Toast.LENGTH_SHORT).show();
        }
        // block from popup TL4
        //if (popupView != null) {
        //    windowManager.removeView(popupView);
        //}
    }

    private void requestLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private double roundTo5(double value) {
        return Math.round(value * 100000.0) / 100000.0;
    }

 // stage 3 code for pop up
 //*************************************************************************

 // Method to show the popup window and handle the input for color and timer
 private void showPopup(String input) {

     Context context = this; // or getActivity() if inside Fragment

     if (!Settings.canDrawOverlays(context)) {
         Settings.canDrawOverlays(context);
         Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                 Uri.parse("package:" + context.getPackageName()));
         context.startActivity(intent); // opens the system settings
         Toast.makeText(context, "Please grant overlay permission", Toast.LENGTH_SHORT).show();
         return;
     }

     if (popupView != null) {
         windowManager.removeView(popupView);
     }

     //✅⬇️
     Toast.makeText(MainActivity.this, "Popup #348", Toast.LENGTH_SHORT).show();

     Log.d("Show pop up", "test1");
     LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
     popupView = inflater.inflate(R.layout.popup_tl4, null);
     Log.d("Show pop up", "test2");
     // Set up WindowManager for overlay
     windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
     layoutParams = new WindowManager.LayoutParams(
             WindowManager.LayoutParams.WRAP_CONTENT,
             WindowManager.LayoutParams.WRAP_CONTENT,
             WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,  // Overlay type for top-most window
             WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,        // Doesn't take focus away from other apps
             WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
     );
     Log.d("Show pop up", "test3");
     layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
     layoutParams.x = 20; // Move left from the right edge (right margin)
     layoutParams.y = 10; // Move down from the top edge (top margin)
     windowManager.addView(popupView, layoutParams);
     // Set margin from the top and right (in pixels)


     // Initialize Views and TextViews
     leftView = popupView.findViewById(R.id.left_rowView);
     middleView = popupView.findViewById(R.id.middle_rowView);
     rightView = popupView.findViewById(R.id.right_rowView);

     leftTimerTextView = popupView.findViewById(R.id.leftTimer);
     middleTimerTextView = popupView.findViewById(R.id.middleTimer);
     rightTimerTextView = popupView.findViewById(R.id.rightTimer);

     // Apply blinking animation to left view
     Animation blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.blink);
     leftView.startAnimation(blinkAnimation);
     Log.d("Show pop up", "test4");
     //temp commented
     /*
     middleView.setOnClickListener(v -> {
         if (popupView != null) {
             windowManager.removeView(popupView); // Remove the popup
         }
         finish(); // Close the activity
     });  */

     //Toast.makeText(MainActivity.this, "Popup #392", Toast.LENGTH_SHORT).show();
     // Process the input string (e.g., "R20,G40,Y60")
     handleInput(input);
     Log.d("Show pop up", "test5");
     // on click of middle view drop down pop up
     middleView.setOnClickListener(v -> {
         if (popupView != null) {
             windowManager.removeView(popupView); // Remove the popup
         }
         finish(); // Close the activity
     });
 }

    // Method to handle the input string and apply color and timers
    private void handleInput(String input) {
        // added code for (TL4) convert input from one to three
        colorcode = input.charAt(0);
        String time = input.substring(1);
        switch (colorcode) {
            case 'G':
                input = "g" + time + ",G" + time + ",G" + time;
                break;
            case 'R':
                input = "g" + time + ",R" + time + ",R" + time;
                break;
            case 'Y':
                input = "g" + time + ",Y" + time + ",Y" + time;
                break;
        }

        // TL4 code is over
        // Cancel any existing timers
        if (leftTimer != null) leftTimer.cancel();
        if (middleTimer != null) middleTimer.cancel();
        if (rightTimer != null) rightTimer.cancel();
        // now input will be "
        // Split the input string (e.g., "R20,G40,Y60")
        // new code for TL4 input will be R20 or G30
        String[] parts = input.split(",");
        int colorIndex = 0;

        for (String part : parts) {
            char colorCode = part.charAt(0);  // Get the color character (e.g., 'R', 'G', 'Y')
            int timerValue = Integer.parseInt(part.substring(1));  // Get the timer value (e.g., 20, 60, 30)

            switch (colorIndex) {
                case 0:
                    applyColorAndStartTimer(leftView, leftTimerTextView, colorCode, timerValue);
                    break;
                case 1:
                    applyColorAndStartTimer(middleView, middleTimerTextView, colorCode, timerValue);
                    break;
                case 2:
                    applyColorAndStartTimer(rightView, rightTimerTextView, colorCode, timerValue);
                    break;
            }

            colorIndex++;
        }
    }

    // Method to apply the color and start the timer for a specific view and textView
    private void applyColorAndStartTimer(View view, TextView timerTextView, char colorCode, int timerValue) {
        int color = Color.RED;
        boolean shouldFlash = false;

        // Determine the color and whether to apply flashing effect
        switch (colorCode) {
            case 'R':
                color = Color.RED;
                break;
            case 'G':
                color = Color.GREEN;
                break;
            case 'Y':
                color = Color.YELLOW;
                break;
            case 'r':  // Flashing effect for lowercase 'r'
                color = Color.RED;
                shouldFlash = true;
                break;
            case 'g':  // Flashing effect for lowercase 'g'
                color = Color.GREEN;
                shouldFlash = true;
                break;
            case 'y':  // Flashing effect for lowercase 'y'
                color = Color.YELLOW;
                shouldFlash = true;
                break;
        }

        // Update the view's color
        view.setBackgroundTintList(ColorStateList.valueOf(color));

        // Apply the flashing effect if lowercase letter is detected
        if (shouldFlash) {
            startFlashingEffect(view);
        } else {
            stopFlashingEffect(view);
        }

        // Set up the countdown timer for the TextView
        CountDownTimer timer = new CountDownTimer(timerValue * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                //added for TL4
                long timeDisp = millisUntilFinished / 1000 + 1;
                //this for test
                //flagD should be set from outside if index is mismatch BAindex and heading index or distance less than 2
                //if (timeDisp == 10)
                //    flagD = '0';
                //timerTextView.setText(String.valueOf(millisUntilFinished / 1000));
                if (timeDisp > 0)
                    timerTextView.setText(String.valueOf(timeDisp));
                if (flagD == '0'){
                    flagD = 'X';
                    Toast.makeText(view.getContext(), "Now OFF", Toast.LENGTH_SHORT).show();
                    //windowManager.removeView(popupView);
                }


            }


            public void onFinish() {
                timerTextView.setText("0");

                Toast.makeText(view.getContext(), "Node " + colorcode , Toast.LENGTH_SHORT).show();
                //middleView.getSolidColor()

                if (colorcode == 'G')
                    handleInput("Y5");
                else{
                    // Fetch new input from the main activity when timer reaches 0
                    //String newInput = inputText.getText().toString();
                    String newInput = pushCode;
                    if (!newInput.isEmpty()) {
                        handleInput(newInput);
                    }
                }

            }
        };

        // Start the timer
        timer.start();

        // Assign the timer to the corresponding member variable
        if (view == leftView) {
            leftTimer = timer;
        } else if (view == middleView) {
            middleTimer = timer;
        } else if (view == rightView) {
            rightTimer = timer;
        }
    }
    private void startFlashingEffect(View view) {
        Animation blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.blink);
        view.startAnimation(blinkAnimation);
    }

    private void stopFlashingEffect(View view) {
        view.clearAnimation();  // Stop any animation
    }


    /*
    //merge in above onDestroy
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (popupView != null) {
            windowManager.removeView(popupView);
        }
    }
    */

}
