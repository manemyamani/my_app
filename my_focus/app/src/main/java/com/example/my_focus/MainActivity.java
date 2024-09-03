package com.example.my_focus;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView usefulAppsTextView;
    private TextView nonUsefulAppsTextView;
    private ArrayList<String> usefulApps = new ArrayList<>();
    private ArrayList<String> nonUsefulApps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usefulAppsTextView = findViewById(R.id.useful_apps_text_view);
        nonUsefulAppsTextView = findViewById(R.id.non_useful_apps_text_view);

        if (!hasUsageStatsPermission(this)) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Please grant Usage Access permission", Toast.LENGTH_LONG).show();
        } else {
            trackAppUsage();
            updateUI();
        }
    }

    private boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void trackAppUsage() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

        for (UsageStats usageStats : usageStatsList) {
            String packageName = usageStats.getPackageName();
            long timeInForeground = usageStats.getTotalTimeInForeground();
            String appCategory = getAppCategory(packageName);

            if (appCategory != null) {
                if (isNonUsefulCategory(appCategory)) {
                    nonUsefulApps.add(packageName + " - Time used: " + timeInForeground / 1000 + " seconds");
                    handleNonUsefulAppUsage(packageName, timeInForeground);
                } else {
                    usefulApps.add(packageName + " - Time used: " + timeInForeground / 1000 + " seconds");
                    handleUsefulAppUsage(packageName, timeInForeground);
                }
            }
        }
    }

    private String getAppCategory(String packageName) {
        PackageManager packageManager = getPackageManager();
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                return "SYSTEM";
            } else if (packageName.toLowerCase().contains("social") || packageName.toLowerCase().contains("game")) {
                return "SOCIAL/ENTERTAINMENT";
            } else {
                return "OTHER";
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("MainActivity", "App not found: " + packageName, e);
            return null;
        }
    }

    private boolean isNonUsefulCategory(String category) {
        return category.equals("SOCIAL/ENTERTAINMENT");
    }

    private void handleNonUsefulAppUsage(String packageName, long timeInForeground) {
        long oneHourInMillis = 60 * 60 * 1000;

        if (timeInForeground > oneHourInMillis) {
            Toast.makeText(this, "You've spent over an hour on " + packageName + "! Consider taking a break.", Toast.LENGTH_LONG).show();

            long alertIntervalInMillis = 15 * 60 * 1000;
            new android.os.Handler().postDelayed(() ->
                            Toast.makeText(this, "You’re still using " + packageName + ". Please take a break.", Toast.LENGTH_LONG).show(),
                    alertIntervalInMillis);
        }
    }

    private void handleUsefulAppUsage(String packageName, long timeInForeground) {
        Log.d("TrackAppUsage", packageName + " is a useful app. Keep up the good work!");
    }

    private void updateUI() {
        String usefulAppsText = "Useful Apps:\n" + String.join("\n", usefulApps);
        String nonUsefulAppsText = "Non-Useful Apps:\n" + String.join("\n", nonUsefulApps);

        usefulAppsTextView.setText(usefulAppsText);
        nonUsefulAppsTextView.setText(nonUsefulAppsText);
    }
}
