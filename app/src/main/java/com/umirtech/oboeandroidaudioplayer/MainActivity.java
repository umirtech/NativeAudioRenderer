package com.umirtech.oboeandroidaudioplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.umirtech.nativeaudiorenderer.HardAudioDecoder;
import com.umirtech.nativeaudiorenderer.NativeAudioRenderer;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity {

    HardAudioDecoder audioDecoder;
    boolean isDataSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NativeAudioRenderer.init(this);

        Button playButton = findViewById(R.id.playButton);
        audioDecoder = new HardAudioDecoder();


        Button selectFile = findViewById(R.id.selectFile);
        selectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectFile();
            }
        });
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isDataSet)
                {
                    if (audioDecoder.isRunning())
                    {
                        audioDecoder.stop();
                    }else {
                        audioDecoder.seekTo(0);
                        audioDecoder.start();
                    }
                }

            }
        });




    }


    public void selectFile()
    {
        if (audioDecoder.isRunning())
        {
            audioDecoder.stop();
        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, 1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri;
        if(requestCode == 1 && resultCode == RESULT_OK){
            if (data != null) {
                uri = data.getData();
               String path = getPath(uri);
               Log.e("path ",""+ path);
               audioDecoder.init(path);
                isDataSet = true;
            }

        }
    }



    public String getPath(Uri uri)
    {
        String destinationFilePath = null;
        try {
            destinationFilePath = getExternalFilesDir("audio") + "/tempFile.mp3";
            InputStream inputStream = getContentResolver().openInputStream(uri);
            OutputStream outputStream = new FileOutputStream(destinationFilePath);

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return destinationFilePath;
    }

}