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

package playground.michalm.chargerlocation.berlin;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.util.distance.DistanceCalculators;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.utils.io.IOUtils;

import playground.michalm.berlin.BerlinZoneUtils;
import playground.michalm.chargerlocation.*;
import playground.michalm.chargerlocation.ChargerLocationProblem.ChargerLocationSolution;


public class TaxiBerlinChargerLocationOptimization
{
    //TST'15 paper
    private static final double DELTA_SOC = 16;//kWh
    private static final int HOURS = 16;

    private static final int vehicleHours = 32064;//ANT'15 paper (Tue 4am - Wed 4am, 16-17 Apr 2014)
    private static final int vehicleCount = vehicleHours / 16; //each vehicle operates 16 hours (2 shifts)

    //MIP
    //this restrictions influences mostly the outskirts
    private static final double MAX_DISTANCE = 3_000;//m

    //high value -> no influence (the current approach); low value -> lack of chargers at TXL
    private static final int MAX_CHARGERS_PER_ZONE = 30;


    private enum EScenario
    {
        //works with rechargeBeforeEnd=true/false
        //        STANDARD(41.75, 50, 1.5), //
        //        HOT_SUMMER(69.75, 50, 1.2), //
        //        COLD_WINTER(105.75, 25, 1.05), //
        //        COLD_WINTER_FOSSIL_HEATING(41.75, 25, 1.2);

        COLD_WINTER_FOSSIL_HEATING(41.75, 25, 1.1);

        private final double energyPerVehicle;//kWh
        private final double chargePower;//kW
        private final double oversupply; //smaller demand -> larger oversupply (also sensitive to includeDeltaSOC)


        private EScenario(double energyPerVehicle, double chargePower, double oversupply)
        {
            this.energyPerVehicle = energyPerVehicle;
            this.chargePower = chargePower;
            this.oversupply = oversupply;
        }
    }


    //TST'15 paper
    private final boolean rechargeBeforeEnd;
    private final EScenario eScenario;

    private ChargerLocationProblem problem;
    private ChargerLocationSolution solution;

    private final String dir = "d:/eclipse/shared-svn/projects/sustainability-w-michal-and-dlr/data/";
    private final String zonesXmlFile = dir + "shp_merged/berlin_zones.xml";
    private final String zonesShpFile = dir + "shp_merged/berlin_zones.shp";
    private final String potentialFile = dir
            + "taxi_berlin/2014/status/idleVehiclesPerZoneAndHour.txt";


    public TaxiBerlinChargerLocationOptimization(EScenario eScenario, boolean includeDeltaSoc)
    {
        this.eScenario = eScenario;
        this.rechargeBeforeEnd = includeDeltaSoc;

    }


    private void createProblem()
    {
        Map<Id<Zone>, Zone> zones = BerlinZoneUtils.readZones(zonesXmlFile, zonesShpFile);
        DemandData<Zone> demandData = createDemandData(zones, potentialFile);
        ChargerLocationData<Zone> chargerData = new ChargerLocationData<>(zones.values());

        double totalEnergyRequired = vehicleCount
                * Math.max(eScenario.energyPerVehicle - (rechargeBeforeEnd ? 0 : DELTA_SOC), 0);

        problem = new ChargerLocationProblem(demandData, chargerData,
                DistanceCalculators.BEELINE_DISTANCE_CALCULATOR, HOURS, eScenario.chargePower,
                totalEnergyRequired, eScenario.oversupply, MAX_DISTANCE, MAX_CHARGERS_PER_ZONE);
    }


    private DemandData<Zone> createDemandData(Map<Id<Zone>, Zone> zones, String potentialFile)
    {
        Map<Id<Zone>, Double> zonePotentials = new HashMap<>();

        //from: 20140407000000
        //to: 20140413230000 (incl.)
        final int FROM_DAY = 1;//monday
        final int TO_DAY = 4;//thursday (incl.)
        final int FROM_HOUR = 7;
        final int TO_HOUR = 19;//inclusive

        try (Scanner s = new Scanner(IOUtils.getBufferedReader(potentialFile))) {
            while (s.hasNext()) {
                String zoneId = StringUtils.leftPad(s.next(), 8, '0');
                Id<Zone> id = Id.create(zoneId, Zone.class);
                if (!zones.containsKey(id)) {
                    s.nextLine();
                    continue;
                }

                double potential = 0;
                for (int d = 1; d <= 7; d++) {
                    for (int h = 1; h <= 24; h++) {
                        double p = s.nextDouble();
                        if (FROM_DAY <= d && d <= TO_DAY && FROM_HOUR <= h && h <= TO_HOUR) {
                            potential += p;
                        }
                    }
                }

                zonePotentials.put(id, potential);
            }
        }

        return new DemandData<Zone>(zones.values(), zonePotentials);
    }


    private void solveProblem()
    {
        solution = new ChargerLocationSolver(problem).solve(null);
    }


    private void writeSolution()
    {
        String name = eScenario.name() + (rechargeBeforeEnd ? "_noDeltaSOC" : "_DeltaSOC");
        ChargerLocationSolutionWriter writer = new ChargerLocationSolutionWriter(problem, solution);
        writer.writeChargers(dir + "chargers_out_of_" + problem.maxChargers + "_" + name + ".csv");
        writer.writeFlows(dir + "flows_" + name + ".csv");
    }


    public static void main(String[] args)
    {
        for (EScenario es : EScenario.values()) {
            System.err.println("==========================" + es.name());
            boolean rechargeBeforeEnd = !true;
            TaxiBerlinChargerLocationOptimization optimRunner = new TaxiBerlinChargerLocationOptimization(
                    es, rechargeBeforeEnd);
            optimRunner.createProblem();
            optimRunner.solveProblem();
            optimRunner.writeSolution();
        }
    }
}
