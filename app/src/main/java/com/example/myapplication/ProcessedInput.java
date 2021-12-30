package com.example.myapplication;

import java.util.Arrays;

public class ProcessedInput {
    Integer n, sensor_ID[];
    String str;
    String[] vet_sensors; //size is n+1
    String[] single_sensor_data; //size of each row is n, each cell having 3 elements [sensor, T, P]
    Float temp[], pressure[];


    public ProcessedInput(Integer n, String str){
        this.n = n;
        this.str = str;
    }

    public void run(){
        vet_sensors = str.split("s");
        for (int i=1; i<n; i++){
            single_sensor_data = vet_sensors[i].split("\t");
            System.out.println(i+"Thread 2: "+ Arrays.toString(single_sensor_data)+ Arrays.toString(vet_sensors));


        }
    }

}
