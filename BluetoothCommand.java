package bleproj.bluetooth;

/**
 * * Class to describe a bluetooth command.
 */
public class BluetoothCommand {

    public String serviceUUID;
    public String characteristicUUID;
    public byte[] command;

    public BluetoothCommand(String serviceUUID, String characteristicUUID, byte[] command) {
        this.serviceUUID = serviceUUID;
        this.characteristicUUID = characteristicUUID;
        this.command = command;
    }

    public BluetoothCommand(String serviceUUID, String characteristicUUID){
        this.serviceUUID = serviceUUID;
        this.characteristicUUID = characteristicUUID;
    }
}
