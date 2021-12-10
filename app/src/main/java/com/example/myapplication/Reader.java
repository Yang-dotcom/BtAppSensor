package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;

public class Reader implements Runnable {
    public Boolean stopThread;
    private BlockingQueue<String> queue;
    InputStream inputStream;
    String str;

    public Reader(BlockingQueue<String> queue1, Boolean stopThread1, InputStream inputStream){
        this.queue = queue1;
        this.stopThread = stopThread1;
        this.inputStream = inputStream;

    }

    @Override
    public void run(){
        BufferedReader btInputStream = new BufferedReader(new InputStreamReader(inputStream));
        while(!Thread.currentThread().isInterrupted() && !stopThread){
            try {
                int byteCount = inputStream.available();
                if(byteCount > 0){
                    String string = btInputStream.readLine();
                    queue.put(string);
                    System.out.println("thread1 working");
                }


            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
                stopThread =true;
            }
        }
    }
}
