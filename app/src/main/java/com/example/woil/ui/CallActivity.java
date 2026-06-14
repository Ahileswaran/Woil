package com.example.woil.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.woil.BuildConfig;
import com.example.woil.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CallActivity extends AppCompatActivity {

    private static final String TAG = "CallActivity";
    private static final int PERMISSION_REQ_CODE = 2002;
    private static final String STREAM_ID = "stream0";

    private SurfaceViewRenderer localView;
    private SurfaceViewRenderer remoteView;
    private View localViewContainer;
    private View layoutCallingInfo;
    private ImageView ivAvatar;
    private TextView tvName;
    private TextView tvStatus;

    private FloatingActionButton btnToggleVideo;
    private FloatingActionButton btnToggleAudio;
    private FloatingActionButton btnSwitchCamera;
    private FloatingActionButton btnEndCall;
    private FloatingActionButton btnToggleSpeaker;
    private FloatingActionButton btnAcceptCall;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration callListener;
    private ListenerRegistration candidatesListener;
    private ListenerRegistration cancelListener;

    private String currentUid;
    private String contactUid;
    private String contactName;
    private String callType; // "audio" or "video"
    private String callId;
    private boolean isCaller = false;
    private boolean isCallAccepted = false;

    private EglBase eglBase;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;

    private JavaAudioDeviceModule audioDeviceModule;
    private AudioFocusRequest audioFocusRequest;
    private AudioManager audioManager;

    private CameraVideoCapturer videoCapturer;
    private SurfaceTextureHelper surfaceTextureHelper;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;

    private boolean isMuted = false;
    private boolean isVideoPaused = false;
    private boolean isSpeakerphoneOn = false;
    private android.media.Ringtone ringtone;
    private com.google.firebase.firestore.DocumentSnapshot latestCallSnapshot = null;

    private final List<IceCandidate> queuedRemoteCandidates = new ArrayList<>();
    private final Set<String> seenCandidates = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_call);

        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(1002);
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        Intent intent = getIntent();
        contactUid = intent.getStringExtra("contactUid");
        contactName = intent.getStringExtra("contactName");
        callType = intent.getStringExtra("callType");
        if (callType == null) callType = "video";
        callId = intent.getStringExtra("callId");

        isCaller = TextUtils.isEmpty(callId);
        if (isCaller) {
            callId = currentUid + "_" + contactUid + "_" + System.currentTimeMillis();
        }

        bindViews();
        setupAudioManager();
        loadContactProfile();

        if (isCaller) {
            if (checkPermissions()) {
                initWebRTC();
            } else {
                requestPermissions();
            }
        } else {
            startRingtone();
            listenForIncomingCallCancellation();
        }
    }

    private void bindViews() {
        localView = findViewById(R.id.local_view);
        remoteView = findViewById(R.id.remote_view);
        localViewContainer = findViewById(R.id.local_view_container);
        layoutCallingInfo = findViewById(R.id.layout_calling_info);
        ivAvatar = findViewById(R.id.iv_call_user_avatar);
        tvName = findViewById(R.id.tv_call_user_name);
        tvStatus = findViewById(R.id.tv_call_status);

        btnToggleVideo = findViewById(R.id.btn_toggle_video);
        btnToggleAudio = findViewById(R.id.btn_toggle_audio);
        btnSwitchCamera = findViewById(R.id.btn_switch_camera);
        btnEndCall = findViewById(R.id.btn_end_call);
        btnToggleSpeaker = findViewById(R.id.btn_toggle_speaker);
        btnAcceptCall = findViewById(R.id.btn_accept_call);

        tvName.setText(TextUtils.isEmpty(contactName) ? "User" : contactName);
        tvStatus.setText(isCaller ? "Calling..." : "Incoming Call...");

        btnEndCall.setOnClickListener(v -> endCall());
        btnToggleAudio.setOnClickListener(v -> toggleMute());
        btnToggleVideo.setOnClickListener(v -> toggleVideo());
        btnSwitchCamera.setOnClickListener(v -> switchCamera());
        btnToggleSpeaker.setOnClickListener(v -> toggleSpeaker());
        btnAcceptCall.setOnClickListener(v -> acceptCall());

        // Initialize mute button state (default to active/unmuted)
        isMuted = false;
        btnToggleAudio.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.orange_header_main)
        ));

        // Initialize speakerphone state (video calls default to speaker ON, audio calls default to speaker OFF)
        isSpeakerphoneOn = "video".equalsIgnoreCase(callType);
        btnToggleSpeaker.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, isSpeakerphoneOn ? R.color.orange_header_main : R.color.dark_gray)
        ));

        if (isCaller) {
            btnAcceptCall.setVisibility(View.GONE);
            if ("audio".equalsIgnoreCase(callType)) {
                localViewContainer.setVisibility(View.GONE);
                btnToggleVideo.setVisibility(View.GONE);
                btnSwitchCamera.setVisibility(View.GONE);
            }
        } else {
            btnToggleVideo.setVisibility(View.GONE);
            btnToggleAudio.setVisibility(View.GONE);
            btnSwitchCamera.setVisibility(View.GONE);
            btnToggleSpeaker.setVisibility(View.GONE);
            localViewContainer.setVisibility(View.GONE);
            btnAcceptCall.setVisibility(View.VISIBLE);
        }
    }

    private void setupAudioManager() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    private void activateAudioForCall() {
        if (audioManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build())
                    .build();
            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }

        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(isSpeakerphoneOn);
        audioManager.setMicrophoneMute(false);
    }

    private void loadContactProfile() {
        if (TextUtils.isEmpty(contactUid)) return;

        db.collection("profiles").document(contactUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("displayName");
                        if (TextUtils.isEmpty(name)) {
                            String first = doc.getString("firstName");
                            String last = doc.getString("lastName");
                            name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
                        }
                        if (!TextUtils.isEmpty(name)) {
                            tvName.setText(name);
                        }

                        String avatar = doc.getString("photoUrl");
                        if (TextUtils.isEmpty(avatar)) avatar = doc.getString("photo");
                        if (TextUtils.isEmpty(avatar)) avatar = doc.getString("avatar");

                        if (!TextUtils.isEmpty(avatar)) {
                            Glide.with(CallActivity.this)
                                    .load(avatar)
                                    .placeholder(R.drawable.photo_placeholder)
                                    .error(R.drawable.photo_placeholder)
                                    .into(ivAvatar);
                        }
                    }
                });
    }

    private boolean checkPermissions() {
        if ("audio".equalsIgnoreCase(callType)) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                            == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if ("audio".equalsIgnoreCase(callType)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQ_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQ_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQ_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                granted &= result == PackageManager.PERMISSION_GRANTED;
            }

            if (granted) {
                initWebRTC();
                if (latestCallSnapshot != null) {
                    processCallSnapshot(latestCallSnapshot);
                }
            } else {
                Toast.makeText(this, "Camera/Mic permissions are required for calling",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initWebRTC() {
        if (peerConnectionFactory != null) {
            return;
        }

        activateAudioForCall();

        eglBase = EglBase.create();

        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);
        org.webrtc.Logging.enableLogToDebugOutput(org.webrtc.Logging.Severity.LS_VERBOSE);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        DefaultVideoEncoderFactory encoderFactory =
                new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true);
        DefaultVideoDecoderFactory decoderFactory =
                new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());

        // Note: Forcing software fallback echo cancellation (false) is highly recommended on devices
        // with broken hardware audio integrations (like Unisoc/Spreadtrum chipsets).
        audioDeviceModule = JavaAudioDeviceModule.builder(this)
                .setUseHardwareAcousticEchoCanceler(false)
                .setUseHardwareNoiseSuppressor(false)
                .setUseStereoInput(false)
                .setUseStereoOutput(false)
                .setAudioRecordErrorCallback(new JavaAudioDeviceModule.AudioRecordErrorCallback() {
                    @Override
                    public void onWebRtcAudioRecordInitError(String errorMessage) {
                        Log.e(TAG, "Audio record init error: " + errorMessage);
                    }

                    @Override
                    public void onWebRtcAudioRecordStartError(
                            JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode,
                            String errorMessage) {
                        Log.e(TAG, "Audio record start error: " + errorMessage);
                    }

                    @Override
                    public void onWebRtcAudioRecordError(String errorMessage) {
                        Log.e(TAG, "Audio record runtime error: " + errorMessage);
                    }
                })
                .setAudioTrackErrorCallback(new JavaAudioDeviceModule.AudioTrackErrorCallback() {
                    @Override
                    public void onWebRtcAudioTrackInitError(String errorMessage) {
                        Log.e(TAG, "Audio track init error: " + errorMessage);
                    }

                    @Override
                    public void onWebRtcAudioTrackStartError(
                            JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode,
                            String errorMessage) {
                        Log.e(TAG, "Audio track start error: " + errorMessage);
                    }

                    @Override
                    public void onWebRtcAudioTrackError(String errorMessage) {
                        Log.e(TAG, "Audio track runtime error: " + errorMessage);
                    }
                })
                .createAudioDeviceModule();

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(audioDeviceModule)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();

        localView.init(eglBase.getEglBaseContext(), null);
        localView.setMirror(true);
        localView.setEnableHardwareScaler(true);

        remoteView.init(eglBase.getEglBaseContext(), null);
        remoteView.setEnableHardwareScaler(true);

        setupLocalTracks();
        createPeerConnection();
        setupSignaling();
    }

    private void setupLocalTracks() {
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));

        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio0", audioSource);
        localAudioTrack.setEnabled(!isMuted);

        if ("video".equalsIgnoreCase(callType)) {
            videoCapturer = createVideoCapturer();
            if (videoCapturer != null) {
                surfaceTextureHelper = SurfaceTextureHelper.create(
                        "CaptureThread", eglBase.getEglBaseContext());

                videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
                videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());

                try {
                    videoCapturer.startCapture(1280, 720, 30);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start camera capture", e);
                }

                localVideoTrack = peerConnectionFactory.createVideoTrack("video0", videoSource);
                localVideoTrack.setEnabled(true);
                localVideoTrack.addSink(localView);
            }
        }
    }

    private CameraVideoCapturer createVideoCapturer() {
        CameraEnumerator enumerator;
        if (Camera2Enumerator.isSupported(this)) {
            enumerator = new Camera2Enumerator(this);
        } else {
            enumerator = new Camera1Enumerator(true);
        }

        for (String name : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(name)) {
                CameraVideoCapturer capturer = enumerator.createCapturer(name, null);
                if (capturer != null) return capturer;
            }
        }

        for (String name : enumerator.getDeviceNames()) {
            if (enumerator.isBackFacing(name)) {
                CameraVideoCapturer capturer = enumerator.createCapturer(name, null);
                if (capturer != null) return capturer;
            }
        }

        return null;
    }

    private void createPeerConnection() {
        // --- ICE Server Configuration ---
        // STUN: tells peers their public IP. Works only on WiFi / Full-Cone NAT.
        // TURN: relays all media. REQUIRED on mobile carrier networks (Symmetric NAT).
        // Credentials are read from local.properties → BuildConfig (never committed to git)
        String turnUser = BuildConfig.TURN_USERNAME;
        String turnPass = BuildConfig.TURN_PASSWORD;

        List<PeerConnection.IceServer> iceServers = new ArrayList<>();

        // Google STUN (for same-WiFi / open NAT)
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());

        // Metered.ca TURN relay — required for mobile carrier (Symmetric NAT) networks
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.relay.metered.ca:80")
                .createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80")
                .setUsername(turnUser).setPassword(turnPass).createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turn:global.relay.metered.ca:80?transport=tcp")
                .setUsername(turnUser).setPassword(turnPass).createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turn:global.relay.metered.ca:443")
                .setUsername(turnUser).setPassword(turnPass).createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turn:global.relay.metered.ca:443?transport=tcp")
                .setUsername(turnUser).setPassword(turnPass).createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turns:global.relay.metered.ca:443?transport=tcp")
                .setUsername(turnUser).setPassword(turnPass).createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        // Force TURN relay to bypass Symmetric NAT on mobile networks.
        // Remove this line once voice is confirmed working to allow direct P2P on WiFi.
        rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.RELAY;

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "ICE connection state: " + iceConnectionState);

                runOnUiThread(() -> {
                    if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                        tvStatus.setText("Connected");
                        layoutCallingInfo.setVisibility(View.GONE);
                    } else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED ||
                            iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
                        tvStatus.setText("Disconnected");
                        endCall();
                    }
                });
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Map<String, Object> candidate = new HashMap<>();
                candidate.put("sdpMid", iceCandidate.sdpMid);
                candidate.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                candidate.put("sdp", iceCandidate.sdp);
                candidate.put("sender", isCaller ? "caller" : "receiver");

                db.collection("calls").document(callId)
                        .collection("iceCandidates")
                        .add(candidate)
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to upload ICE candidate to Firestore", e);
                        });
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(TAG, "Stream added: " + mediaStream.getId() + " | Video tracks: " + mediaStream.videoTracks.size() + " | Audio tracks: " + mediaStream.audioTracks.size());
                if (mediaStream.videoTracks.size() > 0) {
                    VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                    runOnUiThread(() -> remoteVideoTrack.addSink(remoteView));
                }
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
            }

            @Override
            public void onRenegotiationNeeded() {
            }

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                MediaStreamTrack track = rtpReceiver.track();
                if (track == null) return;

                if (track instanceof AudioTrack) {
                    AudioTrack remoteAudioTrack = (AudioTrack) track;
                    remoteAudioTrack.setEnabled(true);
                    Log.d(TAG, "Remote audio track received and enabled");
                } else if (track instanceof VideoTrack) {
                    VideoTrack remoteVideoTrack = (VideoTrack) track;
                    runOnUiThread(() -> remoteVideoTrack.addSink(remoteView));
                    Log.d(TAG, "Remote video track received");
                }
            }
        });

        if (peerConnection == null) {
            throw new IllegalStateException("Failed to create PeerConnection");
        }

        // Add local tracks using a stream ID so the receiver gets audio/video reliably.
        if (localAudioTrack != null) {
            List<String> streamIds = Arrays.asList(STREAM_ID);
            RtpSender audioSender = peerConnection.addTrack(localAudioTrack, streamIds);
            Log.d(TAG, "Audio sender created: " + (audioSender != null));
        }

        if (localVideoTrack != null) {
            List<String> streamIds = Arrays.asList(STREAM_ID);
            RtpSender videoSender = peerConnection.addTrack(localVideoTrack, streamIds);
            Log.d(TAG, "Video sender created: " + (videoSender != null));
        }
    }

    private void setupSignaling() {
        if (isCaller) {
            Map<String, Object> call = new HashMap<>();
            call.put("callId", callId);
            call.put("callerUid", currentUid);
            call.put("receiverUid", contactUid);
            call.put("callerName", "User");
            call.put("receiverName", contactName);
            call.put("type", callType);
            call.put("status", "INITIATED");
            call.put("createdAt", FieldValue.serverTimestamp());

            db.collection("calls").document(callId).set(call)
                    .addOnSuccessListener(unused -> startCallerHandshake())
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to create call document", e));
        }

        callListener = db.collection("calls").document(callId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Firestore snapshot listener error", e);
                        return;
                    }
                    if (snapshot == null) return;
                    latestCallSnapshot = snapshot;
                    processCallSnapshot(snapshot);
                });

        candidatesListener = db.collection("calls").document(callId)
                .collection("iceCandidates")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Firestore candidates listener error", error);
                        return;
                    }
                    if (snapshot == null) return;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String sender = doc.getString("sender");
                        boolean isRemote = (isCaller && "receiver".equalsIgnoreCase(sender)) ||
                                (!isCaller && "caller".equalsIgnoreCase(sender));

                        if (!isRemote) continue;

                        String sdp = doc.getString("sdp");
                        String sdpMid = doc.getString("sdpMid");
                        Long sdpMLineIndex = doc.getLong("sdpMLineIndex");

                        if (sdp == null || sdpMid == null || sdpMLineIndex == null) continue;

                        String candidateKey = sdpMid + ":" + sdpMLineIndex + ":" + sdp;

                        if (seenCandidates.contains(candidateKey)) {
                            continue;
                        }

                        IceCandidate candidate = new IceCandidate(
                                sdpMid,
                                sdpMLineIndex.intValue(),
                                sdp
                        );

                        if (peerConnection != null && peerConnection.getRemoteDescription() != null) {
                            peerConnection.addIceCandidate(candidate);
                            seenCandidates.add(candidateKey);
                        } else {
                            queuedRemoteCandidates.add(candidate);
                            seenCandidates.add(candidateKey);
                        }
                    }
                });
    }

    private void processCallSnapshot(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) return;

        String status = snapshot.getString("status");
        if ("REJECTED".equalsIgnoreCase(status) || "ENDED".equalsIgnoreCase(status)) {
            Toast.makeText(this, "Call Ended", Toast.LENGTH_SHORT).show();
            cleanupAndFinish();
            return;
        } else if ("CONNECTED".equalsIgnoreCase(status) && !isCaller) {
            tvStatus.setText("Connected");
        }

        if (!isCaller && isCallAccepted && peerConnection != null
                && snapshot.contains("sdpOffer")
                && peerConnection.getRemoteDescription() == null) {

            Map<String, Object> sdpMap = (Map<String, Object>) snapshot.get("sdpOffer");
            if (sdpMap != null) {
                String sdp = (String) sdpMap.get("sdp");
                SessionDescription offer = new SessionDescription(SessionDescription.Type.OFFER, sdp);

                peerConnection.setRemoteDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        drainRemoteCandidates();
                        createAnswer();
                    }
                }, offer);
            }
        }

        if (isCaller && snapshot.contains("sdpAnswer") && peerConnection != null
                && peerConnection.getRemoteDescription() == null) {

            Map<String, Object> sdpMap = (Map<String, Object>) snapshot.get("sdpAnswer");
            if (sdpMap != null) {
                String sdp = (String) sdpMap.get("sdp");
                SessionDescription answer = new SessionDescription(SessionDescription.Type.ANSWER, sdp);

                peerConnection.setRemoteDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        drainRemoteCandidates();
                    }
                }, answer);
            }
        }
    }


    private void drainRemoteCandidates() {
        if (peerConnection == null || peerConnection.getRemoteDescription() == null) return;

        for (IceCandidate candidate : queuedRemoteCandidates) {
            peerConnection.addIceCandidate(candidate);
        }
        queuedRemoteCandidates.clear();
    }

    private void startCallerHandshake() {
        MediaConstraints sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo",
                "video".equalsIgnoreCase(callType) ? "true" : "false"));

        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        Map<String, Object> sdpOffer = new HashMap<>();
                        sdpOffer.put("type", "offer");
                        sdpOffer.put("sdp", sessionDescription.description);

                        db.collection("calls").document(callId)
                                .update("sdpOffer", sdpOffer)
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to upload SDP offer", e));
                    }
                }, sessionDescription);
            }
        }, sdpConstraints);
    }

    private void createAnswer() {
        MediaConstraints sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo",
                "video".equalsIgnoreCase(callType) ? "true" : "false"));

        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SimpleSdpObserver() {
                    @Override
                    public void onSetSuccess() {
                        Map<String, Object> sdpAnswer = new HashMap<>();
                        sdpAnswer.put("type", "answer");
                        sdpAnswer.put("sdp", sessionDescription.description);

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("sdpAnswer", sdpAnswer);
                        updates.put("status", "CONNECTED");

                        db.collection("calls").document(callId)
                                .set(updates, SetOptions.merge())
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to upload SDP answer", e));
                    }
                }, sessionDescription);
            }
        }, sdpConstraints);
    }

    private void toggleMute() {
        isMuted = !isMuted;
        if (localAudioTrack != null) {
            localAudioTrack.setEnabled(!isMuted);
        }
        btnToggleAudio.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, isMuted ? R.color.dark_gray : R.color.orange_header_main)
        ));
    }

    private void toggleVideo() {
        isVideoPaused = !isVideoPaused;
        if (localVideoTrack != null) {
            localVideoTrack.setEnabled(!isVideoPaused);
        }
        localViewContainer.setVisibility(isVideoPaused ? View.GONE : View.VISIBLE);
        btnToggleVideo.setImageResource(isVideoPaused ? R.drawable.ic_camera : R.drawable.ic_video_white);
    }

    private void switchCamera() {
        if (videoCapturer != null) {
            videoCapturer.switchCamera(null);
        }
    }

    private void toggleSpeaker() {
        if (audioManager != null) {
            isSpeakerphoneOn = !isSpeakerphoneOn;
            audioManager.setSpeakerphoneOn(isSpeakerphoneOn);
            btnToggleSpeaker.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, isSpeakerphoneOn ? R.color.orange_header_main : R.color.dark_gray)
            ));
        }
    }

    private void endCall() {
        Map<String, Object> updates = new HashMap<>();
        if (!isCaller && !isCallAccepted) {
            updates.put("status", "REJECTED");
        } else {
            updates.put("status", "ENDED");
        }
        updates.put("endedAt", FieldValue.serverTimestamp());

        db.collection("calls").document(callId)
                .set(updates, SetOptions.merge())
                .addOnCompleteListener(task -> cleanupAndFinish())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update end call status in Firestore", e);
                    cleanupAndFinish();
                });
    }

    private void cleanupAndFinish() {

        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                Log.e(TAG, "Error stopping capture", e);
            }
            try {
                videoCapturer.dispose();
            } catch (Exception ignored) {
            }
            videoCapturer = null;
        }

        if (surfaceTextureHelper != null) {
            try {
                surfaceTextureHelper.dispose();
            } catch (Exception ignored) {
            }
            surfaceTextureHelper = null;
        }

        if (videoSource != null) {
            try {
                videoSource.dispose();
            } catch (Exception ignored) {
            }
            videoSource = null;
        }

        if (audioSource != null) {
            try {
                audioSource.dispose();
            } catch (Exception ignored) {
            }
            audioSource = null;
        }

        if (peerConnection != null) {
            try {
                peerConnection.close();
            } catch (Exception ignored) {
            }
            try {
                peerConnection.dispose();
            } catch (Exception ignored) {
            }
            peerConnection = null;
        }

        if (peerConnectionFactory != null) {
            try {
                peerConnectionFactory.dispose();
            } catch (Exception ignored) {
            }
            peerConnectionFactory = null;
        }

        if (audioDeviceModule != null) {
            try {
                audioDeviceModule.release();
            } catch (Exception ignored) {
            }
            audioDeviceModule = null;
        }

        if (eglBase != null) {
            try {
                eglBase.release();
            } catch (Exception ignored) {
            }
            eglBase = null;
        }

        if (localView != null) {
            try {
                localView.release();
            } catch (Exception ignored) {
            }
        }

        if (remoteView != null) {
            try {
                remoteView.release();
            } catch (Exception ignored) {
            }
        }

        if (callListener != null) {
            callListener.remove();
            callListener = null;
        }

        if (candidatesListener != null) {
            candidatesListener.remove();
            candidatesListener = null;
        }

        if (cancelListener != null) {
            cancelListener.remove();
            cancelListener = null;
        }

        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }

        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(false);
            audioManager.setMicrophoneMute(false);
            audioManager.setMode(AudioManager.MODE_NORMAL);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            } else {
                audioManager.abandonAudioFocus(null);
            }
        }

        finish();
    }

    private void startRingtone() {
        try {
            android.net.Uri notification = android.media.RingtoneManager.getDefaultUri(
                    android.media.RingtoneManager.TYPE_RINGTONE);
            ringtone = android.media.RingtoneManager.getRingtone(getApplicationContext(), notification);
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone.setLooping(true);
                }
                ringtone.play();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to play ringtone", e);
        }
    }

    private void listenForIncomingCallCancellation() {
        cancelListener = db.collection("calls").document(callId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Firestore cancellation listener error", e);
                        return;
                    }
                    if (snapshot == null || !snapshot.exists()) return;
                    latestCallSnapshot = snapshot;

                    String status = snapshot.getString("status");
                    if ("REJECTED".equalsIgnoreCase(status) || "ENDED".equalsIgnoreCase(status)) {
                        Toast.makeText(this, "Call Ended", Toast.LENGTH_SHORT).show();
                        cleanupAndFinish();
                    }
                });
    }

    private void acceptCall() {
        isCallAccepted = true;

        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }

        if (cancelListener != null) {
            cancelListener.remove();
            cancelListener = null;
        }

        btnAcceptCall.setVisibility(View.GONE);

        if ("video".equalsIgnoreCase(callType)) {
            btnToggleVideo.setVisibility(View.VISIBLE);
            btnSwitchCamera.setVisibility(View.VISIBLE);
            localViewContainer.setVisibility(View.VISIBLE);
        }

        btnToggleAudio.setVisibility(View.VISIBLE);
        btnToggleSpeaker.setVisibility(View.VISIBLE);

        if (checkPermissions()) {
            initWebRTC();
            if (latestCallSnapshot != null) {
                processCallSnapshot(latestCallSnapshot);
            }
        } else {
            requestPermissions();
        }
    }

    @Override
    protected void onDestroy() {
        cleanupAndFinish();
        super.onDestroy();
    }

    private static class SimpleSdpObserver implements SdpObserver {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
        }

        @Override
        public void onSetSuccess() {
        }

        @Override
        public void onCreateFailure(String s) {
            Log.e(TAG, "SDP Create Failure: " + s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.e(TAG, "SDP Set Failure: " + s);
        }
    }
}
