package com.example.livechatapp;
//TODO: Uncomment all service related code

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.*;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class ChatActivity extends ActionBarActivity {

    private FirebaseDatabase dataBase;
    private ValueEventListener chatRoomEventListener;
    private BaseAdapter chatMessageAdapter;
    private String chatRoomName;
    private String usernameExtra;
    private boolean incognitoMode;
    private ArrayList<Message> messageList;
    private DatabaseReference chatRoomRef;
    private boolean chatRoomEmpty;
    private ListView listView;
    Intent myServiceIntent;
    private NotificationService myNotificationService;
    Context ctx;
    public Context getCtx() {
        return ctx;
    }
    private ServiceConnection sConnection;
    private Messenger messenger;

    //Used for service message handlers
    private static final int STOP_LISTENERS = 1;
    private static final int START_LISTENERS = 2;
    private static final int ADD_CHAT_ROOM = 3;
    private static final int MESSAGE_SUCCESS = 4;
    private static final int MESSAGE_FAIL = 5;

    private boolean serviceSuccess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        listView = (ListView) findViewById(R.id.chatViewArea);

        Log.d("Testing", "Activity Started");

        //a Message() takes five values:
        //Message(String username, long sendTime, String chatMessage, String messageType, boolean isComplete)
        messageList = new ArrayList<Message>();

        final SimpleDateFormat enterDateFormat = new SimpleDateFormat("E MMM d, yyyy 'at' hh:mm:ss a zzz");
        final SimpleDateFormat sendDateFormat = new SimpleDateFormat("hh:mm a");

        chatRoomName = getIntent().getExtras().getString("chatRoomName");
        getSupportActionBar().setTitle(chatRoomName);
        usernameExtra = getIntent().getExtras().getString("username");
        incognitoMode = getIntent().getExtras().getBoolean("incognitoValue");
        Log.d("Testing", "incognitoMode = " + incognitoMode);

        final Spannable enterSpannable = new SpannableString(" has entered");
        enterSpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#3d3d3d")), 0, enterSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        final Spannable exitSpannable = new SpannableString(" has left");
        exitSpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#3d3d3d")), 0, exitSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        final Spannable errorSpannable = new SpannableString("Error: Message Incomplete");
        errorSpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#ff0000")), 0, errorSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        dataBase = FirebaseDatabase.getInstance();
        chatRoomRef = dataBase.getReference().child("ChatRooms").child(chatRoomName);

        chatMessageAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                int count = messageList.size();
                return count;
            }

            @Override
            public Object getItem(int i) {
                return null;
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override //Partially modelled after http://stackoverflow.com/questions/35761897/how-do-i-make-a-relative-layout-an-item-of-my-listview-and-detect-gestures-over
            public View getView(int position, View convertView, ViewGroup parent) {
                LayoutInflater layoutInflater;
                ViewHolder listViewHolder;
                String messageType = messageList.get(position).getMessageType();
                if( convertView == null ){
                    layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = layoutInflater.inflate(R.layout.relativelayout_chat_box, parent, false);

                    listViewHolder = new ViewHolder();
                    listViewHolder.sendTime = (TextView) convertView.findViewById(R.id.chatSendTimeTextView);
                    listViewHolder.username = (TextView) convertView.findViewById(R.id.chatUsernameTextView);
                    listViewHolder.message = (TextView) convertView.findViewById(R.id.chatMessageTextView);
                    convertView.setTag(listViewHolder);
                } else {
                    listViewHolder = (ViewHolder) convertView.getTag();
                }

                //Checks if the message is complete
                if(messageList.get(position).getIsComplete()) {

                    //Converts the long value to proper time and date format
                    Date sendDate = new Date(messageList.get(position).getSendTime());
                    String time = enterDateFormat.format(sendDate);
                    if (messageType.equals("chat")) {
                        time = sendDateFormat.format(sendDate);
                    }

                    Spannable usernameSpannable = new SpannableString(messageList.get(position).getUsername());
                    String usernameColor = "#3d3d3d";
                    if (messageList.get(position).getUsername().equals("~Anonymous~")) {
                        usernameColor = "#ff3f7f";
                    } else if (messageList.get(position).getUsername().equals("1678")) {
                        usernameColor = "#5BE300";
                    } else if (messageList.get(position).getUsername().contains("/#")) { //If username includes hexadecimal, removes hexadecimal and makes color that hexadecimal
                        final int hexIndex = messageList.get(position).getUsername().indexOf("/#");
                        String possibleHexColor = "#";
                        boolean isHex = true;
                        if(hexIndex + 6 < messageList.get(position).getUsername().length()) {
                            for (int currentIndex = hexIndex + 2; currentIndex <= hexIndex + 7; currentIndex++) {
                                char currentChar = messageList.get(position).getUsername().charAt(currentIndex);
                                if (Character.isDigit(currentChar) || Character.isLetter(currentChar)) {
                                    possibleHexColor += messageList.get(position).getUsername().charAt(currentIndex);
                                } else {
                                    currentIndex = hexIndex + 8;
                                    isHex = false;
                                }
                            }
                        } else isHex = false;
                        if(isHex) {
                            usernameColor = possibleHexColor;

                            //Ensures that the hex value is only removed if the username contains characters in addition to the hex value
                            if(messageList.get(position).getUsername().replaceFirst("/" + possibleHexColor, "").replace(" ", "").length() != 0) {
                                usernameSpannable = new SpannableString(messageList.get(position).getUsername().replaceFirst("/" + possibleHexColor, ""));
                            }
                        }
                    }

                    usernameSpannable.setSpan(new ForegroundColorSpan(Color.parseColor(usernameColor)), 0, usernameSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    listViewHolder.username.setText(usernameSpannable);

                    listViewHolder.sendTime.setText(time);

                    if (messageType.equals("chat")) {
                        listViewHolder.message.setText(messageList.get(position).getChatMessage());
                        listViewHolder.message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                    } else if (messageType.equals("enter")) {
                        listViewHolder.username.append(enterSpannable);
                        listViewHolder.message.setText(null);
                        listViewHolder.message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 0);
                    } else if (messageType.equals("exit")) {
                        listViewHolder.username.append(exitSpannable);
                        listViewHolder.message.setText(null);
                        listViewHolder.message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 0);
                    }

                } else {
                    listViewHolder.username.setText(errorSpannable);
                    listViewHolder.message.setText(null);
                    listViewHolder.message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 0);
                    listViewHolder.sendTime.setText(null);
                }

                return convertView;
            }
        };

        chatRoomEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.child("Messages").child("" + (dataSnapshot.child("Messages").getChildrenCount() - 1)).child("isComplete").getValue().equals(null)) {
                    if(dataSnapshot.child("Messages").child("" + (dataSnapshot.child("Messages").getChildrenCount() - 1)).child("isComplete").getValue(Boolean.class)) {
                        chatRoomEmpty = dataSnapshot.child("Empty").getValue(Boolean.class);
                        messageList.clear();
                        if (!chatRoomEmpty) {
                            for (int i = 0; i < dataSnapshot.child("Messages").getChildrenCount(); i++) {
                                //Checks that the current message is complete.
                                boolean tempIsComplete = dataSnapshot.child("Messages").child("" + i).child("isComplete").getValue(Boolean.class);
                                long tempSendTime = -1;
                                String tempMessage = null;
                                String tempUsername = null;
                                String tempMessageType = null;
                                if(tempIsComplete) {
                                    tempSendTime = dataSnapshot.child("Messages").child("" + i).child("sendTime").getValue(Long.class);
                                    tempMessage = dataSnapshot.child("Messages").child("" + i).child("message").getValue(String.class);
                                    tempUsername = dataSnapshot.child("Messages").child("" + i).child("username").getValue(String.class);
                                    tempMessageType = dataSnapshot.child("Messages").child("" + i).child("messageType").getValue(String.class);
                                }

                                messageList.add(new Message(tempUsername, tempSendTime, tempMessage, tempMessageType, tempIsComplete));
                            }
                            chatMessageAdapter.notifyDataSetChanged();

                            //Scrolls now when sending and receiving.
                            listView.clearFocus();
                            listView.post(new Runnable() {
                                @Override
                                public void run() {
                                    listView.setSelection(chatMessageAdapter.getCount() - 1);
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Error", "ChatRoomEventListener Cancelled");
                Toast connectionErrorToast = Toast.makeText(getApplicationContext(), "Connection Error", Toast.LENGTH_SHORT);
                connectionErrorToast.setGravity(Gravity.CENTER, 0, 0);
                connectionErrorToast.show();
            }
        };
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void sendMessage(View view) {
        EditText chatBoxEditText = (EditText) findViewById(R.id.chatBox);
        String unsentMessage = chatBoxEditText.getText().toString();

        //Still uses "" instead of the trim method so that people can say stuff like "     ".
        if(!unsentMessage.equals("")) {
            chatBoxEditText.setText("");
            final int newMessageArrayNum = messageList.size();
            chatRoomRef.removeEventListener(chatRoomEventListener);

            chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("isComplete").setValue(false);
            chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("message").setValue(unsentMessage);
            chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("sendTime").setValue(System.currentTimeMillis());
            chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("messageType").setValue("chat");

            if(incognitoMode) {
                chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("username").setValue("~Anonymous~");
                //chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("incognito").setValue(true);
            } else {
                chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("username").setValue(usernameExtra);
            }

            if(chatRoomEmpty) {
                chatRoomRef.child("Empty").setValue(false);
            }


            chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("isComplete").setValue(true);
            chatRoomRef.addValueEventListener(chatRoomEventListener);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        //Important note for future coding: finish() does not kill the activity until all processes are done (like active eventListeners)
        chatRoomRef.removeEventListener(chatRoomEventListener);
        this.finish();
    }

    @Override
    public void onPause() {
        super.onPause();

        //Checks if the restart is due to orientation change.
        if(this.getResources().getConfiguration().orientation != getIntent().getExtras().getInt("orientation")) {
            getIntent().putExtra("orientRestart", true);
        } else {
            getIntent().putExtra("orientRestart", false);
        }

        if(!getIntent().getExtras().getBoolean("orientRestart")) {
            //Creates user has left message
            chatRoomRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    final long chatRoomSize = dataSnapshot.child("Messages").getChildrenCount();

                    chatRoomRef.removeEventListener(chatRoomEventListener);
                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("isComplete").setValue(false);

                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("sendTime").setValue(System.currentTimeMillis());

                    if (incognitoMode) {
                        chatRoomRef.child("Messages").child("" + chatRoomSize).child("username").setValue("~Anonymous~");
                        //chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("incognito").setValue(true);
                    } else {
                        chatRoomRef.child("Messages").child("" + chatRoomSize).child("username").setValue(usernameExtra);
                    }

                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("messageType").setValue("exit");

                    //Prevents other devices in the rooms from crashing due to lack of information.
                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("isComplete").setValue(true);

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        //Records orientation
        int orientationInt = this.getResources().getConfiguration().orientation;
        getIntent().putExtra("orientation", orientationInt);

        if(!getIntent().getExtras().getBoolean("orientRestart")) {
            chatRoomRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    final long chatRoomSize = dataSnapshot.child("Messages").getChildrenCount();

                    //Creates the 'user has entered' message.
                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("isComplete").setValue(false);

                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("sendTime").setValue(System.currentTimeMillis());

                    if (incognitoMode) {
                        chatRoomRef.child("Messages").child("" + chatRoomSize).child("username").setValue("~Anonymous~");
                        //chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("incognito").setValue(true);
                    } else {
                        chatRoomRef.child("Messages").child("" + chatRoomSize).child("username").setValue(usernameExtra);
                    }

                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("messageType").setValue("enter");

                    //Prevents other devices in the rooms from crashing due to lack of information.
                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("isComplete").setValue(true);

                    chatRoomRef.child("Empty").setValue(false);

                    chatRoomRef.addValueEventListener(chatRoomEventListener);
                    listView.setAdapter(chatMessageAdapter);

                    //Sets onClickListener used for deleting a user's messages (NOTE: ~maybe~ add a way to make sure a user can't change their name to delete another persons messages (HARD))
                    //Remove this section of code if I don't want people to be able to delete messages.
                    listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> arg0, View view, final int position, long id) {
                            if (usernameExtra.equals(messageList.get(position).getUsername()) && messageList.get(position).getMessageType().equals("chat") && messageList.get(position).getIsComplete()) {
                                PopupMenu deleteMessagePopup = new PopupMenu(ChatActivity.this, view);
                                deleteMessagePopup.getMenuInflater().inflate(R.menu.deletepopup_menu, deleteMessagePopup.getMenu());
                                deleteMessagePopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        chatRoomRef.child("Messages").child("" + position).child("message").setValue("[Deleted]");

                                        return true;
                                    }
                                });

                                deleteMessagePopup.show();

                            }
                            return true;
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        } else {
            chatRoomRef.addValueEventListener(chatRoomEventListener);
            listView.setAdapter(chatMessageAdapter);

            //Sets onClickListener used for deleting a user's messages (NOTE: ~maybe~ add a way to make sure a user can't change their name to delete another persons messages (HARD))
            //Remove this section of code if I don't want people to be able to delete messages.
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View view, final int position, long id) {
                    if (usernameExtra.equals(messageList.get(position).getUsername()) && messageList.get(position).getMessageType().equals("chat") && messageList.get(position).getIsComplete()) {
                        PopupMenu deleteMessagePopup = new PopupMenu(ChatActivity.this, view);
                        deleteMessagePopup.getMenuInflater().inflate(R.menu.deletepopup_menu, deleteMessagePopup.getMenu());
                        deleteMessagePopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                chatRoomRef.child("Messages").child("" + position).child("message").setValue("[Deleted]");

                                return true;
                            }
                        });

                        deleteMessagePopup.show();

                    }
                    return true;
                }
            });
        }
    }

    //check if this is the way to do this
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }

    //The following overrides are for the notification service

    //Broadcasts to service to check if it is running, after recieving return broadcast, if it is running it broadcasts for it to stop
    @Override
    public void onStart() { //Consider moving onStart() and onStop() code to onResume() and onPause(), respectively
        super.onStart();

        //NOTE: Bound services have foreground priority, just create and bind here, it won't be killed unless the activity is killed first.
        ctx = this;
        myNotificationService = new NotificationService(getCtx());
        myServiceIntent = new Intent(getCtx(), myNotificationService.getClass());
        sConnection = new ServiceConnection() {

            @Override
            public void onServiceDisconnected(ComponentName name) {
                messenger = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                // We are connected to the service
                messenger = new Messenger(service);
            }
        };

        //Starts the notification service if it does not exist; otherwise it tells
        // the currently running service to stop its listeners,
        // binds in both cases
        if(!isServiceRunning(myNotificationService.getClass())) {
            startService(myServiceIntent); //TODO: Figure out how to run in a separate thread (or whatever keeps the service alive when the activity is killed).
            Log.d("Testing", "Service started");
            bindService(myServiceIntent, sConnection, Context.BIND_AUTO_CREATE);
            Log.d("Testing", "Service bound");
        } else {
            bindService(myServiceIntent, sConnection, Context.BIND_AUTO_CREATE);
            Log.d("Testing", "Service bound");
            android.os.Message serviceMessage = android.os.Message.obtain(null, STOP_LISTENERS);
            serviceMessage.replyTo = new Messenger(new ResponseHandler());
            try {
                messenger.send(serviceMessage);
            } catch(RemoteException e) {
                e.printStackTrace();
            }
        }
        android.os.Message serviceMessage = android.os.Message.obtain(null, ADD_CHAT_ROOM);
        serviceMessage.replyTo = new Messenger(new ResponseHandler());
        Bundle bundle = new Bundle();
        bundle.putString("data", "string"/*put chat room string here*/);
        serviceMessage.setData(bundle);
        try {
            messenger.send(serviceMessage);
        } catch(RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        //TODO: Code to check if the service is running (and bound), if it is, resume the listeners, if it isn't, create the service
        // Afterwards, it must unbind from the service
    }


    //Used to check if the notification service is already running
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false; //No need to call else because return finishes method
    }
}

//TODO: This class is nowhere near fully or properly implemented, just here so I know it needs to be finished
// This class handles the Service response
class ResponseHandler extends Handler {

    @Override //Fixed, problem was caused by my own class being called Message (oops) //WHY, ERRORS, WHY?!? (╯°□°）╯︵ ┻━┻
    public void handleMessage(android.os.Message msg) {
        int respCode = msg.what;

        //TODO: Add cases to respond to MESSAGE_SUCCESS & MESSAGE_FAIL
        switch (respCode) {
            case 2 /*add in proper variable name*/: {
                //TODO: Code to tell the rest of the activity that the service succeeded (use global boolean?)
            }
            case 3 /*add in proper variable name*/: {
                //TODO: Code to tell the rest of the activity hat the service failed
            }
            default: {
                //super.handleMessage(msg);

                // -OR-

                //TODO: Code that assumes service failed
            }
        }
    }

}

//For temporarily holding values of each chat box.
class ViewHolder {
    TextView username;
    TextView sendTime;
    TextView message;
}
