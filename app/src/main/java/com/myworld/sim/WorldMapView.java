package com.myworld.sim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Actual Natural Earth 1:110m outlines, public domain (NOT guessed silhouettes).
 * 165 included countries have polygon geometry at world scale. Tiny countries
 * omitted at this scale use catalog geographic coordinates as zoomed-in pins.
 * Country polygons may show disputed regions following Natural Earth conventions.
 */
public final class WorldMapView extends View {
    public interface OnCountryClick {void picked(int countryId);}
    private static final int OCEAN=Color.rgb(11,29,49);
    private static final int LIGHT=Color.rgb(230,249,252);
    private static final int ACCENT=Color.rgb(105,233,230);
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<Geo> polygons=new ArrayList<>();
    private final HashMap<String,Integer> ids=new HashMap<>();
    private final ScaleGestureDetector zoomDetector;
    private WorldState world;
    private OnCountryClick listener;
    private float zoom=1f,panX=0,panY=0;
    private float downX,downY,lastX,lastY;
    private boolean dragging=false,scaling=false;
    private int selected,metricMode=0;

    private static final class Geo {
        final int id;
        final Path outline;
        final Region hitbox;
        Geo(int id,Path outline,Region hitbox){this.id=id;this.outline=outline;this.hitbox=hitbox;}
    }
    public WorldMapView(Context context,WorldState state){
        super(context);
        world=state;selected=state.selected;
        for(WorldState.Nation n:state.nations)ids.put(n.iso3,n.id);
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        loadNaturalEarth();
        zoomDetector=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScaleBegin(ScaleGestureDetector detector){scaling=true;return true;}
            @Override public boolean onScale(ScaleGestureDetector detector){
                float old=zoom;
                zoom=Math.max(1f,Math.min(12f,zoom*detector.getScaleFactor()));
                if(Math.abs(zoom-old)<0.0001)return true;
                float base=baseScale();
                float focusX=detector.getFocusX(),focusY=detector.getFocusY();
                float mapX=(focusX-centerX()-panX)/(base*old);
                float mapY=(focusY-centerY()-panY)/(base*old);
                panX=focusX-centerX()-mapX*(base*zoom);
                panY=focusY-centerY()-mapY*(base*zoom);
                clampPan();invalidate();return true;
            }
        });
    }
    private void loadNaturalEarth(){
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(
                getContext().getAssets().open("world_borders.txt"),"UTF-8"))){
            String line;
            while((line=reader.readLine())!=null){
                if(line.isEmpty()||line.charAt(0)=='#')continue;
                int delim=line.indexOf('|');
                if(delim<0)continue;
                Integer nation=ids.get(line.substring(0,delim));
                if(nation==null)continue;
                Path path=new Path();
                String[] islands=line.substring(delim+1).split("~");
                for(String polygon:islands){
                    String[] points=polygon.split(";");
                    boolean started=false;
                    int prevX=0,prevY=0,edges=0;
                    for(String point:points){
                        String[] xy=point.split(",");
                        if(xy.length!=2)continue;
                        int x=Integer.parseInt(xy[0]),y=Integer.parseInt(xy[1]);
                        // A discontinuity across the ±180° seam must never
                        // draw a phantom country straight through the world.
                        if(!started || Math.abs(x-prevX)>480){
                            if(edges>2)path.close();
                            path.moveTo(x,y);started=true;edges=1;
                        }else{
                            path.lineTo(x,y);edges++;
                        }
                        prevX=x;prevY=y;
                    }
                    if(edges>2)path.close();
                }
                Region region=new Region();
                region.setPath(path,new Region(0,0,1001,501));
                polygons.add(new Geo(nation,path,region));
            }
        }catch(Exception e){
            android.util.Log.e("MYWORLD","Could not load world border geometry",e);
        }
    }
    public int loadedCountryBorders(){return polygons.size();}
    public void setWorld(WorldState state){world=state;selected=state.selected;invalidate();}
    public void setOnCountryClick(OnCountryClick c){listener=c;}
    public void setMetricMode(int mode){metricMode=Math.max(0,Math.min(2,mode));invalidate();}
    public void resetZoom(){zoom=1;panX=0;panY=0;invalidate();}
    public void focus(int country){
        if(world==null||country<0||country>=world.nations.size())return;
        WorldState.Nation n=world.nations.get(country);
        if(n.mapX<0||n.mapY<0)return;
        selected=country;zoom=3.7f;
        panX=getWidth()/2f-centerX()-n.mapX*baseScale()*zoom;
        panY=getHeight()/2f-centerY()-n.mapY*baseScale()*zoom;
        clampPan();invalidate();
    }
    @Override protected void onMeasure(int wSpec,int hSpec){
        int width=MeasureSpec.getSize(wSpec);
        setMeasuredDimension(width,resolveSize((int)(width*0.65f),hSpec));
    }
    private float baseScale(){return Math.min(getWidth()/1000f,getHeight()/500f);}
    private float centerX(){return (getWidth()-1000*baseScale())/2f;}
    private float centerY(){return (getHeight()-500*baseScale())/2f;}
    private void clampPan(){
        float scale=baseScale()*zoom;
        float actualWidth=1000*scale,actualHeight=500*scale;
        if(actualWidth<=getWidth())panX=(getWidth()-actualWidth)/2-centerX();
        else{
            panX=Math.max(getWidth()-centerX()-actualWidth,Math.min(-centerX(),panX));
        }
        if(actualHeight<=getHeight())panY=(getHeight()-actualHeight)/2-centerY();
        else{
            panY=Math.max(getHeight()-centerY()-actualHeight,Math.min(-centerY(),panY));
        }
    }
    private int color(int score){
        float t=Math.max(0,Math.min(1,score/100f));
        int r=(int)(172-t*104),g=(int)(81+t*104),b=(int)(89+t*61);
        return Color.rgb(r,g,b);
    }
    private int metric(WorldState.Nation n){
        if(metricMode==1)return n.economy;
        if(metricMode==2)return n.environment;
        return n.stability;
    }
    private float countryRadius(){return 2.5f+2f/zoom;}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        paint.setShader(new LinearGradient(0,0,getWidth(),getHeight(),
            Color.rgb(9,22,43),Color.rgb(16,48,65),Shader.TileMode.CLAMP));
        canvas.drawColor(OCEAN);
        canvas.drawRect(0,0,getWidth(),getHeight(),paint);
        paint.setShader(null);

        if(world==null)return;
        canvas.save();
        float sc=baseScale()*zoom;
        canvas.translate(centerX()+panX,centerY()+panY);
        canvas.scale(sc,sc);
        paint.setStrokeWidth(1.0f/Math.max(.8f,zoom));
        paint.setColor(Color.argb(35,115,189,195));
        for(int xx=0;xx<=1000;xx+=100)canvas.drawLine(xx,0,xx,500,paint);
        for(int yy=0;yy<=500;yy+=100)canvas.drawLine(0,yy,1000,yy,paint);

        // Fill each country separately: real country shape and political borders.
        for(Geo geo:polygons){
            WorldState.Nation n=world.nations.get(geo.id);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(geo.id==selected?Color.rgb(39,162,178):color(metric(n)));
            canvas.drawPath(geo.outline,paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(geo.id==selected?2.3f/zoom:0.6f/zoom);
            paint.setColor(geo.id==selected?Color.rgb(212,252,255):Color.argb(190,13,41,48));
            canvas.drawPath(geo.outline,paint);
            paint.setStyle(Paint.Style.FILL);
        }
        if(zoom>=2.2f){
            for(WorldState.Nation n:world.nations){
                boolean polygonExists=false;
                for(Geo g:polygons){if(g.id==n.id){polygonExists=true;break;}}
                if(polygonExists||n.mapX<0||n.mapY<0)continue;
                paint.setColor(Color.rgb(13,43,61));
                canvas.drawCircle(n.mapX,n.mapY,8f/zoom,paint);
                paint.setColor(Color.rgb(228,224,131));
                canvas.drawCircle(n.mapX,n.mapY,5.0f/zoom,paint);
            }
        }
        // Highlight selected state's real location for tiny countries or
        // countries currently outside a visible-scale polygon.
        WorldState.Nation chosen=world.nations.get(selected);
        if(chosen.mapX>=0&&chosen.mapY>=0){
            paint.setColor(Color.rgb(246,233,135));
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2f/zoom);
            canvas.drawCircle(chosen.mapX,chosen.mapY,10f/zoom,paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(240,252,255));
            canvas.drawCircle(chosen.mapX,chosen.mapY,3f/zoom,paint);
        }
        canvas.restore();

        // UI chrome stays readable while the user zooms.
        paint.setColor(Color.argb(225,10,29,49));
        canvas.drawRoundRect(10,9,getWidth()-10,39,10,10,paint);
        paint.setColor(LIGHT);paint.setTextSize(13f*getResources().getDisplayMetrics().scaledDensity);
        paint.setFakeBoldText(true);
        String field=metricMode==0?"사회 안정":metricMode==1?"경제 성장":"환경 품질";
        canvas.drawText("🌐  실제 국가 경계  |  "+field,20,29,paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(10f*getResources().getDisplayMetrics().scaledDensity);
        paint.setColor(Color.rgb(210,230,239));
        canvas.drawText("두 손가락 확대 · 드래그 이동 · 국가 터치",13,getHeight()-11,paint);
    }
    private void clickAt(float x,float y){
        float sc=baseScale()*zoom;
        float mx=(x-centerX()-panX)/sc,my=(y-centerY()-panY)/sc;
        int ix=Math.round(mx),iy=Math.round(my);
        for(int i=polygons.size()-1;i>=0;i--){
            Geo g=polygons.get(i);
            if(g.hitbox.contains(ix,iy)){
                selected=g.id;invalidate();
                if(listener!=null)listener.picked(g.id);
                return;
            }
        }
        int nearest=-1;double best=Double.POSITIVE_INFINITY;
        for(WorldState.Nation n:world.nations){
            if(n.mapX<0||n.mapY<0)continue;
            double dist=Math.hypot(n.mapX-mx,n.mapY-my)*sc;
            if(dist<best){best=dist;nearest=n.id;}
        }
        if(best<(zoom>=2.2f?20f:9f) && nearest>=0){
            selected=nearest;invalidate();if(listener!=null)listener.picked(nearest);
        }
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        zoomDetector.onTouchEvent(e);
        if(e.getPointerCount()>1){
            scaling=true;
            getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        }
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            downX=lastX=e.getX();downY=lastY=e.getY();
            dragging=false;scaling=false;
            getParent().requestDisallowInterceptTouchEvent(true);return true;
        }
        if(action==MotionEvent.ACTION_MOVE){
            float x=e.getX(),y=e.getY();
            if(Math.hypot(x-downX,y-downY)>9f)dragging=true;
            if(dragging&&!scaling){
                panX+=x-lastX;panY+=y-lastY;clampPan();invalidate();
            }
            lastX=x;lastY=y;return true;
        }
        if(action==MotionEvent.ACTION_UP){
            if(!dragging&&!scaling)clickAt(e.getX(),e.getY());
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
