
package com.example.icourseraterproject;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class CourseRaterService extends Service {

    String userEmail;
    int NOTIFICATION_ID = 2;
    int NOTIFICATION_department = 2;
    int NOTIFICATION_courses= 2;
    int NOTIFICATION_reviews= 2;
    FirebaseFirestore db; //TS member variable
    String userName;
    boolean newCourse;
    private Timer timer,reviewtimer,coursetimer;
    Boolean departmentNotfication;
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    SharedPreferences sharedpref;
    int currentDeptmartment=0;
    int newDeptmartment=0;

    @Override
    public void onCreate() {
        Log.d("News reader", "Service created");
        db = FirebaseFirestore.getInstance(); //TS in onCreate
        sharedpref=getSharedPreferences("Sharedpref",MODE_PRIVATE);
        userEmail=sharedpref.getString("userEmail","");
        db.collection("Departments")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                currentDeptmartment++;

                            }
                        } else {
                            Log.d("CMP354:", "Error getting documents: ",
                                    task.getException());
                        }
                        Log.d("Check:", currentDeptmartment+"");
                    }
                });

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("News reader", "Service started");
        //TS: display a notification here to inform the use of the foreground service
        //This is a must in foreground services
        //This notification cannot be cleared by the user until the service is killed
        //
        //Must add the following persmission to manifest as well
        //<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
        //
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, DepartmentsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText("Foreground Service Example in Android")
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .setOngoing(true) //sticky notification
                .build();

        startForeground(NOTIFICATION_ID, notification);

        //TS: when the system attemps to re-create the service
        //onStartCommand will be called again (not onCreate)
        //So call the startTimer() here
        startTimer();
        return START_STICKY;
    }
    private void createNotificationChannel()
    {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,"Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("News reader", "Service bound - not used!");
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d("News reader", "Service destroyed");
        stopTimer();
    }

    private void startTimer() {
        TimerTask task = new TimerTask()
        {

            @Override
            public void run() {

                checkforDepartmentNotfication();
                checkforCourseNotfication();
                checkforReviewNotfication();


/// end of timertask
            }
        };

        timer = new Timer(true);
        int delay = 1000*20;//1000 * 60 * 60;      // 1 hour
        int interval = 1000*20;//1000 * 60 * 60;   // 1 hour
        timer.schedule(task, delay, interval);
    }
    private void checkforDepartmentNotfication() {
        DocumentReference docRef = db.collection("Users").document(userEmail);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                departmentNotfication = documentSnapshot.getBoolean("departmentnotfication");
                if(departmentNotfication)
                {
                    checkforDepartmentUpdates();
                }
            }
        });
    }

    public void checkforDepartmentUpdates() {

        newDeptmartment=0;
        db.collection("Departments")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                newDeptmartment++;
                            }
                        } else {
                            Log.d("CMP354:", "Error getting documents: ",
                                    task.getException());
                        }
                        Log.d("Check:", currentDeptmartment+" "+newDeptmartment);
                        if(newDeptmartment>currentDeptmartment)
                        {
                            sendNotificationDepartment();
                            currentDeptmartment=newDeptmartment;

                        }
                    }
                });
    }

    private void checkforCourseNotfication() {
        db.collection("Departments")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String departmentName = document.getId();
                                ArrayList<String> list = (ArrayList<String>) document.get("notificationList");

                                if(list.contains(userEmail))
                                {
                                    db.collection("Departments").document(departmentName)
                                            .collection("Courses")
                                            .whereEqualTo("isNew", true)
                                            .get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        newCourse=true;
                                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                                            String courseName = document.getString("name");
                                                            sendNotificationCourse(document.getId(),departmentName,courseName);
                                                        }

                                                    } else {
                                                        Log.d("CMP354:", "Error getting documents: ",
                                                                task.getException());
                                                    }

                                                }
                                            });
                                }
                                ///


                                    //////

                            }
                        } else {
                            Log.d("CMP354:", "Error getting documents: ",
                                    task.getException());
                        }
                    }
                });
    }

    private void checkforReviewNotfication() {
        db.collection("Departments")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for(QueryDocumentSnapshot dept : task.getResult())
                        {
                            String deptname=dept.getId();
                            db.collection("Departments").document(deptname)
                                    .collection("Courses")
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            for(QueryDocumentSnapshot course :task.getResult())
                                            {
                                                String coursecode = course.getId();
                                                db.collection("Departments").document(deptname)
                                                        .collection("Courses").document(coursecode)
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                DocumentSnapshot coursedocument = task.getResult();
                                                                ArrayList<String> list = (ArrayList<String>) coursedocument.get("notificationList");
                                                                if(list.contains(userEmail))
                                                                {
                                                                    db.collection("Departments").document(deptname)
                                                                            .collection("Courses").document(coursecode)
                                                                            .collection("Reviews")
                                                                            .whereEqualTo("isNew", true)
                                                                            .get()
                                                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                                    if (task.isSuccessful()) {
                                                                                        for (QueryDocumentSnapshot review : task.getResult()) {
                                                                                            String reviewname=review.get("name").toString();
                                                                                            String reviewemail = review.get("email").toString();
                                                                                            if(!reviewemail.equals(userEmail))
                                                                                            {
                                                                                                sendNotificationReview(coursecode,reviewname,deptname);
                                                                                            }
                                                                                        }

                                                                                    } else {
                                                                                        Log.d("CMP354:", "Error getting documents: ",
                                                                                                task.getException());
                                                                                    }

                                                                                }
                                                                            });
                                                                }
                                                                ////
                                                                //////

                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }
    public void sendNotificationDepartment() {
        // create the intent for the notification
        Intent notificationIntent = new Intent(this, DepartmentsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // create the pending intent
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, flags);

        // create the variables for the notification
        int icon = R.drawable.logo;
        CharSequence tickerText = "News update available!";
        CharSequence contentTitle = "New Department Added";
        CharSequence contentText = "Department was added";

        /*
        //or display it with a timestamp
        Date date = new Date(System.currentTimeMillis());
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+4"));
        String dateFormatted = formatter.format(date);
        CharSequence contentText = text + " @: " + dateFormatted;
        */

        NotificationChannel notificationChannel =
                new NotificationChannel("Channel_ID", "My Notifications", NotificationManager.IMPORTANCE_HIGH);

        NotificationManager manager = (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(notificationChannel);


        // create the notification and set its data
        Notification notification = new NotificationCompat
                .Builder(this, "Channel_ID")
                .setSmallIcon(icon)
                .setTicker(tickerText)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setChannelId("Channel_ID")
                .build();
        manager.notify(NOTIFICATION_ID++, notification);

    }
    public void sendNotificationCourse(String courseCode, String departmentName,String courseName) {
        // create the intent for the notification
        Intent notificationIntent = new Intent(this, DepartmentsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // create the pending intent
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, flags);

        // create the variables for the notification
        int icon = R.drawable.logo;
        CharSequence tickerText = "News update available!";
        CharSequence contentTitle = "New Course Added";
        CharSequence contentText = "New Course ("+ courseCode+ " || " + courseName+") was added in Department " +departmentName;

        /*
        //or display it with a timestamp
        Date date = new Date(System.currentTimeMillis());
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+4"));
        String dateFormatted = formatter.format(date);
        CharSequence contentText = text + " @: " + dateFormatted;
        */

        NotificationChannel notificationChannel =
                new NotificationChannel("Channel_ID", "My Notifications", NotificationManager.IMPORTANCE_HIGH);

        NotificationManager manager = (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(notificationChannel);


        // create the notification and set its data
        Notification notification = new NotificationCompat
                .Builder(this, "Channel_ID")
                .setSmallIcon(icon)
                .setTicker(tickerText)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setChannelId("Channel_ID")
                .build();
        NOTIFICATION_courses++;
        manager.notify(NOTIFICATION_courses, notification);
        //////////



        //////////

    }
    public void sendNotificationReview(String courseCode,String userName,String deptname) {
        // create the intent for the notification
        Intent notificationIntent = new Intent(this, DepartmentsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // create the pending intent
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, flags);

        // create the variables for the notification
        int icon = R.drawable.logo;
        CharSequence tickerText = "News update available!";
        CharSequence contentTitle = "New Review Added";
        CharSequence contentText = "New Review was added in Course " +courseCode +" by "+userName;

        /*
        //or display it with a timestamp
        Date date = new Date(System.currentTimeMillis());
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+4"));
        String dateFormatted = formatter.format(date);
        CharSequence contentText = text + " @: " + dateFormatted;
        */

        NotificationChannel notificationChannel =
                new NotificationChannel("Channel_ID", "My Notifications", NotificationManager.IMPORTANCE_HIGH);

        NotificationManager manager = (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(notificationChannel);


        // create the notification and set its data
        Notification notification = new NotificationCompat
                .Builder(this, "Channel_ID")
                .setSmallIcon(icon)
                .setTicker(tickerText)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setChannelId("Channel_ID")
                .build();
        NOTIFICATION_reviews++;
        manager.notify(NOTIFICATION_reviews, notification);

        //////////

        //////////

    }
}
