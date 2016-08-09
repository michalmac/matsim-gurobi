/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.network.*;
import org.matsim.contrib.util.CompactCSVWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.io.IOUtils;

import playground.michalm.chargerlocation.ChargerLocationProblem.ChargerLocationSolution;
import playground.michalm.ev.EvUnitConversions;
import playground.michalm.ev.data.*;


public class ChargerLocationSolutionWriter
{
    private static final double MIN_FLOW_LIMIT = 1e-2;

    private final ChargerLocationProblem problem;
    private final ChargerLocationSolution solution;


    public ChargerLocationSolutionWriter(ChargerLocationProblem problem,
            ChargerLocationSolution solution)
    {
        this.problem = problem;
        this.solution = solution;
    }

    
    public List<Charger> generateChargers(Network network, double chargePower)
    {
        return generateChargers(network, new IdentityTransformation(), chargePower);
    }


    public List<Charger> generateChargers(Network network,
            CoordinateTransformation locToNetCoordTransform, double chargePower)
    {
        List<Charger> chargers = new ArrayList<>(problem.maxChargers);
        for (int j = 0; j < problem.J; j++) {
            if (solution.x[j] > 0.5) {
                BasicLocation<?> loc = problem.chargerData.locations.get(j);
                Id<Charger> id = Id.create(loc.getId(), Charger.class);
                double power = chargePower * EvUnitConversions.W_PER_kW;
                int plugs = (int)Math.round(solution.x[j]);
                Coord coord = locToNetCoordTransform.transform(loc.getCoord());
                Link link = NetworkUtils.getNearestLink(network, coord);
                chargers.add(new ChargerImpl(id, power, plugs, link));
            }
        }
        return chargers;
    }


    public void writeChargers(String file)
    {
        List<? extends BasicLocation<?>> locations = problem.chargerData.locations;

        try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file))) {
            for (int j = 0; j < problem.J; j++) {
                if (solution.x[j] > 0.5) {
                    writer.writeNext(//
                            locations.get(j).getId() + "", //
                            solution.x[j] + "");
                }
            }
        }
    }


    public void writeFlows(String file)
    {
        List<? extends DemandData.Entry<?>> demandEntries = problem.demandData.entries;
        List<? extends BasicLocation<?>> chargerLocations = problem.chargerData.locations;

        try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file))) {
            for (int i = 0; i < problem.I; i++) {
                for (int j = 0; j < problem.J; j++) {
                    if (solution.f[i][j] >= MIN_FLOW_LIMIT) {
                        writer.writeNext(//
                                demandEntries.get(i).location.getId() + "", //
                                chargerLocations.get(j).getId() + "", //
                                solution.f[i][j] + "");
                    }
                }
            }
        }
    }
}
