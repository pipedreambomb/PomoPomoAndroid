package com.vngrs.android.pomodoro.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.vngrs.android.pomodoro.App;
import com.vngrs.android.pomodoro.R;
import com.vngrs.android.pomodoro.data.PomodoroDatabase;
import com.vngrs.android.pomodoro.data.PomodoroProvider.Pomodoros;
import com.vngrs.android.pomodoro.shared.NotificationBuilder;
import com.vngrs.android.pomodoro.shared.PomodoroMaster;
import com.vngrs.android.pomodoro.shared.model.ActivityType;
import com.vngrs.android.pomodoro.shared.service.BaseNotificationService;
import com.vngrs.android.pomodoro.ui.MainActivity;
import com.vngrs.android.pomodoro.util.Analytics;

import javax.inject.Inject;

import dagger.Lazy;

/**
 * @see BaseNotificationService
 *
 * Created by Said Tahsin Dane on 12/4/15.
 */
public class PomodoroNotificationService extends BaseNotificationService {

    @Inject Lazy<Analytics> analytics;

    @Override
    public void onCreate() {
        App.get(this).component().inject(this);
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null) {
            handleAnalytics(intent);
        }

        super.onHandleIntent(intent);
    }

    private void handleAnalytics(Intent intent) {
        String action = null, label = "";
        switch (intent.getAction()) {
            case ACTION_START:
            case ACTION_STOP:
            case ACTION_FINISH_ALARM:
            case ACTION_RESET:
                action = intent.getAction().replace("com.vngrs.android.pomodoro.action.", "");
                if (intent.getAction().equals(ACTION_START)) {
                    label = ActivityType.fromValue(intent.getIntExtra(EXTRA_ACTIVITY_TYPE, 0)).toString();
                } else {
                    label = pomodoroMaster.getActivityType().toString();
                }
                break;
            default:
                break;
        }

        if (action != null) {
            /* [ANALYTICS:EVENT]
             * TRIGGER:   Taking one of the Pomodoro Actions.
             * CATEGORY:  'Pomodoro'
             * ACTION:    'Feedback'
             * LABEL:     Activity Type if the action is Start
             * [/ANALYTICS]
             */
            analytics.get().sendEvent("Pomodoro", action, label);

            ContentValues values = new ContentValues();
            values.put(PomodoroDatabase.PomodoroColumns.ACTION, action);
            values.put(PomodoroDatabase.PomodoroColumns.ACTIVITY_TYPE, label);
            getContentResolver().insert(Pomodoros.CONTENT_URI, values);
        }
    }

    @Nullable
    @Override
    public Notification buildNotification(Context context, PomodoroMaster pomodoroMaster) {
        if (pomodoroMaster.getActivityType() != ActivityType.NONE) {
            NotificationBuilder builder = new NotificationBuilder(context,
                    pomodoroMaster,
                    R.drawable.ic_stat_pomodoro,
                    R.drawable.ic_action_start_phone,
                    R.drawable.ic_action_stop_phone,
                    R.drawable.ic_action_reset_96dp,
                    R.drawable.ic_skip_next_white_48dp);
            final Intent contentIntent = new Intent(context, MainActivity.class);
            return builder
                    .buildNotificationPhone(PendingIntent.getActivity(context, 0, contentIntent, 0));
        } else {
            return null;
        }
    }
}
