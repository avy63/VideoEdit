package com.example.user.videoediting;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    FFmpeg fFmpeg;
    TextView txtViewShowlist;
    Button btnVideoselect, btnAudioselect, btnSave;
    private static final int SELECT_VIDEOS = 1;
    private static final int SELECT_AUDIOS = 2;
    private static final int SELECT_VIDEOS_KITKAT = 1;
    private List<String> selectedVideos;
    private String selectAudio;
    private String rootPath;
    private String tempPath;
    public ExtraTask extraTask;
    //private ProgressBar loading_progress_xml;
    private static final String FILEPATH = "filepath";
    int len = 0;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 23) {
            getPermission();
        }
        viewInitialize();
        isFFmpegAvailavle();

    }

    private void getPermission() {
        String[] params = null;
        String writeExternalStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        String readExternalStorage = Manifest.permission.READ_EXTERNAL_STORAGE;

        int hasWriteExternalStoragePermission = ActivityCompat.checkSelfPermission(this, writeExternalStorage);
        int hasReadExternalStoragePermission = ActivityCompat.checkSelfPermission(this, readExternalStorage);
        List<String> permissions = new ArrayList<String>();

        if (hasWriteExternalStoragePermission != PackageManager.PERMISSION_GRANTED)
            permissions.add(writeExternalStorage);
        if (hasReadExternalStoragePermission != PackageManager.PERMISSION_GRANTED)
            permissions.add(readExternalStorage);

        if (!permissions.isEmpty()) {
            params = permissions.toArray(new String[permissions.size()]);
        }
        if (params != null && params.length > 0) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    params,
                    100);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 100: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    viewInitialize();
                    isFFmpegAvailavle();
                }
            }
            break;


        }
    }

    public void viewInitialize() {

        txtViewShowlist = (TextView) findViewById(R.id.txtViewShowlist);
        btnVideoselect = (Button) findViewById(R.id.btnVideoselect);
        btnAudioselect = (Button) findViewById(R.id.btnAudioselect);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnVideoselect.setOnClickListener(this);
        btnAudioselect.setOnClickListener(this);

        btnSave.setOnClickListener(this);

        selectedVideos = new ArrayList<String>();
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(null);
        progressDialog.setCancelable(false);
        extraTask = ExtraTask.getInstance();

    }

    public void isFFmpegAvailavle() {
        try {
            if (fFmpeg == null) {
                fFmpeg = FFmpeg.getInstance(this);

            }
            fFmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    showToastmessage(getApplicationContext(), "Failed to load FFmpeg");
                }

                @Override
                public void onSuccess() {
                    showToastmessage(getApplicationContext(), "Huuary Succesful");
                }


            });
        } catch (FFmpegNotSupportedException e) {
            Log.e("Error:", e.toString());
        } catch (Exception e) {
            Log.d("Error", "EXception no controlada : " + e);
        }
    }


    private void showToastmessage(Context applicationContext, String value) {
        // Toast.makeText(applicationContext,value,Toast.LENGTH_SHORT).show();
        Toast.makeText(getBaseContext(), value, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnVideoselect:


                selectVideos();
                break;
            case R.id.btnAudioselect:

                selectAudio();
                break;
            case R.id.btnSave:
                executeMerging();
                break;

        }
    }

    // Select audio from device
    private void selectAudio() {
        Intent intent;
        intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/mpeg");
        startActivityForResult(Intent.createChooser(intent, "Select Audio"), SELECT_AUDIOS);
    }


    // merging videos....
    private void mergingAudioandVideo() {
        if (fFmpeg != null) {
            try {
                File moviesDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MOVIES
                );
                String filePrefix = "Output";
                String fileExtn = ".mp4";
                File dest = new File(moviesDir, filePrefix + fileExtn);
                int fileNo = 0;
                while (dest.exists()) {
                    fileNo++;
                    dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
                }
                rootPath = dest.getAbsolutePath();
                Log.e("Location", rootPath);


                String command[] = {"-i", tempPath, "-i", selectAudio, "-c:v", "copy", "-c:a", "aac", "-map", "0:v:0", "-map", "1:a:0", "-shortest", rootPath};

                //  String tempo=",\"&&\",\"-y\", \"-i\",rootPath + \"output3.mp4\",\"-i\",selectAudio,\"-c:v copy -c:a aac -strict experimental -map 0:v:0 -map 1:a:0\",rootPath + \"musk.mp4\""

                fFmpeg.execute(command, new ExecuteBinaryResponseHandler() {

                    @Override
                    public void onSuccess(String s) {
                        showToastmessage(getApplicationContext(), "Succesfully merge video and Audios");

                        Intent intent = new Intent(MainActivity.this, PreviewActivity.class);
                        intent.putExtra(FILEPATH, rootPath);
                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(String s) {
                        showToastmessage(getApplicationContext(), s);
                        Log.e("Error", s);

                        progressDialog.dismiss();
                    }

                    @Override
                    public void onStart() {

                        progressDialog.setMessage("Processing...");
                        progressDialog.show();
                    }

                    @Override
                    public void onProgress(String s) {

                        progressDialog.setMessage("progress : Merging Audio and Video " + s);
                    }

                    @Override
                    public void onFinish() {

                        progressDialog.dismiss();
                    }
                });
            } catch (FFmpegCommandAlreadyRunningException e) {

            }
        } else {
            fFmpeg = FFmpeg.getInstance(this);
        }

    }

    private void executeMerging() {
        if (fFmpeg != null) {
            try {
                File moviesDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MOVIES
                );
                String filePrefix = "Output";
                String fileExtn = ".mp4";
                File dest = new File(moviesDir, filePrefix + fileExtn);
                int fileNo = 0;
                while (dest.exists()) {
                    fileNo++;
                    dest = new File(moviesDir, filePrefix + fileNo + fileExtn);
                }
                tempPath = dest.getAbsolutePath();
                String[] complexCommand = new String[]{"-y", "-i", selectedVideos.get(0).toString(), "-i", selectedVideos.get(1).toString(), "-strict", "experimental", "-filter_complex",
                        "[0:v]scale=640x640,setsar=1:1[v0];[1:v]scale=640x640,setsar=1:1[v1];[v0][0:a][v1][1:a] concat=n=2:v=1:a=1",
                        "-ab", "48000", "-ac", "2", "-ar", "22050", "-s", "640x640", "-vcodec", "libx264", "-crf", "27", "-q", "4", "-preset", "ultrafast", tempPath};

                //  String tempo=",\"&&\",\"-y\", \"-i\",rootPath + \"output3.mp4\",\"-i\",selectAudio,\"-c:v copy -c:a aac -strict experimental -map 0:v:0 -map 1:a:0\",rootPath + \"musk.mp4\""

                fFmpeg.execute(complexCommand, new ExecuteBinaryResponseHandler() {

                    @Override
                    public void onSuccess(String s) {
                        showToastmessage(getApplicationContext(), "Succesfully merge videos");

                        mergingAudioandVideo();
                    }

                    @Override
                    public void onFailure(String s) {
                        showToastmessage(getApplicationContext(), s);
                        Log.e("Error", s);

                    }

                    @Override
                    public void onProgress(String s) {

                        progressDialog.setMessage("progress : Merging Videos " + s);
                    }

                    @Override
                    public void onStart() {

                        progressDialog.setMessage("Processing...");
                        progressDialog.show();
                    }

                    @Override
                    public void onFinish() {


                    }
                });
            } catch (FFmpegCommandAlreadyRunningException e) {

            }
        } else {
            fFmpeg = FFmpeg.getInstance(this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {


        if (resultCode == RESULT_OK && requestCode == SELECT_VIDEOS) {
            selectedVideos = getSelectedVideos(requestCode, data);
            Log.d("path", selectedVideos.toString());
            txtViewShowlist.setText(selectedVideos.toString());
           /* tempPath=selectedVideos.get(0).toString();

            for(int j=tempPath.length()-1;j>=0;j--){
                if(tempPath.charAt(j)=='/'){
                   len=j;
                   break;
                }

            }
            rootPath=tempPath.substring(0,len+1);
            Log.d("rootPath",rootPath);
            showToastmessage(getApplicationContext(),"Rpptpath is:"+rootPath);*/

        }
        if (requestCode == SELECT_AUDIOS && resultCode == RESULT_OK) {
            Log.d("path", selectedVideos.toString());
            if ((data != null) && (data.getData() != null)) {
                Uri audioFileUri = data.getData();
                String selectedPath = "";
                selectedPath = extraTask.getPath(getApplicationContext(), audioFileUri);
                selectAudio = selectedPath;
                Log.d("path", selectedVideos.toString());
                txtViewShowlist.setText(selectedVideos.toString() + " , " + selectAudio);
                // Now you can use that Uri to get the file path, or upload it, ...
            }
        }

    }


    private List<String> getSelectedVideos(int requestCode, Intent data) {
        List<String> result = new ArrayList<>();

        ClipData clipData = data.getClipData();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                ClipData.Item videoItem = clipData.getItemAt(i);
                Uri videoURI = videoItem.getUri();
                String filePath = extraTask.getPath(this, videoURI);
                result.add(filePath);
            }
        } else {
            Uri videoURI = data.getData();
            String filePath = extraTask.getPath(this, videoURI);
            result.add(filePath);
        }

        return result;
    }

    //Select videos from device
    private void selectVideos() {
        if (Build.VERSION.SDK_INT < 19) {
            Intent intent = new Intent();
            intent.setType("video/mp4");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select videos"), SELECT_VIDEOS);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setType("video/mp4");
            startActivityForResult(intent, SELECT_VIDEOS_KITKAT);
        }
    }
}
