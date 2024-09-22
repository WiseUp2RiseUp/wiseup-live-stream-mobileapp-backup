package io.openvidu.openvidu_android.activities;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import org.jetbrains.annotations.NotNull;
import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.openvidu.openvidu_android.R;
import io.openvidu.openvidu_android.fragments.PermissionsDialogFragment;
import io.openvidu.openvidu_android.openvidu.LocalParticipant;
import io.openvidu.openvidu_android.openvidu.RemoteParticipant;
import io.openvidu.openvidu_android.openvidu.Session;
import io.openvidu.openvidu_android.utils.CustomHttpClient;
import io.openvidu.openvidu_android.websocket.CustomWebSocket;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.hardware.usb.*;


public class SessionActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 101;
    private static final int MY_PERMISSIONS_REQUEST = 102;
    private final String TAG = "SessionActivity";
    @BindView(R.id.views_container)
    LinearLayout views_container;
    @BindView(R.id.start_finish_call)
    Button start_finish_call;

    @BindView(R.id.go_live)
    Button go_live;

    @BindView(R.id.usb_check)
    CheckBox usb_check;

    @BindView(R.id.cam_check)
    CheckBox cam_check;

    @BindView(R.id.session_name)
    EditText session_name;
    @BindView(R.id.participant_name)
    EditText participant_name;
    @BindView(R.id.application_server_url)
    EditText application_server_url;
    @BindView(R.id.local_gl_surface_view)
    SurfaceViewRenderer localVideoView;
    @BindView(R.id.main_participant)
    TextView main_participant;
    @BindView(R.id.selectedPersion)
    TextView selectedPersion;
    @BindView(R.id.peer_container)
    FrameLayout peer_container;

    @BindView(R.id.spinner1)
    Spinner chanel;

    private String APPLICATION_SERVER_URL;
    private Session session;
    private CustomHttpClient httpClient;
    private EglBase rootEglBase = EglBase.create();

    private  CustomWebSocket webSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        askForPermissions();
        ButterKnife.bind(this);
        Random random = new Random();
        int randomIndex = random.nextInt(100);
        participant_name.setText(participant_name.getText().append(String.valueOf(randomIndex)));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

        // Create a list of items for the spinner.
        String[] items = new String[]{"Cayman Sports", "Front Stage", "Bigger Picture", "Default"};

        // Create an adapter to describe how the items are displayed.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);

        // Set the spinner's adapter to the previously created one.
        chanel.setAdapter(adapter);
        chanel.setOnItemSelectedListener(this);

        getSupportActionBar().hide();
        if (!arePermissionGranted()) {
            askForPermissions();
        }

        go_live.setOnClickListener(this::goLiveClick);
        requestPermission1();
        initViews();

        usb_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               if(usb_check.isChecked()) cam_check.setEnabled(false);
               else  cam_check.setEnabled(true);
            }
        });
        cam_check.setEnabled(false);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {

        switch (position) {
            case 0:
               session_name.setText("cayman-sports-live-2");
                break;
            case 1:
                session_name.setText("front-stage-live");
                break;
            case 2:
                session_name.setText("the-bigger-picture-live");
                break;
            case 3:
                Random random = new Random();
                int randomIndex = random.nextInt(100);
                session_name.setText("WiseUp-" + randomIndex);
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Random random = new Random();
        int randomIndex = random.nextInt(100);
        session_name.setText("WiseUp-" + randomIndex);
    }

    public void askForPermissions() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST);
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }
    }

    public void buttonPressed(View view) {
        if (start_finish_call.getText().equals(getResources().getString(R.string.hang_up))) {
            // Already connected to a session
            leaveSession();
            chanel.setVisibility(View.VISIBLE);

            go_live.setEnabled(false);
            usb_check.setEnabled(true);
            if(!usb_check.isChecked()) cam_check.setEnabled(true);
            return;
        }
        if (session_name.getText().length() == 0) {
            return;
        } else {
            chanel.setVisibility(View.GONE);
        }

        if(session_name.getText().length()==0) return;
        else chanel.setVisibility(View.GONE);

        if (arePermissionGranted()) {

            usb_check.setEnabled(false);
            cam_check.setEnabled(false);

            viewToConnectingState();

            APPLICATION_SERVER_URL = application_server_url.getText().toString();
            httpClient = new CustomHttpClient(APPLICATION_SERVER_URL);

            String sessionId = session_name.getText().toString();
            getToken(sessionId);

        } else {
            DialogFragment permissionsFragment = new PermissionsDialogFragment();
            permissionsFragment.show(getSupportFragmentManager(), "Permissions Fragment");
        }
    }

    public void goLiveClick(View view) {
        Log.e(TAG,"Clicked");
        runOnUiThread(()->{
            go_live.setEnabled(false);
        });
    }


    private void getToken(String sessionId) {
        try {
            // Session Request
            RequestBody sessionBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "{\"customSessionId\": \"" + sessionId + "\"}");
            httpClient.httpCall("/api/sessions", "POST", "application/json", sessionBody, new Callback() {

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    Log.d(TAG, "responseString: " + response.body().string());

                    // Token Request
                    RequestBody tokenBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "{}");
                    httpClient.httpCall("/api/sessions/" + sessionId + "/connections", "POST", "application/json", tokenBody, new Callback() {

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) {
                            String responseString = null;
                            try {
                                responseString = response.body().string();
                            } catch (IOException e) {
                                Log.e(TAG, "Error getting body", e);
                            }
                            getTokenSuccess(responseString, sessionId);
                        }

                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            Log.e(TAG, "Error POST /api/sessions/SESSION_ID/connections", e);
                            connectionError(APPLICATION_SERVER_URL);
                        }
                    });
                }

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Log.e(TAG, "Error POST /api/sessions", e);
                    connectionError(APPLICATION_SERVER_URL);
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error getting token", e);
            e.printStackTrace();
            connectionError(APPLICATION_SERVER_URL);
        }
    }

    private void requestPermission1() {
        UsbManager manager = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        }
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(getIntent()),
                PendingIntent.FLAG_IMMUTABLE);
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            manager.requestPermission(device, mPermissionIntent);
        }

    }

    private void getTokenSuccess(String token, String sessionId) {
        // Initialize our session
        session = new Session(sessionId, token, views_container, this);

        // Initialize our local participant and start local camera
        String participantName = participant_name.getText().toString();
        LocalParticipant localParticipant = new LocalParticipant(participantName, session, getApplicationContext(), localVideoView);
        localParticipant.startCamera(usb_check.isChecked(), cam_check.isChecked());
        runOnUiThread(() -> {
            // Update local participant view
            main_participant.setText(participant_name.getText().toString());
            main_participant.setPadding(20, 3, 20, 3);

            localVideoView.setOnClickListener((v)->{
                selectedPersion.setText("Selected User ID: " + participantName.substring(participantName.indexOf("-")+1));
            });
        });

        // Initialize and connect the websocket to OpenVidu Server
        startWebSocket();
    }

    private void startWebSocket() {
        webSocket = new CustomWebSocket(session, this);
        webSocket.execute();
        session.setWebSocket(webSocket);
    }

    private void connectionError(String url) {
        Runnable myRunnable = () -> {
            Toast toast = Toast.makeText(this, "Error connecting to " + url, Toast.LENGTH_LONG);
            toast.show();
            viewToDisconnectedState();
        };
        new Handler(getMainLooper()).post(myRunnable);
    }

    private void initViews() {
        localVideoView.init(rootEglBase.getEglBaseContext(), null);
//        localVideoView.setMirror(true);
        localVideoView.setEnableHardwareScaler(true);
        localVideoView.setZOrderMediaOverlay(true);
    }

    public void viewToDisconnectedState() {
        runOnUiThread(() -> {
            try {
                localVideoView.clearImage();
//                localVideoView.release();
                start_finish_call.setText(getResources().getString(R.string.start_button));
                start_finish_call.setEnabled(true);
                application_server_url.setEnabled(true);
                application_server_url.setFocusableInTouchMode(true);
                session_name.setEnabled(true);
                session_name.setFocusableInTouchMode(true);
                participant_name.setEnabled(true);
                participant_name.setFocusableInTouchMode(true);
                main_participant.setText(null);
                main_participant.setPadding(0, 0, 0, 0);

                go_live.setEnabled(false);
            }
            catch (Exception e) {
                Log.e(TAG, e.getMessage().toString());
            }
        });
    }

    public void viewToConnectingState() {
        runOnUiThread(() -> {
            start_finish_call.setEnabled(false);
            application_server_url.setEnabled(false);
            application_server_url.setFocusable(false);
            session_name.setEnabled(false);
            session_name.setFocusable(false);
            participant_name.setEnabled(false);
            participant_name.setFocusable(false);
        });
    }

    public void viewToConnectedState() {
        runOnUiThread(() -> {
            start_finish_call.setText(getResources().getString(R.string.hang_up));
            start_finish_call.setEnabled(true);
        });
    }

    public void createRemoteParticipantVideo(final RemoteParticipant remoteParticipant) {
        Handler mainHandler = new Handler(getMainLooper());
        Runnable myRunnable = () -> {
            View rowView = getLayoutInflater().inflate(R.layout.peer_video, null);

            int rowId = View.generateViewId();
            rowView.setId(rowId);
            views_container.addView(rowView);
            SurfaceViewRenderer videoView = (SurfaceViewRenderer) ((ViewGroup) rowView).getChildAt(0);
            remoteParticipant.setVideoView(videoView);
//            videoView.setMirror(true);
            videoView.init(rootEglBase.getEglBaseContext(), null);
            videoView.setZOrderMediaOverlay(true);

            View textView = ((ViewGroup) rowView).getChildAt(1);
            remoteParticipant.setParticipantNameText((TextView) textView);
            remoteParticipant.setView(rowView);

            remoteParticipant.getParticipantNameText().setText(remoteParticipant.getParticipantName());
            remoteParticipant.getParticipantNameText().setPadding(20, 3, 20, 3);

            videoView.setOnClickListener((v)->{
                selectedPersion.setText("Selected User ID: " + remoteParticipant.getParticipantName().substring(remoteParticipant.getParticipantName().indexOf("-")+1));
            });
        };
        mainHandler.post(myRunnable);
    }

    public void setRemoteMediaStream(MediaStream stream, final RemoteParticipant remoteParticipant) {
        final VideoTrack videoTrack = stream.videoTracks.get(0);
        videoTrack.addSink(remoteParticipant.getVideoView());
        runOnUiThread(() -> remoteParticipant.getVideoView().setVisibility(View.VISIBLE));
    }

   public void leaveSession() {
        if(session != null) {
            session.leaveSession();
        }
        if(httpClient != null) {
            httpClient.dispose();
        }
        viewToDisconnectedState();
    }

    public EglBase getRootEglBase() {
        return rootEglBase;
    }

    private boolean arePermissionGranted() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_DENIED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_DENIED);
    }

    @Override
    protected void onDestroy() {
        leaveSession();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        leaveSession();
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        leaveSession();
        super.onStop();
    }

}
