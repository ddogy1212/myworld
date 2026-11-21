package com.myworld.sim;

import java.util.Locale;

/**
 * Deterministic, self-contained monthly civilization simulation.
 * Diplomacy and conflict are abstract social/economic outcomes, not tactics.
 */
public final class WorldEngine {
    private WorldEngine() {}
    public static final String[] POLICIES={
        "공교육·대학 개혁","기초과학 연구","공공보건 투자","재생에너지 확장",
        "식량·농업 혁신","교통·디지털 인프라","주거·노동 복지","시민 참여·행정 개혁"
    };
    public static final String[] POLICY_DETAILS={
        "교육과 연구 역량 향상, 재정 지출 증가",
        "과학 기술 진전과 산업 생산성 상승",
        "건강과 시민 만족도 개선",
        "에너지 안정과 환경 개선, 단기 산업 비용",
        "식량 안보 개선과 농업 분야 만족도 상승",
        "물류·산업 성과 개선, 환경 부담",
        "사회 안정과 시민·노동자 만족도 개선",
        "정치 신뢰·외교 역량·행정 효율 개선"
    };
    public static final int[] POLICY_COST={3,4,3,4,3,4,3,3};
    public static final String[] DIPLOMACY={
        "무역 협정","외교 대화","학술·문화 교류","공동 기후 연구","평화 중재"
    };
    public static final int[] DIPLOMACY_COST={4,2,2,4,5};

    private static int safe(int x){return WorldState.clamp(x);}
    private static void pair(int[][] m,int a,int b,int value){m[a][b]=m[b][a]=value;}
    private static void groups(WorldState.Nation n,int a,int b,int c,int d,int e){
        int[] v={a,b,c,d,e};
        for(int i=0;i<v.length;i++)n.groups[i]=safe(n.groups[i]+v[i]);
    }
    public static int catchUp(WorldState w,long now){
        if(now<w.originUtc)return 0;
        long target=(now-w.originUtc)/WorldState.MONTH_MS;
        int count=0;
        while(w.month<target && count<2400){tick(w);count++;}
        return count;
    }
    public static void tick(WorldState w){
        w.month++;
        w.influence=Math.min(24,w.influence+4);
        w.interventionMonth=-1;
        w.interventionsUsed=0;
        int breakthroughs=0,interactions=0,events=0;
        for(WorldState.Nation n:w.nations){
            economy(w,n);
            autonomousGovernment(w,n);
            if(research(w,n))breakthroughs++;
            society(w,n);
        }
        for(int a=0;a<w.nations.size();a++)
            for(int b=a+1;b<w.nations.size();b++)relationship(w,a,b);
        for(int k=0;k<10;k++){
            int a=w.dice(w.nations.size()),b=w.dice(w.nations.size());
            if(a!=b && diplomacy(w,a,b))interactions++;
        }
        if(w.dice(100)<26){randomEvent(w);events++;}
        climate(w);
        globalPeace(w);
        w.lastMonthlyDigest=String.format(Locale.KOREA,
            "%s · 기술 진전 %d건 · 외교 활동 %d건 · 주요 사건 %d건 · 진행 중 분쟁 %d건",
            w.date(),breakthroughs,interactions,events,w.conflictsCount());
        w.addEvent("월간",w.lastMonthlyDigest);
        if(w.month%12==0)w.addEvent("연간",w.date()+
            " 연간 평가: 경제 "+w.avg("경제")+" · 연구 "+w.avg("기술")+
            " · 세계 평화 "+w.peace+" · 기후 "+w.climate+".");
    }
    private static void economy(WorldState w,WorldState.Nation n){
        int tradeScore=0;
        for(int b=0;b<w.nations.size();b++){
            if(b!=n.id && w.conflicts[n.id][b]==0)
                tradeScore+=Math.max(0,w.trade[n.id][b])/28+(w.pacts[n.id][b]>0?2:0);
        }
        double productivity=.27*n.industry+.12*n.education+.11*n.health+
            .15*n.transport+.14*n.energy+.09*n.food+.12*n.science;
        double growth=(productivity-n.economy)/48.0+
            (Math.min(n.energy,n.food)/140.0-.42)*.9+
            tradeScore/650.0+(n.stability-50)/140.0+(w.dice(5)-2)*.34;
        if(n.treasury<0)growth-=.8;
        n.economy=safe(n.economy+(int)Math.round(growth));
        double income=2+n.economy/22.0+tradeScore/95.0;
        double expense=2+n.welfare/35.0+n.defense/50.0+n.health/100.0+n.education/110.0;
        n.treasury=Math.max(-100,Math.min(350,n.treasury+income-expense));
        if(n.treasury< -20){
            n.stability=safe(n.stability-1);
            n.groups[0]=safe(n.groups[0]-1);
            if(w.dice(100)<9)w.addEvent("경제",n.name+"에서 재정 압박으로 지출 구조 조정을 논의한다.");
        }
        if(n.energy<n.industry-18 && w.dice(100)<40)n.energy=safe(n.energy-1);
        if(n.food<42)n.stability=safe(n.stability-1);
    }
    private static void autonomousGovernment(WorldState w,WorldState.Nation n){
        int[] preference={
            25+(100-n.education)/2+n.groups[2]/8,
            23+(100-n.science)/2+(n.focus==2?24:0),
            26+(100-n.health)/2+(n.focus==3?12:0),
            25+(100-n.energy)/2+(100-n.environment)/3+(n.focus==1?23:0),
            23+(100-n.food)/2+(n.groups[3]<45?14:0),
            25+(100-n.transport)/2+(n.focus==0?24:0),
            24+(100-n.welfare)/2+(100-n.stability)/3+(n.focus==3?17:0),
            20+(100-n.stability)/2+(100-n.diplomacy)/4+(n.focus==4?18:0)
        };
        int chosen=0,best=-1;
        for(int i=0;i<preference.length;i++){
            int v=preference[i]+w.dice(39);
            if(v>best){best=v;chosen=i;}
        }
        int budget=2+POLICY_COST[chosen];
        if(n.treasury<budget){
            n.lastPolicy=-1;
            if(w.dice(100)<8)w.addEvent("정치",n.name+"의 예산 부족으로 계획했던 개혁을 연기했다.");
            return;
        }
        n.treasury-=budget;
        n.lastPolicy=chosen;
        policy(w,n,chosen,false);
        if(w.dice(100)<11 || (n.stability<35&&w.dice(100)<40))
            w.addEvent("정치",n.name+" 정부가 '"+POLICIES[chosen]+"' 정책을 추진한다.");
    }
    private static void policy(WorldState w,WorldState.Nation n,int p,boolean intervention){
        int bonus=intervention?2:0;
        switch(p){
            case 0:
                n.education=safe(n.education+2+bonus);n.science=safe(n.science+1);
                groups(n,1,-1,3+bonus,0,0);break;
            case 1:
                n.science=safe(n.science+2+bonus);
                int tech=w.dice(WorldState.TECH_BRANCHES);
                n.research[tech]=Math.min(250,n.research[tech]+18+bonus*4);
                groups(n,0,1,3,0,-1);break;
            case 2:
                n.health=safe(n.health+2+bonus);n.stability=safe(n.stability+1);
                groups(n,2,0,0,0,1);break;
            case 3:
                n.energy=safe(n.energy+2+bonus);n.environment=safe(n.environment+2+bonus);
                n.industry=safe(n.industry-1);groups(n,1,-1,1,0,0);break;
            case 4:
                n.food=safe(n.food+3+bonus);n.environment=safe(n.environment+1);
                groups(n,0,0,0,3+bonus,0);break;
            case 5:
                n.transport=safe(n.transport+2+bonus);n.industry=safe(n.industry+1+bonus);
                n.environment=safe(n.environment-1);groups(n,0,2,0,0,1);break;
            case 6:
                n.welfare=safe(n.welfare+2+bonus);n.stability=safe(n.stability+1+bonus);
                groups(n,2+bonus,-1,0,0,3+bonus);break;
            case 7:
                n.stability=safe(n.stability+2+bonus);n.diplomacy=safe(n.diplomacy+1+bonus);
                groups(n,3,0,0,0,1);break;
            default:break;
        }
    }
    private static boolean research(WorldState w,WorldState.Nation n){
        int topic=w.dice(WorldState.TECH_BRANCHES);
        if(n.focus==1 && w.dice(100)<40)topic=3;
        if(n.focus==2 && w.dice(100)<40)topic=2;
        double penalty=n.treasury<0?.6:1;
        int advance=Math.max(1,(int)((n.science+n.education)/24.0+
            2*n.technology[topic]/9.0+w.dice(5))* (int)Math.ceil(penalty));
        if(n.treasury<0)advance=Math.max(1,advance/2);
        n.research[topic]+=advance;
        int need=85+14*n.technology[topic];
        if(n.research[topic]>=need && n.technology[topic]<10){
            n.research[topic]-=need;
            n.technology[topic]++;
            switch(topic){
                case 0:n.energy=safe(n.energy+3);break;
                case 1:n.health=safe(n.health+3);break;
                case 2:n.industry=safe(n.industry+3);break;
                case 3:n.environment=safe(n.environment+3);break;
                case 4:n.science=safe(n.science+3);break;
            }
            w.addEvent("과학","🔬 "+n.name+"이(가) "+WorldState.TECH_NAMES[topic]+
                " 기술을 "+n.technology[topic]+"단계로 발전시켰다.");
            return true;
        }
        if(n.technology[topic]>=10)n.research[topic]=99;
        return false;
    }
    private static void society(WorldState w,WorldState.Nation n){
        double welfare=(n.health+n.food+n.stability+n.welfare)/400.0;
        double growth=.00065*(welfare-.36);
        if(n.food<38 || n.health<40)growth=-.00045;
        n.population=Math.max(.25,Math.min(2000,n.population*(1+growth)));
        if(n.environment<35&&w.dice(100)<18)n.health=safe(n.health-1);
        if(n.stability<35 && w.dice(100)<23){
            groups(n,-2,0,0,0,-2);
            w.addEvent("사회",n.name+"의 시민단체들이 생활 여건 개선을 요구한다.");
        }
        if(n.satisfaction()<36){
            n.stability=safe(n.stability-1);
            if(w.dice(100)<12){
                n.focus=(n.focus+1+w.dice(4))%5;
                for(int i=0;i<5;i++)n.groups[i]=safe(n.groups[i]+4);
                w.addEvent("정치",n.name+"의 시민 요구에 따라 정책 우선순위가 "+n.ideology()+"(으)로 바뀌었다.");
            }
        } else if(n.satisfaction()>65 && w.dice(100)<24){
            n.stability=safe(n.stability+1);
        }
    }
    private static void relationship(WorldState w,int a,int b){
        WorldState.Nation x=w.nations.get(a),y=w.nations.get(b);
        int rel=w.relations[a][b],tension=w.tensions[a][b];
        if(w.conflicts[a][b]>0){
            pair(w.relations,a,b,WorldState.clampSigned(rel-1));
            pair(w.trade,a,b,Math.max(0,w.trade[a][b]-3));
            if(w.dice(100)<22)x.economy=safe(x.economy-1);
            if(w.dice(100)<22)y.economy=safe(y.economy-1);
            if(w.dice(100)<25)x.stability=safe(x.stability-1);
            if(w.dice(100)<25)y.stability=safe(y.stability-1);
            if(w.dice(100)<13){
                pair(w.conflicts,a,b,0);
                pair(w.tensions,a,b,Math.max(15,tension-18));
                w.addEvent("평화","🕊️ "+x.name+"과 "+y.name+"이(가) 분쟁 중단 및 협상에 합의했다.");
            }
            return;
        }
        if(w.pacts[a][b]>0){
            if(w.dice(100)<29)pair(w.trade,a,b,Math.min(100,w.trade[a][b]+1));
            if(w.dice(100)<16)pair(w.relations,a,b,WorldState.clampSigned(rel+1));
            if(w.dice(200)==0){
                pair(w.pacts,a,b,0);
                w.addEvent("외교",x.name+"과 "+y.name+"의 기존 협정이 만료돼 재협상이 필요해졌다.");
            }
        }
        // Civilian-centered abstract diplomatic disputes grow from resource
        // shortages, economic competition and existing distrust.
        int disputeChance=x.region.equals(y.region)?5:2; // per mille
        if((x.energy<42||y.food<42))disputeChance+=3;
        if(w.dice(1000)<disputeChance){
            pair(w.relations,a,b,WorldState.clampSigned(rel-3-w.dice(5)));
            pair(w.tensions,a,b,Math.min(100,tension+6+w.dice(7)));
            pair(w.trade,a,b,Math.max(0,w.trade[a][b]-3));
            if(w.dice(100)<35)
                w.addEvent("외교",x.name+"과 "+y.name+" 사이에서 공급망·무역 문제로 외교적 마찰이 발생했다.");
        } else {
            if(rel< -30 && w.dice(100)<16)
                pair(w.tensions,a,b,Math.min(100,tension+1+w.dice(2)));
            if(rel>=0 && w.dice(100)<22)
                pair(w.tensions,a,b,Math.max(0,tension-1));
        }
        // Escalation is never a military tactic: only social, budget and
        // diplomatic consequences are modelled.
        int latestTrust=w.relations[a][b],latestTension=w.tensions[a][b];
        if(latestTension>=74&&latestTrust<=-45&&w.dice(100)<4){
            pair(w.conflicts,a,b,1);pair(w.pacts,a,b,0);
            w.addEvent("분쟁","⚠️ "+x.name+"과 "+y.name+" 사이의 장기 외교 위기가 지역 분쟁으로 확대됐다. 국제사회는 중재를 요구한다.");
        }
    }
    private static boolean diplomacy(WorldState w,int a,int b){
        if(a==b)return false;
        WorldState.Nation x=w.nations.get(a),y=w.nations.get(b);
        int rel=w.relations[a][b];
        if(w.conflicts[a][b]>0){
            if(w.dice(100)<24){
                pair(w.tensions,a,b,Math.max(0,w.tensions[a][b]-12));
                if(w.dice(100)<40){
                    pair(w.conflicts,a,b,0);
                    w.addEvent("평화",x.name+"과 "+y.name+"이(가) 중재 회담 끝에 휴전했다.");
                }
                return true;
            }
            return false;
        }
        if(rel>=12&&w.dice(100)<35){
            pair(w.relations,a,b,WorldState.clampSigned(rel+2));
            pair(w.trade,a,b,Math.min(100,w.trade[a][b]+3));
            if(rel>=35&&w.pacts[a][b]==0&&w.dice(100)<25){
                pair(w.pacts,a,b,1);
                w.addEvent("외교",x.name+"과 "+y.name+"이(가) 경제·연구 협약을 체결했다.");
            } else if(w.dice(100)<12){
                w.addEvent("외교",x.name+"과 "+y.name+"이(가) 문화·무역 교류를 확대했다.");
            }
            return true;
        }
        if(rel<0&&w.dice(100)<35){
            pair(w.relations,a,b,WorldState.clampSigned(rel+4));
            pair(w.tensions,a,b,Math.max(0,w.tensions[a][b]-5));
            if(w.dice(100)<20)w.addEvent("외교",x.name+"과 "+y.name+"이(가) 관계 개선을 위한 대화를 시작했다.");
            return true;
        }
        return false;
    }
    private static void randomEvent(WorldState w){
        WorldState.Nation n=w.nations.get(w.dice(w.nations.size()));
        switch(w.dice(8)){
            case 0:n.food=safe(n.food-2-w.dice(6));n.economy=safe(n.economy-1);
                w.addEvent("기후",n.name+"에 이상기후가 발생해 농업 공급이 불안정해졌다.");break;
            case 1:n.health=safe(n.health+3);n.science=safe(n.science+1);
                w.addEvent("과학",n.name+"이(가) 공중보건 기술 개발에 성공했다.");break;
            case 2:n.industry=safe(n.industry+3);n.economy=safe(n.economy+2);
                w.addEvent("경제",n.name+"의 신산업 성장이 고용에 긍정적인 영향을 주었다.");break;
            case 3:n.stability=safe(n.stability-3);groups(n,-3,-1,0,0,-3);
                w.addEvent("사회",n.name+"의 시민들이 주거·생활비 문제 개선을 요구했다.");break;
            case 4:n.environment=safe(n.environment+3);n.food=safe(n.food+2);
                w.addEvent("환경",n.name+"이(가) 생태 복원 사업에서 성과를 거뒀다.");break;
            case 5:n.education=safe(n.education+2);n.science=safe(n.science+2);
                w.addEvent("교육",n.name+"에서 국제 연구 교류로 과학 인력 육성이 가속됐다.");break;
            case 6:n.energy=safe(n.energy-1-w.dice(5));n.industry=safe(n.industry-1);
                w.addEvent("경제",n.name+"의 에너지 공급 문제로 산업 조정이 시작됐다.");break;
            default:n.diplomacy=safe(n.diplomacy+3);n.stability=safe(n.stability+1);
                w.addEvent("외교",n.name+"이(가) 국제 시민·학술 교류를 개최했다.");
        }
    }
    private static void climate(WorldState w){
        int industry=0,green=0;
        for(WorldState.Nation n:w.nations){industry+=n.industry;green+=n.environment;}
        industry/=w.nations.size();green/=w.nations.size();
        int emission=Math.max(0,(industry-green)/4+10);
        if(w.dice(100)<Math.min(65,emission))w.carbon=Math.min(100,w.carbon+1);
        else if(green>industry+8&&w.dice(100)<24)w.carbon=Math.max(0,w.carbon-1);
        if(w.carbon>60&&w.dice(100)<Math.min(45,w.carbon-45))w.climate=safe(w.climate-1);
        else if(w.carbon<50&&green>66&&w.dice(100)<12)w.climate=safe(w.climate+1);
        if(w.climate<38&&w.dice(100)<14){
            WorldState.Nation n=w.nations.get(w.dice(w.nations.size()));
            n.food=safe(n.food-4);n.health=safe(n.health-2);
            w.addEvent("기후",n.name+"에서 기후 변동이 보건과 식량에 부담을 주었다.");
        }
    }
    private static void globalPeace(WorldState w){
        int st=0;for(WorldState.Nation n:w.nations)st+=n.stability;
        int target=safe(st/w.nations.size()+10-w.conflictsCount()*13);
        if(w.peace<target)w.peace=Math.min(target,w.peace+2);
        else if(w.peace>target)w.peace=Math.max(target,w.peace-2);
    }
    private static String valid(WorldState w,int cost){
        if(w.interventionMonth!=w.month){w.interventionMonth=w.month;w.interventionsUsed=0;}
        if(w.interventionsUsed>=3)return "이번 달 직접 개입 3회를 사용했습니다. 다음 달을 기다려주세요.";
        if(w.influence<cost)return "영향력이 부족합니다. 게임 내 한 달마다 4점이 회복됩니다.";
        return null;
    }
    public static String playerPolicy(WorldState w,int nation,int policy){
        if(nation<0||nation>=w.nations.size()||policy<0||policy>=POLICIES.length)
            return "잘못된 정책 선택입니다.";
        int cost=POLICY_COST[policy];String error=valid(w,cost);if(error!=null)return error;
        WorldState.Nation n=w.nations.get(nation);
        if(n.treasury<cost)return n.name+"의 정책 시행 예산이 부족합니다.";
        w.influence-=cost;w.interventionsUsed++;n.treasury-=cost;
        policy(w,n,policy,true);
        String message="🌱 "+n.name+"에서 '"+POLICIES[policy]+"'이(가) 시행됐다.";
        w.addEvent("플레이어",message);return message;
    }
    public static String playerDiplomacy(WorldState w,int a,int b,int kind){
        if(a<0||b<0||a>=w.nations.size()||b>=w.nations.size()||a==b
                ||kind<0||kind>=DIPLOMACY.length)return "서로 다른 두 국가를 선택해주세요.";
        String error=valid(w,DIPLOMACY_COST[kind]);if(error!=null)return error;
        if(kind==0&&w.conflicts[a][b]>0)return "분쟁 중에는 무역 협정보다 평화 중재를 먼저 권장합니다.";
        WorldState.Nation x=w.nations.get(a),y=w.nations.get(b);
        w.influence-=DIPLOMACY_COST[kind];w.interventionsUsed++;
        int rel=w.relations[a][b];String impact="";
        switch(kind){
            case 0:
                pair(w.pacts,a,b,1);pair(w.trade,a,b,Math.min(100,w.trade[a][b]+13));
                pair(w.relations,a,b,WorldState.clampSigned(rel+10));
                x.economy=safe(x.economy+1);y.economy=safe(y.economy+1);
                impact="새로운 무역·공급망 협정을 체결했다.";break;
            case 1:
                pair(w.relations,a,b,WorldState.clampSigned(rel+15));
                pair(w.tensions,a,b,Math.max(0,w.tensions[a][b]-12));
                impact="국제 대화 채널을 복원해 갈등을 완화했다.";break;
            case 2:
                pair(w.relations,a,b,WorldState.clampSigned(rel+10));
                x.education=safe(x.education+1);y.education=safe(y.education+1);
                impact="학생·연구자·시민 교류를 확대했다.";break;
            case 3:
                pair(w.relations,a,b,WorldState.clampSigned(rel+10));
                x.environment=safe(x.environment+2);y.environment=safe(y.environment+2);
                x.research[3]+=9;y.research[3]+=9;
                impact="공동 환경 연구 프로젝트를 시작했다.";break;
            default:
                pair(w.tensions,a,b,Math.max(0,w.tensions[a][b]-30));
                pair(w.relations,a,b,WorldState.clampSigned(rel+18));
                if(w.conflicts[a][b]>0)pair(w.conflicts,a,b,0);
                impact="분쟁 중재와 공개 협의체 구성에 합의했다.";
        }
        String result="🤝 "+x.name+" · "+y.name+": "+impact;
        w.addEvent("플레이어",result);return result;
    }
}
