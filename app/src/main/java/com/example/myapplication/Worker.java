package com.example.myapplication;

import android.app.Activity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


// to implement inputStream.close();
//        socket.close(); on this thread
public class Worker implements Runnable {
    TableLayout tblLayout;
    Integer table_refresh_ms = 300;
    public Boolean stopThread;
    private final BlockingQueue<String> queue;
    LinkedBlockingQueue[] vet_queues = new LinkedBlockingQueue[6];
    Activity act;
    String str;
    String[] vet_str_per_sensor;
    String [][] vet_single_data;
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
                str = queue.take();
                // vet_str_per_sensor will have 7 elements given 6 sensors, the first is $MEA n%d, so start extrapolating from [1] to [6]
                //processedInput = new ProcessedInput(n_sensors, str);
                //processedInput.run();
                vet_str_per_sensor = str.split("s");
                //System.out.println("Thread 2: "+ Arrays.toString(processedInput.mtx_str_per_sensor[0])+ str);
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TableRow row = (TableRow)tblLayout.getChildAt(1);
                        TextView act_str = (TextView)row.getChildAt(0); // get child index on particular row
                        act_str.setText(vet_str_per_sensor[1]);
                        /*for(int i=0;i<n_sensors;i++)
                        {
                            TableRow row = (TableRow)tblLayout.getChildAt(i);
                            TextView act_str = (TextView)row.getChildAt(0); // get child index on particular row
                            act_str.setText(vet_str_per_sensor[i+1]);
                            /*for(int j=0;j<1;j++){
                                TextView act_str = (TextView)row.getChildAt(j); // get child index on particular row
                                act_str.setText(vet_str_per_sensor[i+1]);
                            }//
                        }*/
                    }
                });
                Thread.sleep(table_refresh_ms);
                // split str into an array of strings, where each element
                // contains the data related to a specific sensor, with the
                // splitting happening at the letter "s" (stands for sensor).
                // the letter s will not be included in the elements of the array.
                //vet_str_per_sensor = str.split("s");
                // * vet_str_per_sensor = str.split(" ");
                //for (int j = 0; j< n_sensors; j++){


                    // vet_str_per_sensor[j+1] since sensors measurament start on the second element onward
                    // of vet_str_per_sensor
                    //vet_queues[j].put(vet_str_per_sensor[j+1]);
                    //vet_queues[j].put(str);
                //}

            } catch (InterruptedException e) {
                e.printStackTrace();
                stopThread = true;
            }
        }
    }
}



/*    // Create 6 blockinqueues for 6 different subworker threads that will be able to
    // process info incoming from 6 different sensors
    void create_subworkers(){
        Thread daemonThread;
        for (int i = 0; i< n_sensors; i++){
            vet_queues[i] = new LinkedBlockingQueue<String>();
            daemonThread = new Thread(new SubWorker(vet_queues[i], temp, act));
            daemonThread.setDaemon(true);
            daemonThread.start();
        }
    }
}*/



