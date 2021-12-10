package com.example.myapplication;


import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
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
import android.os.Handler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    Button startButton, sendButton,clearButton,stopButton;
    TextView address, pressure, temp, textView;
    EditText editText;
    boolean deviceConnected=false;
    Thread thread;
    byte buffer[];
    BufferedReader btInputStream;
    int bufferPosition;
    boolean stopThread;
    int capacity = 60;
    BlockingQueue<String> queue = new LinkedBlockingQueue<>(capacity);
    public Reader reader;
    Activity act = MainActivity.this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton = (Button) findViewById(R.id.buttonStart);
        sendButton = (Button) findViewById(R.id.buttonSend);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        editText = (EditText) findViewById(R.id.editText);
        pressure = (TextView) findViewById(R.id.pressure);
        temp = (TextView) findViewById(R.id.temp);
        address = (TextView) findViewById(R.id.address);
        textView = (TextView) findViewById(R.id.textView);
        reader = new Reader(queue, stopThread, temp, act);
        setUiEnabled(false);

    }
    public void setUiEnabled(boolean bool)
    {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);

    }

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
            pressure.append("Device not paired");
        } else {
            String device_name = "CTechLogger";
            for (BluetoothDevice iterator : bondedDevices) {
                if (iterator.getName().equals(device_name)) {
                    device = bluetoothAdapter.getRemoteDevice(String.valueOf(iterator));
                    found = true;
                    pressure.setText(iterator.getName());
                    temp.setText(device.getAddress());
                    break;
                }


            }
        }
        return found;
    }

    public boolean BTconnect()
    {
        boolean connected=true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected=false;
            textView.append("\nConnection not Opened!\n");
        }
        if(connected)
        {
            try {
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream=socket.getInputStream();
                BufferedReader btInputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }



        return connected;
    }


    public void onClickStart(View view) {
        if(BTinit())
        {
            if(BTconnect())
            {
                setUiEnabled(true);
                deviceConnected=true;
                beginListenForData();
                textView.setText("\nConnection Opened!\n");
            }

        }
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        stopThread = false;
        BufferedReader btInputStream = new BufferedReader(new InputStreamReader(inputStream));
        Thread thread  = new Thread(new Runnable()

        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        int byteCount = inputStream.available();
                        if(byteCount > 0)
                        {
                            //byte[] rawBytes = new byte[byteCount];
                            //inputStream.read(rawBytes);
                             //String string=new String(rawBytes, StandardCharsets.UTF_8);
                            String string = btInputStream.readLine();
                            queue.put(string);
                            System.out.println("thread1 working");
                           /*handler.post(new Runnable() {
                                public void run()
                                {
                                    try {
                                        temp.setText(queue.take());
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });*/

                        }
                    }
                    catch (IOException | InterruptedException ex)
                    {
                        stopThread = true;
                        reader.stopThread = true;
                    }
                }
            }
        });

        thread.start();
        new Thread(reader).start();
    }

    public void onClickSend(View view) {
        String string = editText.getText().toString();
        string.concat("\n");
        try {
            outputStream.write(string.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        textView.append("\nSent Data:"+string+"\n");

    }

    public void onClickStop(View view) throws IOException {
        stopThread = true;
        reader.stopThread = true;
        outputStream.close();
        inputStream.close();
        socket.close();
        setUiEnabled(false);
        deviceConnected=false;
        textView.append("\nConnection Closed!\n");
    }

    public void onClickClear(View view) {
        textView.setText("");
        pressure.setText("");
        temp.setText("");
    }

    public void updateText(String updatedText){
        textView.setText(updatedText);
    }

}


