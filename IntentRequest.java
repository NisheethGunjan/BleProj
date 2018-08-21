package bleproj.bluetooth;

public class IntentRequest {
    public static final int REQUEST_ENABLE_BLUETOOTH = 1;
    public static final int REQUEST_BLUETOOTH_PAIRING = 2;
    public static final int BLUETOOTH_DISPLAY = 3;
    public static final int ACCESS_COARSE_LOCATION = 4;
    public final class DataKey {
        public static final String BLUETOOTHDEVICE = "bluetoothDevice";
        public static final String EXTRA_BYTE_VALUE = "extraByteValue";
        public static final String EXTRA_BYTE_UUID_VALUE = "extraByteUUIDValue";
        public static final String EXTRA_BYTE_INSTANCE_VALUE = "extraByteInstanceValue";
        public static final String EXTRA_BYTE_SERVICE_UUID_VALUE =
                "extraByteServiceUUIDValue";
        public static final String EXTRA_BYTE_SERVICE_INSTANCE_VALUE =
                "extraByteServiceInstanceValue";
        public static final String FIRMWARE_VERSION = "firmwareVersion";
        public static final String HARDWARE_REVISION = "hardwareRevision";
        public static final String MANUFACTURER_NAME = "manufacturerName";
        public static final String UART_DATA = "uartData";
    }
}
