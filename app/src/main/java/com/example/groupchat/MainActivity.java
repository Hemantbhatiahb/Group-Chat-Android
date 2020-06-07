package com.example.groupchat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    FirebaseAuth mFirebaseAuth ;
    FirebaseAuth.AuthStateListener authStateListener ;

    FirebaseDatabase firebaseDatabase ;
    DatabaseReference databaseReference ;

    private FirebaseStorage firebaseStorage ;
    private StorageReference storageReference ;



    Button send_button ;
    EditText messageText ;
    ImageButton photoButton ;
    ListView mMessageListView ;

    MessageAdapter mMessageAdapter ;

    private String mUsername="anonymus" ;
    private static int RC_SIGN_IN = 123;
    private static int RC_PHOTO_PICKER=111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseDatabase =FirebaseDatabase.getInstance() ;
        databaseReference = firebaseDatabase.getReference();
        mFirebaseAuth = FirebaseAuth.getInstance() ;

        firebaseStorage = FirebaseStorage.getInstance();
        storageReference= firebaseStorage.getReference().child("chat_photos") ;

        send_button = (Button) findViewById(R.id.sendButton) ;
        messageText = (EditText) findViewById(R.id.messagEditText);
        mMessageListView = (ListView) findViewById(R.id.messageListView) ;
        photoButton = (ImageButton) findViewById(R.id.photoPickerButton) ;

        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT) ;
                intent.setType("image/jpg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY,true) ;
                startActivityForResult(Intent.createChooser(intent,"Complete action using"),RC_PHOTO_PICKER);
            }
        });


        messageText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                sendMessage( );
                return true ;
            }
        });

        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage() ;
            }
        });

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser() ;
                if(user!=null) {
                    // user already exists
                    Toast.makeText(getApplicationContext(),"User sign in",Toast.LENGTH_SHORT).show();

                } else {
                    //user is sign out
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.GoogleBuilder().build()
                                            )
                                    ).build(),
                            RC_SIGN_IN);
                }

            }
        };

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN) {
            if(resultCode == RESULT_OK ) {
                Toast.makeText(getApplicationContext() ,"Sign in Successful" ,Toast.LENGTH_SHORT).show();
            } else if(resultCode == RESULT_CANCELED){
                Toast.makeText(getApplicationContext() ,"Sign in unSuccessful" ,Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        // todo : store the photos to firebase storage
        if (requestCode == RC_PHOTO_PICKER && resultCode ==RESULT_OK) {

            Uri selectedImageUri = data.getData() ;  // image come from local device in form of uri (from data.getData() )
            final StorageReference photoRef = storageReference.child(selectedImageUri.getLastPathSegment()) ; // get reference to store the file //getting the last reference folder where the file is stored

            //upload the image to cloud storage
            photoRef.putFile(selectedImageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            // put the download url to the database reference
                            FriendlyMessage friendlyMessage = new FriendlyMessage(null , mUsername,uri.toString()) ;
                            databaseReference.child("messages").push().setValue(friendlyMessage) ;
                        }
                    });
                }
            });
        }
    }

    public void sendMessage() {
        String current_msg = messageText.getText().toString();
        String user = mFirebaseAuth.getCurrentUser().getDisplayName();
        if(!current_msg.equals("")) {
            Log.d("nonstatic" ,"evrything is fine");
            FriendlyMessage friendlyMessage = new FriendlyMessage(current_msg ,user,null);
            databaseReference.child("messages").push().setValue(friendlyMessage);
            messageText.setText("");
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater() ;
        menuInflater.inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.signout_item :
                //signout
                AuthUI.getInstance().signOut(this) ;
                Toast.makeText(getApplicationContext() ,"user signed out",Toast.LENGTH_SHORT).show();
                return true ;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mFirebaseAuth.removeAuthStateListener(authStateListener);
    }


    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser() ;

        if(user!=null)
            mUsername =user.getDisplayName();
        else mUsername ="Anonymus" ;

        mMessageAdapter = new MessageAdapter(this,databaseReference,mUsername) ;
        mMessageListView.setAdapter(mMessageAdapter);

    }

    @Override
    protected void onStop() {
        super.onStop();

        mMessageAdapter.cleanUp();
    }
}
