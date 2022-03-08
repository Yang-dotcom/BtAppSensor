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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import android.os.Handler;

public class MainActivity extends AppCompatActivity {
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");//Serial Port Service ID
    private BluetoothDevice device;
    BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    Set<BluetoothDevice> bondedDevices;
    private InputStream inputStream;
    Button startButton,clearButton,stopButton, connect, changedDevice;
    TextView textView, force_value, force;
    TableLayout tblLayout;
    //update 1
    EditText refresh_rate;
    boolean deviceConnected=false;
    boolean reset0values= true;
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
    // change Device switch
    Boolean switch_device = true;
    ArrayList<String> list = new ArrayList();
    ListView lv;
    float[] t0, p0;


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
        lv = (ListView) findViewById(R.id.listView);
        connect = (Button) findViewById(R.id.connect);
        force = (TextView) findViewById(R.id.force);
        force_value = (TextView) findViewById(R.id.force_value);
        changedDevice = (Button) findViewById(R.id.change_device);
        setTitle("CTech Reader");
        setUiEnabled(UI_switch);


        //crash handling
        appInitialization();
        //set to connect screen
        connectScreen();

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

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3){
                String selected = list.get(position);
                for (Iterator<BluetoothDevice> it = bondedDevices.iterator(); it.hasNext();){
                    BluetoothDevice bt_device = it.next();
                    if (bt_device.getName().equals(selected)){
                        device = bluetoothAdapter.getRemoteDevice(String.valueOf(bt_device));
                        tblScreen();
                    }
                }
            }

        });
    }

    @Override
    // handling of device orientantion change
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

        if (reader != null && timer != null){
            reader.stopThread = true;
            timer.cancel();
        }
        if (inputStream != null && socket != null){
            try {
                inputStream.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        UI_switch = false;
        setUiEnabled(UI_switch);
        deviceConnected=false;
        textView.setText("\nConnection Closed!\n");
        Thread.sleep(400);
        k = 0;
    }
    private Thread.UncaughtExceptionHandler defaultUEH;

    private void appInitialization() {
        defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(_unCaughtExceptionHandler);
    }

    private final Thread.UncaughtExceptionHandler _unCaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(@NonNull Thread thread, Throwable ex) {
            ex.printStackTrace();
            try {
                inputStream.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    // Custom Switch to turn on/off clear/stop btn when startbtn is clicked
    public void setUiEnabled(boolean bool)
    {
        tblLayout.setEnabled(bool);
        startButton.setEnabled(!bool);
        stopButton.setEnabled(bool);
        clearButton.setEnabled(!bool);

    }


    public void onClickConnect(View view){
        BTinit();
    }

    /* Initialize a BluetoothDevice class using .getremoteDevice method on bluetoothadapter, which we got through getdefaultadapter method;
        if Bluetooth is not enabled on the android device yet, request permission to enable it and proceed with activation.
        return true if the BluetoothDevice has successfully paired with the selected bluetooth device*/
    @SuppressLint("SetTextI18n")
    public boolean BTinit() {
        boolean found = false;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Device doesnt Support Bluetooth", Toast.LENGTH_SHORT).show();
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 0);
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Please Pair the Device first", Toast.LENGTH_SHORT).show();
        } else  if (switch_device){
            // if switch_device == true, it will display a refreshed list of available bluetooth devices
            list.clear();
            for (BluetoothDevice bt : bondedDevices) list.add(bt.getName());
            Toast.makeText(getApplicationContext(), "Showing Paired Devices",Toast.LENGTH_SHORT).show();
            ArrayAdapter adapter = new  ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
            lv.setAdapter(adapter);
            found = true;
            return found;
        }else{
            for (BluetoothDevice bt : bondedDevices){
                if(bt.getName().equals(device.getName())){
                    found = true;
                    return found;
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

    void initializationStep(){
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
        if (reset0values){
            p0 = new float[6];
            t0 = new float[6];
            System.out.println(Arrays.toString(p0));
            System.out.println(Arrays.toString(t0));
            processedInput = new ProcessedInput(n_sensors, valuestr, p0, t0);
            processedInput.run();
            System.out.println(Arrays.toString(processedInput.temp));
            System.out.println(Arrays.toString(processedInput.pressure));
            t0 = processedInput.temp.clone();
            p0 = processedInput.pressure.clone();
            System.out.println(Arrays.toString(p0));
            System.out.println(Arrays.toString(t0));
            reset0values = false;
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


        initializationStep();

        //create a timer schedule a TimerTask with tbl_refresh_ms interval to the timer class
        timer = new Timer();
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
            DecimalFormat df = new DecimalFormat("0.00");
            //Create instance of ProcessedInput given an instance of valuestr
            processedInput = new ProcessedInput(n_sensors, valuestr, p0, t0);
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
                @SuppressLint("DefaultLocale") String rounded = String.format("%.0f", processedInput.pressure[i]);
                pressure.setText(rounded);

                // display temp of i-th sensor on cell[i][2]
                TextView temp = (TextView) row.getChildAt(2);
                float t1 = processedInput.temp[i];
                t1 = Float.parseFloat(df.format(t1));
                temp.setText(Float.toString(t1));

            }
            float f1 = processedInput.weightedForce;
            f1 = Float.parseFloat(df.format(f1));
            force_value.setText(Float.toString(f1));
        }
    };

    //UpdateGUI() takes a string value from queue and assigns it to valuestr, then increases the counter k
    //finally it calls myRunnable to modify the UI using a Handler poster.
    private void UpdateGUI() throws InterruptedException {
        try {
            valuestr = queue.take();
            System.out.println(Arrays.toString(p0) + Arrays.toString(t0));
            //System.out.println(valuestr);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        k++;
        myHandler.post(myRunnable);
    }




    public void onClickStop(View view) throws IOException, InterruptedException {
        reader.stopThread = true;
        timer.cancel();
        closing();
    }

    public void onClickClear(View view) throws InterruptedException {
        reset0values = true;
        /*TableLayout tblLayout = (TableLayout)findViewById(R.id.tableLayout);
        for(int i = 1; i <7; i++){
            TableRow row = (TableRow)tblLayout.getChildAt(i);
            for (int j=0; j<3; j++){
                TextView txt = (TextView)row.getChildAt(j);
                txt.setText("0");
            }
        }*/

    }

    public void onClickChangeDevice(View view) throws InterruptedException {
        connectScreen();
        closing();
    }

    public void tblScreen(){
        force_value.setVisibility(View.VISIBLE);
        force.setVisibility(View.VISIBLE);
        textView.setVisibility(View.VISIBLE);
        refresh_rate.setVisibility(View.VISIBLE);
        tblLayout.setVisibility(View.VISIBLE);
        startButton.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.VISIBLE);
        clearButton.setVisibility(View.VISIBLE);
        changedDevice.setVisibility(View.VISIBLE);
        lv.setVisibility(View.INVISIBLE);
        connect.setVisibility(View.INVISIBLE);
        switch_device = false;
    }

    public void connectScreen(){
        switch_device = true;
        force_value.setVisibility(View.INVISIBLE);
        force.setVisibility(View.INVISIBLE);
        textView.setVisibility(View.INVISIBLE);
        refresh_rate.setVisibility(View.INVISIBLE);
        tblLayout.setVisibility(View.INVISIBLE);
        changedDevice.setVisibility(View.INVISIBLE);
        stopButton.setVisibility(View.INVISIBLE);
        clearButton.setVisibility(View.INVISIBLE);
        startButton.setVisibility(View.INVISIBLE);
        lv.setVisibility(View.VISIBLE);
        connect.setVisibility(View.VISIBLE);
    }
}




