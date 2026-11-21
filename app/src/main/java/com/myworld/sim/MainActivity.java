package com.myworld.sim;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Native offline country/world/diplomacy/chronicle UI.
 * The local world is simulated in actual elapsed-month increments, independently
 * of whether this screen was opened every day.
 */
public final class MainActivity extends Activity {
    private static final int BG=Color.rgb(8,18,33);
    private static final int CARD=Color.rgb(19,34,54);
    private static final int LIGHT=Color.rgb(232,241,250);
    private static final int DIM=Color.rgb(161,181,202);
    private static final int CYAN=Color.rgb(89,216,234);
    private static final int GREEN=Color.rgb(111,225,177);
    private static final int YELLOW=Color.rgb(252,210,132);
    private int tab=0;
    private int target=1;
    private int observedMonth=-1;
    private TextView countdown;
    private final Handler handler=new Handler();
    private final Runnable heartbeat=new Runnable(){
        @Override public void run(){
            WorldState s=WorldRepository.read(MainActivity.this,w->w);
            if(s.month!=observedMonth){
                observedMonth=s.month;
                WorldWidget.updateAll(MainActivity.this);
                render();
            } else if(countdown!=null){
                countdown.setText(timeUntil(s.nextMonthAt()));
            }
            handler.postDelayed(this,30000);
        }
    };
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        WorldJob.schedule(this);
        render();
    }
    @Override protected void onResume(){
        super.onResume();
        WorldJob.schedule(this);
        WorldRepository.advance(this);
        WorldWidget.updateAll(this);
        render();
        handler.removeCallbacks(heartbeat);
        handler.postDelayed(heartbeat,30000);
    }
    @Override protected void onPause(){
        handler.removeCallbacks(heartbeat);
        super.onPause();
    }
    private int d(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private GradientDrawable shape(int color,int radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(color);g.setCornerRadius(d(radius));
        g.setStroke(d(1),Color.rgb(42,63,83));return g;
    }
    private LinearLayout vertical(){
        LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;
    }
    private LinearLayout horizontal(){
        LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);return v;
    }
    private TextView label(String text,int size,int color,boolean heavy){
        TextView t=new TextView(this);
        t.setText(text);t.setTextSize(size);t.setTextColor(color);
        if(heavy)t.setTypeface(null,Typeface.BOLD);
        return t;
    }
    private void gap(LinearLayout p,int dp){
        p.addView(new View(this),new LinearLayout.LayoutParams(1,d(dp)));
    }
    private void pad(LinearLayout l,int px){l.setPadding(d(px),d(px),d(px),d(px));}
    private void section(LinearLayout p,String name,String subtitle){
        p.addView(label(name,20,LIGHT,true));gap(p,5);
        if(subtitle!=null)p.addView(label(subtitle,12,DIM,false));
        gap(p,12);
    }
    private LinearLayout card(LinearLayout parent){
        LinearLayout c=vertical();c.setPadding(d(15),d(14),d(15),d(14));
        c.setBackground(shape(CARD,15));
        parent.addView(c,new LinearLayout.LayoutParams(-1,-2));
        return c;
    }
    private Button button(String title,boolean strong,Runnable click){
        Button b=new Button(this);
        b.setText(title);b.setAllCaps(false);b.setTextSize(12);
        b.setTextColor(strong?BG:LIGHT);
        b.setBackground(shape(strong?CYAN:Color.rgb(30,48,69),12));
        b.setPadding(d(8),d(8),d(8),d(8));
        b.setOnClickListener(v->click.run());
        return b;
    }
    private WorldState state(){return WorldRepository.read(this,w->w);}
    private String one(double v){return String.format(Locale.KOREA,"%.1f",v);}
    private String timeUntil(long future){
        long seconds=Math.max(0,(future-System.currentTimeMillis())/1000);
        return String.format(Locale.KOREA,"다음 달까지 %02d시간 %02d분",
            seconds/3600,(seconds/60)%60);
    }
    private void render(){
        WorldState world=state();
        observedMonth=world.month;
        LinearLayout root=vertical();root.setBackgroundColor(BG);
        setContentView(root);
        LinearLayout header=vertical();
        header.setPadding(d(16),d(14),d(16),d(12));
        header.addView(label("MYWORLD  🌍",24,CYAN,true));
        LinearLayout line=horizontal();
        TextView clock=label(world.date()+" · "+world.nations.size()+"개국",13,LIGHT,true);
        line.addView(clock,new LinearLayout.LayoutParams(0,-2,1));
        TextView point=label("영향력 ✦ "+world.influence+"/24",12,YELLOW,true);
        line.addView(point);header.addView(line);
        root.addView(header);
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout content=vertical();content.setPadding(d(15),d(8),d(15),d(20));
        scroll.addView(content);
        switch(tab){
            case 0:overview(content,world);break;
            case 1:nation(content,world);break;
            case 2:diplomacy(content,world);break;
            case 3:chronicle(content,world);break;
            default:settings(content,world);
        }
        LinearLayout nav=horizontal();nav.setPadding(d(3),d(6),d(3),d(7));
        String[] names={"🌍 세계","🏛 국가","🤝 외교","📰 역사","⚙ 정보"};
        for(int i=0;i<names.length;i++){
            final int which=i;
            TextView item=label(names[i],12,i==tab?CYAN:DIM,i==tab);
            item.setGravity(Gravity.CENTER);item.setPadding(d(2),d(11),d(2),d(11));
            item.setBackground(shape(i==tab?Color.rgb(23,57,81):BG,9));
            item.setOnClickListener(v->{tab=which;render();});
            nav.addView(item,new LinearLayout.LayoutParams(0,-2,1));
        }
        root.addView(nav);
    }
    private void stat(LinearLayout parent,String title,String value,int color){
        LinearLayout c=vertical();c.setPadding(d(9),d(12),d(9),d(12));
        c.setBackground(shape(CARD,12));
        c.addView(label(title,11,DIM,false));gap(c,4);
        c.addView(label(value,20,color,true));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
        lp.leftMargin=d(3);lp.rightMargin=d(3);parent.addView(c,lp);
    }
    private void metric(LinearLayout parent,String name,int value,int color){
        LinearLayout row=horizontal();
        TextView nameText=label(name,13,LIGHT,false);
        row.addView(nameText,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(label(String.valueOf(value),13,color,true));
        parent.addView(row);gap(parent,5);
        LinearLayout track=horizontal();track.setBackground(shape(Color.rgb(37,58,76),7));
        View fill=new View(this);fill.setBackground(shape(color,7));
        track.addView(fill,new LinearLayout.LayoutParams(0,d(8),Math.max(1,value)));
        View blank=new View(this);track.addView(blank,new LinearLayout.LayoutParams(0,d(8),Math.max(1,100-value)));
        parent.addView(track,new LinearLayout.LayoutParams(-1,d(8)));gap(parent,13);
    }
    private void eventLine(LinearLayout parent,WorldState world,WorldState.Event event){
        LinearLayout c=vertical();
        c.setPadding(d(10),d(9),d(10),d(9));
        c.setBackground(shape(CARD,10));
        c.addView(label(world.eventDate(event.month)+"  ·  "+event.type,11,
            event.type.equals("플레이어")?YELLOW:event.type.equals("분쟁")?Color.rgb(253,156,140):GREEN,true));
        gap(c,4);c.addView(label(event.text,13,LIGHT,false));
        parent.addView(c);gap(parent,7);
    }
    private void overview(LinearLayout c,WorldState w){
        section(c,"살아 있는 또 하나의 지구","세계는 앱을 닫아도 현실 시간에 맞춰 변화해요.");
        WorldMapView map=new WorldMapView(this,w);
        map.setOnCountryClick(id->{chooseCountry(id);tab=1;render();});
        c.addView(map,new LinearLayout.LayoutParams(-1,-2));
        gap(c,13);
        LinearLayout row=horizontal();
        stat(row,"세계 평화",w.peace+"/100",GREEN);
        stat(row,"세계 경제",w.avg("경제")+"/100",YELLOW);
        c.addView(row);gap(c,7);
        row=horizontal();
        stat(row,"기후 건전성",w.climate+"/100",CYAN);
        stat(row,"연구 역량",w.avg("기술")+"/100",Color.rgb(179,171,255));
        c.addView(row);gap(c,16);

        LinearLayout report=card(c);
        report.addView(label("⏳ 세계 시간",16,CYAN,true));gap(report,6);
        countdown=label(timeUntil(w.nextMonthAt()),14,LIGHT,false);
        report.addView(countdown);gap(report,4);
        report.addView(label("현실 24시간 = 게임 1개월 · 현재 "+w.date(),12,DIM,false));
        gap(c,16);

        section(c,"문명 현황","정책·교류·시민 행동이 서로 영향을 줘요.");
        LinearLayout box=card(c);
        info(box,"관찰 국가",w.nations.size()+"개");
        info(box,"관찰 국가 총인구",one(w.totalPopulation()/100.0)+"억 명 (가상)");
        info(box,"무역·협력 협정",w.tradePacts()+"개");
        info(box,"진행 중인 지역 분쟁",w.conflictsCount()+"개");
        info(box,"탄소 압력",w.carbon+"/100");
        gap(c,16);
        section(c,"최근 일어난 일","사람과 정부가 스스로 만든 역사");
        for(int i=0;i<Math.min(w.events.size(),6);i++)eventLine(c,w,w.events.get(i));
        gap(c,8);
        c.addView(button("모든 사건·연대기 보기  →",false,()->{tab=3;render();}));
    }
    private void info(LinearLayout c,String name,String value){
        LinearLayout row=horizontal();
        row.addView(label(name,13,DIM,false),new LinearLayout.LayoutParams(0,-2,1));
        row.addView(label(value,13,LIGHT,true));
        c.addView(row);gap(c,10);
    }
    private void chooseCountry(int id){
        WorldRepository.change(this,w->{w.selected=id;return null;});
        WorldWidget.updateAll(this);
    }
    private void chooseNationDialog(boolean targetSelection){
        WorldState w=state();
        String[] titles=new String[w.nations.size()];
        for(int i=0;i<titles.length;i++){
            WorldState.Nation n=w.nations.get(i);
            titles[i]=n.name+"  ·  "+n.region+"  ·  경제 "+n.economy;
        }
        new AlertDialog.Builder(this)
            .setTitle(targetSelection?"외교 상대 국가":"국가 선택")
            .setItems(titles,(dialog,which)->{
                if(targetSelection)target=which;else chooseCountry(which);
                render();
            }).setNegativeButton("닫기",null).show();
    }
    private void nation(LinearLayout c,WorldState w){
        WorldState.Nation n=w.nations.get(w.selected);
        section(c,"국가 운영","세계의 국가들은 개별 정책과 사회집단을 가지고 있어요.");
        c.addView(button("🔎 국가 변경: "+n.name+"  ⌄",true,()->chooseNationDialog(false)));
        gap(c,11);
        LinearLayout summary=card(c);
        summary.addView(label(n.name,24,LIGHT,true));
        gap(summary,4);
        summary.addView(label(n.region+" · 정부 우선순위 "+n.ideology(),12,CYAN,false));
        gap(summary,12);
        info(summary,"인구",one(n.population/100.0)+"억");
        info(summary,"재정",one(n.treasury)+" 단위");
        info(summary,"지난달 자율 정책",n.lastPolicy<0?"예산 부족으로 미시행":WorldEngine.POLICIES[n.lastPolicy]);
        info(summary,"시민 만족도",n.satisfaction()+"/100");
        gap(c,16);

        section(c,"사회·경제 기반","수치는 게임용 지표(0~100)이며 현실 통계가 아니에요.");
        LinearLayout metrics=card(c);
        metric(metrics,"경제",n.economy,YELLOW);
        metric(metrics,"정치·사회 안정",n.stability,GREEN);
        metric(metrics,"교육",n.education,CYAN);
        metric(metrics,"보건",n.health,Color.rgb(225,169,234));
        metric(metrics,"산업",n.industry,YELLOW);
        metric(metrics,"과학",n.science,Color.rgb(180,174,242));
        metric(metrics,"환경",n.environment,GREEN);
        metric(metrics,"식량",n.food,YELLOW);
        metric(metrics,"에너지",n.energy,CYAN);
        metric(metrics,"교통",n.transport,GREEN);
        metric(metrics,"복지",n.welfare,Color.rgb(225,169,234));
        metric(metrics,"외교",n.diplomacy,Color.rgb(180,174,242));
        gap(c,15);

        section(c,"기술 발전","장기 연구로 기술 단계가 높아지고 사회 지표에 영향을 줘요.");
        LinearLayout techCard=card(c);
        for(int i=0;i<WorldState.TECH_BRANCHES;i++){
            techCard.addView(label(WorldState.TECH_NAMES[i]+"  Lv."+n.technology[i]+
                "/10  ·  연구 점수 "+n.research[i],13,LIGHT,true));
            gap(techCard,8);
        }
        gap(c,15);

        section(c,"사회집단 여론","국가 내부에서도 집단별 만족도가 달라요.");
        LinearLayout groupsCard=card(c);
        for(int i=0;i<WorldState.GROUP_NAMES.length;i++)
            metric(groupsCard,WorldState.GROUP_NAMES[i],n.groups[i],GREEN);
        gap(c,15);

        section(c,"직접 정책 개입","한 게임 달에 최대 3회. 영향력이 필요하고 국가 재정도 소모돼요.");
        LinearLayout policyCard=card(c);
        for(int i=0;i<WorldEngine.POLICIES.length;i++){
            final int pick=i;
            policyCard.addView(button(WorldEngine.POLICIES[i]+"  ·  ✦"+WorldEngine.POLICY_COST[i],
                false,()->policyDialog(n.id,pick)));
            gap(policyCard,6);
        }
    }
    private void policyDialog(int nationId,int policy){
        WorldState w=state();
        WorldState.Nation n=w.nations.get(nationId);
        new AlertDialog.Builder(this)
            .setTitle(n.name+" · "+WorldEngine.POLICIES[policy])
            .setMessage(WorldEngine.POLICY_DETAILS[policy]+"\n\n세계 영향력 "+
                WorldEngine.POLICY_COST[policy]+"점 소모\n국가 예산 "+
                WorldEngine.POLICY_COST[policy]+"단위 소모\n\n정책을 시행할까요?")
            .setNegativeButton("돌아가기",null)
            .setPositiveButton("정책 시행",(dialog,which)->{
                String message=WorldRepository.change(this,s->
                    WorldEngine.playerPolicy(s,nationId,policy));
                Toast.makeText(this,message,Toast.LENGTH_LONG).show();
                WorldWidget.updateAll(this);render();
            }).show();
    }
    private void diplomacy(LinearLayout c,WorldState w){
        WorldState.Nation source=w.nations.get(w.selected);
        if(target==source.id)target=(target+1)%w.nations.size();
        WorldState.Nation other=w.nations.get(target);
        section(c,"세계 외교실","플레이어 개입 외에도 국가들은 자체적으로 협상하고 협정을 맺어요.");
        c.addView(button("나의 관찰 국가: "+source.name+"  ⌄",true,()->chooseNationDialog(false)));
        gap(c,8);
        c.addView(button("외교 대상: "+other.name+"  ⌄",false,()->chooseNationDialog(true)));
        gap(c,15);

        LinearLayout s=card(c);
        s.addView(label(source.name+" ↔ "+other.name,17,CYAN,true));gap(s,12);
        metric(s,"양국 관계",Math.max(0,(w.relations[source.id][target]+100)/2),GREEN);
        metric(s,"무역 활성도",w.trade[source.id][target],YELLOW);
        metric(s,"외교 긴장",w.tensions[source.id][target],Color.rgb(247,141,145));
        info(s,"국제 협력 조약",w.pacts[source.id][target]>0?"체결":"없음");
        info(s,"지역 분쟁",w.conflicts[source.id][target]>0?"진행 중":"없음");
        gap(c,17);
        section(c,"국제 행동","경제 협력·문화 교류·평화 중재로 관계를 바꿔요.");
        for(int i=0;i<WorldEngine.DIPLOMACY.length;i++){
            final int action=i;
            c.addView(button(WorldEngine.DIPLOMACY[i]+"  ·  영향력 "+WorldEngine.DIPLOMACY_COST[i],
                false,()->diplomacyDialog(source.id,target,action)));
            gap(c,7);
        }
        gap(c,13);
        section(c,"외교 연대기",null);
        int count=0;
        for(WorldState.Event e:w.events){
            if(e.type.equals("외교")||e.type.equals("평화")||e.type.equals("분쟁")){
                eventLine(c,w,e);if(++count==7)break;
            }
        }
        if(count==0)c.addView(label("아직 주요 국제 사건은 없습니다.",13,DIM,false));
    }
    private void diplomacyDialog(int a,int b,int kind){
        WorldState w=state();
        new AlertDialog.Builder(this).setTitle(WorldEngine.DIPLOMACY[kind])
            .setMessage(w.nations.get(a).name+" ↔ "+w.nations.get(b).name+
                "\n\n세계 영향력 "+WorldEngine.DIPLOMACY_COST[kind]+
                "점을 소모해 국제 관계에 개입합니다.")
            .setNegativeButton("취소",null).setPositiveButton("진행",(d,which)->{
                String outcome=WorldRepository.change(this,s->
                    WorldEngine.playerDiplomacy(s,a,b,kind));
                Toast.makeText(this,outcome,Toast.LENGTH_LONG).show();
                WorldWidget.updateAll(this);render();
            }).show();
    }
    private void chronicle(LinearLayout c,WorldState w){
        section(c,"세계 연대기","정책, 시민 행동, 기술 발전, 국제 사건을 기록합니다.");
        LinearLayout stats=card(c);
        info(stats,"누적 시뮬레이션",w.month+"개월");
        info(stats,"공식 기록",w.events.size()+"건 (최근 기록 최대 260건)");
        info(stats,"최근 월간 보고",w.lastMonthlyDigest.isEmpty()?"개국 이후 아직 한 달이 안 지났습니다.":w.lastMonthlyDigest);
        gap(c,16);
        for(WorldState.Event event:w.events)eventLine(c,w,event);
    }
    private void settings(LinearLayout c,WorldState w){
        section(c,"세계 시계와 규칙","게임은 오프라인에서도 저장되어 시간이 흐릅니다.");
        LinearLayout rules=card(c);
        info(rules,"세계 날짜",w.date());
        info(rules,"속도","현실 24시간 → 게임 1개월");
        info(rules,"현재 세계 나이",w.month+"개월");
        info(rules,"다음 자동 진행",timeUntil(w.nextMonthAt()));
        info(rules,"자율 국가 수",w.nations.size()+"개");
        info(rules,"플레이어 영향력",w.influence+"/24");
        info(rules,"월별 최대 개입",Math.max(0,3-w.interventionsUsed)+"/3회 남음");
        gap(c,13);
        c.addView(button("🔄 세계 시간 다시 확인",true,()->{
            int months=WorldRepository.advance(this);
            WorldWidget.updateAll(this);
            Toast.makeText(this,months+"개월 진행했습니다.",Toast.LENGTH_SHORT).show();
            render();
        }));
        gap(c,16);
        section(c,"앱을 켜지 않아도 변화하는 이유","두 층의 진행 시스템이 세이브 데이터를 지켜줘요.");
        LinearLayout explanation=card(c);
        explanation.addView(label("① 백그라운드 예약 작업",14,CYAN,true));
        gap(explanation,4);
        explanation.addView(label("안드로이드가 허용할 때 주기적으로 세계 상태를 업데이트합니다. 절전 모드에서는 지연될 수 있습니다.",13,LIGHT,false));
        gap(explanation,12);
        explanation.addView(label("② 놓친 달 따라잡기",14,CYAN,true));
        gap(explanation,4);
        explanation.addView(label("앱 또는 위젯을 열면 마지막 저장 시각부터 경과한 달을 계산해 순서대로 시뮬레이션합니다. 하루도 접속하지 않아도 국가 정책은 스스로 진행돼요.",13,LIGHT,false));
        gap(explanation,12);
        explanation.addView(label("③ 완전 오프라인 · 로컬 저장",14,CYAN,true));
        gap(explanation,4);
        explanation.addView(label("기기 내부의 원자적 JSON 저장 방식. 인터넷 연결이나 사용자 계정이 필요 없습니다.",13,LIGHT,false));
        gap(c,16);
        section(c,"홈 화면 위젯 2종","홈 화면을 길게 누르기 → 위젯 → MYWORLD");
        LinearLayout widgets=card(c);
        widgets.addView(label("🌍 1×1 : 한눈에 보는 작은 지구",14,LIGHT,true));gap(widgets,8);
        widgets.addView(label("🌐 2×2 : 연도·경제·평화·환경·인구·주요 뉴스",14,LIGHT,true));gap(widgets,8);
        widgets.addView(label("위젯을 터치하면 현재 세계가 열립니다.",12,DIM,false));
        gap(c,18);
        section(c,"주의 사항","실제 역사·현재 국제관계에 대한 예측이 아니에요.");
        c.addView(label("지도는 게임 전용 도식이고 실제 국경을 정확히 표현하지 않습니다. 국가별 초기 능력치와 경제·사회 반응은 재미를 위한 허구의 변수입니다. 분쟁도 인구·외교·경제 영향 중심으로만 묘사합니다.",13,DIM,false));
        gap(c,22);
        c.addView(button("⚠️ 새로운 지구로 초기화",false,()->new AlertDialog.Builder(this)
            .setTitle("정말 새로운 지구를 만들까요?")
            .setMessage("현재까지 발전한 모든 국가와 수백 개 사건 기록이 삭제됩니다. 되돌릴 수 없습니다.")
            .setNegativeButton("유지",null)
            .setPositiveButton("새 지구 생성",(dialog,which)->{
                WorldRepository.reset(this);target=1;tab=0;
                WorldWidget.updateAll(this);render();
            }).show()));
    }
}
