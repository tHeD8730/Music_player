package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.arges.sepan.argmusicplayer.Enums.AudioType;
import com.arges.sepan.argmusicplayer.IndependentClasses.ArgAudio;
import com.arges.sepan.argmusicplayer.PlayerViews.ArgPlayerFullScreenView;

public class player extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
    Intent intent = getIntent();
        String name = intent.getStringExtra("name");
 String url = intent.getStringExtra("url");
        ArgAudio audio = new ArgAudio("",name,url, AudioType.URL);
        ArgPlayerFullScreenView argPlayerFullScreenView = (ArgPlayerFullScreenView) findViewById(R.id.argmusicplayer);
        argPlayerFullScreenView.play(audio);

    }
}