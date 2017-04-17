package example.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    ListView devicesFound;
    ListView devicesPaired;
    Button discovery;
    ArrayAdapter<String> listDevices;
    ArrayList<BluetoothDevice> bluetoothlistDevices;
    ArrayAdapter<String> listPaired;
    ArrayList<BluetoothDevice> bluetoothlistPaired;
    ArrayList<BluetoothDevice> devices;
    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> deviceArray;
    IntentFilter filter;
    BroadcastReceiver receiver;
    String TAG = "";
    ArrayList<String> pairedDevices;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        if(mBluetoothAdapter == null){
            Toast.makeText(this,"No bluetooth available on this device",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        else {

            if (!mBluetoothAdapter.isEnabled()) {
                turnOnBluetooth();
            }
        }
        getPairedDevices();
        final int CODE = 5; // app defined constant used for onRequestPermissionsResult

        String[] permissionsToRequest =
                {
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.MEDIA_CONTENT_CONTROL
                };

        boolean allPermissionsGranted = true;

        for(String permission : permissionsToRequest)
        {
            allPermissionsGranted = allPermissionsGranted && (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED);
        }

        if(!allPermissionsGranted)
        {
            ActivityCompat.requestPermissions(this, permissionsToRequest, CODE);
        }
        startDiscovery();
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("Broadcast action",action);
                if (BluetoothDevice.ACTION_FOUND.equals(action)){
                    try {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device != null) {
                            devices.add(device);
                            int s = 0;
                            for (int a = 0; a < pairedDevices.size(); a++) {
                                Log.d("paired devices list", pairedDevices.get(a));
                                if (device.getName().equals(pairedDevices.get(a))) {
                                    s = 1;
                                    break;

                                }

                            }
                            if (s == 0) {
                                bluetoothlistDevices.add(device);
                                listDevices.add(device.getName() + "\n" + device.getAddress());
                            } else {
                                bluetoothlistPaired.add(device);
                                listPaired.add("PAIRED" + device.getName() + "\n" + device.getAddress());
                            }

                        }
                    }
                    catch (Exception ex){
                        String message = ex.getMessage();
                    }
                }
                else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                    Toast.makeText(getApplicationContext(),"Discovery Started",Toast.LENGTH_SHORT).show();

                }
                else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                    Toast.makeText(getApplicationContext(),"Discovery Finished",Toast.LENGTH_SHORT).show();

                }
                else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                    if(mBluetoothAdapter.getState() == mBluetoothAdapter.STATE_OFF){
                        turnOnBluetooth();
                    }

                }
            }
        };
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver,filter);
        discovery = (Button) findViewById(R.id.discovery);
        discovery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDiscovery();
            }
        });
        devicesFound.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice bdDevice = bluetoothlistDevices.get(position);

                    Boolean isBonded = false;
                    try {
                        isBonded = createBond(bdDevice);
                        if(isBonded)
                        {
                            //arrayListpaired.add(bdDevice.getName()+"\n"+bdDevice.getAddress());
                            //adapter.notifyDataSetChanged();
                            getPairedDevices();
                            listPaired.add(bdDevice.getName()+"\n"+bdDevice.getAddress());
                            bluetoothlistPaired.add(bdDevice);
                            listPaired.notifyDataSetChanged();
                            listDevices.remove(bdDevice.getName()+"\n"+bdDevice.getAddress());
                            bluetoothlistDevices.remove(position);
                            listDevices.notifyDataSetChanged();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }//connect(bdDevice);
                    Log.i("Log", "The bond is created: "+isBonded);


            }
        });

        devicesPaired.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = bluetoothlistPaired.get(position);
                Log.d("Main Activity",device.getAddress());
                String address = device.getAddress();
                Intent i = new Intent(MainActivity.this,Chat.class);
                i.putExtra("ADDRESS",address);
                startActivity(i);
            }
        });

    }


    private void init() {
        devicesFound = (ListView)findViewById(R.id.devicesFound);
        listDevices = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,0);
        bluetoothlistDevices = new ArrayList<BluetoothDevice>();
        devicesFound.setAdapter(listDevices);
        devicesPaired = (ListView)findViewById(R.id.devicesPaired);
        listPaired = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,0);
        bluetoothlistPaired = new ArrayList<BluetoothDevice>();
        devicesPaired.setAdapter(listPaired);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        devices = new ArrayList<BluetoothDevice>();
        pairedDevices = new ArrayList<String>();


    }

    private void startDiscovery() {
        mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.startDiscovery();
    }
    private void getPairedDevices() {
        deviceArray = mBluetoothAdapter.getBondedDevices();
        if(deviceArray.size()>0){
            for(BluetoothDevice device:deviceArray){
                pairedDevices.add(device.getName());
            }
        }
    }


    private void turnOnBluetooth() {
        Intent startBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(startBluetooth, 100);
    }
    @Override
    protected void onPause() {
        super.onPause();
    //    unregisterReceiver(receiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            unregisterReceiver(receiver);
        }
        catch (Exception ex){
            Log.d("Exception",ex.getMessage());
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RESULT_CANCELED){
            Toast.makeText(this, "Bluetooth must be enabled ",Toast.LENGTH_SHORT).show();
            finish();
        }
        else if (requestCode == RESULT_OK){
            Intent discoverableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(
                    BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);

        }
    }
    public boolean createBond(BluetoothDevice btDevice)
            throws Exception
    {
        Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
        Method createBondMethod = class1.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

}
