package com.example.photoreminder;

import static com.example.photoreminder.Methods.alarmPendingIntent;
import static com.example.photoreminder.Methods.getLastCapturedPhotoDetails;
import static com.example.photoreminder.TimePickerFragment.pickWhatTime;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public static String SHARED_PREFERENCES_FILE_NAME = "AppPreferences";
    public static int durationInSeconds = -1;
    public static int fromTime = -1;
    public static int toTime = -1;

    Button grantPermissionsButton;

    public ImageView lastImageImageView;
    TextView timeTakenTextView;
    Button periodFromButton;
    Button periodToButton;

    Button setTimeIntervalButton;
    Switch onOffSwitch;
    Button openCameraButton;
    Button openGalleryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        grantPermissionsButton = findViewById(R.id.grantPermissionsButton);
        grantPermissionsButton.setOnClickListener(grantPermissionOnClickListener);

        lastImageImageView = findViewById(R.id.lastImageImageView);
        timeTakenTextView = findViewById(R.id.timeTakenTextView);
        periodFromButton = findViewById(R.id.periodFromButton);
        periodToButton = findViewById(R.id.periodToButton);
        setTimeIntervalButton = findViewById(R.id.setTimeIntervalButton);
        onOffSwitch = findViewById(R.id.onOffSwitch);
        openCameraButton = findViewById(R.id.openCameraButton);
        openGalleryButton = findViewById(R.id.openGalleryButton);

        periodFromButton.setOnClickListener(periodFromButtonOnClickListener);
        periodToButton.setOnClickListener(periodToButtonOnClickListener);

        setTimeIntervalButton.setOnClickListener(setTimeIntervalButtonOnClickListener);

        openCameraButton.setOnClickListener(openCameraOnClickListener);
        openGalleryButton.setOnClickListener(openGalleryOnClickListener);

        setUpOnOffSwitch();
        updatePeriodButtons();
        updateSetIntervalButton();

        handlePermissions();
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            handlePermissions();
        } else {
            Toast.makeText(getApplicationContext(),"App cannot function due to lack of permission",Toast.LENGTH_SHORT).show();
        }
    });

    public View.OnClickListener grantPermissionOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            handlePermissions();
        }
    };

    public void handlePermissions(){

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                if (!alarmManager.canScheduleExactAlarms()) {
                    Intent i = new Intent();
                    i.setData(Uri.parse("package:" + getPackageName()));
                    i.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivityForResult(i,24);
                } else {
                    permissionsGranted();
                }
            }else {
                permissionsGranted();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==24){
            if (resultCode==RESULT_OK){
                permissionsGranted();
            }else {
                Toast.makeText(getApplicationContext(),"App cannot function due to lack of permission",Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void permissionsGranted(){

        grantPermissionsButton.setVisibility(View.GONE);
        lastImageImageView.setVisibility(View.VISIBLE);
        cancelJobAndConsiderStartingAgain();

        String[] lastPhotoDetails = getLastCapturedPhotoDetails(getApplicationContext());

        if (lastImageImageView!=null) {
            Bitmap bitmap = BitmapFactory.decodeFile(lastPhotoDetails[0]);
            lastImageImageView.setImageBitmap(bitmap);
        }
        if (timeTakenTextView!=null){
            if (lastPhotoDetails[1]==null){
                timeTakenTextView.setText("");
            }else {
                Date date = new Date(Long.parseLong(lastPhotoDetails[1]));
                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat sdf2 = new SimpleDateFormat("hh:mm:ss");
                timeTakenTextView.setText("Taken on " + sdf1.format(date) + " at " + sdf2.format(date));
            }
        }
    }

    public void permissionNotGranted(){

        grantPermissionsButton.setVisibility(View.VISIBLE);
        lastImageImageView.setVisibility(View.GONE);
    }

    public void cancelJobAndConsiderStartingAgain(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager!=null && alarmPendingIntent!=null)
            alarmManager.cancel(alarmPendingIntent);

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
        int onOrOff = sharedPref.getInt("On/Off", -1);

        if (onOrOff != 0) {
            Methods.scheduleJob(getApplicationContext());
        }
    }

    public View.OnClickListener periodFromButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            pickWhatTime = "From";
            showTimePickerDialog();
        }
    };

    public View.OnClickListener periodToButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            pickWhatTime = "To";
            showTimePickerDialog();
        }
    };

    public void showTimePickerDialog() {
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public void updatePeriodButtons(){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
        fromTime = sharedPref.getInt("From", -1);
        toTime = sharedPref.getInt("To", -1);

        String fromTimeString;
        String toTimeString;
        if (fromTime>=0) {
            fromTimeString= "From " + String.format("%02d", fromTime / 100) + ":" + String.format("%02d", fromTime % 100);
        }else {
            fromTimeString = "Set From";
        }
        if (toTime>=0){
            toTimeString = "To " + String.format("%02d", toTime / 100) + ":" + String.format("%02d", toTime % 100);
        }else {
            toTimeString = "Set To";
        }
        periodFromButton.setText(fromTimeString);
        periodToButton.setText(toTimeString);
    }

    public View.OnClickListener setTimeIntervalButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            DialogFragment newFragment = new SetIntervalFragment();
            newFragment.show(getSupportFragmentManager(), "setInterval");
        }
    };

    public void updateSetIntervalButton(){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
        int hours = sharedPref.getInt("IntervalHours", -1);
        int minutes = sharedPref.getInt("IntervalMinutes", -1);

        if (hours<0 || minutes<0){
            setTimeIntervalButton.setText("Set Time Interval");
            durationInSeconds = -1;
        }else {
            durationInSeconds = 3600*hours + 60*minutes;
            setTimeIntervalButton.setText("Time Interval: " + String.format("%02d", hours) + ":" + String.format("%02d", minutes));
        }
    }

    public CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            int onOrOff = -1;
            if (isChecked){
                onOffSwitch.setText("On");
                onOrOff = 1;

                Methods.scheduleJob(getApplicationContext());
            }else {
                onOffSwitch.setText("Off");
                onOrOff = 0;

                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                if(alarmManager!=null && alarmPendingIntent!=null) {
                    alarmManager.cancel(alarmPendingIntent);
                }
            }
            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_FILE_NAME,Context.MODE_PRIVATE);
            sharedPref.edit().putInt("On/Off", onOrOff).apply();
        }
    };

    public void setUpOnOffSwitch(){

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
        int onOrOff = sharedPref.getInt("On/Off", -1);

        onOffSwitch.setChecked(onOrOff != 0);
        onOffSwitch.setText(onOrOff != 0 ? "On" : "Off");
        onOffSwitch.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    public View.OnClickListener openCameraOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            /*Intent intent = getPackageManager().getLaunchIntentForPackage("com.oneplus.camera");
            startActivity( intent );*/
        }
    };

    public View.OnClickListener openGalleryOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            /*Intent intent = getPackageManager().getLaunchIntentForPackage("com.oneplus.gallery");
            startActivity( intent );*/
        }
    };
}