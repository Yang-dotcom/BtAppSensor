package com.example.myapplication;


import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
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
    TableLayout tblLayout;
    EditText refresh_rate;
    boolean deviceConnected=false;
    public Reader reader;
    ProcessedInput processedInput;
    String valuestr;
    final Handler myHandler = new Handler();
    int n_sensors;
    //refresh rate of table
    //int tbl_refresh_ms = 300;
    //capacity of the blockingqueue
    int capacity = 500;
    LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(capacity);
    //measurement counter
    Integer k = 0;
    // default ms refresh value
    Integer tbl_refresh_ms = 100;
    //timer class
    Timer timer = new Timer();
    // UI button switch;
    Boolean UI_switch = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton = (Button) findViewById(R.id.buttonStart);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        textView = (TextView) findViewById(R.id.textView);
        tblLayout = (TableLayout) findViewById(R.id.tableLayout);
        refresh_rate = (EditText) findViewById(R.id.refresh_rate);
        setTitle("CTech Reader");
        setUiEnabled(UI_switch);

        refresh_rate.setOnEditorActionListener(new TextView.OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    tbl_refresh_ms = Integer.parseInt(v.getText().toString().trim());
                    timer.cancel();
                    timer = new Timer();
                    if (UI_switch){
                        beginListenForData();
                        //System.out.println("working event");
                    }
                    return true;
                }
                return false;
            }
        });

    }

    @Override
    // device orientation changing handling
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            closing();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // closing operation. ends reader thread; closes inputstream and scoket.
    public void closing() throws InterruptedException {
        reader.stopThread = true;
        timer.cancel();
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
        UI_switch = false;
        setUiEnabled(UI_switch);
        deviceConnected=false;
        textView.setText("\nConnection Closed!\n");
        Thread.sleep(400);
        k = 0;
    }

    // Custom Switch to turn on/off clear/stop btn when startbtn is clicked
    public void setUiEnabled(boolean bool)
    {
        tblLayout.setEnabled(bool);
        startButton.setEnabled(!bool);
        stopButton.setEnabled(bool);
        clearButton.setEnabled(!bool);

    }

/*    refresh_rate.setOnEditorActionListener(new TextView.OnEditorActionListener(){
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                System.out.println("working event");
                return true;
            }
            return false;
        }
    });*/

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
                UI_switch = true;
                setUiEnabled(UI_switch);
                deviceConnected=true;
                beginListenForData();
                textView.setText("\nConnection Opened!\n");
            }

        }
    }

    void beginListenForData()
    {

        // initialize a reader thread, then start it with a Thread wrapper.

        // a share BlockingQueue queue is utilized for both threads as to maintain thread-safety
        //reader thread listens to the logger, receives data as a single line (till \n) and puts a string in a blockingqueue
        //Timer thread processes one string at a time from the blockingqueue and displays them out on UI

        reader = new Reader(queue, inputStream, socket);
        new Thread(reader).start();


        //Initialization step (first reading is lost)
        try {
            valuestr = queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String[] vet_str_per_sensor = valuestr.split("s");
        //the first element of the array after the split
        // is "$MEA n23" and the first sensor data is thus on the second element onward.
        n_sensors = vet_str_per_sensor.length - 1;


        //schedule a TimerTask with tbl_refresh_ms interval to the timer class
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    //Timertask calls UpdateGUI()
                    UpdateGUI();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 0, tbl_refresh_ms);
    }

    final Runnable myRunnable = new Runnable() {
        @SuppressLint("SetTextI18n")
        @Override
        public void run() {
            //Create instance of ProcessedInput given an instance of valuestr
            processedInput = new ProcessedInput(n_sensors, valuestr);
            processedInput.run();
            TableLayout tblLayout = (TableLayout) findViewById(R.id.tableLayout);
            textView.setText("Measurement n. "+String.valueOf(k));

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

    //UpdateGUI() takes a string value from queue and assigns it to valuestr, then increases the counter k
    //finally it calls myRunnable to modify the UI using a Handler poster.
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




    public void onClickStop(View view) throws IOException, InterruptedException {
        closing();
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


