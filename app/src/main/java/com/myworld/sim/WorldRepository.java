package com.myworld.sim;

import android.content.Context;
import android.util.AtomicFile;
import android.util.Log;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** Atomic private local save; single process synchronized access. */
public final class WorldRepository {
    private static final String NAME="world_v1.json";
    private static WorldState cached;
    public interface Action<T>{T execute(WorldState world);}
    private WorldRepository(){}

    private static synchronized WorldState load(Context context){
        if(cached!=null)return cached;
        AtomicFile file=new AtomicFile(new File(context.getApplicationContext().getFilesDir(),NAME));
        try{
            byte[] bytes=file.readFully();
            cached=WorldState.from(new JSONObject(new String(bytes,StandardCharsets.UTF_8)));
        }catch(Exception error){
            Log.w("MYWORLD","No valid save. Initializing new world: "+error.getMessage());
            cached=WorldState.fresh(System.currentTimeMillis());
            save(context,cached);
        }
        return cached;
    }
    private static void save(Context context,WorldState world){
        AtomicFile file=new AtomicFile(new File(context.getApplicationContext().getFilesDir(),NAME));
        FileOutputStream stream=null;
        try{
            byte[] data=world.json().toString().getBytes(StandardCharsets.UTF_8);
            stream=file.startWrite();
            stream.write(data);
            file.finishWrite(stream);
        }catch(Exception e){
            if(stream!=null)file.failWrite(stream);
            Log.e("MYWORLD","World save failed",e);
        }
    }
    public static synchronized <T> T read(Context context,Action<T> action){
        WorldState w=load(context);
        int progressed=WorldEngine.catchUp(w,System.currentTimeMillis());
        if(progressed>0)save(context,w);
        return action.execute(w);
    }
    public static synchronized <T> T change(Context context,Action<T> action){
        WorldState w=load(context);
        WorldEngine.catchUp(w,System.currentTimeMillis());
        T result=action.execute(w);
        save(context,w);
        return result;
    }
    public static synchronized int advance(Context context){
        WorldState w=load(context);
        int n=WorldEngine.catchUp(w,System.currentTimeMillis());
        if(n>0)save(context,w);
        return n;
    }
    public static synchronized void reset(Context context){
        cached=WorldState.fresh(System.currentTimeMillis());
        save(context,cached);
    }
}
