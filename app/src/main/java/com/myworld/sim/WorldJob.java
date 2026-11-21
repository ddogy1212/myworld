package com.myworld.sim;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Opportunistic background advancement. Android may defer periodic jobs.
 * WorldRepository compensates by processing elapsed real-time months on open.
 */
public final class WorldJob extends JobService {
    private static final int JOB_ID=204001;
    public static void schedule(Context context){
        try{
            JobScheduler scheduler=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if(scheduler==null)return;
            JobInfo info=new JobInfo.Builder(JOB_ID,new ComponentName(context,WorldJob.class))
                .setPersisted(true)
                .setPeriodic(15*60*1000L)
                .build();
            scheduler.schedule(info);
        }catch(RuntimeException e){Log.w("MYWORLD","Could not schedule background job",e);}
    }
    @Override public boolean onStartJob(JobParameters params){
        new Thread(()->{
            try{
                WorldRepository.advance(getApplicationContext());
                WorldWidget.updateAll(getApplicationContext());
            }catch(Exception e){Log.e("MYWORLD","Background advance failed",e);}
            new Handler(Looper.getMainLooper()).post(()->jobFinished(params,false));
        },"MYWORLD-monthly").start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters params){return true;}
}
