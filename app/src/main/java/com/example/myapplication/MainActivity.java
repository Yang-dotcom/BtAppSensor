package com.example.myapplication;


import android.widget.*;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;

import java.io.*;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private InputStream inputStream;
    Button startButton,clearButton,stopButton;
    TextView textView;
    boolean deviceConnected=false;
    boolean stopThread;
    int capacity = 1;
    public Worker worker;
    public Reader reader;
    Activity act = MainActivity.this;
    LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(capacity);
    ProcessedInput processedInput;
    String valuestr;
    Integer k = 0;
    final Handler myHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton = (Button) findViewById(R.id.buttonStart);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        textView = (TextView) findViewById(R.id.textView);
        setTitle("CTech Reader");
        setUiEnabled(false);

    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            reader.stopThread = true;
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            setUiEnabled(false);
            deviceConnected=false;
            textView.setText("\nConnection Closed!\n");
        } else {
            reader.stopThread = true;
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            setUiEnabled(false);
            deviceConnected=false;
            textView.setText("\nConnection Closed!\n");
        }
    }


    // Custom Switch to turn on/off clear/stop btn when startbtn is clicked
    public void setUiEnabled(boolean bool)
    {
        startButton.setEnabled(!bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);
        clearButton.setEnabled(!bool);

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
        } else {
            String device_name = "CTechLogger";
            for (BluetoothDevice iterator : bondedDevices) {
                if (iterator.getName().equals(device_name)) {
                    device = bluetoothAdapter.getRemoteDevice(String.valueOf(iterator));
                    found = true;
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

        // queue and tbllayout have to be initialized in the same method where the threads are initialized to prevent issues
        TableLayout tblLayout = (TableLayout)findViewById(R.id.tableLayout);
        //worker = new Worker(queue, stopThread, act, tblLayout);
        reader = new Reader(queue, stopThread, inputStream, socket);
        new Thread(reader).start();
        stopThread = false;
        byte[] buffer = new byte[1024];

        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                BufferedReader btInputStream = new BufferedReader(new InputStreamReader(inputStream));
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        int byteCount = inputStream.available();
                        if(byteCount > 0)
                        {
                            String string = btInputStream.readLine();
                            boolean isFound = string.contains("$MEA");
                            if (string.length() > 25 && isFound){
                                queue.put(string);
                                System.out.println("thread1 working" + byteCount);
                            }

                        }
                    }
                    catch (IOException | InterruptedException ex)
                    {
                        System.out.println("thread1 not working");
                        stopThread = true;
                    }
                }
            }
        });
        Thread thread2  = new Thread(new Runnable()
        {
            public void run()
            {
                BufferedReader btInputStream = new BufferedReader(new InputStreamReader(inputStream));
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try {
                        String str2 =  queue.take();
                        System.out.println("Thread 2:"+ str2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        });

        //thread.start();
        //thread2.start();
        //new Thread(worker).start();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    UpdateGUI();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 300);
    }

    final Runnable myRunnable = new Runnable() {
        @SuppressLint("SetTextI18n")
        @Override
        public void run() {

            //n sensors to be changed dynamically
            processedInput = new ProcessedInput(6, valuestr);
            processedInput.run();
            TableLayout tblLayout = (TableLayout) findViewById(R.id.tableLayout);
            textView.setText("Measurement n.: "+String.valueOf(k));

            // loop through the cells in the tblLayout
            for (int i = 0; i < processedInput.n; i++) {
                // get row  i of the tbl
                TableRow row = (TableRow) tblLayout.getChildAt(i + 1); // get child index on particular row

                // display sensor_id of i-th sensor on cell[i][0]
                TextView sensor = (TextView) row.getChildAt(0);
                //sensor.setText((processedInput.sensor_ID[i]).toString());
                sensor.setText((processedInput.sensor_ID[i]).toString());

                // display pressure of i-th sensor on cell[i][1]
                TextView pressure = (TextView) row.getChildAt(1);
                pressure.setText((processedInput.pressure[i]).toString());

                // display temp of i-th sensor on cell[i][2]
                TextView temp = (TextView) row.getChildAt(2);
                temp.setText((processedInput.temp[i]).toString());
            }
        }
    };
    private void UpdateGUI() throws InterruptedException {
        try {
            valuestr = queue.take();
            System.out.println(valuestr);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        k++;
        myHandler.post(myRunnable);
    }




    public void onClickStop(View view) throws IOException {
        // stop reader and worker threads, closes socket/inputStream
        //worker.stopThread = true;
        reader.stopThread = true;
        inputStream.close();
        socket.close();
        setUiEnabled(false);
        deviceConnected=false;
        textView.setText("\nConnection Closed!\n");
    }

    public void onClickClear(View view) {
        //clears textView table
        textView.setText("0");
        TableLayout tblLayout = (TableLayout)findViewById(R.id.tableLayout);
        for(int i = 1; i <7; i++){
            TableRow row = (TableRow)tblLayout.getChildAt(i);
            for (int j=0; j<3; j++){
                TextView txt = (TextView)row.getChildAt(j);
                txt.setText("0");
            }
        }

    }
}


