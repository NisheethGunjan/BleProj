package bleproj.bluetooth;



/**
 * Listener for the different DeviceControl event.
 *
 *  *
 */
public interface DeviceHelperListener {

    /**
     * Called when the connection state change.
     *
     * @param connected True if connected.
     */
    void onConnectionStateChanged(boolean connected);

    /**
     * Called when the services are discovered.
     */
    void onServicesDiscovered();

    /**
     * Called when there is an error during initialization. Probably due to the Bluetooth begin
     * turned off.
     */
    void onInitializationError();
}