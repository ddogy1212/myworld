package com.myworld.sim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Twelve genuine simulated-month observations. Not generated chart decoration. */
public final class WorldTrendView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private WorldState state;
    public WorldTrendView(Context context,WorldState world){super(context);state=world;}
    @Override protected void onMeasure(int w,int h){
        int width=MeasureSpec.getSize(w);
        setMeasuredDimension(width,resolveSize((int)(width*.48),h));
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        final float d=getResources().getDisplayMetrics().density;
        float left=36*d,top=20*d,right=getWidth()-16*d,bottom=getHeight()-35*d;
        canvas.drawColor(Color.rgb(19,34,54));
        p.setTextSize(10*d);
        p.setColor(Color.rgb(158,184,202));
        for(int value=0;value<=100;value+=25){
            float y=bottom-(bottom-top)*value/100;
            p.setStrokeWidth(d);
            p.setColor(Color.argb(55,177,196,211));
            canvas.drawLine(left,y,right,y,p);
            p.setColor(Color.rgb(165,188,202));
            canvas.drawText(String.valueOf(value),4*d,y+4*d,p);
        }
        plot(canvas,state.economyTrend,Color.rgb(244,203,118),left,top,right,bottom,d);
        plot(canvas,state.peaceTrend,Color.rgb(117,223,172),left,top,right,bottom,d);
        plot(canvas,state.climateTrend,Color.rgb(112,207,243),left,top,right,bottom,d);
        p.setTextSize(11*d);
        p.setColor(Color.rgb(232,243,252));
        canvas.drawText("지난 12개월",left,bottom+23*d,p);
        canvas.drawText("경제",right-115*d,bottom+23*d,p);
        p.setColor(Color.rgb(244,203,118));canvas.drawCircle(right-123*d,bottom+18*d,4*d,p);
        p.setColor(Color.rgb(232,243,252));canvas.drawText("평화",right-69*d,bottom+23*d,p);
        p.setColor(Color.rgb(117,223,172));canvas.drawCircle(right-77*d,bottom+18*d,4*d,p);
        p.setColor(Color.rgb(232,243,252));canvas.drawText("환경",right-20*d,bottom+23*d,p);
        p.setColor(Color.rgb(112,207,243));canvas.drawCircle(right-27*d,bottom+18*d,4*d,p);
    }
    private void plot(Canvas canvas,int[] values,int color,float l,float top,float right,float bottom,float d){
        Path path=new Path();
        for(int i=0;i<values.length;i++){
            float x=l+(right-l)*i/(values.length-1f);
            float y=bottom-(bottom-top)*Math.max(0,Math.min(100,values[i]))/100f;
            if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
        }
        p.setColor(color);p.setStrokeWidth(2.4f*d);p.setStyle(Paint.Style.STROKE);
        canvas.drawPath(path,p);p.setStyle(Paint.Style.FILL);
        float lastX=right,lastY=bottom-(bottom-top)*Math.max(0,Math.min(100,values[values.length-1]))/100f;
        canvas.drawCircle(lastX,lastY,3.7f*d,p);
    }
}
