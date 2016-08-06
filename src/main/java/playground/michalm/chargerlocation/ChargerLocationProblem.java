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

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.contrib.util.distance.DistanceCalculator;


public class ChargerLocationProblem
{
    public static class ChargerLocationSolution
    {
        public final int[] x;
        public final double[][] f;


        ChargerLocationSolution(int[] x, double[][] f)
        {
            this.x = x;
            this.f = f;
        }
    }


    public final DemandData<?> demandData;
    public final ChargerLocationData<?> chargerData;
    
    public final double potentialSatisfiedByCharger;

    public final double maxDistance;
    public final int maxChargersAtLocation;
    public final int maxChargers;

    public final int I;
    public final int J;

    public final double[][] distances;


    public ChargerLocationProblem(DemandData<?> demandData, ChargerLocationData<?> chargerData,
            DistanceCalculator distanceCalculator, double hours, double chargePower,
            double totalEnergyRequired, double oversupply, double maxDistance,
            int maxChargersAtLocation)
    {
        this.demandData = demandData;
        this.chargerData = chargerData;
        
        double energyProducedByCharger = hours * chargePower;
        double energyRequiredPerPotential = totalEnergyRequired / demandData.totalDemand;
        potentialSatisfiedByCharger = energyProducedByCharger / energyRequiredPerPotential;

        this.maxChargersAtLocation = maxChargersAtLocation;
        this.maxDistance = maxDistance;
        maxChargers = (int)Math.ceil(oversupply * totalEnergyRequired / energyProducedByCharger);

        I = demandData.entries.size();
        J = chargerData.locations.size();

        distances = calcDistanceMatrix(distanceCalculator);
    }


    private double[][] calcDistanceMatrix(DistanceCalculator distanceCalculator)
    {
        double[][] d = new double[I][J];
        for (int i = 0; i < I; i++) {
            BasicLocation<?> demandLocation_i = demandData.entries.get(i).location;

            for (int j = 0; j < J; j++) {
                BasicLocation<?> location_j = chargerData.locations.get(j);
                d[i][j] = distanceCalculator.calcDistance(demandLocation_i.getCoord(),
                        location_j.getCoord());
            }
        }
        return d;
    }
}
