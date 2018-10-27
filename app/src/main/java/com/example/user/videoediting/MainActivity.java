package com.example.user.videoediting;

import android.Manifest;
import android.annotation.SuppressLint;
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
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    FFmpeg fFmpeg;
    TextView txtViewShowlist;
    Button btnVideoselect,btnAudioselect,btnSave;
    private static final int SELECT_VIDEOS = 1;
    private static final int SELECT_AUDIOS = 2;
    private static final int SELECT_VIDEOS_KITKAT = 1;
    private List<String> selectedVideos;
    private String selectAudio;
    private String rootPath;
    private String tempPath;
    private ProgressBar loading_progress_xml;
    int len=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE+Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},100);
                return;
            }
        }
        viewInitialize();
        isFFmpegAvailavle();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 100 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
            viewInitialize();
        }else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},100);
            }
        }
    }
    public void viewInitialize(){
        loading_progress_xml=(ProgressBar)findViewById(R.id.loading_progress_xml);
        loading_progress_xml.setVisibility(View.GONE);
        txtViewShowlist=(TextView)findViewById(R.id.txtViewShowlist);
        btnVideoselect=(Button)findViewById(R.id.btnVideoselect);
        btnAudioselect=(Button)findViewById(R.id.btnAudioselect);
        btnSave=(Button)findViewById(R.id.btnSave);
        btnVideoselect.setOnClickListener(this);
        btnAudioselect.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        selectedVideos=new ArrayList<String>();
    }
    public void isFFmpegAvailavle(){
        if (fFmpeg==null){
            fFmpeg=FFmpeg.getInstance(this);
            try {
                fFmpeg.loadBinary(new FFmpegLoadBinaryResponseHandler() {
                    @Override
                    public void onFailure() {
                        showToastmessage(getApplicationContext(),"Failed to load FFmpeg");
                    }

                    @Override
                    public void onSuccess() {
                        showToastmessage(getApplicationContext(),"Huuary Succesful");
                    }

                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onFinish() {

                    }
                });
            } catch (FFmpegNotSupportedException e) {
                e.printStackTrace();
            }
        }
    }

    private void showToastmessage(Context applicationContext, String value) {
        Toast.makeText(applicationContext,value,Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
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
        startActivityForResult(Intent.createChooser(intent,"Select Audio"), SELECT_AUDIOS);
    }
    // merging videos....
    private void executeMerging() {
        if(fFmpeg!=null){
            try {
                Log.e("Location",getApplicationContext().getFilesDir().toString());
                String[] complexCommand = new String[]{"-y", "-i", selectedVideos.get(0).toString(), "-i", selectedVideos.get(1).toString(), "-strict", "experimental", "-filter_complex",
                        "[0:v]scale=640x640,setsar=1:1[v0];[1:v]scale=640x640,setsar=1:1[v1];[v0][0:a][v1][1:a] concat=n=2:v=1:a=1",
                        "-ab", "48000", "-ac", "2", "-ar", "22050", "-s", "640x640", "-vcodec", "libx264", "-crf", "27", "-q", "4", "-preset", "ultrafast", rootPath + "output3.mp4"};

                //  String tempo=",\"&&\",\"-y\", \"-i\",rootPath + \"output3.mp4\",\"-i\",selectAudio,\"-c:v copy -c:a aac -strict experimental -map 0:v:0 -map 1:a:0\",rootPath + \"musk.mp4\""

                fFmpeg.execute(complexCommand, new ExecuteBinaryResponseHandler() {

                    @Override
                    public void onSuccess(String s) {
                        showToastmessage(getApplicationContext(),"Succesfully merge videos");
                        loading_progress_xml.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailure(String s) {
                        showToastmessage(getApplicationContext(),s);
                        Log.e("Error",s);
                        loading_progress_xml.setVisibility(View.GONE);
                    }
                    @Override
                    public void onStart() {
                        loading_progress_xml.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onFinish() {

                    }
                });
            }catch (FFmpegCommandAlreadyRunningException e){

            }
        }else{
            fFmpeg=FFmpeg.getInstance(this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {


        if (resultCode == RESULT_OK &&requestCode==SELECT_VIDEOS) {
            selectedVideos = getSelectedVideos(requestCode, data);
            Log.d("path",selectedVideos.toString());
            txtViewShowlist.setText(selectedVideos.toString());
            tempPath=selectedVideos.get(0).toString();

            for(int j=tempPath.length()-1;j>=0;j--){
                if(tempPath.charAt(j)=='/'){
                   len=j;
                   break;
                }

            }
            rootPath=tempPath.substring(0,len+1);
            Log.d("rootPath",rootPath);
            showToastmessage(getApplicationContext(),"Rpptpath is:"+rootPath);

        }if (requestCode == SELECT_AUDIOS && resultCode == RESULT_OK){
            Log.d("path",selectedVideos.toString());
            if ((data != null) && (data.getData() != null)){
                Uri audioFileUri = data.getData();
                String selectedPath = "";
                selectedPath = getPath(getApplicationContext(),audioFileUri);
                selectAudio=selectedPath;
                Log.d("path",selectedVideos.toString());
                txtViewShowlist.setText(selectedVideos.toString()+" , "+selectAudio);
                // Now you can use that Uri to get the file path, or upload it, ...
            }
        }

    }



    private List<String> getSelectedVideos(int requestCode, Intent data) {
        List<String> result = new ArrayList<>();

        ClipData clipData = data.getClipData();
        if(clipData != null) {
            for(int i=0;i<clipData.getItemCount();i++) {
                ClipData.Item videoItem = clipData.getItemAt(i);
                Uri videoURI = videoItem.getUri();
                String filePath = getPath(this, videoURI);
                result.add(filePath);
            }
        }
        else {
            Uri videoURI = data.getData();
            String filePath = getPath(this, videoURI);
            result.add(filePath);
        }

        return result;
    }
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
    //Select videos from device
    private void selectVideos() {
        if (Build.VERSION.SDK_INT <19){
            Intent intent = new Intent();
            intent.setType("video/mp4");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select videos"),SELECT_VIDEOS);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setType("video/mp4");
            startActivityForResult(intent, SELECT_VIDEOS_KITKAT);
        }
    }
}
