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

package playground.michalm.chargerlocation.audiAV;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.file.ChargerWriter;
import org.matsim.contrib.util.CSVReaders;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import com.google.common.collect.ImmutableList;

import playground.michalm.TaxiBerlin.TaxiBerlinZoneUtils;
import playground.michalm.chargerlocation.ChargerLocationData;
import playground.michalm.chargerlocation.ChargerLocationProblem;
import playground.michalm.chargerlocation.ChargerLocationProblem.ChargerLocationSolution;
import playground.michalm.chargerlocation.ChargerLocationSolutionWriter;
import playground.michalm.chargerlocation.ChargerLocationSolver;
import playground.michalm.chargerlocation.DemandData;

public class AudiAVChargerLocationOptimization {
	// private static final int VEHICLES = 100_000;//100% scenario (ANT'16 paper)
	private static final int VEHICLES = 11_000;// 10% scenario (11% demand and supply)
	// private static final int VEHICLES = 200;//small scenario

	// influences mostly the outskirts
	// private static final double MAX_DISTANCE = 3_000;//[m] 100% scenario
	private static final double MAX_DISTANCE = 5_000;// [m] 10% scenario
	// private static final double MAX_DISTANCE = 30_000;//[m] small scenario

	// high value -> no influence (current approach); low value -> lack of chargers at hot spots
	private static final int MAX_CHARGERS_PER_ZONE = 3000;

	// smaller demand -> larger oversupply
	private static final double OVERSUPPLY = 1.1;

	private static final double SOC_RECHARGE = 12;// kWh, which corresponds to 20%->80% SOC

	// JAIHC'16 paper
	// TODO consult with GS/BO
	private enum EScenario {
		ONLY_DRIVE(5.5, 50), // over-optimistic
		PLUS_20(4, 50), //
		ZERO(3.5, 25), //
		MINUS_20(2.5, 25), // most critical
		FOSSIL_FUEL_MINUS_20(5, 25); // half green solution

		private final double hours;// hours, period during the whole fleet goes 80%->20%
		private final double chargePower_kW;

		private EScenario(double hours, double chargePower) {
			this.hours = hours;
			this.chargePower_kW = chargePower;
		}
	}

	private final EScenario eScenario;

	private ChargerLocationProblem problem;
	private ChargerLocationSolution solution;
	private Network network;

	private final String dir = "../shared-svn/projects/audi_av/";
	private final String zonesXmlFile = dir + "shp/berlin_zones.xml";
	private final String zonesShpFile = dir + "shp/berlin_zones.shp";
	private final String networkFile = dir + "scenario/networkc.xml.gz";
	private final String potentialFile = dir + "scenario/JAIHC_paper/dropoffs_per_link.txt";
	// private final String outDir = "../runs-svn/avsim_time_variant_network/chargers/";
	private final String outDir = "../runs-svn/avsim_time_variant_network/chargers_10pct/";
	// private final String outDir = "../runs-svn/avsim_time_variant_network/chargers_small/";

	public AudiAVChargerLocationOptimization(EScenario eScenario) {
		this.eScenario = eScenario;
	}

	private void createProblem() {
		network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);

		@SuppressWarnings("unchecked")
		Map<Id<Link>, Link> links = (Map<Id<Link>, Link>)network.getLinks();
		DemandData<Link> demandData = createDemandData(links, potentialFile);

		Map<Id<Zone>, Zone> zones = TaxiBerlinZoneUtils.readZones(zonesXmlFile, zonesShpFile);
		ChargerLocationData<Zone> chargerData = new ChargerLocationData<>(ImmutableList.copyOf(zones.values()));

		double totalEnergyRequired = VEHICLES * SOC_RECHARGE;
		problem = new ChargerLocationProblem(demandData, chargerData, DistanceUtils::calculateDistance, eScenario.hours,
				eScenario.chargePower_kW, totalEnergyRequired, OVERSUPPLY, MAX_DISTANCE, MAX_CHARGERS_PER_ZONE);
	}

	private DemandData<Link> createDemandData(Map<Id<Link>, Link> links, String potentialFile) {
		Map<Id<Link>, Double> linkPotentials = new HashMap<>();
		for (String[] r : CSVReaders.readTSV(potentialFile)) {
			linkPotentials.put(Id.createLinkId(r[0]), new Double(r[1]));
		}
		return new DemandData<Link>(links.values(), linkPotentials);
	}

	private void solveProblem() {
		System.err.println("solveProblem() started");
		solution = new ChargerLocationSolver(problem).solve(null);
	}

	private void writeSolution() {
		String name = eScenario.name();
		ChargerLocationSolutionWriter writer = new ChargerLocationSolutionWriter(problem, solution);
		writer.writeChargers(outDir + "chargers_" + problem.maxChargers + "_" + name + ".csv");
		writer.writeFlows(outDir + "flows_" + name + ".csv");

		List<Charger> chargers = writer.generateChargers(network, eScenario.chargePower_kW);
		new ChargerWriter(chargers).write(outDir + "chargers_" + problem.maxChargers + "_" + name + ".xml");
	}

	public static void main(String[] args) {
		for (EScenario es : EScenario.values()) {
			System.err.println("==========================" + es.name());
			AudiAVChargerLocationOptimization optimRunner = new AudiAVChargerLocationOptimization(es);
			optimRunner.createProblem();
			optimRunner.solveProblem();
			optimRunner.writeSolution();
		}
	}
}
