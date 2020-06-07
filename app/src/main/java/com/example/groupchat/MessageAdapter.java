package com.example.groupchat;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Build;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.solver.widgets.Snapshot;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static java.security.AccessController.getContext;

public class MessageAdapter extends BaseAdapter{

    private Activity mActivity ;
    private DatabaseReference mDatabaseReference;
    private ArrayList<DataSnapshot> snapShotList ;
    private String displayName ;

    private static  final int appUniqueId = 10121;
    public static final String channel_id ="channel1";

    ChildEventListener mChildEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            snapShotList.add(dataSnapshot) ;
            notifyDataSetChanged();   // to refersh the list
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    } ;
    public MessageAdapter(Activity activity ,DatabaseReference ref , String name) {
        mActivity = activity ;
        mDatabaseReference = ref.child("messages") ;
        mDatabaseReference.addChildEventListener(mChildEventListener) ;
        displayName  =name ;
        snapShotList = new ArrayList<>();
    }

    static class ViewHolder {
        TextView author ;
        TextView message ;
        ImageView photo ;
        LinearLayout.LayoutParams params ;
    }

    @Override
    public int getCount() {
        return snapShotList.size();
    }

    @Override
    public FriendlyMessage getItem(int position) {
        DataSnapshot snapshot = snapShotList.get(position) ;
        return snapshot.getValue(FriendlyMessage.class) ;               // returning the msg in form of object as snapshot contain in form of json object
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, android.view.ViewGroup parent) {

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_message ,parent,false) ;

            final ViewHolder holder = new ViewHolder() ;
            holder.author = (TextView) convertView.findViewById(R.id.nameTextView) ;
            holder.message = (TextView) convertView.findViewById(R.id.messageTextView) ;
            holder.photo = (ImageView) convertView.findViewById(R.id.photoImageView) ;
            holder.params = (LinearLayout.LayoutParams) holder.author.getLayoutParams();

            convertView.setTag(holder);        // temporarily store the value in holder of each row
        }

        final FriendlyMessage friendlyMessage = getItem(position);
        final ViewHolder holder = (ViewHolder) convertView.getTag() ;     // getting the value stored temporarily

        // checking if the msg is send by user itself or by friend
        boolean isMe = friendlyMessage.getName().equals(displayName);
        setChatRowAppearance(isMe,holder);

        boolean isPhoto =friendlyMessage.getPhotoUrl() !=null; // checking if the photo is send or not

        if(isPhoto ) {
            Log.d("gchat","image :-"+friendlyMessage.getPhotoUrl());
            holder.message.setVisibility(View.INVISIBLE);
            holder.photo.setVisibility(View.VISIBLE);
            Glide.with(mActivity)
                    .load(friendlyMessage.getPhotoUrl())
                    .into(holder.photo);
          //  sendNotifications(friendlyMessage.getName() , "",friendlyMessage.getPhotoUrl());
        }
        else {
            Log.d("gchat","message will appear");
            holder.photo.setVisibility(View.INVISIBLE);
            holder.message.setVisibility(View.VISIBLE);
            holder.message.setText(friendlyMessage.getMessage());
          //  sendNotifications(friendlyMessage.getName() ,friendlyMessage.getMessage(),"");
        }
//        String messageText = friendlyMessage.getMessage();
//        holder.message.setText(messageText);

        String name = friendlyMessage.getName();
        holder.author.setText(name);

         return convertView ;
    }

    public void sendNotifications(String displayName ,String message ,String photoUrl) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mActivity,channel_id);
        builder.setSmallIcon(R.drawable.icon_notification)
                .setPriority(Notification.PRIORITY_MAX)
                .setAutoCancel(true)
                .setContentTitle(displayName)
                .setWhen(System.currentTimeMillis());

        if(photoUrl.equals(""))
            builder.setContentText(message) ;
        else builder.setContentText("an image file") ;

        Intent intent = new Intent( mActivity, MainActivity.class);
        PendingIntent actionIntent = PendingIntent.getActivity(mActivity , 0 ,
                intent ,PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(actionIntent) ;

        NotificationManager notificationManager = (NotificationManager) mActivity.getSystemService(Context.NOTIFICATION_SERVICE) ;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel notificationChannel = new NotificationChannel(channel_id , "new Channel" ,NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("This is channel 1");
            notificationManager.createNotificationChannel(notificationChannel);
            builder.setChannelId(channel_id) ;
        }

        notificationManager.notify(appUniqueId,builder.build());


    }

    public void setChatRowAppearance(boolean isMe , ViewHolder holder) {
        if(isMe) {
            holder.params.gravity = Gravity.END ;
            holder.author.setTextColor(Color.GREEN);
            holder.message.setBackgroundResource(R.drawable.bubble2);
        } else {
            holder.params.gravity = Gravity.START ;
            holder.author.setTextColor(Color.BLUE);
            holder.message.setBackgroundResource(R.drawable.bubble1);
        }

        holder.author.setLayoutParams(holder.params);
        holder.message.setLayoutParams(holder.params);
        holder.photo.setLayoutParams(holder.params);
    }



    public void cleanUp() {
        mDatabaseReference.removeEventListener(mChildEventListener);
    }
}
