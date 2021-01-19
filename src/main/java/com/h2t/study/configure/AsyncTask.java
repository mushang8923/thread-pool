package com.h2t.study.configure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
@Slf4j
public class AsyncTask {
    @Async("123123")
    public void doTask1(){
        log.info("i get a task");
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL("https://www.baidu.com/");

            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));

            String line;
            StringBuilder stringBuilder;
            while ((line = reader.readLine()) != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(line);
            }
        }

        catch (IOException e) {

        } finally {
            if(reader != null) {
                try {
                    reader.close();
                }
                catch(Exception e) {

                }
            }
            if (connection != null)
                connection.disconnect();
        }
    }

    @Async("123123")
    public void doTask2(){
        log.info("1231231111");

    }
}
