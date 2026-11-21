package com.myworld.sim;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** 2×2 detailed Earth dashboard, updated with the exact same world state. */
public class WorldWidgetLarge extends AppWidgetProvider {
    @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){
        WorldJob.schedule(context);
        for(int id:ids)manager.updateAppWidget(id,views(context));
    }
    static void updateAll(Context context){
        AppWidgetManager manager=AppWidgetManager.getInstance(context);
        int[] ids=manager.getAppWidgetIds(new ComponentName(context,WorldWidgetLarge.class));
        if(ids.length>0){
            RemoteViews rv=views(context);
            for(int id:ids)manager.updateAppWidget(id,rv);
        }
    }
    private static RemoteViews views(Context context){
        RemoteViews v=new RemoteViews(context.getPackageName(),R.layout.world_widget_large);
        WorldRepository.read(context,w->{
            v.setTextViewText(R.id.large_date,w.date());
            v.setTextViewText(R.id.large_economy,"경제 "+w.avg("경제"));
            v.setTextViewText(R.id.large_peace,"평화 "+w.peace);
            v.setTextViewText(R.id.large_climate,"환경 "+w.climate);
            v.setTextViewText(R.id.large_population,String.format(java.util.Locale.KOREA,
                "인구 %.1f억",w.totalPopulation()/100.0));
            v.setTextViewText(R.id.large_news,
                !w.lastImpact.isEmpty()&&w.lastImpactMonth==w.month
                ? "⚡ "+w.lastImpact.split("\\n")[0] :
                (w.lastMonthlyDigest.isEmpty()?
                "🌍 197개 국가의 새 역사가 시작됩니다.":w.lastMonthlyDigest));
            return null;
        });
        PendingIntent open=PendingIntent.getActivity(context,42,new Intent(context,MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.large_root,open);
        return v;
    }
}
