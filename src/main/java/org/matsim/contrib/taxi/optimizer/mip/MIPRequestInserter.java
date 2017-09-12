/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer.mip;

import java.util.Collection;
import java.util.SortedSet;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.router.DijkstraWithDijkstraTreeCache;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.UnplannedRequestInserter;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

/**
 * @author michalm
 */
public class MIPRequestInserter implements UnplannedRequestInserter {
	private final TaxiConfigGroup taxiCfg;
	private final Network network;
	private final Fleet fleet;
	private final TaxiScheduler scheduler;
	private final MobsimTimer timer;
	private final TravelTime travelTime;
	private final TravelDisutility travelDisutility;

	private final PathTreeBasedTravelTimeCalculator pathTreeTravelTimeCalc;

	private boolean hasPickedUpReqsRecently = false;
	private boolean wasLastPlanningHorizonFull = false;// in order to run optimization for the first request

	private int optimCounter = 0;

	public MIPRequestInserter(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduler scheduler, Network network,
			MobsimTimer timer, TravelTime travelTime, TravelDisutility travelDisutility) {
		this.taxiCfg = taxiCfg;
		this.fleet = fleet;
		this.scheduler = scheduler;
		this.network = network;
		this.timer = timer;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;

		// TODO should they be taken from optimContext????; what to used then in TaxiScheduler?
		TravelTime treeTravelTime = new FreeSpeedTravelTime();
		TravelDisutility treeTravelDisutility = new TimeAsTravelDisutility(treeTravelTime);
		pathTreeTravelTimeCalc = new PathTreeBasedTravelTimeCalculator(new DijkstraWithDijkstraTreeCache(network,
				treeTravelDisutility, treeTravelTime, TimeDiscretizer.CYCLIC_24_HOURS));
	}

	@Override
	public void scheduleUnplannedRequests(Collection<TaxiRequest> unplannedRequests) {
		if (unplannedRequests.isEmpty()) {
			// nothing new to be planned and we want to avoid extra re-planning of what has been
			// already planned (high computational cost while only marginal improvement)
			return;
		}

		if (wasLastPlanningHorizonFull && // last time we planned as many requests as possible, and...
				!hasPickedUpReqsRecently) {// ...since then no empty space has appeared in the planning horizon
			return;
		}

		MIPProblem mipProblem = new MIPProblem(taxiCfg, fleet, scheduler, network, timer, travelTime, travelDisutility,
				pathTreeTravelTimeCalc);
		mipProblem.scheduleUnplannedRequests((SortedSet<TaxiRequest>)unplannedRequests);

		optimCounter++;
		if (optimCounter % 10 == 0) {
			System.err.println(optimCounter + "; time=" + timer.getTimeOfDay());
		}

		wasLastPlanningHorizonFull = mipProblem.isPlanningHorizonFull();
		hasPickedUpReqsRecently = false;
	}

	void notifyRequestPickedUp() {
		hasPickedUpReqsRecently = true;
	}
}
