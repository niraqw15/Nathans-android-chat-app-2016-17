package com.example.livechatapp;

/**
 * Created by niraq on 2/11/2017.
 */

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.os.Message;

/**
 * Created by fabio on 30/01/2016.
 */
public class NotificationService extends Service { //IMPORTANT NOTE: Need to replace all counting code with eventlistener and notification code

    private Messenger msg = new Messenger(new ConvertHandler());

    public NotificationService(Context applicationContext) {
        super();
        Log.i("HERE", "here I am!");
    }

    public NotificationService() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("EXIT", "ondestroy!");
        Intent broadcastIntent = new Intent("livechatapp.example.com.ActivityRecognition.RestartSensor");
        sendBroadcast(broadcastIntent);
    }

    //Only here to enable messengers
    @Override
    public IBinder onBind(Intent intent) {
        return msg.getBinder();
    }

    //Just to prevent warning messages. Check into remoteservices
    //private final IBinder mBinder = new LocalBinder();

    //Temporary! Only garunteed to work when in the same process. Look into IPC
    public class LocalBinder extends Binder {
        NotificationService getService() {
            return NotificationService.this;
        }
    }

    //TODO: Add methods for listening, stop listening, add chat room,
}

class ConvertHandler extends Handler {

    //Used for service message handlers
    private static final int STOP_LISTENERS = 1;
    private static final int START_LISTENERS = 2;
    private static final int ADD_CHAT_ROOM = 3;
    private static final int MESSAGE_SUCCESS = 4;
    private static final int MESSAGE_FAIL = 5;

    @Override
    public void handleMessage(Message msg) {
        // This is the action

        int msgType = msg.what;

        //I think I need to create a case for all the messages I expect to recieve (turn on, turn off, add to chat room list)
        switch (msgType) {
            /*case 1/*MY_CASE/: {
                try {
                    // Incoming data
                    String data = msg.getData().getString("data");
                    Message resp = Message.obtain(null, 1/*MY_RESPONSE/);
                    Bundle bResp = new Bundle();
                    bResp.putString("respData", /*myData/data.toUpperCase());
                    resp.setData(bResp);

                    msg.replyTo.send(resp);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            }*/
            case STOP_LISTENERS: {
                //TODO: stops listeners
                Log.d("Testing Service", "STOP_LISTENERS");
                Message resp = Message.obtain(null, MESSAGE_SUCCESS);

                try {
                    msg.replyTo.send(resp);
                } catch(RemoteException e) {
                    e.printStackTrace();
                }
                break;
            }
            case START_LISTENERS: {
                //TODO: starts listeners
                Log.d("Testing Service", "START_LISTENERS");
                Message resp = Message.obtain(null, MESSAGE_SUCCESS);

                try {
                    msg.replyTo.send(resp);
                } catch(RemoteException e) {
                    e.printStackTrace();
                }
                break;
            }
            case ADD_CHAT_ROOM: {
                //TODO: adds chat room to list of chat rooms
                Log.d("Testing Service", "ADD_CHAT_ROOM");
                Message resp = Message.obtain(null, MESSAGE_SUCCESS);

                try {
                    msg.replyTo.send(resp);
                } catch(RemoteException e) {
                    e.printStackTrace();
                }
                break;
            }
            default:
                super.handleMessage(msg);
        }
    }
}


