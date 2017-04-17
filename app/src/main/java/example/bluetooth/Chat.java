package example.bluetooth;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DownloadManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

public class Chat extends AppCompatActivity {
    private static final String TAG = "BluetoothChatFragment";
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private StringBuffer mOutStringBuffer;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothChatService mChatService = null;
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private Button buttonSend;
    private static final UUID MY_UUID =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private String address ="";
    private ImageView receivedPic;
    private byte[] buffer = new byte[8192];
    private ImageView image;
    private String action = "";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        mConversationView = (ListView) findViewById(R.id.in);
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        image = (ImageView) findViewById(R.id.imageView1);
        receivedPic = (ImageView) findViewById(R.id.receivedPic);
        mSendButton = (Button) findViewById(R.id.button_send);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        buttonSend = (Button) findViewById(R.id.button_picture);
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setType("image/*");
                i.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(i,"SELECT PICTURE"),100);
            }
        });
        getContentResolver().registerContentObserver(android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI, true,
                new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        Log.d("your_tag","Internal Media has been changed");
                        super.onChange(selfChange);
                        Long timestamp = readLastDateFromMediaStore(getApplicationContext(), MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                        // comapare with your stored last value and do what you need to do
                        if(timestamp>0){
                            Toast.makeText(getApplicationContext(),"Something is changed"+timestamp,Toast.LENGTH_SHORT).show();
                        }

                    }
                }
        );
        getContentResolver().registerContentObserver(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true,
                new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        Log.d("your_tag","External Media has been changed");
                        super.onChange(selfChange);

                        Long timestamp = readLastDateFromMediaStore(getApplicationContext(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        // comapare with your stored last value and do what you need to do
                        if(timestamp>0){
                            Toast.makeText(getApplicationContext(),"Something is changed"+timestamp,Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );


       // mChatService = new BluetoothChatService(this,mHandler);
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available",Toast.LENGTH_SHORT).show();
            finish();
        }

        Intent data = getIntent();
        address = data.getStringExtra("ADDRESS");
        Log.d("CHAT",address);
            if(data!=null)
            {
            Toast.makeText(this, "Intent Received",Toast.LENGTH_SHORT).show();
            connectDevice(data, true);
            }
        else
            Toast.makeText(this, "Intent is empty",Toast.LENGTH_SHORT).show();
      //  AcceptData acceptData = new AcceptData();
      // acceptData.start();
      // Bitmap bm1 = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
      //  image.setImageBitmap(bm1);

    }
    private Long readLastDateFromMediaStore(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, "date_added DESC");
        //PhotoHolder media = null;
        Long dateAdded = 0L;
        if (cursor.moveToNext()) {
             dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED));
        }
        cursor.close();
        return dateAdded;
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                if (data != null)
                {
                    try
                    {
                        Uri imageUri = data.getData();
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_SEND);
                        intent.setType("image/png");
                        intent.putExtra(Intent.EXTRA_STREAM, imageUri);
                        startActivity(intent);



                    /*    PackageManager pm = getPackageManager();
                        List<ResolveInfo> appsList = pm.queryIntentActivities( intent, 0);
                        if(appsList.size() > 0) {

                        String packageName = null;
                        String className = null;
                        boolean found = false;
                        for(ResolveInfo info: appsList){
                            packageName = info.activityInfo.packageName;
                            if( packageName.equals("com.android.bluetooth")){
                                className = info.activityInfo.name;
                                found = true;
                                break;
                            }
                        }
                        if(! found){
                            Toast.makeText(this, "Bluetooth is not found on this device", Toast.LENGTH_SHORT).show();
                        }
                            else {
                            intent.setClassName(packageName, className);
                            startActivity(intent);

                        }}*/




                        SendData send = new SendData();
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                        image.setImageBitmap(bitmap);
                       // send.sendMessage(bitmap);
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                }
            } else if (resultCode == Activity.RESULT_CANCELED)
            {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
       // mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget

                    String message = mOutEditText.getText().toString();
                    sendMessage(message);


            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }


    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, "You are not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }
    private void setStatus(int resId) {
     //   FragmentActivity activity = getActivity();
        if (null == this) {
            return;
        }
        final ActionBar actionBar = this.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }
    private void setStatus(CharSequence subTitle) {
      //  FragmentActivity activity = getActivity();
        if (null == this) {
            return;
        }
        final ActionBar actionBar = this.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        //    FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != this) {
                        Toast.makeText(getApplicationContext(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != this) {
                        Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString("ADDRESS");
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    class SendData extends Thread {
        private BluetoothDevice device = null;
        private BluetoothSocket btSocket = null;
        private OutputStream outStream = null;


        public SendData(){
            device = mBluetoothAdapter.getRemoteDevice(address);
            try
            {
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            }
            catch (Exception e) {
                // TODO: handle exception
            }
            mBluetoothAdapter.cancelDiscovery();
            try {
                btSocket.connect();
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                }
            }
            Toast.makeText(getBaseContext(), "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
            try {
                outStream = btSocket.getOutputStream();
            } catch (IOException e) {
            }
        }

        public void sendMessage(Bitmap bm)
        {
            try {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
               // Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.white);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 100,baos); //bm is the bitmap object
                byte[] b = baos.toByteArray();
                Toast.makeText(getApplicationContext(), String.valueOf(b.length), Toast.LENGTH_SHORT).show();
                outStream.write(b);
                outStream.flush();
            } catch (IOException e) {
            }
        }
    }

    class AcceptData extends Thread{
        private final BluetoothServerSocket mmServerSocket;
        private BluetoothSocket socket = null;
        private InputStream mmInStream;
        private String device;
        public AcceptData() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("Bluetooth", MY_UUID);
            } catch (IOException e) {
                //
            }
            mmServerSocket = tmp;
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                //
            }
            device = socket.getRemoteDevice().getName();
            Toast.makeText(getBaseContext(), "Connected to " + device, Toast.LENGTH_SHORT).show();
            InputStream tmpIn = null;
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                //
            }
            mmInStream = tmpIn;
            int byteNo;
            try {
                byteNo = mmInStream.read(buffer);
                if (byteNo != -1) {
                    //ensure DATAMAXSIZE Byte is read.
                    int byteNo2 = byteNo;
                    int bufferSize = 7340;
                    while(byteNo2 != bufferSize){
                        bufferSize = bufferSize - byteNo2;
                        byteNo2 = mmInStream.read(buffer,byteNo,bufferSize);
                        if(byteNo2 == -1){
                            break;
                        }
                        byteNo = byteNo+byteNo2;
                    }
                }
                if (socket != null) {
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        //
                    }
                }
            }
            catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
}




