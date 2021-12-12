package com.example.myapplication;


import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private InputStream inputStream;
    Button startButton,clearButton,stopButton;
    TextView address, pressure, temp, textView;
    EditText editText;
    boolean deviceConnected=false;
    boolean stopThread;
    int capacity = 60;
    BlockingQueue<String> queue = new LinkedBlockingQueue<>(capacity);
    public Worker worker;
    public Reader reader;
    Activity act = MainActivity.this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton = (Button) findViewById(R.id.buttonStart);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        pressure = (TextView) findViewById(R.id.pressure);
        temp = (TextView) findViewById(R.id.temp);
        address = (TextView) findViewById(R.id.address);
        textView = (TextView) findViewById(R.id.textView);


        worker = new Worker(queue, stopThread, temp, act);
        reader = new Reader(queue, stopThread, inputStream);
        setUiEnabled(false);

    }
    // Custom Switch to turn on/off clear/stop btn when startbtn is clicked
    public void setUiEnabled(boolean bool)
    {
        startButton.setEnabled(!bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);

    }

    /* Initialize a BluetoothDevice class using .getremoteDevice method on bluetoothadapter, which we got through getdefaultadapter method;
        if Bluetooth is not enabled on the android device yet, request permission to enable it and proceed with activation.
        return true if the BluetoothDevice with name "CTechLogger" is found in the list of previously connected devices, otherwise return false*/
    @SuppressLint("SetTextI18n")
    public boolean BTinit() {
        boolean found = false;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Device doesnt Support Bluetooth", Toast.LENGTH_SHORT).show();
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 0);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please Pair the Device first", Toast.LENGTH_SHORT).show();
            pressure.setText("Device not paired");
        } else {
            String device_name = "CTechLogger";
            for (BluetoothDevice iterator : bondedDevices) {
                if (iterator.getName().equals(device_name)) {
                    device = bluetoothAdapter.getRemoteDevice(String.valueOf(iterator));
                    found = true;
                    pressure.setText(iterator.getName());
                    address.setText(device.getAddress());
                    break;
                }


            }
        }
        return found;
    }


    /* create a bluetooth socket using the BluetoothDevice device and having port = PORT_UUID, and connect to it.
    *  In case of successful connection, the function will procceed to create an InputStream inputstream object from socket.getInputStream() */
    public boolean BTconnect()
    {
        boolean connected=true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected=false;
            textView.setText("\nConnection not Opened!\n");
        }
        if(connected)
        {
            try {
                inputStream=socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return connected;
    }

    // Start Button click method
    public void onClickStart(View view) {
        if(BTinit())
        {
            if(BTconnect())
            {
                // if Both BTinit() and BTconnect() return true, enable clear/stop btn
                // call beginListenForData()
                setUiEnabled(true);
                deviceConnected=true;
                beginListenForData();
                Toast.makeText(getApplicationContext(), "Please Pair the Device first", Toast.LENGTH_SHORT).show();
                textView.setText("\nConnection Opened!\n");
            }

        }
    }

    void beginListenForData()
    {
        // initialize a worker/reader thread, then start them with a Thread wrapper.

        // a share BlockingQueue queue is utilized for both threads as to maintain thread-safety
        //reader thread listens to the logger, receives data as a single line (till \n) and puts a string in a blockingqueue
        //worker thread processes one string at a time from the blockingqueue and print them out on UI
        worker = new Worker(queue, stopThread, temp, act);
        reader = new Reader(queue, stopThread, inputStream);
        new Thread(reader).start();
        new Thread(worker).start();
    }

    public void onClickStop(View view) throws IOException {
        // stop reader and worker threads, closes socket/inputStream
        reader.stopThread = true;
        worker.stopThread = true;
        inputStream.close();
        socket.close();
        setUiEnabled(false);
        deviceConnected=false;
        textView.append("\nConnection Closed!\n");
    }

    public void onClickClear(View view) {
        //clears textView table
        textView.setText("");
        pressure.setText("");
        temp.setText("");
    }

}


