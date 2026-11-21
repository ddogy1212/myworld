package com.myworld.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline Korean / English natural-language rule interpreter. It does not use
 * generative AI and will never execute a command without an unambiguous
 * matching domain and an explicit confirmation in MainActivity.
 */
public final class WorldCommand {
    private WorldCommand(){}
    public static final class Plan {
        public static final int INVALID=0,POLICY=1,DIPLOMACY=2,FISCAL=3,CLIMATE=4;
        public int type=INVALID;
        public int country=-1,other=-1,policy=-1,intensity=1,months=1;
        public boolean higherTax;
        public String explanation="";
        public String error="";
        public boolean valid(){return type!=INVALID;}
        public String execute(WorldState w){
            switch(type){
                case POLICY:return WorldEngine.playerPolicyText(w,country,policy,intensity,months);
                case DIPLOMACY:return WorldEngine.playerDiplomacy(w,country,other,policy);
                case FISCAL:return WorldEngine.playerFiscal(w,country,higherTax,intensity);
                case CLIMATE:return WorldEngine.playerClimateSummit(w);
                default:return error.isEmpty()?"해석할 수 없는 명령입니다.":error;
            }
        }
    }
    private static final class Mention {
        int index,start,length;
        Mention(int i,int s,int len){index=i;start=s;length=len;}
    }
    private static String compact(String text){
        return text.toLowerCase(Locale.ROOT).replaceAll("[\\s·,\\.()\\[\\]{}]","");
    }
    private static boolean has(String text,String...words){
        for(String s:words)if(text.contains(compact(s)))return true;
        return false;
    }
    private static ArrayList<Integer> countries(WorldState w,String original){
        String text=compact(original);
        ArrayList<Mention> hits=new ArrayList<>();
        for(int i=0;i<w.nations.size();i++){
            WorldState.Nation n=w.nations.get(i);
            String[] data=NationCatalog.ROWS[i].split("\\|",-1);
            ArrayList<String> aliases=new ArrayList<>();
            aliases.add(n.name);aliases.add(n.iso3);aliases.add(data[2]);
            if("KOR".equals(n.iso3)){aliases.add("한국");aliases.add("남한");}
            if("PRK".equals(n.iso3)){aliases.add("북한");}
            if("USA".equals(n.iso3)){aliases.add("미국");}
            if("GBR".equals(n.iso3)){aliases.add("영국");}
            if("CHN".equals(n.iso3)){aliases.add("중국");}
            if("JPN".equals(n.iso3)){aliases.add("일본");}
            if("RUS".equals(n.iso3)){aliases.add("러시아");}
            if("TUR".equals(n.iso3)){aliases.add("터키");}
            if("ZAF".equals(n.iso3)){aliases.add("남아공");}
            if("XKX".equals(n.iso3)){aliases.add("코소보");}
            for(String alias:aliases){
                String a=compact(alias);
                if(a.length()<2)continue;
                int at=text.indexOf(a);
                if(at>=0)hits.add(new Mention(i,at,a.length()));
            }
        }
        Collections.sort(hits,(a,b)->{
            if(a.length!=b.length)return b.length-a.length;
            return a.start-b.start;
        });
        ArrayList<Mention> chosen=new ArrayList<>();
        for(Mention candidate:hits){
            boolean overlap=false;
            for(Mention seen:chosen){
                if(seen.index==candidate.index||
                        (candidate.start<seen.start+seen.length&&seen.start<candidate.start+candidate.length)){
                    overlap=true;break;
                }
            }
            if(!overlap)chosen.add(candidate);
            if(chosen.size()>=2)break;
        }
        chosen.sort(Comparator.comparingInt(a->a.start));
        ArrayList<Integer> ids=new ArrayList<>();
        for(Mention m:chosen)ids.add(m.index);
        return ids;
    }
    private static int numberIn(String text,String suffix){
        Matcher m=Pattern.compile("(\\d{1,3})\\s*"+suffix).matcher(text);
        if(!m.find())return -1;
        try{return Integer.parseInt(m.group(1));}
        catch(NumberFormatException e){return -1;}
    }
    public static Plan interpret(WorldState w,String raw){
        Plan p=new Plan();
        String line=raw==null?"":raw.trim();
        if(line.isEmpty()){p.error="먼저 어떤 행동을 하고 싶은지 문장을 입력해줘.";return p;}
        if(line.length()>350){p.error="명령은 350자 이내로 입력해줘.";return p;}
        String text=compact(line);
        ArrayList<Integer> ids=countries(w,line);
        int percentage=numberIn(line,"%");
        p.intensity=percentage<0?1:percentage<=15?1:percentage<=40?2:3;
        p.months=numberIn(line,"(?:개)?개월");
        if(p.months<1)p.months=numberIn(line,"월간");
        if(p.months<1){
            int years=numberIn(line,"년");
            p.months=years>0?years*12:1;
        }
        if(p.months>24){p.error="장기 정책은 게임 속 최대 24개월까지만 설정 가능해.";return p;}
        boolean global=has(text,"전세계","전국가","세계전체","모든국가","모든나라","국제사회");
        if(global&&has(text,"기후","탄소","환경","온실가스")){
            p.type=Plan.CLIMATE;
            p.explanation="전 세계 기후 협력 프로그램\n영향력 12점을 사용하며 197개 모든 국가의 환경·외교 지수가 소폭 상승해. 실제 2026 국제기구의 결정은 아니야.";
            return p;
        }
        boolean tax=has(text,"증세","감세","세금","세율","조세");
        if(tax){
            p.type=Plan.FISCAL;
            p.country=ids.isEmpty()?w.selected:ids.get(0);
            p.higherTax=has(text,"인상","올려","늘려","증세","높여","강화");
            if(has(text,"감세","인하","낮춰","줄여","완화"))p.higherTax=false;
            p.explanation=w.nations.get(p.country).name+" · 조세 부담 "+
                    (p.higherTax?"인상":"완화")+"\n효과 강도 "+p.intensity+
                    " · 영향력 "+(2*p.intensity)+"점 사용.\n입력한 %는 게임용 정책 강도로 환산돼. 실제 세율을 그대로 적용하지는 않아.";
            return p;
        }

        boolean diplomacy=has(text,"무역협정","수교","외교","협상","협력","동맹","교류","휴전","중재","평화",
                "회담","조약","기후협약","공동연구");
        if(diplomacy){
            p.type=Plan.DIPLOMACY;
            if(ids.size()>=2){p.country=ids.get(0);p.other=ids.get(1);}
            else if(ids.size()==1){p.country=w.selected;p.other=ids.get(0);}
            else{
                p.error="외교 명령에는 서로 다른 두 국가를 적어줘. 예: 한국과 일본 무역협정";
                p.type=Plan.INVALID;return p;
            }
            if(p.country==p.other){
                p.error="외교 명령에는 서로 다른 두 국가가 필요해.";p.type=Plan.INVALID;return p;
            }
            if(has(text,"평화","중재","휴전"))p.policy=4;
            else if(has(text,"기후","환경","탄소")&&has(text,"공동","협력","협약","연구"))p.policy=3;
            else if(has(text,"문화","교육","교류","유학","학생"))p.policy=2;
            else if(has(text,"무역","경제","시장","공급망","조약"))p.policy=0;
            else p.policy=1;
            p.explanation=w.nations.get(p.country).name+" ↔ "+w.nations.get(p.other).name+
                    "\n국제 행동: "+WorldEngine.DIPLOMACY[p.policy]+
                    "\n세계 영향력 "+WorldEngine.DIPLOMACY_COST[p.policy]+"점 사용. "+ 
                    "신뢰·긴장·무역·협정에 영향을 줘.";
            return p;
        }

        String[][] keys={
            {"교육","학교","대학","장학","교사","문해","학습"},
            {"기초과학","연구","과학","기술","우주","반도체","인공지능","r&d"},
            {"보건","의료","병원","백신","의사","건강","치료"},
            {"재생에너지","태양광","풍력","에너지","친환경","배출","탄소"},
            {"식량","농업","농장","농민","곡물","농산","관개"},
            {"교통","철도","항만","인프라","고속철","도로","물류"},
            {"복지","주거","노동","임금","빈곤","실업","사회보장"},
            {"행정","선거","참여","부패","투명","거버넌스","시민권"}
        };
        // Match strongest explicit policy signal, not any isolated verb.
        int best=-1,score=0;
        for(int i=0;i<keys.length;i++){
            int current=0;
            for(String term:keys[i])if(text.contains(compact(term)))current+=term.length();
            if(current>score){best=i;score=current;}
        }
        if(best>=0){
            p.type=Plan.POLICY;p.policy=best;
            p.country=ids.isEmpty()?w.selected:ids.get(0);
            p.explanation=w.nations.get(p.country).name+" · "+
                    WorldEngine.POLICIES[best]+
                    "\n강도 "+p.intensity+" · 게임 속 "+p.months+"개월"+
                    "\n시작 영향력 "+(WorldEngine.POLICY_COST[best]*p.intensity)+
                    "점, 국가 예산도 소모돼. 기간이 2개월 이상이면 다음 달부터 자동 계속돼."+
                    (percentage>0?"\n입력한 "+percentage+"%는 게임 정책 강도 "+p.intensity+"로 환산돼.":"");
            return p;
        }
        p.error="이 명령은 아직 정확히 해석할 수 없어.\n"+
            "국가 이름 + 교육·의료·연구·에너지·농업·교통·복지·행정, "+
            "또는 두 국가 + 외교·무역·평화 중재로 입력해줘.";
        return p;
    }
}
