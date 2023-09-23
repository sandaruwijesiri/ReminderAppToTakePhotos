package com.example.photoreminder;

import static android.content.Context.ALARM_SERVICE;
import static com.example.photoreminder.MainActivity.SHARED_PREFERENCES_FILE_NAME;
import static com.example.photoreminder.MainActivity.durationInSeconds;
import static com.example.photoreminder.MyBroadcastReceiver.durationOffset;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class Methods {

    public static int JOB_ID = 0;
    public static PendingIntent alarmPendingIntent;

    public static void scheduleJob(Context context){

        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREFERENCES_FILE_NAME,Context.MODE_PRIVATE);
        int hours = sharedPref.getInt("IntervalHours", -1);
        int minutes = sharedPref.getInt("IntervalMinutes", -1);
        int from = sharedPref.getInt("From", -1);
        int to = sharedPref.getInt("To", -1);

        if (hours<0 || minutes<0){
            durationInSeconds = -1;
        }else {
            durationInSeconds = 3600*hours + 60*minutes;
        }

        int totalDurationInSeconds = durationInSeconds + durationOffset;
        durationOffset=0;
        if (totalDurationInSeconds>0 && from>=0 && to>=0) {


            Calendar calendar = Calendar.getInstance();
            int rightNow = calendar.get(Calendar.HOUR_OF_DAY)*100 + calendar.get(Calendar.MINUTE);
            calendar.add(Calendar.SECOND,totalDurationInSeconds);
            int timeScheduled = calendar.get(Calendar.HOUR_OF_DAY)*100 + calendar.get(Calendar.MINUTE);

            int setJobInThisManySeconds;

            if (from==to){
                setJobInThisManySeconds = totalDurationInSeconds;
            }else {
                if (from <= to) {
                    if (from <= timeScheduled && timeScheduled <= to) {
                        setJobInThisManySeconds = totalDurationInSeconds;
                    } else {
                        setJobInThisManySeconds = getTimeLeftToFromTime(rightNow, from);
                    }
                } else {
                    if ((from <= timeScheduled && timeScheduled <= 2400) || (0 <= timeScheduled && timeScheduled <= to)) {
                        setJobInThisManySeconds = totalDurationInSeconds;
                    } else {
                        setJobInThisManySeconds = getTimeLeftToFromTime(rightNow, from);
                    }
                }
            }

            Intent alarmIntent = new Intent(context,MyBroadcastReceiver.class);
            alarmIntent.setAction("com.example.photoreminder AlarmGoingOff");
            alarmPendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            alarmManager.setExact(AlarmManager.RTC,System.currentTimeMillis()+setJobInThisManySeconds* 1000L,alarmPendingIntent);
        }

    }

    public static int getTimeLeftToFromTime(int rightNow, int from){
        if (rightNow<=from){
            return (from/100 - rightNow/100)*3600 + (from%100 - rightNow%100)*60;
        }else {
            return (24 - rightNow/100)*3600 + (-rightNow % 100)*60
                    + (from/100)*3600 + (from%100)*60;
        }
    }

    public static String[] getLastCapturedPhotoDetails(Context context){
        String[] retStringArr = new String[2];
        retStringArr[0] = null;
        retStringArr[1] = null;

        final String[] imageColumns = { MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED};
        final String imageOrderBy = MediaStore.Images.Media.DATE_ADDED + " DESC";

        ContentResolver contentResolver = context.getContentResolver();
        //Cursor imageCursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageColumns, MediaStore.Images.Media.DATA + " LIKE '%DCIM%'", null, imageOrderBy);
        //
        Bundle bundle = new Bundle();
        bundle.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, new String[]{MediaStore.Images.Media.DATE_ADDED});
        bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, 1);
        bundle.putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING);
        bundle.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, MediaStore.Images.Media.DATA + " LIKE '%DCIM%'");

        Cursor imageCursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageColumns, bundle,null);

        if (imageCursor.moveToFirst()) {
            int columnIndex = imageCursor.getColumnIndex(MediaStore.Images.Media.DATA);
            if (columnIndex >= 0) {
                int dateAddedColumnIndex = imageCursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);
                do {
                    String fullPath = imageCursor.getString(columnIndex);
                    if (fullPath.contains("DCIM")) {
                        //--last image from camera --

                        retStringArr[0] = fullPath;
                        retStringArr[1] = imageCursor.getString(dateAddedColumnIndex) + "000";  //multiplying by 1000, since otherwise it's seconds since epoch/
                        break;
                    }
                }
                while (imageCursor.moveToNext());
            }
        }
        imageCursor.close();

        return retStringArr;
    }
}
