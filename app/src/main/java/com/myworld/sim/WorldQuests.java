package com.myworld.sim;

/** Clear first-session goals with idempotent rewards. */
public final class WorldQuests {
    private WorldQuests(){}
    public static final String[] TITLE={
        "첫 정책을 시행하자","첫 국제 대화를 시작하자","사회 안정도를 75까지 끌어올리자",
        "연구 문명 1단계를 발전시키자","세계가 1년 지나도록 지켜보자"
    };
    public static final String[] DESCRIPTION={
        "국가 화면에서 교육·의료·환경 정책을 실행","외교 화면에서 두 나라 간 대화 또는 협정 실행",
        "관찰 국가의 안정도 75 이상 달성","관찰 국가의 기술 중 한 분야 7단계 이상 달성",
        "현실 12일이 지나면 게임 속 12개월 진행"
    };
    public static final int[] REWARD={2,3,3,4,6};
    public static boolean done(WorldState w,int quest){
        WorldState.Nation n=w.nations.get(w.selected);
        switch(quest){
            case 0:return w.playerPolicies>0;
            case 1:return w.playerDiplomacy>0;
            case 2:return n.stability>=75;
            case 3:
                for(int level:n.technology)if(level>=7)return true;
                return false;
            case 4:return w.month>=12;
            default:return false;
        }
    }
    public static boolean claimed(WorldState w,int quest){
        return (w.questClaimMask&(1<<quest))!=0;
    }
    public static String claim(WorldState w,int quest){
        if(quest<0||quest>=TITLE.length)return "알 수 없는 목표입니다.";
        if(claimed(w,quest))return "이미 보상을 받았어요.";
        if(!done(w,quest))return "아직 달성하지 못했어요.";
        w.questClaimMask|=(1<<quest);
        int before=w.influence;
        w.influence=Math.min(24,w.influence+REWARD[quest]);
        int earned=w.influence-before;
        String m="🏆 목표 완료: "+TITLE[quest]+" · 영향력 +"+earned;
        w.addEvent("업적",m);
        return m;
    }
    public static int completed(WorldState w){
        int n=0;for(int i=0;i<TITLE.length;i++)if(claimed(w,i))n++;return n;
    }
}
