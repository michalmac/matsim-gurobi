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

import org.matsim.api.core.v01.BasicLocation;

import com.google.common.collect.Lists;


public class ChargerLocationData<C extends BasicLocation<C>>
{
    public final List<C> locations;


    public ChargerLocationData(Iterable<C> basicLocations)
    {
        locations = Lists.newArrayList(basicLocations);
        Collections.sort(locations, (l1, l2) -> l1.getId().compareTo(l2.getId()));//??
    }
}
