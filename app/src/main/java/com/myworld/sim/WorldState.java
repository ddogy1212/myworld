package com.myworld.sim;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Serializable, self-contained, fictional alternate-Earth world state. */
public final class WorldState {
    public static final long MONTH_MS = 24L * 60 * 60 * 1000;
    public static final int TECH_BRANCHES = 5;
    public static final String[] TECH_NAMES = {"에너지", "의료", "자동화", "환경", "우주"};
    public static final String[] GROUP_NAMES = {"시민", "산업계", "연구자", "농업인", "노동자"};
    private static final int MAX_EVENTS = 260;

    public static class Nation {
        public int id;
        public String name, region, iso3, sovereignty;
        public int populationYear, gdpYear;
        public double gdpUsd;
        public int mapX, mapY;
        public double population;
        public double treasury;
        public int economy, stability, education, health, environment, food, energy;
        public int industry, science, transport, welfare, defense, diplomacy;
        public int focus;
        public int[] technology = new int[TECH_BRANCHES];
        public int[] research = new int[TECH_BRANCHES];
        public int[] groups = new int[GROUP_NAMES.length];
        public int lastPolicy; // policy index for the current month
        public int plannedPolicy=-1, plannedMonths=0, plannedStrength=1;
        public int yearsOfPeace;

        public String ideology() {
            switch (focus) {
                case 0: return "산업 성장";
                case 1: return "환경 보전";
                case 2: return "연구 개발";
                case 3: return "복지·생활";
                default: return "외교·협력";
            }
        }

        public int satisfaction() {
            int sum=0; for(int g:groups) sum+=g;
            return sum / groups.length;
        }

        public JSONObject json() throws JSONException {
            JSONObject j=new JSONObject();
            j.put("id",id);j.put("name",name);j.put("region",region);
            j.put("iso3",iso3);j.put("sovereignty",sovereignty);
            j.put("populationYear",populationYear);j.put("gdpYear",gdpYear);j.put("gdpUsd",gdpUsd);
            j.put("x",mapX);j.put("y",mapY);j.put("population",population);
            j.put("treasury",treasury);j.put("economy",economy);
            j.put("stability",stability);j.put("education",education);
            j.put("health",health);j.put("environment",environment);
            j.put("food",food);j.put("energy",energy);
            j.put("industry",industry);j.put("science",science);
            j.put("transport",transport);j.put("welfare",welfare);
            j.put("defense",defense);j.put("diplomacy",diplomacy);
            j.put("focus",focus); j.put("lastPolicy",lastPolicy);j.put("peace",yearsOfPeace);
            j.put("plannedPolicy",plannedPolicy);j.put("plannedMonths",plannedMonths);
            j.put("plannedStrength",plannedStrength);
            j.put("technology",arr(technology));j.put("research",arr(research));j.put("groups",arr(groups));
            return j;
        }

        public static Nation from(JSONObject j) throws JSONException {
            Nation n=new Nation();
            n.id=j.getInt("id");n.name=j.getString("name");n.region=j.getString("region");
            n.iso3=j.optString("iso3","");n.sovereignty=j.optString("sovereignty","MEMBER");
            n.populationYear=j.optInt("populationYear",0);n.gdpYear=j.optInt("gdpYear",0);
            n.gdpUsd=j.optDouble("gdpUsd",0);
            n.mapX=j.getInt("x");n.mapY=j.getInt("y");
            n.population=j.getDouble("population");n.treasury=j.getDouble("treasury");
            n.economy=j.getInt("economy");n.stability=j.getInt("stability");
            n.education=j.getInt("education");n.health=j.getInt("health");
            n.environment=j.getInt("environment");n.food=j.getInt("food");
            n.energy=j.getInt("energy");n.industry=j.getInt("industry");
            n.science=j.getInt("science");n.transport=j.getInt("transport");
            n.welfare=j.getInt("welfare");n.defense=j.getInt("defense");
            n.diplomacy=j.getInt("diplomacy");n.focus=j.optInt("focus",0);
            n.lastPolicy=j.optInt("lastPolicy",-1);n.yearsOfPeace=j.optInt("peace",0);
            n.plannedPolicy=j.optInt("plannedPolicy",-1);
            n.plannedMonths=j.optInt("plannedMonths",0);
            n.plannedStrength=j.optInt("plannedStrength",1);
            n.technology=readArr(j.getJSONArray("technology"),TECH_BRANCHES);
            n.research=readArr(j.getJSONArray("research"),TECH_BRANCHES);
            n.groups=readArr(j.getJSONArray("groups"),GROUP_NAMES.length);
            return n;
        }
    }

    public static final class Event {
        public int month;
        public String type, text;
        public Event(int month, String type,String text){
            this.month=month;this.type=type;this.text=text;
        }
        public JSONObject json() throws JSONException {
            return new JSONObject().put("month",month).put("type",type).put("text",text);
        }
        public static Event from(JSONObject j) {
            return new Event(j.optInt("month",0),j.optString("type","일반"),j.optString("text",""));
        }
    }

    private static JSONArray arr(int[] a) {
        JSONArray j=new JSONArray();
        for(int x:a) j.put(x);
        return j;
    }
    private static int[] readArr(JSONArray a,int length){
        int[] out=new int[length];
        for(int i=0;i<length;i++)out[i]=a.optInt(i,0);
        return out;
    }
    private static JSONArray matrix(int[][] values) {
        JSONArray outer=new JSONArray();
        for(int[] a:values)outer.put(arr(a));
        return outer;
    }
    private static int[][] readMatrix(JSONArray arr,int count) throws JSONException {
        if(arr.length()!=count)throw new JSONException("Matrix dimension mismatch");
        int[][] out=new int[count][count];
        for(int i=0;i<count;i++)out[i]=readArr(arr.getJSONArray(i),count);
        return out;
    }

    public long originUtc;
    public int month;
    public long random;
    public int influence = 16;
    public int interventionMonth = -1;
    public int interventionsUsed = 0;
    public int selected = 0;
    public int carbon = 41;
    public int climate = 67;
    public int peace = 76;
    public int globalEvents = 0;
    // The last twelve simulated months, a real game telemetry trail.
    public int[] economyTrend=new int[12];
    public int[] peaceTrend=new int[12];
    public int[] climateTrend=new int[12];
    public static void addTrend(int[] trend,int current){
        for(int i=0;i<trend.length-1;i++)trend[i]=trend[i+1];
        trend[trend.length-1]=current;
    }

    public final ArrayList<Nation> nations=new ArrayList<>();
    public final ArrayList<Event> events=new ArrayList<>();
    public int[][] relations, trade, tensions, pacts, conflicts;
    public String lastMonthlyDigest="";
    // Saved player accomplishments and the latest visible policy effect.
    public int playerPolicies=0, playerDiplomacy=0, questClaimMask=0;
    public String lastImpact="";
    public int lastImpactMonth=-1;

    private static String koreanCountry(String iso2,String iso3,String english){
        if("XKX".equals(iso3))return "코소보";
        if("VAT".equals(iso3))return "바티칸 시국";
        if("PSE".equals(iso3))return "팔레스타인";
        if("TWN".equals(iso3))return "대만";
        String display=new java.util.Locale("",iso2).getDisplayCountry(java.util.Locale.KOREAN);
        if(display==null||display.trim().isEmpty()||display.equals(iso2))return english;
        return display;
    }

    public static WorldState fresh(long now){
        WorldState w=new WorldState();
        w.originUtc=now;w.random=0x6A09E667F3BCC909L;
        for(int i=0;i<NationCatalog.size();i++){
            String[] f=NationCatalog.ROWS[i].split("\\|",-1);
            if(f.length!=11)throw new IllegalStateException("Invalid catalogue row "+i);
            Nation n=new Nation();
            n.id=i;n.iso3=f[0];
            n.name=koreanCountry(f[1],f[0],f[2]);
            n.sovereignty=f[3];n.region=f[4];
            n.mapX=Integer.parseInt(f[5]);n.mapY=Integer.parseInt(f[6]);
            n.population=Double.parseDouble(f[7]);n.populationYear=Integer.parseInt(f[8]);
            n.gdpUsd=Double.parseDouble(f[9]);n.gdpYear=Integer.parseInt(f[10]);
            double perCap=(n.gdpUsd>0&&n.population>0)
                    ? n.gdpUsd/(n.population*1_000_000.0):0;
            // Only GDP and population are sourced measurements.
            // All gameplay attributes are explicitly generated model indices.
            int derived=perCap>0?clamp((int)(27+15*Math.log10(Math.max(1,perCap)))):50;
            n.economy=derived;
            n.science=clamp(derived-9+w.dice(25)-12);
            n.education=clamp(derived-7+w.dice(25)-12);
            n.environment=clamp(48+w.dice(33)-16);
            n.stability=clamp(54+w.dice(35)-17);
            n.health=clamp(42+derived/3+w.dice(20)-10);
            n.industry=clamp(derived+w.dice(26)-13);
            n.transport=clamp(derived+w.dice(26)-13);
            n.welfare=clamp(46+derived/3+w.dice(24)-12);
            n.energy=clamp(43+w.dice(40));
            n.food=clamp(44+w.dice(42));
            n.defense=clamp(31+derived/2+w.dice(19));
            n.diplomacy=clamp(46+w.dice(36));
            n.treasury=20+derived*0.35;
            n.focus=w.dice(5);n.lastPolicy=-1;
            for(int t=0;t<TECH_BRANCHES;t++){
                n.technology[t]=Math.max(0,Math.min(10,(n.science+w.dice(24)-11)/13));
                n.research[t]=w.dice(90);
            }
            for(int g=0;g<n.groups.length;g++)
                n.groups[g]=clamp(n.stability+w.dice(32)-16);
            w.nations.add(n);
        }
        int count=w.nations.size();
        w.relations=new int[count][count];w.trade=new int[count][count];
        w.tensions=new int[count][count];w.pacts=new int[count][count];
        w.conflicts=new int[count][count];
        for(int a=0;a<count;a++)for(int b=a+1;b<count;b++){
            Nation x=w.nations.get(a),y=w.nations.get(b);
            int trust=clampSigned(8+(x.region.equals(y.region)?11:0)+w.dice(42)-21);
            w.relations[a][b]=w.relations[b][a]=trust;
            w.trade[a][b]=w.trade[b][a]=Math.max(0,12+trust/3+w.dice(18));
            w.tensions[a][b]=w.tensions[b][a]=Math.max(0,22-trust/2);
        }
        // Default tutorial focus: South Korea. All other countries remain selectable.
        for(Nation nation:w.nations)if("KOR".equals(nation.iso3)){w.selected=nation.id;break;}
        java.util.Arrays.fill(w.economyTrend,w.avg("경제"));
        java.util.Arrays.fill(w.peaceTrend,w.peace);
        java.util.Arrays.fill(w.climateTrend,w.climate);
        // The game does NOT assert that these are 2026 diplomatic measurements.
        // International baseline is hypothetical; avoid asserting real conflict.
        w.addEvent("개국","197개 정치 실체가 있는 가상 지구의 역사가 시작됐다. 기본 인구·GDP는 공개 연도별 자료로 초기화됐다.");
        w.addEvent("자료","인구: 세계은행 자료 최대 2025년 / 명목 GDP: 확보 데이터 최대 2023년. 정치·사회·외교 지수는 게임용 가상 수치.");
        return w;
    }

    /**
     * Migrate pre-v2 24-country worlds by ISO identity. Keep prior events,
     * time and player investment instead of deleting the save.
     */
    public static WorldState migrateLegacy(WorldState old){
        if(old.nations.size()==NationCatalog.size())return old;
        if(old.nations.size()!=NationCatalog.LEGACY_ISO.length)
            throw new IllegalArgumentException("Unsupported old country count "+old.nations.size());
        WorldState upgraded=fresh(old.originUtc);
        upgraded.month=old.month;upgraded.random=old.random;
        upgraded.influence=old.influence;
        upgraded.interventionMonth=old.interventionMonth;
        upgraded.interventionsUsed=old.interventionsUsed;
        upgraded.carbon=old.carbon;upgraded.climate=old.climate;
        upgraded.peace=old.peace;upgraded.globalEvents=old.globalEvents;
        upgraded.lastMonthlyDigest=old.lastMonthlyDigest;
        upgraded.economyTrend=old.economyTrend.clone();
        upgraded.peaceTrend=old.peaceTrend.clone();
        upgraded.climateTrend=old.climateTrend.clone();
        upgraded.playerPolicies=old.playerPolicies;
        upgraded.playerDiplomacy=old.playerDiplomacy;
        upgraded.questClaimMask=old.questClaimMask;
        upgraded.lastImpact=old.lastImpact;
        upgraded.lastImpactMonth=old.lastImpactMonth;
        int[] mapping=new int[old.nations.size()];
        for(int i=0;i<mapping.length;i++){
            String code=NationCatalog.LEGACY_ISO[i];
            int at=-1;
            for(int j=0;j<upgraded.nations.size();j++)
                if(code.equals(upgraded.nations.get(j).iso3)){at=j;break;}
            if(at<0)throw new IllegalStateException("Country missing in migration: "+code);
            mapping[i]=at;
            Nation source=old.nations.get(i),dest=upgraded.nations.get(at);
            dest.economy=source.economy;dest.stability=source.stability;
            dest.education=source.education;dest.health=source.health;
            dest.environment=source.environment;dest.food=source.food;
            dest.energy=source.energy;dest.industry=source.industry;
            dest.science=source.science;dest.transport=source.transport;
            dest.welfare=source.welfare;dest.defense=source.defense;
            dest.diplomacy=source.diplomacy;dest.treasury=source.treasury;
            dest.focus=source.focus;dest.lastPolicy=source.lastPolicy;
            dest.plannedPolicy=source.plannedPolicy;dest.plannedMonths=source.plannedMonths;
            dest.plannedStrength=source.plannedStrength;
            dest.technology=source.technology.clone();
            dest.research=source.research.clone();dest.groups=source.groups.clone();
        }
        for(int a=0;a<mapping.length;a++)for(int b=0;b<mapping.length;b++){
            int i=mapping[a],j=mapping[b];
            upgraded.relations[i][j]=old.relations[a][b];
            upgraded.trade[i][j]=old.trade[a][b];
            upgraded.tensions[i][j]=old.tensions[a][b];
            upgraded.pacts[i][j]=old.pacts[a][b];
            upgraded.conflicts[i][j]=old.conflicts[a][b];
        }
        upgraded.selected=mapping[Math.max(0,Math.min(old.selected,mapping.length-1))];
        upgraded.events.clear();upgraded.events.addAll(old.events);
        upgraded.addEvent("시스템","국가 목록을 24개에서 197개로 확장했으며 기존 24개국과 세계 기록을 보존했다.");
        return upgraded;
    }

    public void addEvent(String type,String text){
        events.add(0,new Event(month,type,text));
        if(events.size()>MAX_EVENTS)events.remove(events.size()-1);
        if(!type.equals("월간"))globalEvents++;
    }

    public int dice(int bound) {
        random ^= (random << 13);
        random ^= (random >>> 7);
        random ^= (random << 17);
        return (int)((random>>>1) % Math.max(1,bound));
    }

    public double uniform() {return (dice(1000000)+0.5)/1000000.0;}
    public static int clamp(int v){return Math.min(100,Math.max(0,v));}
    public static int clampSigned(int v){return Math.min(100,Math.max(-100,v));}
    public String date(){int t=2026*12+10+month;return String.format(Locale.KOREA,"%d년 %02d월",t/12,1+t%12);}
    public String dateShort(){int t=2026*12+10+month;return String.format(Locale.KOREA,"%d.%02d",t/12,1+t%12);}
    public String eventDate(int at){int t=2026*12+10+at;return String.format(Locale.KOREA,"%d.%02d",t/12,1+t%12);}
    public long nextMonthAt(){return originUtc+(month+1L)*MONTH_MS;}
    public int tradePacts(){int n=0;for(int a=0;a<nations.size();a++)for(int b=a+1;b<nations.size();b++)if(pacts[a][b]>0)n++;return n;}
    public int conflictsCount(){int n=0;for(int a=0;a<nations.size();a++)for(int b=a+1;b<nations.size();b++)if(conflicts[a][b]>0)n++;return n;}
    public double totalPopulation(){double p=0;for(Nation n:nations)p+=n.population;return p;}
    public int avg(String key){
        int sum=0;
        for(Nation n:nations){
            switch(key){
                case "경제":sum+=n.economy;break;
                case "기술":sum+=n.science;break;
                case "환경":sum+=n.environment;break;
                case "안정":sum+=n.stability;break;
                case "보건":sum+=n.health;break;
                default:sum+=n.education;
            }
        }
        return sum/Math.max(1,nations.size());
    }

    public JSONObject json() throws JSONException {
        JSONObject j=new JSONObject();
        j.put("schema",1);
        j.put("originUtc",originUtc);j.put("month",month);j.put("random",random);
        j.put("influence",influence);j.put("interventionMonth",interventionMonth);
        j.put("interventionsUsed",interventionsUsed);j.put("selected",selected);
        j.put("carbon",carbon);j.put("climate",climate);j.put("peace",peace);
        j.put("globalEvents",globalEvents);j.put("digest",lastMonthlyDigest);
        j.put("economyTrend",arr(economyTrend));
        j.put("peaceTrend",arr(peaceTrend));
        j.put("climateTrend",arr(climateTrend));
        j.put("playerPolicies",playerPolicies);j.put("playerDiplomacy",playerDiplomacy);
        j.put("questClaimMask",questClaimMask);j.put("lastImpact",lastImpact);
        j.put("lastImpactMonth",lastImpactMonth);
        JSONArray ns=new JSONArray();for(Nation n:nations)ns.put(n.json());j.put("nations",ns);
        JSONArray es=new JSONArray();for(Event e:events)es.put(e.json());j.put("events",es);
        j.put("relations",matrix(relations));j.put("trade",matrix(trade));
        j.put("tensions",matrix(tensions));j.put("pacts",matrix(pacts));
        j.put("conflicts",matrix(conflicts));
        return j;
    }

    public static WorldState from(JSONObject j) throws JSONException {
        if(j.optInt("schema",0)!=1)throw new JSONException("Unsupported MYWORLD save format");
        WorldState w=new WorldState();
        w.originUtc=j.getLong("originUtc");w.month=j.getInt("month");
        w.random=j.getLong("random");w.influence=j.getInt("influence");
        w.interventionMonth=j.optInt("interventionMonth",-1);
        w.interventionsUsed=j.optInt("interventionsUsed",0);
        w.selected=j.optInt("selected",0);w.carbon=j.optInt("carbon",41);
        w.climate=j.optInt("climate",67);w.peace=j.optInt("peace",76);
        w.globalEvents=j.optInt("globalEvents",0);
        if(j.has("economyTrend"))w.economyTrend=readArr(j.getJSONArray("economyTrend"),12);
        if(j.has("peaceTrend"))w.peaceTrend=readArr(j.getJSONArray("peaceTrend"),12);
        if(j.has("climateTrend"))w.climateTrend=readArr(j.getJSONArray("climateTrend"),12);
        w.lastMonthlyDigest=j.optString("digest","");
        w.playerPolicies=j.optInt("playerPolicies",0);
        w.playerDiplomacy=j.optInt("playerDiplomacy",0);
        w.questClaimMask=j.optInt("questClaimMask",0);
        w.lastImpact=j.optString("lastImpact","");
        w.lastImpactMonth=j.optInt("lastImpactMonth",-1);
        JSONArray a=j.getJSONArray("nations");
        for(int i=0;i<a.length();i++)w.nations.add(Nation.from(a.getJSONObject(i)));
        if(w.nations.size()!=NationCatalog.size() && w.nations.size()!=NationCatalog.LEGACY_ISO.length)
            throw new JSONException("Incomplete or incompatible country data");
        a=j.getJSONArray("events");
        for(int i=0;i<a.length();i++)w.events.add(Event.from(a.getJSONObject(i)));
        int n=w.nations.size();
        w.relations=readMatrix(j.getJSONArray("relations"),n);
        w.trade=readMatrix(j.getJSONArray("trade"),n);
        w.tensions=readMatrix(j.getJSONArray("tensions"),n);
        w.pacts=readMatrix(j.getJSONArray("pacts"),n);
        w.conflicts=readMatrix(j.getJSONArray("conflicts"),n);
        w.selected=Math.min(n-1,Math.max(0,w.selected));
        return w;
    }
}
