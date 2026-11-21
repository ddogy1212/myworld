package com.myworld.sim;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.LruCache;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Esri World Imagery real satellite/aerial imagery overlaid with Natural Earth
 * state borders. Satellite tiles are fetched ONLY for visible viewport cells.
 * Ephemeral in-memory cache, max 24 MiB; no bulk download, no disk storage.
 *
 * Imagery is NOT live; capture dates vary by place. Provider attribution is
 * always drawn on-screen. Offline/error => exact-data vector border fallback.
 *
 * This renderer uses Web Mercator and makes country polygons selectable even
 * when imagery is on. The game can be played entirely offline.
 */
public final class WorldSatelliteView extends View {
    public interface OnCountryClick {void picked(int id);}
    private static final double PI=Math.PI;
    private static final int TILE=256;
    private static final int SEA=Color.rgb(8,24,43);
    private static final int CYAN=Color.rgb(103,233,228);
    private static final String ESRI_TILE =
        "https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/";
    private static final String PROVIDERS="Powered by Esri · Esri, Maxar, Earthstar Geographics, GIS Community";
    private static final LruCache<String,Bitmap> tiles=new LruCache<String,Bitmap>(24*1024*1024){
        @Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getByteCount();}
    };
    private static final ThreadPoolExecutor pool=new ThreadPoolExecutor(
        2,2,30L,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(55));
    private static final HashSet<String> loading=new HashSet<>();
    private static final HashMap<String,Long> errors=new HashMap<>();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final ScaleGestureDetector gesture;
    private final ArrayList<Poly> borders=new ArrayList<>();
    private final HashSet<Integer> outlined=new HashSet<>();
    private final HashMap<String,Integer> isoToId=new HashMap<>();
    private WorldState world;
    private OnCountryClick listener;
    private int chosen,metricMode=0;
    private boolean satellite=true,dragging=false,scaling=false;
    private boolean everLoadedTile=false;
    private double centerLon=20,centerLat=15,zoom=1.2;
    private float lastX,lastY,startX,startY;
    private static final class Poly {
        int nation;
        ArrayList<float[]> rings=new ArrayList<>();
    }
    public WorldSatelliteView(Context context,WorldState data){
        super(context);
        world=data;chosen=data.selected;
        for(WorldState.Nation n:data.nations)isoToId.put(n.iso3,n.id);
        readBorders();
        gesture=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScaleBegin(ScaleGestureDetector detector){scaling=true;return true;}
            @Override public boolean onScale(ScaleGestureDetector detector){
                float x=detector.getFocusX(),y=detector.getFocusY();
                double[] before=screenToNorm(x,y);
                zoom=Math.max(.55,Math.min(17.5,zoom+Math.log(detector.getScaleFactor())/Math.log(2)));
                double full=worldPixels();
                double xNorm=before[0]-(x-getWidth()/2.0)/full;
                double yNorm=before[1]-(y-getHeight()/2.0)/full;
                setCenterNorm(xNorm,yNorm);
                invalidate();return true;
            }
        });
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
    }
    public void setWorld(WorldState data){world=data;chosen=data.selected;invalidate();}
    public void setOnCountryClick(OnCountryClick l){listener=l;}
    public void setMetricMode(int mode){metricMode=Math.max(0,Math.min(2,mode));invalidate();}
    public void setSatelliteEnabled(boolean enabled){satellite=enabled;invalidate();}
    public boolean isSatelliteEnabled(){return satellite;}
    public void resetZoom(){zoom=1.1;centerLon=20;centerLat=15;invalidate();}
    public void focus(int id){
        if(world==null||id<0||id>=world.nations.size())return;
        WorldState.Nation n=world.nations.get(id);
        if(n.mapX<0||n.mapY<0)return;
        chosen=id;
        centerLon=n.mapX*360.0/1000.0-180.0;
        centerLat=90.0-n.mapY*180.0/500.0;
        if(centerLat>85)centerLat=85;if(centerLat< -85)centerLat=-85;
        zoom=outlined.contains(id)?5.8:10.5;
        invalidate();
    }
    private static double mercY(double lat){
        double r=Math.toRadians(Math.max(-85.05112878,Math.min(85.05112878,lat)));
        return (1-Math.log(Math.tan(r)+1/Math.cos(r))/PI)/2;
    }
    private static double fromMercY(double y){
        return Math.toDegrees(Math.atan(Math.sinh(PI*(1-2*y))));
    }
    private double fullX(){return (centerLon+180)/360.0;}
    private double fullY(){return mercY(centerLat);}
    private double worldPixels(){return TILE*Math.pow(2.0,zoom);}
    private static double wrap(double n){return n-Math.floor(n);}
    private void setCenterNorm(double x,double y){
        centerLon=wrap(x)*360.0-180.0;
        centerLat=fromMercY(Math.max(.00001,Math.min(.99999,y)));
    }
    private double[] screenToNorm(float x,float y){
        double full=worldPixels();
        return new double[]{wrap(fullX()+(x-getWidth()/2.0)/full),
            Math.max(0,Math.min(1,fullY()+(y-getHeight()/2.0)/full))};
    }
    private float screenX(double x){
        double delta=x-fullX();
        if(delta >.5)delta-=1;
        if(delta<-.5)delta+=1;
        return (float)(getWidth()/2.0+delta*worldPixels());
    }
    private float screenY(double y){
        return (float)(getHeight()/2.0+(y-fullY())*worldPixels());
    }
    private void readBorders(){
        try(BufferedReader input=new BufferedReader(new InputStreamReader(
            getContext().getAssets().open("world_borders.txt"),"UTF-8"))){
            String row;
            while((row=input.readLine())!=null){
                if(row.startsWith("#")||row.isEmpty())continue;
                int separator=row.indexOf('|');if(separator<0)continue;
                Integer id=isoToId.get(row.substring(0,separator));if(id==null)continue;
                Poly p=new Poly();p.nation=id;
                for(String island:row.substring(separator+1).split("~")){
                    String[] points=island.split(";");
                    float[] xy=new float[points.length*2];
                    int count=0;
                    for(String point:points){
                        String[] coordinates=point.split(",");
                        if(coordinates.length!=2)continue;
                        int x=Integer.parseInt(coordinates[0]),y=Integer.parseInt(coordinates[1]);
                        double lat=90.0-y*180.0/500.0;
                        xy[count++]=(float)(x/1000.0);
                        xy[count++]=(float)mercY(lat);
                    }
                    if(count>=6){
                        float[] trimmed=new float[count];
                        System.arraycopy(xy,0,trimmed,0,count);
                        p.rings.add(trimmed);
                    }
                }
                if(!p.rings.isEmpty()){borders.add(p);outlined.add(id);}
            }
        }catch(Exception e){android.util.Log.e("MYWORLD","Satellite border asset failed",e);}
    }
    private int metric(WorldState.Nation n){
        return metricMode==1?n.economy:metricMode==2?n.environment:n.stability;
    }
    private int categoryColor(int n){
        double t=Math.min(1,Math.max(0,n/100.0));
        return Color.rgb((int)(220-t*110),(int)(66+t*165),(int)(68+t*120));
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(width,resolveSize((int)(width*.83f),heightSpec));
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        canvas.drawColor(SEA);
        paint.setShader(new LinearGradient(0,0,getWidth(),getHeight(),
            Color.rgb(10,30,51),Color.rgb(17,59,78),Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,getWidth(),getHeight(),paint);
        paint.setShader(null);
        if(satellite)renderSatelliteTiles(canvas);
        drawBorders(canvas);
        drawStatus(canvas);
    }
    private void renderSatelliteTiles(Canvas canvas){
        // HTTP tiles have a Web-Mercator tile scheme (z, row/y, column/x).
        int z=(int)Math.floor(zoom);
        int n=1<<z;
        double nativeSize=TILE*Math.pow(2,zoom-z);
        double full=worldPixels();
        double xCenter=fullX()*full,yCenter=fullY()*full;
        int fromX=(int)Math.floor((xCenter-getWidth()/2.0)/nativeSize)-1;
        int toX=(int)Math.floor((xCenter+getWidth()/2.0)/nativeSize)+1;
        int fromY=Math.max(0,(int)Math.floor((yCenter-getHeight()/2.0)/nativeSize)-1);
        int toY=Math.min(n-1,(int)Math.floor((yCenter+getHeight()/2.0)/nativeSize)+1);
        int requested=0;
        for(int iy=fromY;iy<=toY;iy++){
            for(int ix=fromX;ix<=toX;ix++){
                int column=((ix%n)+n)%n;
                String key=z+"/"+iy+"/"+column;
                float sx=(float)(getWidth()/2.0+ix*nativeSize-xCenter);
                float sy=(float)(getHeight()/2.0+iy*nativeSize-yCenter);
                Bitmap bitmap;
                synchronized(tiles){bitmap=tiles.get(key);}
                if(bitmap!=null&&!bitmap.isRecycled()){
                    paint.setAlpha(255);
                    canvas.drawBitmap(bitmap,null,
                        new android.graphics.RectF(sx,sy,(float)(sx+nativeSize+1),(float)(sy+nativeSize+1)),paint);
                    everLoadedTile=true;
                }else if(requested<35){
                    scheduleTile(key,z,iy,column);requested++;
                }
            }
        }
    }
    private void scheduleTile(String key,int level,int y,int x){
        synchronized(loading){
            Long failedAt=errors.get(key);
            if(failedAt!=null && SystemClock.elapsedRealtime()-failedAt<90000)return;
            synchronized(tiles){if(tiles.get(key)!=null)return;}
            if(loading.contains(key)||loading.size()>42||pool.getQueue().remainingCapacity()<1)return;
            loading.add(key);
        }
        WeakReference<WorldSatelliteView> reference=new WeakReference<>(this);
        try{
            pool.execute(()->{
                HttpURLConnection conn=null;
                Bitmap bitmap=null;
                try{
                    URL url=new URL(ESRI_TILE+level+"/"+y+"/"+x);
                    conn=(HttpURLConnection)url.openConnection();
                    conn.setConnectTimeout(6500);conn.setReadTimeout(8500);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent","MYWORLD Android satellite map");
                    conn.setRequestProperty("Accept","image/jpeg,image/png,image/*");
                    if(conn.getResponseCode()==200){
                        String type=conn.getContentType();
                        if(type!=null && type.toLowerCase().startsWith("image/")){
                            try(InputStream in=conn.getInputStream()){
                                bitmap=BitmapFactory.decodeStream(in);
                            }
                        }
                    }
                }catch(Exception ignored){}
                finally{if(conn!=null)conn.disconnect();}
                synchronized(loading){
                    loading.remove(key);
                    if(bitmap==null)errors.put(key,SystemClock.elapsedRealtime());
                    else errors.remove(key);
                    if(errors.size()>220)errors.clear();
                }
                if(bitmap!=null){
                    synchronized(tiles){tiles.put(key,bitmap);}
                }
                WorldSatelliteView v=reference.get();
                if(v!=null)v.postInvalidateOnAnimation();
            });
        }catch(Exception e){
            synchronized(loading){loading.remove(key);}
        }
    }
    private Path screenPath(Poly polygon){
        Path path=new Path();
        for(float[] ring:polygon.rings){
            boolean active=false;
            float oldX=0;
            int points=0;
            for(int i=0;i+1<ring.length;i+=2){
                float x=screenX(ring[i]),y=screenY(ring[i+1]);
                if(!active||Math.abs(x-oldX)>worldPixels()*.47){
                    if(points>=3)path.close();
                    path.moveTo(x,y);
                    active=true;points=1;
                }else{
                    path.lineTo(x,y);points++;
                }
                oldX=x;
            }
            if(points>=3)path.close();
        }
        return path;
    }
    private void drawBorders(Canvas canvas){
        if(world==null)return;
        for(Poly poly:borders){
            WorldState.Nation n=world.nations.get(poly.nation);
            Path p=screenPath(poly);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(categoryColor(metric(n)));
            paint.setAlpha(satellite?(poly.nation==chosen?110:30):230);
            canvas.drawPath(p,paint);
            paint.setAlpha(255);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(poly.nation==chosen?3.0f:0.85f);
            paint.setColor(poly.nation==chosen?CYAN:Color.argb(170,237,249,250));
            canvas.drawPath(p,paint);
            paint.setStyle(Paint.Style.FILL);
        }
        // Small independent states are not part of the 1:110m geometry.
        // Mark them only at sufficiently high zoom to avoid clutter.
        if(zoom>=5.0){
            for(WorldState.Nation n:world.nations){
                if(outlined.contains(n.id)||n.mapX<0||n.mapY<0)continue;
                double lng=n.mapX*360.0/1000.0-180.0;
                double lat=90.0-n.mapY*180.0/500.0;
                float sx=screenX((lng+180)/360),sy=screenY(mercY(lat));
                if(sx< -10||sy< -10||sx>getWidth()+10||sy>getHeight()+10)continue;
                paint.setColor(Color.BLACK);canvas.drawCircle(sx,sy,6,paint);
                paint.setColor(Color.rgb(244,218,117));canvas.drawCircle(sx,sy,4,paint);
            }
        }
        WorldState.Nation selected=world.nations.get(chosen);
        if(selected.mapX>=0&&selected.mapY>=0){
            double lng=selected.mapX*360/1000.0-180,lat=90-selected.mapY*180/500.0;
            float x=screenX((lng+180)/360),y=screenY(mercY(lat));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.5f);paint.setColor(Color.rgb(255,225,111));
            canvas.drawCircle(x,y,11,paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }
    private void drawStatus(Canvas canvas){
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(225,3,22,39));
        canvas.drawRoundRect(9,8,getWidth()-9,54,10,10,paint);
        paint.setColor(Color.WHITE);paint.setTextSize(14f*getResources().getDisplayMetrics().scaledDensity);
        paint.setFakeBoldText(true);
        canvas.drawText(satellite?"🛰 위성사진 · 국가별 실제 지형":"🌐 오프라인 국경 지도",19,27,paint);
        paint.setFakeBoldText(false);paint.setTextSize(10.5f*getResources().getDisplayMetrics().scaledDensity);
        String field=metricMode==1?"경제":metricMode==2?"환경":"사회 안정";
        canvas.drawText("지도: "+field+" · 확대 "+String.format(java.util.Locale.KOREA,"%.1f",zoom)+
            " · 실제 사진은 촬영 시점 상이",19,46,paint);
        if(satellite&&!everLoadedTile){
            paint.setColor(Color.argb(190,5,27,45));
            canvas.drawRoundRect(12,getHeight()/2f-35,getWidth()-12,getHeight()/2f+29,10,10,paint);
            paint.setColor(Color.rgb(221,236,246));paint.setTextSize(12*getResources().getDisplayMetrics().scaledDensity);
            canvas.drawText("위성사진 불러오는 중 · 인터넷 필요",22,getHeight()/2f-3,paint);
            paint.setTextSize(10*getResources().getDisplayMetrics().scaledDensity);
            canvas.drawText("연결이 안 되면 국경 지도 모드로 전환",22,getHeight()/2f+17,paint);
        }
        paint.setColor(Color.argb(225,3,22,39));
        canvas.drawRect(0,getHeight()-40,getWidth(),getHeight(),paint);
        paint.setColor(Color.rgb(238,240,238));paint.setTextSize(9*getResources().getDisplayMetrics().scaledDensity);
        canvas.drawText("국가 터치 · 이동 · 두 손가락 확대",10,getHeight()-26,paint);
        if(satellite){
            paint.setTextSize(7.8f*getResources().getDisplayMetrics().scaledDensity);
            canvas.drawText(PROVIDERS,10,getHeight()-9,paint);
        }else{
            canvas.drawText("Borders: Natural Earth public domain",10,getHeight()-9,paint);
        }
    }
    private int atPoint(float screenX,float screenY){
        int xx=(int)screenX,yy=(int)screenY;
        Region clip=new Region(0,0,getWidth(),getHeight());
        for(int i=borders.size()-1;i>=0;i--){
            Poly p=borders.get(i);
            Path path=screenPath(p);
            Region region=new Region();
            region.setPath(path,clip);
            if(region.contains(xx,yy))return p.nation;
        }
        int nearest=-1;double distance=Double.MAX_VALUE;
        for(WorldState.Nation n:world.nations){
            if(n.mapX<0||n.mapY<0)continue;
            if(outlined.contains(n.id)&&zoom<8)continue;
            double lng=n.mapX*360.0/1000-180,lat=90.0-n.mapY*180/500;
            double d=Math.hypot(this.screenX((lng+180)/360)-screenX,
                this.screenY(mercY(lat))-screenY);
            if(d<distance){distance=d;nearest=n.id;}
        }
        return distance<(zoom>=5?27:10)?nearest:-1;
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        gesture.onTouchEvent(event);
        int action=event.getActionMasked();
        if(event.getPointerCount()>1){
            scaling=true;getParent().requestDisallowInterceptTouchEvent(true);return true;
        }
        if(action==MotionEvent.ACTION_DOWN){
            startX=lastX=event.getX();startY=lastY=event.getY();
            scaling=false;dragging=false;
            getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        }
        if(action==MotionEvent.ACTION_MOVE){
            float x=event.getX(),y=event.getY();
            if(Math.hypot(x-startX,y-startY)>10)dragging=true;
            if(dragging&&!scaling){
                double full=worldPixels();
                setCenterNorm(fullX()-(x-lastX)/full,fullY()-(y-lastY)/full);
                invalidate();
            }
            lastX=x;lastY=y;return true;
        }
        if(action==MotionEvent.ACTION_UP){
            if(!dragging&&!scaling){
                int id=atPoint(event.getX(),event.getY());
                if(id>=0){
                    chosen=id;invalidate();
                    if(listener!=null)listener.picked(id);
                }
            }
            getParent().requestDisallowInterceptTouchEvent(false);
            performClick();return true;
        }
        if(action==MotionEvent.ACTION_CANCEL){
            getParent().requestDisallowInterceptTouchEvent(false);
            return true;
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
