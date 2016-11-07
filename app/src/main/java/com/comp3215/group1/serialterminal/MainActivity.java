package com.comp3215.group1.serialterminal;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    String command;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Example of a call to a native method
        final TextView tv = (TextView) findViewById(R.id.sample_text);
        final EditText input = (EditText) findViewById(R.id.editCommand);
        Button  sendComand= (Button) findViewById(R.id.buttonCommand);
        Button  usbButton  = (Button) findViewById(R.id.usbButton);

        sendComand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                command = input.getText().toString();
                Toast.makeText(getApplicationContext(),command +"BOOP",Toast.LENGTH_LONG).show();
            }
        });
        tv.setText(stringFromJNI());


        String devicesString = "";
        final UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        final PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        usbButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashMap<String,UsbDevice> devices = manager.getDeviceList();
                Iterator<UsbDevice> deviceIt = devices.values().iterator();
                UsbDevice cableUSB = null ;
                while(deviceIt.hasNext()){
                    UsbDevice  tempUsb= deviceIt.next();
                    if(tempUsb.getProductId() == 24596 && tempUsb.getVendorId() == 1027){
                        Toast.makeText(getApplicationContext(), "Found USB cable", Toast.LENGTH_SHORT).show();
                        cableUSB = tempUsb;
                    }
                }
                if(cableUSB != null){
                    manager.requestPermission(cableUSB, mPermissionIntent);
                    tv.setText("Number of interfaces " +cableUSB.getInterfaceCount() + "\n");

                    UsbInterface cableInterface = cableUSB.getInterface(0);
                    tv.append("Number of endpoints for interface id "+  cableInterface.getId() + " is:" + cableInterface.getEndpointCount() + "\n");
                    for(int i=0;i< cableInterface.getEndpointCount();i++){
                        UsbEndpoint cableEndpoints =cableInterface.getEndpoint(i);
                        tv.append(cableEndpoints.toString()+"\n"+ cableEndpoints.getDirection()+ "\n"+ cableEndpoints.getAttributes()+ "\n\n");
                    }
                }
                else Toast.makeText(getApplicationContext(),"No interfaces found",Toast.LENGTH_LONG).show();
                //Toast.makeText(getApplicationContext(),test,Toast.LENGTH_LONG).show();
                //tv.setText(test);
            }
        });
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
}
