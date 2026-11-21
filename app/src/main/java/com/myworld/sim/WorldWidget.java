package com.myworld.sim;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** 1×1 compact Earth dashboard. */
public class WorldWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){
        WorldJob.schedule(context);
        for(int id:ids)manager.updateAppWidget(id,views(context));
    }
    public static void updateAll(Context context){
        AppWidgetManager manager=AppWidgetManager.getInstance(context);
        int[] small=manager.getAppWidgetIds(new ComponentName(context,WorldWidget.class));
        if(small.length>0){
            RemoteViews rv=views(context);
            for(int id:small)manager.updateAppWidget(id,rv);
        }
        WorldWidgetLarge.updateAll(context);
    }
    private static RemoteViews views(Context context){
        RemoteViews v=new RemoteViews(context.getPackageName(),R.layout.world_widget);
        WorldRepository.read(context,w->{
            v.setTextViewText(R.id.world_widget_date,w.dateShort());
            v.setTextViewText(R.id.world_widget_health,"평화 "+w.peace);
            v.setTextViewText(R.id.world_widget_eco,"경제 "+w.avg("경제"));
            return null;
        });
        Intent intent=new Intent(context,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending=PendingIntent.getActivity(context,41,intent,
            PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.world_widget_root,pending);
        return v;
    }
}
