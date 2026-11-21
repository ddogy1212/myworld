package com.myworld.sim;

import org.junit.Test;
import static org.junit.Assert.*;

/** Pure-Java deterministic engine tests; no device or network required. */
public final class WorldEngineTest {
    private static final long ANCHOR=1700000000000L;

    @Test public void freshWorldContainsDistinctStatesAndValidDiplomacy(){
        WorldState w=WorldState.fresh(ANCHOR);
        assertEquals(197,w.nations.size());
        int members=0,observers=0,specials=0;
        for(WorldState.Nation n:w.nations){
            if("MEMBER".equals(n.sovereignty))members++;
            if("OBSERVER".equals(n.sovereignty))observers++;
            if("SPECIAL".equals(n.sovereignty))specials++;
        }
        assertEquals(193,members);assertEquals(2,observers);assertEquals(2,specials);
        assertEquals(0,w.month);
        assertEquals(16,w.influence);
        assertEquals(ANCHOR,w.originUtc);
        int distinct=0;
        for(int a=0;a<w.nations.size();a++)for(int b=a+1;b<w.nations.size();b++){
            assertEquals(w.relations[a][b],w.relations[b][a]);
            assertEquals(w.tensions[a][b],w.tensions[b][a]);
            if(w.relations[a][b]!=0)distinct++;
        }
        assertTrue("World relations must not be identical",distinct>=1);
    }

    @Test public void oneRealDayAlwaysEqualsOneSimulatedMonth(){
        WorldState w=WorldState.fresh(ANCHOR);
        assertEquals(0,WorldEngine.catchUp(w,ANCHOR+WorldState.MONTH_MS-1));
        assertEquals(1,WorldEngine.catchUp(w,ANCHOR+WorldState.MONTH_MS));
        assertEquals(1,w.month);
        assertEquals(0,WorldEngine.catchUp(w,ANCHOR+WorldState.MONTH_MS));
        assertEquals(11,WorldEngine.catchUp(w,ANCHOR+12*WorldState.MONTH_MS));
        assertEquals(12,w.month);
        assertEquals("2041.01",w.dateShort());
    }

    @Test public void autogovernmentStaysWithinBoundedState(){
        WorldState w=WorldState.fresh(ANCHOR);
        for(int i=0;i<120;i++)WorldEngine.tick(w);
        assertEquals(120,w.month);
        assertTrue(w.events.size()<=260);
        assertTrue(w.totalPopulation()>0);
        for(WorldState.Nation n:w.nations){
            assertTrue(n.population>0 && !Double.isNaN(n.population));
            assertTrue(Double.isFinite(n.treasury));
            assertBetween(n.economy);
            assertBetween(n.stability);assertBetween(n.environment);
            assertBetween(n.education);assertBetween(n.science);
            assertBetween(n.industry);assertBetween(n.health);
            assertBetween(n.food);assertBetween(n.energy);
            assertBetween(n.welfare);assertBetween(n.transport);
            assertBetween(n.diplomacy);assertBetween(n.defense);
            for(int g:n.groups)assertBetween(g);
            for(int t:n.technology)assertTrue(t>=0&&t<=10);
        }
        for(int a=0;a<24;a++)for(int b=a+1;b<24;b++){
            assertEquals(w.relations[a][b],w.relations[b][a]);
            assertEquals(w.tensions[a][b],w.tensions[b][a]);
            assertEquals(w.trade[a][b],w.trade[b][a]);
            assertEquals(w.pacts[a][b],w.pacts[b][a]);
            assertEquals(w.conflicts[a][b],w.conflicts[b][a]);
        }
        assertBetween(w.peace);assertBetween(w.climate);assertBetween(w.carbon);
    }
    private static void assertBetween(int x){assertTrue("Out of range: "+x,x>=0&&x<=100);}

    @Test public void simulationIsDeterministicFromSameState(){
        WorldState a=WorldState.fresh(ANCHOR),b=WorldState.fresh(ANCHOR);
        for(int i=0;i<36;i++){
            WorldEngine.tick(a);WorldEngine.tick(b);
        }
        assertEquals(a.random,b.random);
        assertEquals(a.peace,b.peace);
        assertEquals(a.carbon,b.carbon);
        assertEquals(a.tradePacts(),b.tradePacts());
        assertEquals(a.conflictsCount(),b.conflictsCount());
        assertEquals(a.events.size(),b.events.size());
        for(int i=0;i<a.nations.size();i++){
            assertEquals(a.nations.get(i).economy,b.nations.get(i).economy);
            assertEquals(a.nations.get(i).population,b.nations.get(i).population,0.000001);
        }
    }

    @Test public void playerActionsSpendPointsAndLimitMonthlyIntervention(){
        WorldState w=WorldState.fresh(ANCHOR);
        String changed=WorldEngine.playerPolicy(w,0,0);
        assertTrue(changed.contains(w.nations.get(0).name));
        assertEquals(13,w.influence);
        assertEquals(1,w.interventionsUsed);
        int prior=w.relations[0][1];
        String diplomacy=WorldEngine.playerDiplomacy(w,0,1,1);
        assertTrue(diplomacy.contains(w.nations.get(0).name));
        assertEquals(WorldState.clampSigned(prior+15),w.relations[0][1]);
        assertEquals(w.relations[0][1],w.relations[1][0]);
        assertEquals(11,w.influence);
        assertEquals(2,w.interventionsUsed);
        WorldEngine.playerDiplomacy(w,0,2,1);
        assertEquals(3,w.interventionsUsed);
        String denied=WorldEngine.playerPolicy(w,1,0);
        assertTrue(denied.contains("3회"));
        WorldEngine.tick(w);
        assertEquals(0,w.interventionsUsed);
    }

    @Test public void invalidDiplomacyAndCountryIdsDoNotMutateWorld(){
        WorldState w=WorldState.fresh(ANCHOR);
        int initial=w.influence;
        assertTrue(WorldEngine.playerDiplomacy(w,0,0,1).contains("서로 다른"));
        assertTrue(WorldEngine.playerPolicy(w,-1,0).contains("잘못된"));
        assertEquals(initial,w.influence);
    }
}
