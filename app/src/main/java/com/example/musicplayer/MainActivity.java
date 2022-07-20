package com.example.musicplayer;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    boolean checkpermission = false ;
    ListView musiclist ;
    String[] items ;
    Uri uri ;
    String musicname , musicurl ;
    ArrayList<String> songnames = new ArrayList<>();
    ArrayList<String> songurls = new ArrayList<>();
    ArrayAdapter<String> ad ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        musiclist = findViewById(R.id.musiclist);
         getmusic();
         musiclist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
             @Override
             public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                 Intent intent = new Intent(getApplicationContext() , player.class);
                 String name = songnames.get(i) ;
                  String url = songurls.get(i) ;
                  intent.putExtra("name" , name);
                  intent.putExtra("url" , url ) ;
                 startActivity(intent);
             }
         });

    }
    public void getmusic(){
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Songs");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot ds : snapshot.getChildren()){
                    Songdetail songdetail = ds.getValue(Songdetail.class);
                    songnames.add(songdetail.getSongname());
                    songurls.add(songdetail.getSongurl());
                }
                ad = new ArrayAdapter<String>(MainActivity.this,R.layout.support_simple_spinner_dropdown_item , songnames);
                musiclist.setAdapter(ad);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
    // Dexter lib for getting permission from user
    public boolean isCheckpermission(){
        Dexter.withContext(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                     checkpermission = true ;
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                     checkpermission = false ;
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();
        return checkpermission ;
    }

    // menu options which uploads music from device
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bar , menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.upload){
            if(isCheckpermission()){
                Intent intent = new Intent();
                intent.setType("audio/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent , 1);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==1){
            if(resultCode==RESULT_OK){
                uri = data.getData() ;
                Cursor cursor = getApplicationContext().getContentResolver()
                        .query(uri , null , null , null);
                int indexname = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                cursor.moveToFirst();
                musicname = cursor.getString(indexname);
                cursor.close();
                uploadtofirebase();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
   // uploading file to firebase storage
    private void uploadtofirebase() {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("Songs").child(uri.getLastPathSegment());
        String s = storageReference.getPath().toString();
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        storageReference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isComplete());
                Uri songurl = uriTask.getResult();
                musicurl = songurl.toString();
                uploadtodatabase();
                progressDialog.dismiss();
            }
        })
      .addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot snapshot) {
             double progress = (100*snapshot.getBytesTransferred())/snapshot.getTotalByteCount();
             int currentprogress = (int)progress ;
             progressDialog.setMessage("Uploaded: " +currentprogress + "%" );
            }
        });
    }

    // uploading from storage to database
    private void uploadtodatabase() {
        Songdetail songdetail = new Songdetail(musicname , musicurl);
        FirebaseDatabase.getInstance().getReference("Songs")
                .push().setValue(songdetail).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(MainActivity.this, "Success!!", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}