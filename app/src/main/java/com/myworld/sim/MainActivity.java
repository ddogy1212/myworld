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
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.text.TextWatcher;
import android.text.Editable;
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
        if(!getSharedPreferences("myworld_ui",MODE_PRIVATE).getBoolean("tutorial_seen",false)){
            new Handler().postDelayed(this::showFirstLaunch,300);
        }
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
        header.addView(label("명령을 내리면 세계가 변한다 · 197개 국가의 살아있는 역사",11,DIM,false));
        gap(header,7);
        LinearLayout line=horizontal();
        TextView clock=label(world.date()+" · "+world.nations.size()+"개국",13,LIGHT,true);
        line.addView(clock,new LinearLayout.LayoutParams(0,-2,1));
        TextView point=label("영향력 ✦ "+world.influence+"/24",12,YELLOW,true);
        line.addView(point);header.addView(line);
        if(!world.events.isEmpty()){
            String latest=world.events.get(0).text;
            TextView ticker=label("✦ "+(latest.length()>65?latest.substring(0,65)+"…":latest),11,GREEN,false);
            ticker.setSingleLine(true);
            ticker.setEllipsize(android.text.TextUtils.TruncateAt.END);
            ticker.setPadding(d(12),d(7),d(12),d(7));
            ticker.setBackground(shape(CARD,8));
            header.addView(ticker);
        }
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
        section(c,"살아 있는 또 하나의 지구","국경을 눌러 국가를 선택하고, 지도를 두 손가락으로 확대해봐.");
        LinearLayout tools=horizontal();
        tools.addView(button("🔎 국가 검색",true,()->chooseNationDialog(false)),new LinearLayout.LayoutParams(0,-2,1));
        tools.addView(button("🎯 목표 보기",false,()->showHowToPlay()),new LinearLayout.LayoutParams(0,-2,1));
        c.addView(tools);gap(c,9);
        WorldMapView map=new WorldMapView(this,w);
        map.setOnCountryClick(id->{chooseCountry(id);tab=1;render();});
        c.addView(map,new LinearLayout.LayoutParams(-1,-2));
        gap(c,8);
        LinearLayout filters=horizontal();
        filters.addView(button("안정도",true,()->map.setMetricMode(0)),new LinearLayout.LayoutParams(0,-2,1));
        filters.addView(button("경제",false,()->map.setMetricMode(1)),new LinearLayout.LayoutParams(0,-2,1));
        filters.addView(button("환경",false,()->map.setMetricMode(2)),new LinearLayout.LayoutParams(0,-2,1));
        filters.addView(button("전체",false,map::resetZoom),new LinearLayout.LayoutParams(0,-2,1));
        c.addView(filters);gap(c,5);
        c.addView(label("Natural Earth 실측 국경 1:110m · 작은 나라는 확대 후 표시 · 분쟁 지역 경계 표기는 자료 기준",11,DIM,false));
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
        quests(c,w);
        gap(c,13);
        commandConsole(c,w);
        gap(c,17);

        section(c,"문명 현황","정책·교류·시민 행동이 서로 영향을 줘요.");
        LinearLayout box=card(c);
        info(box,"관찰 국가",w.nations.size()+"개");
        info(box,"관찰 국가 총인구",one(w.totalPopulation()/100.0)+"억 명 (가상)");
        info(box,"무역·협력 협정",w.tradePacts()+"개");
        info(box,"진행 중인 지역 분쟁",w.conflictsCount()+"개");
        info(box,"탄소 압력",w.carbon+"/100");
        gap(c,16);
        section(c,"최근 일어난 일","사람과 정부가 스스로 만든 역사");
        if(!w.lastImpact.isEmpty()){
            LinearLayout last=card(c);
            last.addView(label("✨ 네가 바꾼 세계 · "+w.eventDate(w.lastImpactMonth),15,CYAN,true));
            gap(last,6);last.addView(label(w.lastImpact,12,LIGHT,false));
            gap(c,10);
        }
        for(int i=0;i<Math.min(w.events.size(),6);i++)eventLine(c,w,w.events.get(i));
        gap(c,8);
        c.addView(button("모든 사건·연대기 보기  →",false,()->{tab=3;render();}));
    }
    private String populationText(WorldState.Nation n){
        if(n.population<=0)return "공식자료 없음";
        if(n.population>=10)return String.format(Locale.KOREA,"%.2f억 명",n.population/100.0);
        if(n.population>=0.01)return String.format(Locale.KOREA,"%.1f만 명",n.population*100.0);
        return String.format(Locale.KOREA,"%.0f명",n.population*1000000.0);
    }
    private String sovereigntyText(String status){
        if("MEMBER".equals(status))return "유엔 회원국";
        if("OBSERVER".equals(status))return "유엔 비회원 옵서버";
        return "별도 표기 · 국제적 지위에 이견";
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
    /** Searchable 197-country selector, including small nations. */
    private void chooseNationDialog(boolean targetSelection){
        WorldState w=state();
        LinearLayout body=vertical();
        body.setPadding(d(13),d(12),d(13),d(8));
        EditText search=new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(15);
        search.setHint("나라 이름 검색 · 한국어 / 영어 / ISO");
        body.addView(search);
        ListView list=new ListView(this);
        ArrayList<Integer> shown=new ArrayList<>();
        ArrayList<String> labels=new ArrayList<>();
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,
            android.R.layout.simple_list_item_1,labels);
        list.setAdapter(adapter);
        body.addView(list,new LinearLayout.LayoutParams(-1,d(360)));
        final Runnable filter=()->{
            String query=search.getText().toString().toLowerCase(Locale.ROOT).trim();
            shown.clear();labels.clear();
            for(int i=0;i<w.nations.size();i++){
                WorldState.Nation n=w.nations.get(i);
                String[] catalog=NationCatalog.ROWS[i].split("\\|",-1);
                if(query.isEmpty()||n.name.toLowerCase(Locale.ROOT).contains(query)||
                        catalog[2].toLowerCase(Locale.ROOT).contains(query)||
                        n.iso3.toLowerCase(Locale.ROOT).contains(query)){
                    shown.add(i);
                    labels.add(n.name+" · "+n.iso3+" · "+n.region);
                }
            }
            adapter.notifyDataSetChanged();
        };
        search.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                filter.run();
            }
            @Override public void afterTextChanged(Editable editable){}
        });
        filter.run();
        AlertDialog dialog=new AlertDialog.Builder(this)
            .setTitle(targetSelection?"외교 상대 국가 선택":"국가 선택 · "+w.nations.size()+"곳")
            .setView(body).setNegativeButton("닫기",null).create();
        list.setOnItemClickListener((p,v,position,id)->{
            if(position<0||position>=shown.size())return;
            int nation=shown.get(position);
            if(targetSelection)target=nation;else chooseCountry(nation);
            dialog.dismiss();render();
        });
        dialog.show();
    }

    private void commandConsole(LinearLayout parent,WorldState w){
        LinearLayout panel=card(parent);
        panel.addView(label("⌨️ 세계 명령 입력",17,CYAN,true));
        gap(panel,6);
        panel.addView(label("국가·정책·기간을 문장으로 입력하고 확인 후 실행해.",12,DIM,false));
        gap(panel,9);
        EditText typed=new EditText(this);
        typed.setHint("예: 부탄에 12개월 동안 재생에너지 투자");
        typed.setTextColor(LIGHT);typed.setHintTextColor(DIM);
        typed.setTextSize(14);
        typed.setMinLines(2);typed.setMaxLines(4);
        typed.setBackground(shape(Color.rgb(24,46,67),9));
        typed.setPadding(d(9),d(9),d(9),d(9));
        panel.addView(typed,new LinearLayout.LayoutParams(-1,-2));
        gap(panel,8);
        panel.addView(button("명령 분석 및 실행 확인  →",true,()->{
            WorldCommand.Plan plan=WorldCommand.interpret(state(),typed.getText().toString());
            if(!plan.valid()){
                new AlertDialog.Builder(this).setTitle("명령을 해석하지 못했어")
                    .setMessage(plan.error).setPositiveButton("수정할게",null).show();
                return;
            }
            new AlertDialog.Builder(this).setTitle("다음 명령으로 실행할까?")
                .setMessage(plan.explanation+"\n\n진행하면 게임 세계에 반영되고 되돌리기 어려워.")
                .setNegativeButton("입력 수정",null)
                .setPositiveButton("확인 후 실행",(dialog,which)->{
                    String outcome=WorldRepository.change(this,plan::execute);
                    Toast.makeText(this,outcome,Toast.LENGTH_LONG).show();
                    WorldWidget.updateAll(this);
                    render();
                }).show();
        }));
        gap(panel,7);
        panel.addView(label("예: 몰디브 의료 확대 · 한국과 일본 무역협정 · 영국 세금 인하 · 세계 전체 탄소 협력",11,DIM,false));
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
        info(summary,"인구",populationText(n));
        info(summary,"국가 분류",sovereigntyText(n.sovereignty));
        info(summary,"기준 인구 연도",n.populationYear>0?n.populationYear+"년 · 세계은행":"자료 없음");
        info(summary,"명목 GDP",n.gdpYear>0?String.format(Locale.KOREA,"%.2f억 달러 (%d년)",n.gdpUsd/100000000.0,n.gdpYear):"자료 없음");
        info(summary,"GDP 출처",n.gdpYear>0?"세계은행 · 발표 연도별 값":"게임용 가정만 사용");
        info(summary,"게임 수치", "경제·사회·외교 지수는 시뮬레이션 가정");
        info(summary,"장기 사업",n.plannedMonths>0 && n.plannedPolicy>=0?WorldEngine.POLICIES[n.plannedPolicy]+" · "+n.plannedMonths+"개월 남음":"현재 없음");
        info(summary,"재정",one(n.treasury)+" 단위");
        info(summary,"지난달 자율 정책",n.lastPolicy<0?"예산 부족으로 미시행":WorldEngine.POLICIES[n.lastPolicy]);
        info(summary,"시민 만족도",n.satisfaction()+"/100");
        gap(c,12);
        summary.addView(button("⚡ 정책을 내 문장으로 직접 명령하기",true,()->commandPrompt(n.id)));
        gap(c,16);
        commandConsole(c,w);
        gap(c,14);

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
        commandConsole(c,w);
        gap(c,13);
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
        section(c,"세계 시계와 규칙","세계는 오프라인에서도 저장되어 시간이 흐릅니다.");
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
        c.addView(label("지도는 공공영역 Natural Earth 1:110m 국가 경계 데이터입니다. 소국은 경계선이 생략되거나 확대 위치 표시로 나타나며 분쟁 지역은 지도 제작사 분류를 따릅니다. 인구·GDP는 실제 발표연도별 공개자료이지만 사회·정치·외교 수치는 게임 모형입니다.",13,DIM,false));
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
