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

import java.util.List;

import org.matsim.contrib.util.CompactCSVWriter;
import org.matsim.core.utils.io.IOUtils;

import playground.michalm.chargerlocation.ChargerLocationProblem.ChargerLocationSolution;


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


    public void writeChargers(String file)
    {
        List<ChargerLocation> locations = problem.chargerData.locations;

        try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file))) {
            for (int j = 0; j < problem.J; j++) {
                if (solution.x[j] > 0) {
                    writer.writeNext(//
                            locations.get(j).getId() + "", //
                            solution.x[j] + "");
                }
            }
        }
    }


    public void writeFlows(String file)
    {
        List<ZoneData.Entry> zoneEntries = problem.zoneData.entries;
        List<ChargerLocation> locations = problem.chargerData.locations;

        try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(file))) {
            for (int i = 0; i < problem.I; i++) {
                for (int j = 0; j < problem.J; j++) {
                    if (solution.f[i][j] >= MIN_FLOW_LIMIT) {
                        writer.writeNext(//
                                zoneEntries.get(i).zone.getId() + "", //
                                locations.get(j).getId() + "", //
                                solution.f[i][j] + "");
                    }
                }
            }
        }
    }
}
