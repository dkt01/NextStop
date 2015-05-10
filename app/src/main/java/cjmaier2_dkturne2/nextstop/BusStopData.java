package cjmaier2_dkturne2.nextstop;

import java.util.List;

/**
 * Created by David K Turner on 5/5/2015.
 */
public class BusStopData {
    public String name;
    public String stopID;
    public int distance;
    public List<Route> routes;

    BusStopData(String name, String stopID, List<Route> routes, int distance) {
        this.name = name;
        this.stopID = stopID;
        this.routes = routes;
        this.distance = distance;
    }
}
