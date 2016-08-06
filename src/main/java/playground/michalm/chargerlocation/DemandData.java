/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.chargerlocation;

import java.util.*;

import org.matsim.api.core.v01.*;


public class DemandData<D extends BasicLocation<D>>
{
    public static class Entry<D extends BasicLocation<D>>
    {
        public final D location;
        public final double demand;


        public Entry(D location, double demand)
        {
            this.location = location;
            this.demand = demand;
        }
    }


    public final List<Entry<D>> entries = new ArrayList<>();
    public final double totalDemand;


    public DemandData(Iterable<D> locations, Map<Id<D>, Double> locationPotentials)
    {
        double potentialSum = 0;
        for (D l : locations) {
            double p = locationPotentials.get(l.getId());
            entries.add(new Entry<>(l, p));
            potentialSum += p;
        }
        totalDemand = potentialSum;
    }
}
