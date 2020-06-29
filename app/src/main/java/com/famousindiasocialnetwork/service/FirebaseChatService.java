package com.famousindiasocialnetwork.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.famousindiasocialnetwork.R;
import com.famousindiasocialnetwork.activity.MessagesActivity;
import com.famousindiasocialnetwork.model.Attachment;
import com.famousindiasocialnetwork.model.AttachmentTypes;
import com.famousindiasocialnetwork.model.Chat;
import com.famousindiasocialnetwork.model.Message;
import com.famousindiasocialnetwork.model.UserRealm;
import com.famousindiasocialnetwork.util.Constants;
import com.famousindiasocialnetwork.util.FirebaseUploader;
import com.famousindiasocialnetwork.util.Helper;
import com.famousindiasocialnetwork.util.SharedPreferenceUtil;

import java.io.File;
import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;

public class FirebaseChatService extends Service {
    private static final String CHANNEL_ID_MAIN = "my_channel_01";
    private static final String CHANNEL_ID_GROUP = "my_channel_02";
    private static final String CHANNEL_ID_USER = "my_channel_03";

    private DatabaseReference chatRef;
    private Integer myId;
    private Realm rChatDb;
    private SparseArray<UserRealm> userHashMap = new SparseArray<>();
    private UserRealm userMe;
    private ArrayList<UserRealm> myUsers;
    private SharedPreferenceUtil sharedPreferenceUtil;

    public FirebaseChatService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("WeShare", "onCreate");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_MAIN, "Yoohoo chat service", NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null, null);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_MAIN)
                .setSmallIcon(R.drawable.noti_icon)
                .setContentTitle("WeShare")
                .setContentText("Chat service running")
                .setSound(null)
                .build();
        startForeground(1, notification);
        LocalBroadcastManager.getInstance(this).registerReceiver(uploadAndSendReceiver, new IntentFilter(Constants.UPLOAD_AND_SEND));
        LocalBroadcastManager.getInstance(this).registerReceiver(myUsersReceiver, new IntentFilter(Constants.BROADCAST_MY_USERS));
        LocalBroadcastManager.getInstance(this).registerReceiver(logoutReceiver, new IntentFilter(Constants.BROADCAST_LOGOUT));
    }

    private BroadcastReceiver logoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            myId = null;
            stopForeground(true);
            stopSelf();
        }
    };

    private BroadcastReceiver myUsersReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<UserRealm> myUsers = intent.getParcelableArrayListExtra("data");
            if (myUsers != null) {
                for (UserRealm userRealm : myUsers) {
                    if (userHashMap.get(userRealm.getId(), null) == null) {
                        userHashMap.put(userRealm.getId(), userRealm);
                        registerChatUpdates(true, userRealm.getId());
                    }
                }
            }
        }
    };

    private BroadcastReceiver uploadAndSendReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(Constants.UPLOAD_AND_SEND)) {
                Attachment attachment = intent.getParcelableExtra("attachment");
                int type = intent.getIntExtra("attachment_type", -1);
                String attachmentFilePath = intent.getStringExtra("attachment_file_path");
                String attachmentChatChild = intent.getStringExtra("attachment_chat_child");
                int attachmentRecipientId = intent.getIntExtra("attachment_recipient_id", -1);
                uploadAndSend(new File(attachmentFilePath), attachment, type, attachmentChatChild, attachmentRecipientId);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("WeShare", "onStartCommand");
        initVars();
        if (userMe != null) {
            myId = userMe.getId();
            rChatDb = Helper.getRealmInstance();
            if (!FetchMyUsersService.IN_PROGRESS)
                startService(new Intent(FirebaseChatService.this, FetchMyUsersService.class));
        } else {
            stopForeground(true);
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void initVars() {
        sharedPreferenceUtil = new SharedPreferenceUtil(this);
        Log.e("WeShare", "initVars");
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        chatRef = firebaseDatabase.getReference(Constants.REF_CHAT);
        Realm.init(this);

        userMe = UserRealm.fromUserResponse(Helper.getLoggedInUser(sharedPreferenceUtil));
    }

    private void restartService() {
        Log.e("WeShare", "Restart");
        if (Helper.getLoggedInUser(new SharedPreferenceUtil(this)) != null) {
            Intent intent = new Intent(this, FirebaseChatService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 99, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 500, pendingIntent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("WeShare", "onDestroy");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadAndSendReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myUsersReceiver);
        restartService();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        restartService();
        super.onTaskRemoved(rootIntent);
        Log.e("WeShare", "onTaskRemoved");
    }

    private void uploadAndSend(final File fileToUpload, final Attachment attachment, final int attachmentType, final String chatChild, final int recipientId) {
        if (!fileToUpload.exists())
            return;
        final String fileName = Uri.fromFile(fileToUpload).getLastPathSegment();
        final StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child(getString(R.string.app_name)).child(AttachmentTypes.getTypeName(attachmentType)).child(fileName);
        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                //If file is already uploaded
                Attachment attachment1 = attachment;
                if (attachment1 == null) attachment1 = new Attachment();
                attachment1.setName(fileName);
                attachment1.setUrl(uri.toString());
                attachment1.setBytesCount(fileToUpload.length());
                sendMessage(null, attachmentType, attachment1, chatChild, recipientId);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                //Elase upload and then send message
                FirebaseUploader firebaseUploader = new FirebaseUploader(new FirebaseUploader.UploadListener() {
                    @Override
                    public void onUploadFail(String message) {
                        Log.e("DatabaseException", message);
                    }

                    @Override
                    public void onUploadSuccess(String downloadUrl) {
                        Attachment attachment1 = attachment;
                        if (attachment1 == null) attachment1 = new Attachment();
                        attachment1.setName(fileToUpload.getName());
                        attachment1.setUrl(downloadUrl);
                        attachment1.setBytesCount(fileToUpload.length());
                        sendMessage(null, attachmentType, attachment1, chatChild, recipientId);
                    }

                    @Override
                    public void onUploadProgress(int progress) {

                    }

                    @Override
                    public void onUploadCancelled() {

                    }
                }, storageReference);
                firebaseUploader.uploadOthers(getApplicationContext(), fileToUpload);
            }
        });
    }

    private void sendMessage(String messageBody, @AttachmentTypes.AttachmentType int attachmentType, Attachment attachment, String chatChild, int userOrGroupId) {
        //Create message object
        Message message = new Message();
        message.setAttachmentType(attachmentType);
        if (attachmentType != AttachmentTypes.NONE_TEXT)
            message.setAttachment(attachment);
        message.setBody(messageBody);
        message.setDate(System.currentTimeMillis());
        message.setSenderId(userMe.getId());
        message.setSenderName(userMe.getName());
        message.setSent(true);
        message.setDelivered(false);
        message.setRecipientId(userOrGroupId);
        message.setId(chatRef.child(chatChild).push().getKey());

        //Add messages in chat child
        chatRef.child(chatChild).child(message.getId()).setValue(message);
    }

    private void registerChatUpdates(boolean register, Integer id) {
        if (myId != null && id != null) {
            DatabaseReference idChatRef = chatRef.child(Helper.getChatChild(myId, id));
            if (register) {
                idChatRef.addChildEventListener(chatUpdateListener);
            } else {
                idChatRef.removeEventListener(chatUpdateListener);
            }
        }
    }

    private ChildEventListener chatUpdateListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            Message message = dataSnapshot.getValue(Message.class);
            if (message != null) {
                Message result = rChatDb.where(Message.class).equalTo("id", message.getId()).findFirst();
                if (result == null && myId != null) {
                    saveMessage(message);
                    if (!myId.equals(message.getSenderId()) && !message.isDelivered())
                        chatRef.child(dataSnapshot.getRef().getParent().getKey()).child(message.getId()).child("delivered").setValue(true);
                }
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            Message message = dataSnapshot.getValue(Message.class);
            Message result = rChatDb.where(Message.class).equalTo("id", message.getId()).findFirst();
            if (result != null) {
                rChatDb.beginTransaction();
                result.setDelivered(message.isDelivered());
                rChatDb.commitTransaction();
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            Message message = dataSnapshot.getValue(Message.class);

            Helper.deleteMessageFromRealm(rChatDb, message.getId());

            int userOrGroupId = myId.equals(message.getSenderId()) ? message.getRecipientId() : message.getSenderId();
            Chat chat = Helper.getChat(rChatDb, myId, userOrGroupId).findFirst();
            if (chat != null) {
                rChatDb.beginTransaction();
                RealmList<Message> realmList = chat.getMessages();
                if (realmList.size() == 0)
                    RealmObject.deleteFromRealm(chat);
                else {
                    chat.setLastMessage(realmList.get(realmList.size() - 1).getBody());
                    chat.setTimeUpdated(realmList.get(realmList.size() - 1).getDate());
                }
                rChatDb.commitTransaction();
            }
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    private void saveMessage(Message message) {
        if (message.getAttachment() != null && !TextUtils.isEmpty(message.getAttachment().getUrl()) && !TextUtils.isEmpty(message.getAttachment().getName())) {
            String idToCompare = "loading" + message.getAttachment().getBytesCount() + message.getAttachment().getName();
            Helper.deleteMessageFromRealm(rChatDb, idToCompare);
        }

        int userOrGroupId = myId.equals(message.getSenderId()) ? message.getRecipientId() : message.getSenderId();
        Chat chat = Helper.getChat(rChatDb, myId, userOrGroupId).findFirst();
        rChatDb.beginTransaction();
        if (chat == null) {
            chat = rChatDb.createObject(Chat.class);
            chat.setUser(rChatDb.copyToRealm(userHashMap.get(userOrGroupId)));
            chat.setUserId(userOrGroupId);
            chat.setMessages(new RealmList<Message>());
            chat.setLastMessage(message.getBody());
            chat.setMyId(myId);
            chat.setTimeUpdated(message.getDate());
        }

        if (!myId.equals(message.getSenderId()))
            chat.setRead(false);
        chat.setTimeUpdated(message.getDate());
        chat.getMessages().add(message);
        chat.setLastMessage(message.getBody());
        rChatDb.commitTransaction();

        if (!message.isDelivered() && !myId.equals(message.getSenderId()) && !Helper.isUserMute(sharedPreferenceUtil, message.getSenderId()) && (Constants.CURRENT_CHAT_ID == null || !Constants.CURRENT_CHAT_ID.equals(userOrGroupId))) {
            // Construct the Intent you want to end up at
            Intent chatActivity = MessagesActivity.newIntent(this, null, chat.getUser());
            // Construct the PendingIntent for your Notification
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            // This uses android:parentActivityName and
            // android.support.PARENT_ACTIVITY meta-data by default
            stackBuilder.addNextIntentWithParentStack(chatActivity);
            PendingIntent pendingIntent = stackBuilder.getPendingIntent(99, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder notificationBuilder = null;
            String channelId = CHANNEL_ID_USER;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(channelId, "Yoohoo new message notification", NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
                notificationBuilder = new NotificationCompat.Builder(this, channelId);
            } else {
                notificationBuilder = new NotificationCompat.Builder(this);
            }

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            notificationBuilder.setSmallIcon(R.drawable.noti_icon)
                    .setContentTitle(chat.getUser().getName())
                    .setContentText(message.getBody())
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent);
            int msgId = message.getSenderId();
            notificationManager.notify(msgId, notificationBuilder.build());
        }
    }
}
