package cjmaier2_dkturne2.nextstop;

import android.location.Location;

import java.util.List;

/**
 * Created by David K Turner on 5/10/2015.
 */
public class RoutePath {
    private List<Location> waypoints;
    private Location curPos;
    private int curPreIdx; // Stores index of waypoints before curPos
    private final int MAXERROR = 1000; // Maximum correction of dead reckoning error in meters
    private final String tripID;

    public RoutePath(List<Location> waypoints, Location start, String tripID) {
        this.waypoints = waypoints;
        this.curPos = start;
        this.curPreIdx = getPreIdx(start);
        this.tripID = tripID;
    }

    /* Calculates distance along the route path from current position to end.
     * Assumes start occurs before end along path.
     */
    public float distanceAlongRoute(Location end) {
        float retval = 0;
        int postIdx;
        postIdx = getPreIdx(end) + 1;
        if(postIdx == 0)
            return -1;
        for(int i = curPreIdx; i < postIdx; i++) {
            if(i == curPreIdx && i+1 == postIdx)
                return curPos.distanceTo(end);
            if(i == curPreIdx)
                retval += curPos.distanceTo(waypoints.get(i+1));
            else if(i+1 == postIdx)
                retval += end.distanceTo(waypoints.get(i));
            else
                retval += waypoints.get(i).distanceTo(waypoints.get(i+1));
        }
        return retval;
    }

    private int getPreIdx(Location target) {
        float distAB, distBC, distAC;
        for(int i = 0; i < waypoints.size()-1; i++)
        {
            distAB = waypoints.get(i).distanceTo(target);
            distBC = target.distanceTo(waypoints.get(i+1));
            distAC = waypoints.get(i).distanceTo(waypoints.get(i+1));
            if(distAB+distBC-distAC <= 0.01)
                return i;
        }
        return -1;
    }

    public Location getLocation() {
        return curPos;
    }

    public String getTripID() {
        return tripID;
    }

    public Location moveAlongPath(float dist, float bearing) {
        float dist_left = dist;
        Location end_pos = new Location(curPos);
        float distRatio;
        double lat,lon;
        int i = curPreIdx;
        while(dist_left > 0) {
            if(end_pos.distanceTo(waypoints.get(i+1)) <= dist) {
                dist_left -= end_pos.distanceTo(waypoints.get(i+1));
                end_pos = waypoints.get(i + 1);
                i++;
            }
            else {
                distRatio = dist_left / end_pos.distanceTo(waypoints.get(i+1));
                lat = end_pos.getLatitude();
                lat += (waypoints.get(i+1).getLatitude()-lat)*distRatio;
                lon = end_pos.getLongitude();
                lon += (waypoints.get(i+1).getLongitude()-lon)*distRatio;
                end_pos.setLatitude(lat);
                end_pos.setLongitude(lon);
                dist_left = 0;
            }
        }
        if(Math.abs(end_pos.bearingTo(waypoints.get(i+1)) - bearing) < 40){
            curPos = end_pos;
            curPreIdx = i;
            return curPos;
        }
        dist_left = 0;
        i = curPreIdx;
        /* Try to find where turn occured to reset the position */
        while (Math.abs(dist_left - dist) < MAXERROR) {
            if(Math.abs(waypoints.get(i).bearingTo(waypoints.get(i+1)) - bearing) < 40) {
                if(i == curPreIdx) {
                    curPos = waypoints.get(i+1);
                    curPreIdx += 1;
                    return curPos;
                }
                curPos = waypoints.get(i);
                curPreIdx = i;
                return curPos;
            }
            if(i == curPreIdx)
                dist_left += curPos.distanceTo(waypoints.get(i+1));
            else
                dist_left += waypoints.get(i).distanceTo(waypoints.get(i+1));
            i++;
        }
        /* No suitable reset within margin of error.
         * return null to indicate this may not be a good route candidate.
         */
        return null;
    }
}
