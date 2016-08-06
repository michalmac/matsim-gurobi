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


public class ChargerData
{
    public final List<ChargerLocation> locations;
    public final double totalPower;


    public ChargerData(List<ChargerLocation> locations)
    {
        this.locations = locations;
        Collections.sort(locations, (l1, l2) -> l1.getId().compareTo(l2.getId()));

        double powerSum = 0;
        for (ChargerLocation l : locations) {
            powerSum += l.getPower();
        }
        totalPower = powerSum;
    }
}
