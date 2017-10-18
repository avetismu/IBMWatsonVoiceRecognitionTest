package com.test.morfus.ibmwatsontest;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.view.View;
import android.widget.TextView;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Base64;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Random;


import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;



public class MainActivity extends Activity {

    //Fields

    private boolean isRecording = false;
    private ImageButton recordButton;
    private TextView instructionText;
    private TextView resultsText;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private FFmpeg ffmpeg;
    String filename;
    File file;
    private Thread thread;
    private String results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //set updated view
        this.setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();

        recordButton = (ImageButton) findViewById(R.id.record_button);
        instructionText = (TextView) findViewById(R.id.instructionText);
        resultsText = (TextView) findViewById(R.id.resultsText) ;

        setUpPlayer();

        //setup FFmpeg
        ffmpeg = FFmpeg.getInstance(getApplicationContext());
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {}

                @Override
                public void onFailure() {}

                @Override
                public void onSuccess() {}

                @Override
                public void onFinish() {}
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
        }
    }

    public void RecordClick(View v){
        if(isRecording == false){
            isRecording = true;
            instructionText.setText(getApplicationContext().getString(R.string.recording_msg));
            recordButton.setBackground(getApplicationContext().getDrawable(R.drawable.pause_red));
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
            StartRecording();

        }
        else{
            isRecording = false;
            instructionText.setText(getApplicationContext().getString(R.string.record_instruction));
            recordButton.setBackground(getApplicationContext().getDrawable(R.drawable.record));
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
            StopRecording();


        }
    }


    public void StartRecording(){


        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            Log.d("Recording", "Recording...");
        }
        catch(Exception e){
            isRecording = false;
            e.printStackTrace();
        }




    }

    public void StopRecording(){

        try {
            mediaRecorder.stop();
            mediaRecorder.release();


            //convert
            try {
                // to execute "ffmpeg -version" command you just need to pass "-version"
                final String newfilename = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/audio-file.wav";
                String cmd[] = new String[4];
                //cmd[0] = "-version";
                cmd[0] = "-i";
                cmd[2] = "-y";
                cmd[1] = filename ;
                cmd[3] = newfilename;

                ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                    @Override
                    public void onStart() {Log.i("FFmpeg", "Command Started");}

                    @Override
                    public void onProgress(String message) {Log.i("FFmpeg", message);}

                    @Override
                    public void onFailure(String message) { Log.e("FFmpeg", message);}

                    @Override
                    public void onSuccess(String message) {
                        Log.i("FFmpeg", message);
                        filename = newfilename;

                        //media player setup
                        mediaPlayer = new MediaPlayer();
                        try {
                            mediaPlayer.setDataSource(filename);
                        }
                        catch (IOException e){
                            Log.e("Error", e.getMessage());
                        }

                        try {
                            mediaPlayer.prepare();
                            mediaPlayer.start();
                            while (mediaPlayer.isPlaying()) {
                                //Log.d("Player", "Currently Playing");
                            }
                            mediaPlayer.stop();
                            mediaPlayer.release();

                            thread = new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    try  {
                                        HTTPSendFile();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                            thread.start();
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                        setUpPlayer();
                    }

                    @Override
                    public void onFinish() {
                        try {
                            thread.join();
                            resultsText.setText(results);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                });

            } catch (FFmpegCommandAlreadyRunningException e) {
                // Handle if FFmpeg is already running
            }




        }
        catch(Exception e){
            e.printStackTrace();
        }

        setUpPlayer();

    }

    public void HTTPSendFile(){
        try{
            //prepare file



            RandomAccessFile f = new RandomAccessFile(filename, "r");

            /*
            byte[] b = null;

            try {
                InputStream in_s = getResources().openRawResource(R.raw.audio_file);

                b = new byte[in_s.available()];
                in_s.read(b);
            } catch (Exception e) {
                e.printStackTrace();
            }
            */
            byte[] b = new byte[(int)f.length()];
            f.readFully(b);
            Log.i("File Status", "File has been converted to bytes");
            Log.i("File Status", file.getPath());
            Log.i("File Status", filename);




            String url = "https://stream.watsonplatform.net/speech-to-text/api/v1/models/en-US_NarrowbandModel/recognize";
            URL obj = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
            con.setRequestMethod("POST");

            //auth message
            String auth = Base64.encodeToString("c28b358e-3a72-4bee-b2c2-f82fd8e534db:y1P2lpGLiANC".getBytes(), Base64.DEFAULT);
            con.setRequestProperty("Authorization", "Basic " + auth);
            //Log.i("IBM Watson Request", "Authorization: " + auth);

            //content type
            con.setRequestProperty("Content-Type", "audio/wav");

            //transfer encoding
            //con.setRequestProperty("Transfer-Encoding", "chunked");

            con.setDoOutput(true);
            //con.getOutputStream().write(f.read());

            try( DataOutputStream wr = new DataOutputStream( con.getOutputStream())) {
                wr.write( b );
                wr.flush();
                wr.close();
            }

            Log.i("IBM Watson Response", Integer.toString(con.getResponseCode()));
            Log.i("IBM Watson Response", con.getResponseMessage());

            BufferedReader r = null;
            if(con.getResponseCode() == 200){
                r = new BufferedReader(new InputStreamReader(con.getInputStream()));
            }
            else{
                r = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }


            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line).append('\n');
            }
            Log.i("IBM Watson Response", total.toString());
            results = total.toString();



        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    private void setUpPlayer() {
        //recorder set up


        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //filename = getFilesDir() + "/audio-file.3gp";
        //file = new File(getFilesDir(), "audio-file.3gp");
        filename = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/audio-file.3gp";
        file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "audio-file.3gp");
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(filename);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        //mediaRecorder.setAudioSamplingRate(48000);
        //mediaRecorder.setAudioEncodingBitRate(384000);

    }
}


