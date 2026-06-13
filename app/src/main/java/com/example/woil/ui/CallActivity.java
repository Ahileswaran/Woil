package com.example.woil.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.WindowManager;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.woil.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallActivity extends AppCompatActivity {

    private static final String TAG = "CallActivity";
    private static final int PERMISSION_REQ_CODE = 2002;

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
    private CameraVideoCapturer videoCapturer;
    private VideoSource videoSource;
    private AudioSource audioSource;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;

    private boolean isMuted = false;
    private boolean isVideoPaused = false;
    private AudioManager audioManager;
    private android.media.Ringtone ringtone;
    private final List<IceCandidate> queuedRemoteCandidates = new ArrayList<>();
    private final List<String> seenCandidates = new ArrayList<>();

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

        android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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

        btnAcceptCall = findViewById(R.id.btn_accept_call);
        btnAcceptCall.setOnClickListener(v -> acceptCall());

        tvName.setText(TextUtils.isEmpty(contactName) ? "User" : contactName);
        tvStatus.setText(isCaller ? "Calling..." : "Incoming Call...");

        btnEndCall.setOnClickListener(v -> endCall());
        btnToggleAudio.setOnClickListener(v -> toggleMute());
        btnToggleVideo.setOnClickListener(v -> toggleVideo());
        btnSwitchCamera.setOnClickListener(v -> switchCamera());

        if (isCaller) {
            btnAcceptCall.setVisibility(View.GONE);
            if ("audio".equalsIgnoreCase(callType)) {
                localViewContainer.setVisibility(View.GONE);
                btnToggleVideo.setVisibility(View.GONE);
                btnSwitchCamera.setVisibility(View.GONE);
            }
        } else {
            // Receiver pre-accept layout
            btnToggleVideo.setVisibility(View.GONE);
            btnToggleAudio.setVisibility(View.GONE);
            btnSwitchCamera.setVisibility(View.GONE);
            localViewContainer.setVisibility(View.GONE);
            btnAcceptCall.setVisibility(View.VISIBLE);
        }
    }

    private void setupAudioManager() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }

    private void activateAudioForCall() {
        if (audioManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                android.media.AudioFocusRequest focusRequest = new android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(new android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build())
                        .build();
                audioManager.requestAudioFocus(focusRequest);
            } else {
                audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            }
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn("video".equalsIgnoreCase(callType));
        }
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
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initWebRTC();
            } else {
                Toast.makeText(this, "Camera and Mic permissions are required for calling", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initWebRTC() {
        activateAudioForCall();
        eglBase = EglBase.create();

        // Initialize peer connection factory
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory encoderFactory = new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true);
        DefaultVideoDecoderFactory decoderFactory = new DefaultVideoDecoderFactory(eglBase.getEglBaseContext());

        JavaAudioDeviceModule audioDeviceModule = JavaAudioDeviceModule.builder(this)
                .setUseStereoInput(true)
                .setUseStereoOutput(true)
                .createAudioDeviceModule();

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(audioDeviceModule)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();

        // Initialize SurfaceRenderers
        localView.init(eglBase.getEglBaseContext(), null);
        localView.setMirror(true);
        localView.setEnableHardwareScaler(true);

        remoteView.init(eglBase.getEglBaseContext(), null);
        remoteView.setEnableHardwareScaler(true);

        // Setup local media tracks
        setupLocalTracks();

        // Setup PeerConnection
        createPeerConnection();

        // Setup signaling
        setupSignaling();
    }

    private void setupLocalTracks() {
        // Audio
        audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        localAudioTrack = peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource);
        localAudioTrack.setEnabled(true);

        // Video (only if video call)
        if ("video".equalsIgnoreCase(callType)) {
            videoCapturer = createVideoCapturer();
            if (videoCapturer != null) {
                SurfaceTextureHelper surfaceTextureHelper =
                        SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
                videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
                videoCapturer.initialize(surfaceTextureHelper, this, videoSource.getCapturerObserver());
                videoCapturer.startCapture(1280, 720, 30);

                localVideoTrack = peerConnectionFactory.createVideoTrack("ARDAMSv0", videoSource);
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

        String[] deviceNames = enumerator.getDeviceNames();
        // Try front camera first
        for (String name : deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                CameraVideoCapturer capturer = enumerator.createCapturer(name, null);
                if (capturer != null) return capturer;
            }
        }
        // Fallback to back camera
        for (String name : deviceNames) {
            if (enumerator.isBackFacing(name)) {
                CameraVideoCapturer capturer = enumerator.createCapturer(name, null);
                if (capturer != null) return capturer;
            }
        }
        return null;
    }

    private void createPeerConnection() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {}

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
                        Toast.makeText(CallActivity.this, "Connection lost", Toast.LENGTH_SHORT).show();
                        endCall();
                    }
                });
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {}

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {}

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                // Send candidate to Firestore
                Map<String, Object> candidate = new HashMap<>();
                candidate.put("sdpMid", iceCandidate.sdpMid);
                candidate.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                candidate.put("sdp", iceCandidate.sdp);
                candidate.put("sender", isCaller ? "caller" : "receiver");

                db.collection("calls").document(callId)
                        .collection("iceCandidates").add(candidate);
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {}

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(TAG, "Stream added");
                if (mediaStream.videoTracks.size() > 0) {
                    VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                    runOnUiThread(() -> {
                        remoteVideoTrack.addSink(remoteView);
                    });
                }
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {}

            @Override
            public void onDataChannel(DataChannel dataChannel) {}

            @Override
            public void onRenegotiationNeeded() {}

            @Override
            public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                if (rtpReceiver.track() instanceof VideoTrack) {
                    VideoTrack remoteVideoTrack = (VideoTrack) rtpReceiver.track();
                    runOnUiThread(() -> {
                        remoteVideoTrack.addSink(remoteView);
                    });
                }
            }
        });

        // Add local tracks
        if (localAudioTrack != null) {
            peerConnection.addTrack(localAudioTrack);
        }
        if (localVideoTrack != null) {
            peerConnection.addTrack(localVideoTrack);
        }
    }

    private void setupSignaling() {
        if (isCaller) {
            // Create Call document in Firestore
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
                    .addOnSuccessListener(unused -> startCallerHandshake());
        }

        // Listen for call document updates
        callListener = db.collection("calls").document(callId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    String status = snapshot.getString("status");
                    if ("REJECTED".equalsIgnoreCase(status) || "ENDED".equalsIgnoreCase(status)) {
                        Toast.makeText(this, "Call Ended", Toast.LENGTH_SHORT).show();
                        cleanupAndFinish();
                        return;
                    } else if ("CONNECTED".equalsIgnoreCase(status) && !isCaller) {
                        // Accepted
                        tvStatus.setText("Connected");
                    }

                    if (!isCaller && isCallAccepted && peerConnection != null && snapshot.contains("sdpOffer") && peerConnection.getRemoteDescription() == null) {
                        // Receiver accepts and sets remote description
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

                    if (isCaller && snapshot.contains("sdpAnswer") && peerConnection != null && peerConnection.getRemoteDescription() == null) {
                        // Caller sets remote description on receiver answer
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
                });

        // Listen for ICE Candidates
        candidatesListener = db.collection("calls").document(callId)
                .collection("iceCandidates")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String sender = doc.getString("sender");
                        boolean isRemote = (isCaller && "receiver".equalsIgnoreCase(sender)) ||
                                (!isCaller && "caller".equalsIgnoreCase(sender));

                        if (isRemote) {
                            String sdp = doc.getString("sdp");
                            String sdpMid = doc.getString("sdpMid");
                            Long sdpMLineIndex = doc.getLong("sdpMLineIndex");

                            if (sdp != null && sdpMid != null && sdpMLineIndex != null) {
                                IceCandidate candidate = new IceCandidate(sdpMid, sdpMLineIndex.intValue(), sdp);
                                
                                // De-duplicate candidates
                                if (!seenCandidates.contains(sdp)) {
                                    if (peerConnection != null && peerConnection.getRemoteDescription() != null) {
                                        peerConnection.addIceCandidate(candidate);
                                        seenCandidates.add(sdp);
                                    } else {
                                        queuedRemoteCandidates.add(candidate);
                                    }
                                }
                            }
                        }
                    }
                });
    }

    private void drainRemoteCandidates() {
        runOnUiThread(() -> {
            if (peerConnection != null && peerConnection.getRemoteDescription() != null) {
                for (IceCandidate candidate : queuedRemoteCandidates) {
                    if (!seenCandidates.contains(candidate.sdp)) {
                        peerConnection.addIceCandidate(candidate);
                        seenCandidates.add(candidate.sdp);
                    }
                }
                queuedRemoteCandidates.clear();
            }
        });
    }

    private void startCallerHandshake() {
        MediaConstraints sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "video".equalsIgnoreCase(callType) ? "true" : "false"));

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
                                .update("sdpOffer", sdpOffer);
                    }
                }, sessionDescription);
            }
        }, sdpConstraints);
    }

    private void createAnswer() {
        MediaConstraints sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "video".equalsIgnoreCase(callType) ? "true" : "false"));

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
                                .set(updates, SetOptions.merge());
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
        btnToggleAudio.setImageResource(isMuted ? R.drawable.ic_mic : R.drawable.ic_mic_white);
        Toast.makeText(this, isMuted ? "Microphone Muted" : "Microphone Active", Toast.LENGTH_SHORT).show();
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

    private void endCall() {
        Map<String, Object> updates = new HashMap<>();
        if (!isCaller && !isCallAccepted) {
            updates.put("status", "REJECTED");
        } else {
            updates.put("status", "ENDED");
        }
        updates.put("endedAt", FieldValue.serverTimestamp());

        db.collection("calls").document(callId).set(updates, SetOptions.merge())
                .addOnCompleteListener(task -> cleanupAndFinish());
    }

    private void cleanupAndFinish() {
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException ignored) {}
            videoCapturer.dispose();
            videoCapturer = null;
        }

        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }

        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }

        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
        }

        if (localView != null) {
            localView.release();
        }
        if (remoteView != null) {
            remoteView.release();
        }

        if (callListener != null) {
            callListener.remove();
        }
        if (candidatesListener != null) {
            candidatesListener.remove();
        }
        if (cancelListener != null) {
            cancelListener.remove();
        }

        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }

        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }

        finish();
    }

    private void startRingtone() {
        try {
            android.net.Uri notification = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE);
            ringtone = android.media.RingtoneManager.getRingtone(getApplicationContext(), notification);
            if (ringtone != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
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
                    if (e != null || snapshot == null || !snapshot.exists()) return;
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

        if (checkPermissions()) {
            initWebRTC();
        } else {
            requestPermissions();
        }
    }

    @Override
    protected void onDestroy() {
        cleanupAndFinish();
        super.onDestroy();
    }

    // Standard helper class for SDP observer callbacks
    private static class SimpleSdpObserver implements SdpObserver {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {}

        @Override
        public void onSetSuccess() {}

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
