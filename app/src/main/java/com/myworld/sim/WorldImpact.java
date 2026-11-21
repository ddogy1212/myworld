package com.myworld.sim;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * Displays BEFORE/AFTER values of the actual committed simulation mutation,
 * not optimistic estimates. Failed/invalid actions never create an impact.
 */
public final class WorldImpact {
    private WorldImpact(){}
    public interface Action {String run();}
    public static final class Delta {
        public String title;
        public double before,after;
        public boolean higherIsGood;
        public Delta(String title,double before,double after,boolean good){
            this.title=title;this.before=before;this.after=after;this.higherIsGood=good;
        }
        public double change(){return after-before;}
        public String format(double number) {
            if(title.contains("재정"))return String.format(Locale.KOREA,"%.1f",number);
            return String.format(Locale.KOREA,"%.0f",number);
        }
        public String changeLabel(){
            return (change()>0?"+":"")+format(change());
        }
    }
    public static final class Result {
        public String message;
        public boolean success;
        public int country;
        public final ArrayList<Delta> deltas=new ArrayList<>();
    }
    private static LinkedHashMap<String,Double> capture(WorldState w,int nation,int other) {
        LinkedHashMap<String,Double> m=new LinkedHashMap<>();
        m.put("세계 평화", (double)w.peace);
        m.put("기후 압력", (double)w.carbon);
        m.put("평균 환경", (double)w.avg("환경"));
        m.put("보유 영향력", (double)w.influence);
        if(nation>=0 && nation<w.nations.size()){
            WorldState.Nation n=w.nations.get(nation);
            m.put("경제", (double)n.economy);
            m.put("안정", (double)n.stability);
            m.put("교육", (double)n.education);
            m.put("의료", (double)n.health);
            m.put("과학", (double)n.science);
            m.put("환경", (double)n.environment);
            m.put("에너지", (double)n.energy);
            m.put("식량", (double)n.food);
            m.put("산업", (double)n.industry);
            m.put("물류", (double)n.transport);
            m.put("복지", (double)n.welfare);
            m.put("외교력", (double)n.diplomacy);
            m.put("시민 만족", (double)n.satisfaction());
            m.put("재정", n.treasury);
            if(other>=0&&other<w.nations.size()&&other!=nation){
                m.put("양국 신뢰", (double)w.relations[nation][other]);
                m.put("양국 무역", (double)w.trade[nation][other]);
                m.put("외교 긴장", (double)w.tensions[nation][other]);
                m.put("협정", (double)w.pacts[nation][other]);
                m.put("분쟁", (double)w.conflicts[nation][other]);
                WorldState.Nation b=w.nations.get(other);
                m.put("상대 환경", (double)b.environment);
                m.put("상대 교육", (double)b.education);
                m.put("상대 경제", (double)b.economy);
            }
        }
        return m;
    }
    public static Result run(WorldState w,int nation,int other,Action action){
        Result r=new Result();r.country=nation;
        LinkedHashMap<String,Double> prior=capture(w,nation,other);
        int p=w.playerPolicies,d=w.playerDiplomacy;
        String result=action.run();
        r.message=result;
        r.success=w.playerPolicies>p || w.playerDiplomacy>d;
        if(!r.success)return r;
        LinkedHashMap<String,Double> latest=capture(w,nation,other);
        for(String key:prior.keySet()){
            double a=prior.get(key),b=latest.get(key);
            if(Math.abs(a-b)<.005)continue;
            r.deltas.add(new Delta(key,a,b,!key.equals("기후 압력")
                &&!key.equals("외교 긴장")&&!key.equals("분쟁")));
        }
        r.deltas.sort((a,b)->{
            // Effects first, spending of influence is listed separately.
            boolean ap=a.title.equals("보유 영향력"),bp=b.title.equals("보유 영향력");
            if(ap!=bp)return ap?1:-1;
            return Double.compare(Math.abs(b.change()),Math.abs(a.change()));
        });
        StringBuilder history=new StringBuilder(result);
        int limit=0;
        for(Delta delta:r.deltas){
            if(delta.title.equals("보유 영향력"))continue;
            history.append("\n").append(delta.title).append(" ")
                .append(delta.format(delta.before)).append(" → ")
                .append(delta.format(delta.after)).append(" (")
                .append(delta.changeLabel()).append(")");
            if(++limit>=4)break;
        }
        w.lastImpact=history.toString();w.lastImpactMonth=w.month;
        return r;
    }
}
