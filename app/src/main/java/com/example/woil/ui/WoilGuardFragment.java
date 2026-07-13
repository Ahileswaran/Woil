package com.example.woil.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.woil.R;

import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WoilGuardFragment extends Fragment {

    private static final int REQ_BLE = 1001;
    private static final String DEVICE_NAME = "WoilGuard-ESP32";

    private static final UUID SERVICE_UUID =
            UUID.fromString("12345678-1234-1234-1234-1234567890ab");
    private static final UUID STATUS_UUID =
            UUID.fromString("12345678-1234-1234-1234-1234567890ac");
    private static final UUID CMD_UUID =
            UUID.fromString("12345678-1234-1234-1234-1234567890ad");

    private static final UUID CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private RadioGroup rgMode;
    private TextView tvConnectionStatus;
    private TextView tvGuardData;
    private TextView tvDeviceState;
    private TextView tvBattery;
    private TextView tvIncidentType;
    private TextView tvMotionScore;
    private TextView tvAudioScore;
    private TextView tvIncidentLog;
    private Button btnConnectGuard;
    private Button btnReadStatus;
    private Button btnSendTest;
    private Button btnConfirmAlert;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic statusCharacteristic;
    private BluetoothGattCharacteristic cmdCharacteristic;

    private final LinkedList<String> incidentLogs = new LinkedList<>();

    private final Handler liveRefreshHandler = new Handler(Looper.getMainLooper());
    private boolean autoRefreshEnabled = false;
    
    private boolean hasFiredPanicAlert = false;

    private final Runnable liveRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) return;

            if (autoRefreshEnabled && bluetoothGatt != null && statusCharacteristic != null) {
                readBleStatus();
                liveRefreshHandler.postDelayed(this, 1000);
            }
        }
    };

    // latest parsed wearable data
    private String latestState = "IDLE";
    private String latestBattery = "--";
    private String latestIncident = "NONE";
    private String latestMotion = "0.00";
    private String latestAudio = "0.00";
    private String latestSeverity = "LOG";
    private String latestFallProb = "0.00";
    private String latestModel = "--";
    private String latestTimestamp = "";
    private String latestRawPayload = "";

    public WoilGuardFragment() {
        super(R.layout.fragment_woilgurad);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_woilgurad, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupBluetooth();
        initializeUi();
        wireActions();
    }

    private void bindViews(@NonNull View view) {
        rgMode = view.findViewById(R.id.rgCommunicationMode);
        tvConnectionStatus = view.findViewById(R.id.tvConnectionStatus);
        tvGuardData = view.findViewById(R.id.tvGuardData);
        tvDeviceState = view.findViewById(R.id.tvDeviceState);
        tvBattery = view.findViewById(R.id.tvBattery);
        tvIncidentType = view.findViewById(R.id.tvIncidentType);
        tvMotionScore = view.findViewById(R.id.tvMotionScore);
        tvAudioScore = view.findViewById(R.id.tvAudioScore);
        tvIncidentLog = view.findViewById(R.id.tvIncidentLog);

        btnConnectGuard = view.findViewById(R.id.btnConnectGuard);
        btnReadStatus = view.findViewById(R.id.btnReadStatus);
        btnSendTest = view.findViewById(R.id.btnSendTest);
        btnConfirmAlert = view.findViewById(R.id.btnConfirmAlert);
    }

    private void setupBluetooth() {
        BluetoothManager bluetoothManager =
                (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
    }

    private void initializeUi() {
        tvConnectionStatus.setText("Disconnected");
        tvGuardData.setText("Waiting for wearable data...");
        tvDeviceState.setText("State: IDLE");
        tvBattery.setText("Battery: --%");
        tvIncidentType.setText("Incident: NONE");
        tvMotionScore.setText("Motion score: 0.00");
        tvAudioScore.setText("Audio score: 0.00");
        tvIncidentLog.setText("No incidents yet");
    }

    private void wireActions() {
        btnConnectGuard.setOnClickListener(v -> {
            int checkedId = rgMode.getCheckedRadioButtonId();
            if (checkedId == R.id.rbBluetooth) {
                connectUsingBle();
            } else {
                toast("Wi-Fi flow can be added later. BLE is primary for now.");
            }
        });

        btnReadStatus.setOnClickListener(v -> {
            int checkedId = rgMode.getCheckedRadioButtonId();
            if (checkedId == R.id.rbBluetooth) {
                readBleStatus();
            } else {
                toast("Wi-Fi flow can be added later.");
            }
        });

        btnSendTest.setOnClickListener(v -> {
            int checkedId = rgMode.getCheckedRadioButtonId();
            if (checkedId == R.id.rbBluetooth) {
                sendBleCommand("PANIC_TEST");
            } else {
                toast("Wi-Fi flow can be added later.");
            }
        });

        btnConfirmAlert.setOnClickListener(v -> openPanicAlert());
    }

    private void startAutoRefresh() {
        if (autoRefreshEnabled) return;

        autoRefreshEnabled = true;
        liveRefreshHandler.removeCallbacks(liveRefreshRunnable);
        liveRefreshHandler.post(liveRefreshRunnable);
    }

    private void stopAutoRefresh() {
        autoRefreshEnabled = false;
        liveRefreshHandler.removeCallbacks(liveRefreshRunnable);
    }

    // ----------------------------------------------------
    // BLE FLOW
    // ----------------------------------------------------

    private void connectUsingBle() {
        if (!hasBlePermissions()) {
            requestBlePermissions();
            return;
        }

        if (bluetoothAdapter == null) {
            tvConnectionStatus.setText("Bluetooth not supported on this phone");
            return;
        }

        try {
            if (!bluetoothAdapter.isEnabled()) {
                tvConnectionStatus.setText("Enable Bluetooth first");
                return;
            }
        } catch (SecurityException e) {
            tvConnectionStatus.setText("Bluetooth permission denied");
            return;
        }

        if (!hasBluetoothConnectPermission()) {
            requestBlePermissions();
            return;
        }

        tvConnectionStatus.setText("Searching paired BLE devices...");

        try {
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            if (bondedDevices == null || bondedDevices.isEmpty()) {
                tvConnectionStatus.setText("No paired Bluetooth devices found");
                return;
            }

            for (BluetoothDevice device : bondedDevices) {
                String deviceName;
                try {
                    if (!hasBluetoothConnectPermission()) {
                        requestBlePermissions();
                        return;
                    }
                    deviceName = device.getName();
                } catch (SecurityException e) {
                    tvConnectionStatus.setText("Cannot read Bluetooth device name");
                    return;
                }

                if (DEVICE_NAME.equals(deviceName)) {
                    try {
                        if (!hasBluetoothConnectPermission()) {
                            requestBlePermissions();
                            return;
                        }

                        if (bluetoothGatt != null) {
                            try {
                                bluetoothGatt.close();
                            } catch (SecurityException ignored) {
                            }
                            bluetoothGatt = null;
                        }

                        bluetoothGatt = device.connectGatt(requireContext(), false, gattCallback);
                        tvConnectionStatus.setText("Connecting to " + DEVICE_NAME + "...");
                        return;
                    } catch (SecurityException e) {
                        tvConnectionStatus.setText("Cannot connect: Bluetooth permission denied");
                        return;
                    }
                }
            }

            tvConnectionStatus.setText("WoilGuard-ESP32 not paired yet. Pair device first.");
        } catch (SecurityException e) {
            tvConnectionStatus.setText("Bluetooth access denied");
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (getActivity() == null) return;

            requireActivity().runOnUiThread(() -> {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    tvConnectionStatus.setText("BLE connected");
                    WoilGuardData.connected = true;

                    try {
                        if (!hasBluetoothConnectPermission()) {
                            requestBlePermissions();
                            return;
                        }
                        gatt.discoverServices();
                    } catch (SecurityException e) {
                        tvConnectionStatus.setText("Cannot discover services: permission denied");
                    }

                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    tvConnectionStatus.setText("BLE disconnected");
                    WoilGuardData.connected = false;
                    stopAutoRefresh();
                    statusCharacteristic = null;
                    cmdCharacteristic = null;
                } else {
                    tvConnectionStatus.setText("BLE status changed: " + newState);
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (getActivity() == null) return;

            try {
                if (!hasBluetoothConnectPermission()) {
                    requireActivity().runOnUiThread(() -> requestBlePermissions());
                    return;
                }

                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service == null) {
                    requireActivity().runOnUiThread(() ->
                            tvGuardData.setText("WoilGuard BLE service not found"));
                    return;
                }

                statusCharacteristic = service.getCharacteristic(STATUS_UUID);
                cmdCharacteristic = service.getCharacteristic(CMD_UUID);

                requireActivity().runOnUiThread(() -> {
                    if (statusCharacteristic == null || cmdCharacteristic == null) {
                        tvGuardData.setText("BLE characteristics not found");
                    } else {
                        tvGuardData.setText("BLE service discovered. Live monitoring started.");
                        enableStatusNotifications();
                        startAutoRefresh();
                    }
                });

            } catch (SecurityException e) {
                requireActivity().runOnUiThread(() ->
                        tvGuardData.setText("Service discovery failed: permission denied"));
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (getActivity() == null) return;

            if (STATUS_UUID.equals(characteristic.getUuid())) {
                String value;
                try {
                    value = characteristic.getStringValue(0);
                } catch (Exception e) {
                    value = "Unable to read characteristic value";
                }

                final String finalValue = value;
                requireActivity().runOnUiThread(() -> applyWearablePayload(finalValue));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (getActivity() == null) return;

            if (STATUS_UUID.equals(characteristic.getUuid())) {
                String value;
                try {
                    value = characteristic.getStringValue(0);
                } catch (Exception e) {
                    value = "Unable to parse notification";
                }

                final String finalValue = value;
                requireActivity().runOnUiThread(() -> applyWearablePayload(finalValue));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic,
                                            byte[] value) {
            if (getActivity() == null) return;

            if (STATUS_UUID.equals(characteristic.getUuid())) {
                final String finalValue = new String(value, StandardCharsets.UTF_8);
                requireActivity().runOnUiThread(() -> applyWearablePayload(finalValue));
            }
        }
    };


    private void enableStatusNotifications() {
        if (bluetoothGatt == null || statusCharacteristic == null) {
            return;
        }

        try {
            if (!hasBluetoothConnectPermission()) {
                requestBlePermissions();
                return;
            }

            bluetoothGatt.setCharacteristicNotification(statusCharacteristic, true);

            BluetoothGattDescriptor descriptor = statusCharacteristic.getDescriptor(CCCD_UUID);
            if (descriptor != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    bluetoothGatt.writeDescriptor(
                            descriptor,
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    );
                } else {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    bluetoothGatt.writeDescriptor(descriptor);
                }
            }

        } catch (SecurityException e) {
            toast("Notify enable failed: Bluetooth permission denied");
        }
    }

    private void readBleStatus() {
        if (bluetoothGatt == null || statusCharacteristic == null) {
            return;
        }

        try {
            if (!hasBluetoothConnectPermission()) {
                requestBlePermissions();
                return;
            }

            boolean started = bluetoothGatt.readCharacteristic(statusCharacteristic);
            if (!started) {
                tvGuardData.setText("BLE read could not start");
            }
        } catch (SecurityException e) {
            toast("Read failed: Bluetooth permission denied");
        }
    }

    private void sendBleCommand(String cmd) {
        if (bluetoothGatt == null || cmdCharacteristic == null) {
            toast("BLE not connected");
            return;
        }

        try {
            if (!hasBluetoothConnectPermission()) {
                requestBlePermissions();
                return;
            }

            cmdCharacteristic.setValue(cmd);

            boolean result;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                int writeResult = bluetoothGatt.writeCharacteristic(
                        cmdCharacteristic,
                        cmd.getBytes(),
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                );
                result = writeResult == BluetoothGatt.GATT_SUCCESS;
            } else {
                result = bluetoothGatt.writeCharacteristic(cmdCharacteristic);
            }

            if (result) {
                tvGuardData.setText("Command sent: " + cmd);
            } else {
                tvGuardData.setText("Failed to send command");
            }

        } catch (SecurityException e) {
            toast("Write failed: Bluetooth permission denied");
        }
    }

    // ----------------------------------------------------
    // PAYLOAD PARSING / UI
    // ----------------------------------------------------

    private void applyWearablePayload(String payload) {
        latestRawPayload = payload;
        tvGuardData.setText(payload);

        Map<String, String> parsed = parsePayload(payload);

        latestState = parsed.getOrDefault("STATE", latestState);
        latestBattery = parsed.getOrDefault("BAT", latestBattery);
        latestIncident = parsed.getOrDefault("INCIDENT", latestIncident);
        latestSeverity = parsed.getOrDefault("SEVERITY", latestSeverity);
        latestModel = parsed.getOrDefault("MODEL", latestModel);
        latestTimestamp = parsed.getOrDefault("TS", latestTimestamp);

        latestFallProb = parsed.getOrDefault(
                "FALL_PROB",
                parsed.getOrDefault("MOTION_CONF", parsed.getOrDefault("MOTION", latestFallProb))
        );
        latestMotion = latestFallProb;
        latestAudio = parsed.getOrDefault("AUDIO", latestAudio);

        tvDeviceState.setText("State: " + latestState + " | Severity: " + latestSeverity);

        if ("-1".equals(latestBattery)) {
            tvBattery.setText("Battery: disabled");
        } else {
            tvBattery.setText("Battery: " + latestBattery + "%");
        }

        tvIncidentType.setText("Incident: " + latestIncident);
        tvMotionScore.setText("Fall probability: " + latestFallProb);
        tvAudioScore.setText("Audio: " + latestAudio + " | Model: " + latestModel);

        addIncidentLog(buildIncidentLogLine());

        // Publish to shared data holder for dashboard fragments
        WoilGuardData.state = latestState;
        WoilGuardData.battery = latestBattery;
        WoilGuardData.incident = latestIncident;
        WoilGuardData.severity = latestSeverity;

        if ("MOTION_TINYML".equalsIgnoreCase(latestIncident)
                || "MOTION_SUSPICIOUS".equalsIgnoreCase(latestIncident)
                || "PANIC_BUTTON".equalsIgnoreCase(latestIncident)
                || "CRITICAL_FALL".equalsIgnoreCase(latestIncident)
                || "FALL_SUSPECTED".equalsIgnoreCase(latestIncident)
                || "HIGH".equalsIgnoreCase(latestSeverity)
                || "CRITICAL".equalsIgnoreCase(latestSeverity)) {

            tvConnectionStatus.setText("ALERT: " + latestIncident + " / " + latestSeverity);
        }
        
        if ("PANIC_BUTTON".equalsIgnoreCase(latestIncident)
                || "PANIC_TEST".equalsIgnoreCase(latestIncident)
                || "CRITICAL_FALL".equalsIgnoreCase(latestIncident)) {
            if (!hasFiredPanicAlert) {
                hasFiredPanicAlert = true;
                openPanicAlert();
            }
        } else if ("STATUS".equalsIgnoreCase(latestIncident) || "NONE".equalsIgnoreCase(latestIncident) || "IDLE".equalsIgnoreCase(latestState)) {
            hasFiredPanicAlert = false;
        }
    }

    private Map<String, String> parsePayload(String payload) {
        Map<String, String> map = new HashMap<>();
        if (TextUtils.isEmpty(payload)) return map;

        String[] pairs = payload.split(";");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                map.put(parts[0].trim(), parts[1].trim());
            }
        }
        return map;
    }

    private String buildIncidentLogLine() {
        StringBuilder builder = new StringBuilder();
        builder.append(latestIncident);
        builder.append(" | severity=").append(latestSeverity);
        builder.append(" | state=").append(latestState);
        builder.append(" | fall=").append(latestFallProb);
        builder.append(" | audio=").append(latestAudio);

        if (!TextUtils.isEmpty(latestTimestamp)) {
            builder.append(" | ts=").append(latestTimestamp);
        }

        return builder.toString();
    }

    private void addIncidentLog(String line) {
        if (TextUtils.isEmpty(line)) return;

        if (incidentLogs.isEmpty() || !line.equals(incidentLogs.getLast())) {
            if (incidentLogs.size() >= 10) {
                incidentLogs.removeFirst();
            }
            incidentLogs.add(line);
        }

        StringBuilder builder = new StringBuilder();
        for (String item : incidentLogs) {
            builder.append("• ").append(item).append("\n");
        }

        tvIncidentLog.setText(builder.toString().trim());
    }

    // ----------------------------------------------------
    // CCC FLOW HOOK
    // ----------------------------------------------------

    private void openPanicAlert() {
        Intent intent = new Intent(requireContext(), PanicAlertStatusActivity.class);
        intent.putExtra(PanicAlertStatusActivity.EXTRA_SOURCE, "wearable");
        intent.putExtra("incidentType", latestIncident);
        intent.putExtra("severity", deriveSeverity(latestIncident, latestSeverity, latestFallProb, latestAudio));
        intent.putExtra("state", latestState);
        intent.putExtra("battery", latestBattery);
        intent.putExtra("motion", latestFallProb);
        intent.putExtra("audio", latestAudio);
        intent.putExtra("source", "wearable");
        startActivity(intent);
    }

    private String deriveSeverity(String incident, String severity, String fallProb, String audio) {
        if (!TextUtils.isEmpty(severity)
                && !"LOG".equalsIgnoreCase(severity)
                && !"NONE".equalsIgnoreCase(severity)) {
            return severity;
        }

        if ("PANIC_BUTTON".equalsIgnoreCase(incident) || "PANIC_TEST".equalsIgnoreCase(incident)) {
            return "CRITICAL";
        }

        float motionValue = safeParseFloat(fallProb);
        float audioValue = safeParseFloat(audio);

        if (motionValue >= 0.85f) return "HIGH";
        if (motionValue >= 0.65f || audioValue >= 0.50f) return "MEDIUM";
        return "LOW";
    }

    private float safeParseFloat(String value) {
        try {
            return Float.parseFloat(value);
        } catch (Exception e) {
            return 0f;
        }
    }

    // ----------------------------------------------------
    // PERMISSIONS
    // ----------------------------------------------------

    private boolean hasBluetoothConnectPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBluetoothScanPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasLocationPermissionPreS() {
        return ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return hasBluetoothScanPermission() && hasBluetoothConnectPermission();
        } else {
            return hasLocationPermissionPreS();
        }
    }

    private void requestBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, REQ_BLE);
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQ_BLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_BLE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            if (granted) {
                toast("Bluetooth permission granted");
            } else {
                toast("Bluetooth permission denied");
                tvConnectionStatus.setText("Bluetooth permission required");
            }
        }
    }

    private void toast(String msg) {
        if (getContext() != null) {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoRefresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bluetoothGatt != null && statusCharacteristic != null) {
            startAutoRefresh();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        stopAutoRefresh();

        if (bluetoothGatt != null) {
            try {
                if (hasBluetoothConnectPermission()) {
                    bluetoothGatt.close();
                }
            } catch (SecurityException ignored) {
            }
            bluetoothGatt = null;
        }

        statusCharacteristic = null;
        cmdCharacteristic = null;
    }
}

    //private String firstNonEmpty(String a, String fallback) { return (a == null || a.trim().isEmpty()) ? fallback : a; }
