package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.concurrent.LinkedBlockingQueue;



public class Worker implements Runnable {
    TableLayout tblLayout;
    Integer table_refresh_ms = 500;
    public Boolean stopThread;
    private final LinkedBlockingQueue<String> queue;
    Activity act;
    String str;
    String[] vet_str_per_sensor;
    Integer n_sensors;
    ProcessedInput processedInput;

    public Worker(LinkedBlockingQueue<String> queue1, Boolean stopThread1, Activity act, TableLayout tblLayout){
        this.queue = queue1;
        this.stopThread = stopThread1;
        this.act = act;
        this.tblLayout = tblLayout;

    }

    @Override
    public void run(){
        //Initialization step (first reading is lost)
        try {
            str = queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        vet_str_per_sensor = str.split("s");
        //the first element of the array after the split
        // is "$MEA n23" and the first sensor data is thus on the second element onward.
        n_sensors = vet_str_per_sensor.length - 1;



        //looping step
        while(!Thread.currentThread().isInterrupted() && !stopThread){
            try {
                // take a single reading from the blockingqueue, in string form
                str = queue.take();
                //split the str into an array of strings, with the split occurring when "s" is encountered. the character "s" is not included
                vet_str_per_sensor = str.split("s");
                // create an instance of a class ProcessedInput that will process the str and create arrays of (int) sensor_Id, (float)pressure, (float) temperature
                // after running the method .run();
                processedInput = new ProcessedInput(n_sensors, str);
                processedInput.run();
                System.out.println("Thread 2:"+ str);

                // we change UI elements on the UI thread by running the method .runOnUiThread() on the activity
                act.runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {

                        // loop through the cells in the tblLayout
                        for(int i=0;i<n_sensors;i++)
                        {
                            // get row  i of the tbl
                            TableRow row = (TableRow)tblLayout.getChildAt(i+1); // get child index on particular row

                            // display sensor_id of i-th sensor on cell[i][0]
                            TextView sensor = (TextView)row.getChildAt(0);
                            sensor.setText((processedInput.sensor_ID[i]).toString());

                            // display pressure of i-th sensor on cell[i][1]
                            TextView pressure = (TextView)row.getChildAt(1);
                            pressure.setText((processedInput.pressure[i]).toString());

                            // display temp of i-th sensor on cell[i][2]
                            TextView temp = (TextView)row.getChildAt(2);
                            temp.setText((processedInput.temp[i]).toString());

                        }
                    }
                });
                // control update rate of the table by making the thread go to sleep for table_refresh_ms milliseconds
                Thread.sleep(table_refresh_ms);
            } catch (InterruptedException e) {
                e.printStackTrace();
                stopThread = true;
            }
        }
    }
}




