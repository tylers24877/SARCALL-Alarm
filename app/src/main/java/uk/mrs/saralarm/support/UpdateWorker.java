package uk.mrs.saralarm.support;

import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;

import org.jetbrains.annotations.NotNull;

public class UpdateWorker extends Worker {
    public UpdateWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NotNull
    public ListenableWorker.Result doWork() {
        if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("betaChannel", false)) {
            new AppUpdater(getApplicationContext()).setUpdateFrom(UpdateFrom.XML).setDisplay(Display.NOTIFICATION).setUpdateXML("https://raw.githubusercontent.com/tylers24877/MRT-SAR-Alarm/master/update_beta.xml").setCancelable(false).start();
        } else {
            new AppUpdater(getApplicationContext()).setUpdateFrom(UpdateFrom.XML).setDisplay(Display.NOTIFICATION).setUpdateXML("https://raw.githubusercontent.com/tylers24877/MRT-SAR-Alarm/master/update.xml").setCancelable(false).start();
        }
        return ListenableWorker.Result.success();
    }
}