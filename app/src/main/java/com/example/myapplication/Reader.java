package com.example.myapplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Reader implements Runnable {
    public Boolean stopThread;
    private final LinkedBlockingQueue<String> queue;
    InputStream inputStream;

    public Reader(LinkedBlockingQueue<String> queue1, Boolean stopThread1, InputStream inputStream){
        this.queue = queue1;
        this.stopThread = stopThread1;
        this.inputStream = inputStream;

    }

    @Override
    public void run(){
        BufferedReader btInputStream = new BufferedReader(new InputStreamReader(inputStream));
        while(!Thread.currentThread().isInterrupted() && !stopThread){
            try {
                Boolean byteCount = btInputStream.ready();
                //int byteCount = inputStream.read();
                if(byteCount){
                    String string = btInputStream.readLine();
                    boolean isFound = string.contains("$MEA");
                    if (string.length() > 25 && isFound){
                        queue.put(string);
                        //System.out.println("Thread 2:"+ queue.take() + byteCount);
                        System.out.println("thread1 working" + byteCount);
                    }
                }else{
                    System.out.println("thread1 not working"+ byteCount);
                }


            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
                stopThread =true;
            }
        }
    }

}
