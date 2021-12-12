package com.example.myapplication;

import android.app.Activity;
import android.widget.TextView;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Worker implements Runnable {
    TextView temp;
    public Boolean stopThread;
    private final BlockingQueue<String> queue;
    BlockingQueue<String>[] vet_queues = new LinkedBlockingQueue[6];
    Activity act;
    String str;
    String[] vet_str_per_sensor;
    Integer n_sensors;

    public Worker(BlockingQueue<String> queue1, Boolean stopThread1, TextView temp, Activity act){
        this.temp = temp;
        this.queue = queue1;
        this.stopThread = stopThread1;
        this.act = act;

    }

    @Override
    public void run(){
        //Initialization step (first reading is lost)
        try {
            str = queue.take();
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //vet_str_per_sensor = str.split("m");
        //the first element of the array after the split
        // is "$MEA n23" (generalities) and the first sensor data is thus on the second element onward.
        //n_sensors = vet_str_per_sensor.length - 1;
        n_sensors = 1;
        create_subworkers();

        //looping step
        while(!Thread.currentThread().isInterrupted() && !stopThread){
            try {
                str = queue.take();
                Thread.sleep(500);
                /*System.out.println("Thread 2: "+str + stopThread);
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        temp.setText(str);
                    }
                });
                */
                // split str into an array of strings, where each element
                // contains the data related to a specific sensor, with the
                // splitting happening at the letter "m" (stands for sensor).
                // the letter s will not be included in the elements of the array.
                System.out.println("Thread 2:"+str + stopThread);
                //vet_str_per_sensor = str.split("s");
                // * vet_str_per_sensor = str.split(" ");
                for (int j = 0; j< n_sensors; j++){
                    // vet_str_per_sensor[j+1] since sensors measurament start on the second element onward
                    // of vet_str_per_sensor
                    // *vet_queues[j].put(vet_str_per_sensor[j+1]);
                    vet_queues[j].put(str);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                stopThread = true;
            }
        }
    }


    // Create 6 blockinqueues for 6 different subworker threads that will be able to
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
}



