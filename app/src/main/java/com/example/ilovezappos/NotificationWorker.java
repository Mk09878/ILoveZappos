package com.example.ilovezappos;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.ilovezappos.api_requests.Ticker_Req;
import com.example.ilovezappos.getters.TickerGetters;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NotificationWorker extends Worker {
    String priceFile = null;

    Context context;
    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;

    }

    // Reads the price.text file and checks if the current price has fallen below the given price.
    // If yes, displays a notification
    @NonNull
    @Override
    public Result doWork() {

        FileInputStream fileInputStream;
        try {
            fileInputStream = context.openFileInput("price.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            priceFile = bufferedReader.readLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.bitstamp.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Ticker_Req jsonPlaceHolderApi = retrofit.create(Ticker_Req.class);

        Call<TickerGetters> call = jsonPlaceHolderApi.getPrice();

        call.enqueue(new Callback<TickerGetters>() {
            @Override
            public void onResponse(Call<TickerGetters> call, Response<TickerGetters> response) {
                if (Float.parseFloat(response.body().getLast()) < Float.parseFloat(priceFile)) {
                    System.out.println("Show");
                    showNotification("Rates have fallen!","Click here to check new rates");
                }
            }

            @Override
            public void onFailure(Call<TickerGetters> call, Throwable t) {


            }
        });

        return Result.success();

    }
    private void showNotification(String title, String desc) {

        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent1 = new Intent(this.getApplicationContext(), MainActivity.class);
        PendingIntent pendingNotificationIntent = PendingIntent.getActivity(this.getApplicationContext(), 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);

        String channelId = "task_channel";
        String channelName = "task_name";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new
                    NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setContentTitle(title)
                .setContentText(desc)
                .setContentIntent(pendingNotificationIntent)
                .setSmallIcon(R.drawable.ic_error_outline_24px)
                .setAutoCancel(true);


        manager.notify(1, builder.build());

    }
}
