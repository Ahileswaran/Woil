package com.example.woil.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.woil.R;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WoilGuardFragment extends Fragment {

    private static final int REQ_BLE = 1001;

    private static final String DEVICE_NAME = "WoilGuard-ESP32";

    private static final UUID SERVICE_UUID =
            UUID.fromString("12345678-1234-1234-1234-1234567890ab");
    private static final UUID STATUS_UUID =
            UUID.fromString("12345678-1234-1234-1234-1234567890ac");
    private static final UUID CMD_UUID =
            UUID.fromString("12345678-1234-1234-1234-1234567890ad");

    private RadioGroup rgMode;
    private TextView tvConnection;
    private TextView tvData;
    private Button btnConnect, btnReadStatus, btnTestCommand;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic statusCharacteristic;
    private BluetoothGattCharacteristic cmdCharacteristic;

    private final OkHttpClient httpClient = new OkHttpClient();

    // Change this later to your ESP32 local IP
    private String esp32BaseUrl = "http://192.168.1.100";

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

        rgMode = view.findViewById(R.id.rgCommunicationMode);
        tvConnection = view.findViewById(R.id.tvConnectionStatus);
        tvData = view.findViewById(R.id.tvGuardData);
        btnConnect = view.findViewById(R.id.btnConnectGuard);
        btnReadStatus = view.findViewById(R.id.btnReadStatus);
        btnTestCommand = view.findViewById(R.id.btnSendTest);

        BluetoothManager bluetoothManager =
                (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        tvConnection.setText("Disconnected");
        tvData.setText("Demo data: waiting for test connection...");

        btnConnect.setOnClickListener(v -> {
            int checkedId = rgMode.getCheckedRadioButtonId();
            if (checkedId == R.id.rbBluetooth) {
                connectUsingBle();
            } else if (checkedId == R.id.rbWifi) {
                connectUsingWifi();
            }
        });

        btnReadStatus.setOnClickListener(v -> {
            int checkedId = rgMode.getCheckedRadioButtonId();
            if (checkedId == R.id.rbBluetooth) {
                readBleStatus();
            } else if (checkedId == R.id.rbWifi) {
                readWifiStatus();
            }
        });

        btnTestCommand.setOnClickListener(v -> {
            int checkedId = rgMode.getCheckedRadioButtonId();
            if (checkedId == R.id.rbBluetooth) {
                sendBleCommand("PANIC_TEST");
            } else if (checkedId == R.id.rbWifi) {
                sendWifiPanic();
            }
        });
    }

    // ----------------------------------------------------
    // BLE
    // ----------------------------------------------------

    private void connectUsingBle() {
        if (!hasBlePermissions()) {
            requestBlePermissions();
            return;
        }

        if (bluetoothAdapter == null) {
            tvConnection.setText("Bluetooth not supported on this phone");
            return;
        }

        try {
            if (!bluetoothAdapter.isEnabled()) {
                tvConnection.setText("Enable Bluetooth first");
                return;
            }
        } catch (SecurityException e) {
            tvConnection.setText("Bluetooth permission denied");
            return;
        }

        if (!hasBluetoothConnectPermission()) {
            requestBlePermissions();
            return;
        }

        tvConnection.setText("Searching paired BLE devices...");

        try {
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            if (bondedDevices == null || bondedDevices.isEmpty()) {
                tvConnection.setText("No paired Bluetooth devices found");
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
                    tvConnection.setText("Cannot read Bluetooth device name");
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
                        tvConnection.setText("Connecting to " + DEVICE_NAME + "...");
                        return;
                    } catch (SecurityException e) {
                        tvConnection.setText("Cannot connect: Bluetooth permission denied");
                        return;
                    }
                }
            }

            tvConnection.setText("WoilGuard-ESP32 not paired yet. Pair device first.");
        } catch (SecurityException e) {
            tvConnection.setText("Bluetooth access denied");
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (getActivity() == null) return;

            requireActivity().runOnUiThread(() -> {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    tvConnection.setText("BLE connected");

                    try {
                        if (!hasBluetoothConnectPermission()) {
                            requestBlePermissions();
                            return;
                        }
                        gatt.discoverServices();
                    } catch (SecurityException e) {
                        tvConnection.setText("Cannot discover services: permission denied");
                    }

                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    tvConnection.setText("BLE disconnected");
                    statusCharacteristic = null;
                    cmdCharacteristic = null;
                } else {
                    tvConnection.setText("BLE status changed: " + newState);
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
                            tvData.setText("WoilGuard BLE service not found"));
                    return;
                }

                statusCharacteristic = service.getCharacteristic(STATUS_UUID);
                cmdCharacteristic = service.getCharacteristic(CMD_UUID);

                requireActivity().runOnUiThread(() -> {
                    if (statusCharacteristic == null || cmdCharacteristic == null) {
                        tvData.setText("BLE characteristics not found");
                    } else {
                        tvData.setText("BLE service discovered. Ready.");
                    }
                });

            } catch (SecurityException e) {
                requireActivity().runOnUiThread(() ->
                        tvData.setText("Service discovery failed: permission denied"));
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
                requireActivity().runOnUiThread(() ->
                        tvData.setText("Actual data: " + finalValue));
            }
        }
    };

    private void readBleStatus() {
        if (bluetoothGatt == null || statusCharacteristic == null) {
            toast("BLE not connected");
            return;
        }

        try {
            if (!hasBluetoothConnectPermission()) {
                requestBlePermissions();
                return;
            }

            boolean started = bluetoothGatt.readCharacteristic(statusCharacteristic);
            if (!started) {
                tvData.setText("BLE read could not start");
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
                tvData.setText("Command sent: " + cmd);
            } else {
                tvData.setText("Failed to send command");
            }

        } catch (SecurityException e) {
            toast("Write failed: Bluetooth permission denied");
        }
    }

    // ----------------------------------------------------
    // Wi-Fi
    // ----------------------------------------------------

    private void connectUsingWifi() {
        tvConnection.setText("Checking Wi-Fi...");

        Request request = new Request.Builder()
                .url(esp32BaseUrl + "/ping")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() ->
                        tvConnection.setText("Wi-Fi failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String result = response.body() != null ? response.body().string() : "";
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() ->
                        tvConnection.setText("Wi-Fi connected: " + result));
                response.close();
            }
        });
    }

    private void readWifiStatus() {
        Request request = new Request.Builder()
                .url(esp32BaseUrl + "/status")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() ->
                        tvData.setText("Wi-Fi read failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String result = response.body() != null ? response.body().string() : "";
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() ->
                        tvData.setText("Actual data: " + result));
                response.close();
            }
        });
    }

    private void sendWifiPanic() {
        Request request = new Request.Builder()
                .url(esp32BaseUrl + "/panic")
                .post(okhttp3.RequestBody.create(new byte[0]))
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() ->
                        tvData.setText("Wi-Fi command failed: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() ->
                        tvData.setText("Panic test sent"));
                response.close();
            }
        });
    }

    // ----------------------------------------------------
    // Permissions
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
                tvConnection.setText("Bluetooth permission required");
            }
        }
    }

    private void toast(String msg) {
        if (getContext() != null) {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

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