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

import org.matsim.api.core.v01.*;


public class SimpleLocation
    implements BasicLocation<SimpleLocation>
{
    public static SimpleLocation create(long id, double x, double y)
    {
        return new SimpleLocation(Id.create(id, SimpleLocation.class), new Coord(x, y));
    }


    private final Id<SimpleLocation> id;
    private final Coord coord;


    public SimpleLocation(Id<SimpleLocation> id, Coord coord)
    {
        this.id = id;
        this.coord = coord;
    }


    @Override
    public Id<SimpleLocation> getId()
    {
        return id;
    }


    @Override
    public Coord getCoord()
    {
        return coord;
    }
}
