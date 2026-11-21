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
        public String name, region;
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
            j.put("x",mapX);j.put("y",mapY);j.put("population",population);
            j.put("treasury",treasury);j.put("economy",economy);
            j.put("stability",stability);j.put("education",education);
            j.put("health",health);j.put("environment",environment);
            j.put("food",food);j.put("energy",energy);
            j.put("industry",industry);j.put("science",science);
            j.put("transport",transport);j.put("welfare",welfare);
            j.put("defense",defense);j.put("diplomacy",diplomacy);
            j.put("focus",focus); j.put("lastPolicy",lastPolicy);j.put("peace",yearsOfPeace);
            j.put("technology",arr(technology));j.put("research",arr(research));j.put("groups",arr(groups));
            return j;
        }

        public static Nation from(JSONObject j) throws JSONException {
            Nation n=new Nation();
            n.id=j.getInt("id");n.name=j.getString("name");n.region=j.getString("region");
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
    public final ArrayList<Nation> nations=new ArrayList<>();
    public final ArrayList<Event> events=new ArrayList<>();
    public int[][] relations, trade, tensions, pacts, conflicts;
    public String lastMonthlyDigest="";

    private static final String[] SEED_NATIONS = {
            "대한민국|동아시아|864|237|52|78|86|56|78|2",
            "일본|동아시아|897|239|123|78|84|62|80|2",
            "중국|동아시아|797|229|1050|73|76|47|72|0",
            "인도|남아시아|711|285|1180|57|58|50|61|0",
            "인도네시아|동남아시아|816|370|260|63|63|58|74|3",
            "베트남|동남아시아|802|294|100|67|66|59|74|0",
            "미국|북아메리카|185|213|340|83|88|51|76|2",
            "캐나다|북아메리카|184|129|40|79|77|79|85|1",
            "멕시코|북아메리카|171|285|130|62|59|58|68|3",
            "브라질|남아메리카|336|364|210|66|65|63|69|1",
            "아르헨티나|남아메리카|311|439|45|61|60|69|63|3",
            "영국|유럽|473|183|68|79|81|67|80|4",
            "프랑스|유럽|479|211|66|79|80|71|80|4",
            "독일|유럽|506|201|83|83|84|75|82|1",
            "이탈리아|유럽|504|236|59|76|71|68|78|3",
            "스페인|유럽|449|236|48|74|71|73|79|3",
            "러시아|유라시아|670|133|145|68|73|59|66|2",
            "튀르키예|유라시아|568|249|86|65|68|59|66|4",
            "사우디아라비아|중동|610|289|37|76|70|43|75|0",
            "이란|중동|633|265|89|62|68|49|62|2",
            "이집트|아프리카|537|281|112|57|51|41|64|3",
            "나이지리아|아프리카|497|346|230|56|55|45|57|0",
            "남아프리카공화국|아프리카|535|439|63|68|67|54|65|3",
            "호주|오세아니아|869|420|27|81|82|79|84|1"
    };

    public static WorldState fresh(long now) {
        WorldState w=new WorldState();
        w.originUtc=now;w.random=0x6A09E667F3BCC909L;
        for(int i=0;i<SEED_NATIONS.length;i++){
            String[] f=SEED_NATIONS[i].split("\\|");
            Nation n=new Nation();
            n.id=i;n.name=f[0];n.region=f[1];
            n.mapX=Integer.parseInt(f[2]);n.mapY=Integer.parseInt(f[3]);
            n.population=Double.parseDouble(f[4]);
            int e=Integer.parseInt(f[5]), sci=Integer.parseInt(f[6]);
            n.environment=Integer.parseInt(f[7]);n.stability=Integer.parseInt(f[8]);
            n.focus=Integer.parseInt(f[9]);
            n.economy=e;n.science=sci;
            n.education=clamp(sci+w.dice(15)-7);
            n.health=clamp(50+e/3+w.dice(18)-8);
            n.industry=clamp(e+w.dice(23)-11);
            n.transport=clamp(e+w.dice(24)-12);
            n.welfare=clamp(47+e/3+w.dice(22)-11);
            n.energy=clamp(45+w.dice(38));
            n.food=clamp(48+w.dice(39));
            n.defense=clamp(30+e/2+w.dice(17));
            n.diplomacy=clamp(42+w.dice(38));
            n.treasury=20+e*0.35;
            n.lastPolicy=-1;
            for(int t=0;t<TECH_BRANCHES;t++){
                n.technology[t]=Math.max(0,Math.min(10,(sci+w.dice(24)-11)/13));
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
            int rel=clampSigned(15+(x.region.equals(y.region)?13:0)+w.dice(46)-23);
            w.relations[a][b]=w.relations[b][a]=rel;
            w.trade[a][b]=w.trade[b][a]=Math.max(0,12+rel/3+w.dice(18));
            w.tensions[a][b]=w.tensions[b][a]=Math.max(0,22-rel/2);
        }
        // A few deterministic alternate-history rivalries give autonomous
        // negotiation, tension and peacekeeping meaningful initial conditions.
        // Pairings are procedurally selected, not factual geopolitical claims.
        for(int k=0;k<5;k++){
            int left=w.dice(count),right=w.dice(count);
            if(left==right)right=(right+1)%count;
            int rivalry=-36-w.dice(25), tension=56+w.dice(23);
            w.relations[left][right]=w.relations[right][left]=rivalry;
            w.tensions[left][right]=w.tensions[right][left]=tension;
            w.trade[left][right]=w.trade[right][left]=Math.max(1,w.trade[left][right]/3);
        }
        w.addEvent("개국","가상 지구의 새 역사가 시작됐다. 24개 국가가 독립적으로 성장하고 관계를 만들어 간다.");
        w.addEvent("시스템","현실 24시간에 게임 1개월이 흐른다. 국가는 플레이어 접속 없이도 자율적으로 행동한다.");
        return w;
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
    public String date(){return String.format(Locale.KOREA,"%d년 %02d월",2040+month/12,1+month%12);}
    public String dateShort(){return String.format(Locale.KOREA,"%d.%02d",2040+month/12,1+month%12);}
    public String eventDate(int at){return String.format(Locale.KOREA,"%d.%02d",2040+at/12,1+at%12);}
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
        w.lastMonthlyDigest=j.optString("digest","");
        JSONArray a=j.getJSONArray("nations");
        for(int i=0;i<a.length();i++)w.nations.add(Nation.from(a.getJSONObject(i)));
        if(w.nations.size()!=SEED_NATIONS.length)throw new JSONException("Incomplete country data");
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
