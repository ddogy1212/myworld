package com.myworld.sim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;

/** Interactive illustrative world atlas, not survey-accurate borders. */
public final class WorldMapView extends View {
    private WorldState world;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int ocean = Color.rgb(12,41,68);
    private final int sand = Color.rgb(44,113,102);
    private final int label=Color.rgb(240,246,246);
    private int selected;
    public interface OnCountryClick{void picked(int id);}
    private OnCountryClick listener;

    private static final float[][] LAND = {
        {43,86,122,53,210,72,254,108,277,162,239,202,218,265,178,287,156,323,130,277,107,230,76,209,45,160},
        {205,285,267,297,292,339,343,357,353,401,332,431,329,480,292,490,271,444,251,410,229,363},
        {415,120,460,111,477,150,505,158,519,182,491,211,482,239,461,220,448,182,427,170},
        {485,252,532,255,579,283,604,323,577,372,559,432,536,465,503,426,478,361,473,300},
        {524,104,620,79,726,83,837,110,932,134,968,188,922,250,866,262,837,298,799,285,761,315,724,282,708,244,665,270,627,228,591,224,572,183},
        {797,303,835,332,861,358,847,394,806,361,784,339},
        {850,379,896,377,933,414,928,459,888,482,838,449},
        {75,24,180,16,218,41,157,54,88,50},
        {878,26,960,18,972,48,915,63}
    };
    public WorldMapView(Context context,WorldState state){
        super(context);
        world=state;
        selected=state.selected;
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
    }
    public void setOnCountryClick(OnCountryClick l){listener=l;}
    public void setWorld(WorldState state){world=state;selected=state.selected;invalidate();}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec);
        int wanted=(int)(width*0.56f);
        setMeasuredDimension(width,resolveSize(wanted,heightSpec));
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        int w=getWidth(),h=getHeight();
        canvas.save();
        canvas.scale(w/1000f,h/500f);
        paint.setShader(new LinearGradient(0,0,1000,500,
            new int[]{Color.rgb(10,27,57),ocean,Color.rgb(13,65,81)},null,
            Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,1000,500,paint);
        paint.setShader(null);

        paint.setStrokeWidth(1.0f);
        paint.setColor(Color.argb(36,160,210,232));
        for(int y=80;y<500;y+=80)canvas.drawLine(0,y,1000,y,paint);
        for(int x=100;x<1000;x+=100)canvas.drawLine(x,0,x,500,paint);

        paint.setColor(sand);
        for(float[] poly:LAND){
            Path path=new Path();path.moveTo(poly[0],poly[1]);
            for(int k=2;k<poly.length;k+=2)path.lineTo(poly[k],poly[k+1]);
            path.close();canvas.drawPath(path,paint);
            paint.setColor(Color.argb(110,127,206,181));
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);
            canvas.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);
            paint.setColor(sand);
        }
        if(world!=null){
            for(int a=0;a<world.nations.size();a++){
                for(int b=a+1;b<world.nations.size();b++){
                    WorldState.Nation x=world.nations.get(a),y=world.nations.get(b);
                    boolean conflict=world.conflicts[a][b]>0;
                    boolean partnership=world.pacts[a][b]>0;
                    if(!conflict&&!partnership)continue;
                    paint.setColor(conflict?Color.argb(200,245,101,94):
                        Color.argb(105,99,225,183));
                    paint.setStrokeWidth(conflict?2.4f:1.3f);
                    canvas.drawLine(x.mapX,x.mapY,y.mapX,y.mapY,paint);
                }
            }
            for(WorldState.Nation n:world.nations){
                boolean current=n.id==selected;
                int color=n.stability>=65?Color.rgb(106,234,177):
                    n.stability>=40?Color.rgb(250,203,108):Color.rgb(244,116,108);
                if(current){
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.WHITE);
                    paint.setStrokeWidth(2.4f);
                    canvas.drawCircle(n.mapX,n.mapY,11,paint);
                    paint.setStyle(Paint.Style.FILL);
                }
                paint.setColor(Color.argb(180,0,0,0));
                canvas.drawCircle(n.mapX,n.mapY,current?7:5.3f,paint);
                paint.setColor(color);
                canvas.drawCircle(n.mapX,n.mapY,current?5.6f:4.2f,paint);
            }
            WorldState.Nation n=world.nations.get(selected);
            paint.setColor(Color.rgb(220,244,252));paint.setTextSize(18);
            paint.setFakeBoldText(true);
            canvas.drawText(n.name+"  ·  안정 "+n.stability,24,473,paint);
            paint.setFakeBoldText(false);
        }
        paint.setColor(label);paint.setTextSize(13);
        canvas.drawText("MYWORLD · ALTERNATE EARTH",20,31,paint);
        canvas.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(event.getAction()!=MotionEvent.ACTION_UP){
            if(event.getAction()==MotionEvent.ACTION_DOWN)return true;
            return super.onTouchEvent(event);
        }
        if(world==null)return true;
        float x=event.getX()*1000/getWidth();
        float y=event.getY()*500/getHeight();
        int best=-1;double dist=Double.MAX_VALUE;
        for(WorldState.Nation n:world.nations){
            double d=Math.pow((n.mapX-x),2)+Math.pow((n.mapY-y),2);
            if(d<dist){dist=d;best=n.id;}
        }
        if(best>=0&&dist<1600){
            selected=best;invalidate();if(listener!=null)listener.picked(best);
        }
        return true;
    }
}
