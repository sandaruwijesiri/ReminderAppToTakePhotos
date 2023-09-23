package com.example.photoreminder;

import static com.example.photoreminder.App.NOTIFICATION_CHANNEL_ID;
import static com.example.photoreminder.MainActivity.durationInSeconds;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class MyBroadcastReceiver extends BroadcastReceiver {

    int NOTIFICATION_ID = 1;
    public static int durationOffset = 0;

    ArrayList<String> notificationContent = new ArrayList<>(Arrays.asList("Don't forget to capture the little things!",
            "Remember the special moments in life!", "Tell a story with one!", "Look around and capture the beauty!", "Let your creativity flow!",
            "Freeze time with one!", "Capture something that speaks to you!", "Express yourself! Take a photo!", "Appreciate the beauty around you!",
            "Take one to share your moments with loved ones!"));

    Ringtone ringtone;

    @Override
    public void onReceive(Context context, Intent intent) {

        String intentAction = intent.getAction();
        if("com.example.photoreminder NotificationSwiped".equals(intentAction)){
            //Methods.scheduleJob(context);

            if (ringtone!=null) {
                ringtone.stop();
                Toast.makeText(context, "Swiped!!", Toast.LENGTH_SHORT).show();
            }
        }else if("com.example.photoreminder AlarmGoingOff".equals(intentAction)){
            doJob(context);
        }
    }

    public void doJob(Context context){

        long lastAdded = Long.parseLong(Methods.getLastCapturedPhotoDetails(context)[1]);
        if ((System.currentTimeMillis()-lastAdded)<durationInSeconds* 1000L){
            durationOffset = (int) (-(System.currentTimeMillis()-lastAdded)/1000);
            Methods.scheduleJob(context);
        }else {
            remind(context);
            Methods.scheduleJob(context);
        }

    }

    public void remind(Context context){

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(NOTIFICATION_ID, CreateNotification(context));

        // we will use vibrator first
        /*Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(4000);

        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        // setting default ringtone
        ringtone = RingtoneManager.getRingtone(context, alarmUri);

        // play ringtone
        ringtone.play();*/
    }

    public Notification CreateNotification(Context context){

        Intent swipeIntent = new Intent(context,MyBroadcastReceiver.class);
        swipeIntent.setAction("com.example.photoreminder NotificationSwiped");
        PendingIntent swipePendingIntent = PendingIntent.getBroadcast(context, 0, swipeIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle("Take a photo!")
                        .setContentText("It's been some time since you took a photo. " + notificationContent.get((new Random()).nextInt(notificationContent.size())))
                        .setSmallIcon(R.mipmap.ic_launcher)//.setSmallIcon(R.drawable.ic_launcher_background)
                        .setColor(Color.BLUE)
                        //.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.mipmap.ic_launcher_foreground))//.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.drawable.ic_launcher_foreground))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        // Do something when the notification is swiped away
                        .setDeleteIntent(swipePendingIntent)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        return builder.build();
    }
}
