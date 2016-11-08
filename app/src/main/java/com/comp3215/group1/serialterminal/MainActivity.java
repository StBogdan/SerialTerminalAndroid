package com.comp3215.group1.serialterminal;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.driver.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    String command;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    TextView outputDisplay= null;

    private SerialInputOutputManager mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
                  //  Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.updateReceivedData(data);
                        }
                    });
                }
            };


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Boiler plate Android
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Buttons and UI elements
        outputDisplay = (TextView) findViewById(R.id.outputDisplay);
        final TextView connectionDisplay = (TextView) findViewById(R.id.connectionView);
        final EditText input = (EditText) findViewById(R.id.editCommand);
        Button sendComand = (Button) findViewById(R.id.buttonCommand);
        Button usbButton = (Button) findViewById(R.id.usbButton);
        Button configButton = (Button) findViewById(R.id.buttonConfig);
        final Button commButton = (Button) findViewById(R.id.buttonCommSend);

        // USB comms
        final UsbManager managerUSB = (UsbManager) getSystemService(Context.USB_SERVICE);
        final HashMap<String, UsbDevice>[] devicesUSB = new HashMap[]{managerUSB.getDeviceList()};                          //Why array ? -> get past final limitation
        final UsbDevice[] cableUSB = {null};                                                                                 //Why array ? -> get past final limitation
        final UsbInterface[] cableInterface = {null};                                                                        //Why array ? -> get past final limitation
        final PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);



        //Listeners
        usbButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                devicesUSB[0] = managerUSB.getDeviceList();
                outputDisplay.setText("Found devices:\n");
                Iterator<UsbDevice> usbDeviceIterator = devicesUSB[0].values().iterator();
                while (usbDeviceIterator.hasNext()) {
                    UsbDevice tempUsb = usbDeviceIterator.next();
                    connectionDisplay.append(tempUsb.getDeviceName() + " " + tempUsb.getVendorId() + " " + tempUsb.getProductId());
                }
            }
        });

        configButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Iterator<UsbDevice> deviceIt = devicesUSB[0].values().iterator();
                while (deviceIt.hasNext()) {
                    UsbDevice tempUsb = deviceIt.next();
                    if (tempUsb.getProductId() == 24596 && tempUsb.getVendorId() == 1027) {         //Found the cable
//                        Toast.makeText(getApplicationContext(), "Found USB cable", Toast.LENGTH_SHORT).show();
                        connectionDisplay.append("Found USB cable\n");
                        cableUSB[0] = tempUsb;
                        managerUSB.requestPermission(cableUSB[0], mPermissionIntent);       //Get permission
                        cableInterface[0] = cableUSB[0].getInterface(0);                    //Get interface

                    }
                }
            }
        });

        final SerialInputOutputManager[] siom = new SerialInputOutputManager[1];
        final UsbSerialPort[] port = new UsbSerialPort[1];

        commButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Find all available drivers from attached devices.
                List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(managerUSB);

                if (availableDrivers.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "No devices found", Toast.LENGTH_LONG).show();
                    return;
                }

                UsbSerialDriver driver = availableDrivers.get(0);
                UsbDeviceConnection connection = managerUSB.openDevice(cableUSB[0]);

                if (connection == null) {
                    // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
                    Toast.makeText(getApplicationContext(), "Failed to get connection to device", Toast.LENGTH_LONG).show();
                    return;
                }

                // Read some data! Most have just one port (port 0).
                port[0] = driver.getPorts().get(0);

                //Toast.makeText(getApplicationContext(),driver.getPorts().size() + " test ",Toast.LENGTH_LONG).show();
                try {
                    port[0].open(connection);
                    port[0].setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                    siom[0] = new SerialInputOutputManager(port[0],mListener);
                    mExecutor.submit(siom[0]);
                } catch (IOException e) {
                    // Deal with error.
                }
            }
        });


        sendComand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    command = input.getText().toString();
                    byte[] intCommand = new byte[commButton.length()];
                    int i=0;
                    for(char c : command.toCharArray()){
                        intCommand[i]=(byte) (int) c;
                        i++;
                    }
                    port[0].write(intCommand,500);
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),"Something went horribly wrong with sending",Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });


// Stop updates
    //    mUIUpdater.stopUpdates();
        //        port[0].close();

    }

    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            //call method to set up device communication
                            Toast.makeText(getApplicationContext(),"BOOP from "+ device,Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        Toast.makeText(getApplicationContext(),"Permission denied for "+ device,Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    };



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    private void updateReceivedData(byte[] data) {
        String message ="";
        for(int i=0;i<data.length;i++){
            message+= (char) data[i];
        }
        outputDisplay.append(message);
      //Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();
    }





}
