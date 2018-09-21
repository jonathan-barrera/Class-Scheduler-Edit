package com.example.android.classscheduler.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * Created by jonathanbarrera on 6/19/18.
 */

public class TodaysClassesWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TodaysClassesWidgetFactory(this.getApplicationContext(),
                intent);
    }
}
