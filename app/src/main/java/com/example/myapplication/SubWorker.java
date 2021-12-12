package com.example.myapplication;

import android.app.Activity;
import android.widget.TextView;

import java.util.concurrent.BlockingQueue;

public class SubWorker implements Runnable {
    final private Activity act;
    private TextView temp;
    private BlockingQueue<String> sub_queue;
    String[] values;
    String one_str_values;
    Float tempe, pressu;
    //Integer sensor_ID;
    String sensor_ID;

    //each element of sub_queue is a string having format: "%d P%.4f T%.4f" where the first integer is the sensor ID
    public SubWorker(BlockingQueue<String> sub_queue, TextView temp, Activity act){
        this.sub_queue = sub_queue;
        this.temp = temp;
        this.act = act;
    }



    public void run(){
        while (!Thread.currentThread().isInterrupted()){
            try {
                one_str_values = sub_queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            values = one_str_values.split("\t");
            sensor_ID = values[0];
            //tempe = Float.parseFloat(values[1].substring(1));
            //pressu = Float.parseFloat(values[2].substring(1));

            System.out.println("Thread 4: " + values[1]);
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    temp.setText(values[0]);
                }
            });
        }
        }

}
// NEED TO CHECK WHEN "$MEA	26508	12 ERR No new sample"
