package com.example.livechatapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {

    private static FirebaseDatabase dataBase;
    private static DatabaseReference ref;
    public ArrayList<String> chatRoomList;
    private ValueEventListener chatRoomListener;
    public ArrayAdapter<String> dropDownAdapter;
    private AutoCompleteTextView dropDownTextView;
    private MediaPlayer deniedSound;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        editor = pref.edit();

        dataBase = FirebaseDatabase.getInstance();
        ref = dataBase.getReference();

        dropDownTextView = (AutoCompleteTextView) findViewById(R.id.dropDownTextView);

        chatRoomListener = new ValueEventListener() {
            @Override //Fixed, but solution is not as effecient as clear() would be (if it actually worked)
            public void onDataChange(DataSnapshot dataSnapshot) {

                chatRoomList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    chatRoomList.add(snapshot.getKey(/*String.class*/)); //This does update in real time
                }

                //This is the only way i could get it to properly update, preferably I could clear the adapter, but that doesn't work
                dropDownAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.select_dialog_item, chatRoomList);
                dropDownTextView.setAdapter(dropDownAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Error", "ChatRoomListener Cancelled");
            }
        };

        chatRoomList = new ArrayList<String>();

        dropDownAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, chatRoomList);
        dropDownTextView.setAdapter(dropDownAdapter);

        //Check for all available chat rooms. Important: only set up for testing right now. change so that it accesses where the chatrooms are stored.
        ref.child("ChatRooms").addValueEventListener(chatRoomListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    //Takes values from MainActivity, starts ChatActivity, and gives the values to ChatActivity.
    public void enterChat(View view) {
        ref.removeEventListener(chatRoomListener);

        final EditText usernameEditText = (EditText) findViewById(R.id.usernameEditText);
        final Switch incognitoModeSwitch = (Switch) findViewById(R.id.incognitoModeSwitch);
        String chatRoomName = dropDownTextView.getText().toString();
        String username = usernameEditText.getText().toString();
        boolean incognitoValue = incognitoModeSwitch.isChecked();

        //Checks if a chat room name and username was entered. If not, it prevents the user from moving on.
        boolean canContinue = true;

        if(chatRoomName.trim().isEmpty()) {
            Toast chatRoomReqToast = Toast.makeText(getApplicationContext(), "Please Enter a Chat Room Name", Toast.LENGTH_SHORT);
            chatRoomReqToast.setGravity(Gravity.CENTER, 0, 0);
            chatRoomReqToast.show();
            canContinue = false;
        } else if(username.trim().isEmpty()) {
            //Later change to check if the username is already taken. Not implemented yet
            Toast usernameReqToast = Toast.makeText(getApplicationContext(), "Please Enter a Username", Toast.LENGTH_SHORT);
            usernameReqToast.setGravity(Gravity.CENTER, 0, 0);
            usernameReqToast.show();
            canContinue = false;
        } else if(chatRoomName.equals("Super Secret Chat Room")) { //Just a little bit of 'fun'
            Toast usernameReqToast = Toast.makeText(getApplicationContext(), "You must be at least level 50 to join this chat room", Toast.LENGTH_LONG);
            usernameReqToast.setGravity(Gravity.CENTER, 0, 0);
            usernameReqToast.show();
            canContinue = false;

            if(!deniedSound.isPlaying()) deniedSound.start();
        }

        if(canContinue) {
            deniedSound.stop();
            deniedSound.release();
            Intent intent = new Intent(this,ChatActivity.class);

            //Passes values to ChatActivity.java
            intent.putExtra("chatRoomName", chatRoomName);
            intent.putExtra("username", username);
            intent.putExtra("incognitoValue", incognitoValue);

            //Used to prevent chat room enter messages on screen rotate (due to activity restart on rotation).
            intent.putExtra("orientRestart", false);

            //Checks if the chat room name entered exists. If it does, it joins it. If it doesn't, it creates a new chatroom and joins that.
            boolean chatRoomDoesntExist = true;
            for(int i = chatRoomList.size() - 1; i >= 0; i--) {
                //IMPORTANT NOTE: when comparing objects, such as strings, use the .equals() method
                if(chatRoomName.equals(chatRoomList.get(i))) {
                    //do stuff to join the chatroom (add at later point)
                    chatRoomDoesntExist = false;
                    break;
                }
            }
            if(chatRoomDoesntExist) {
                //Creates a chat room with no messages, but sets it's empty value to true
                ref.child("ChatRooms").child(chatRoomName).child("Empty").setValue(true);
            }

            startActivity(intent);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        deniedSound.release();

        //Saves username, chat room name, and incognito value to internal storage as key/value pairs
        EditText usernameEditText = (EditText) findViewById(R.id.usernameEditText);
        Switch incognitoModeSwitch = (Switch) findViewById(R.id.incognitoModeSwitch);
        String chatRoomName = dropDownTextView.getText().toString();
        String username = usernameEditText.getText().toString();
        boolean incognitoValue = incognitoModeSwitch.isChecked();

        //TODO: Code to change strings containing only spaces to null

        editor.putString("username_string", username);
        editor.putString("chat_room_string", chatRoomName);
        editor.putBoolean("incognito_value_boolean", incognitoValue);
        editor.commit();

        //For saving, look at http://www.journaldev.com/9412/android-shared-preferences-example-tutorial
    }

    @Override
    public void onResume() {
        super.onResume();

        deniedSound = MediaPlayer.create(this, R.raw.sound); //Sound is just for fun. (

        //Retrieves username, chat room name, and incognito value from internal storage, and inserts them into correct views
        final String chatRoomName = pref.getString("chat_room_string", null);
        final String username = pref.getString("username_string", null);
        final boolean incognitoValue = pref.getBoolean("incognito_value_boolean", false);
        final EditText usernameEditText = (EditText) findViewById(R.id.usernameEditText);
        final Switch incognitoModeSwitch = (Switch) findViewById(R.id.incognitoModeSwitch);

        usernameEditText.setText(username);
        dropDownTextView.setText(chatRoomName);
        incognitoModeSwitch.setChecked(incognitoValue);
    }
}
