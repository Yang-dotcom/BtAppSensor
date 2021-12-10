package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.widget.TextView;

import java.util.concurrent.BlockingQueue;

public class Reader implements Runnable {
    private TextView temp;
    public Boolean stopThread;
    private BlockingQueue<String> queue;
    final private Activity act;
    String str;

    public Reader(BlockingQueue<String> queue1, Boolean stopThread1, TextView temp, Activity act){
        this.temp = temp;
        this.queue = queue1;
        this.stopThread = stopThread1;
        this.act = act;

    }

    @Override
    public void run(){
        while(!Thread.currentThread().isInterrupted() && !stopThread){
            try {
                str = queue.take();
                Thread.sleep(1000);
                System.out.println("Thread 2: "+str + stopThread);
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        temp.setText(str);
                    }
                });

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
