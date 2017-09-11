/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.data.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder;
import org.matsim.contrib.taxi.optimizer.VehicleData;
import org.matsim.contrib.taxi.optimizer.fifo.FifoSchedulingProblem;
import org.matsim.contrib.taxi.optimizer.mip.MIPProblem.MIPSolution;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiSchedules;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.common.collect.Iterables;

class MIPSolutionFinder {
	private final TaxiConfigGroup taxiCfg;
	private final TaxiScheduler scheduler;
	private final Fleet fleet;
	private final Network network;
	private final MobsimTimer mobsimTimer;
	private final TravelTime travelTime;
	private final TravelDisutility travelDisutility;
	private final MIPRequestData rData;
	private final VehicleData vData;

	MIPSolutionFinder(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduler scheduler, Network network,
			MobsimTimer mobsimTimer, TravelTime travelTime, TravelDisutility travelDisutility, MIPRequestData rData,
			VehicleData vData) {
		this.taxiCfg = taxiCfg;
		this.scheduler = scheduler;
		this.fleet = fleet;
		this.network = network;
		this.mobsimTimer = mobsimTimer;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.rData = rData;
		this.vData = vData;
	}

	MIPSolution findInitialSolution() {
		final int m = vData.getSize();
		final int n = rData.dimension;

		final boolean[][] x = new boolean[m + n][m + n];
		final double[] w = new double[n];

		Queue<TaxiRequest> queue = new PriorityQueue<>(n, Requests.T0_COMPARATOR);
		Collections.addAll(queue, rData.requests);

		BestDispatchFinder dispatchFinder = new BestDispatchFinder(scheduler, network, mobsimTimer, travelTime,
				travelDisutility);
		new FifoSchedulingProblem(fleet, scheduler, dispatchFinder).scheduleUnplannedRequests(queue);

		double t_P = taxiCfg.getPickupDuration();

		for (int k = 0; k < m; k++) {
			Schedule schedule = vData.getEntry(k).vehicle.getSchedule();
			Iterable<TaxiRequest> reqs = TaxiSchedules.getTaxiRequests(schedule);
			Iterable<TaxiRequest> plannedReqs = Iterables.filter(reqs,
					req -> req.getStatus() == TaxiRequestStatus.PLANNED);

			int u = k;
			for (TaxiRequest r : plannedReqs) {
				int i = rData.reqIdToIdx.get(r.getId());
				int v = m + i;

				x[u][v] = true;

				double w_i = r.getPickupTask().getEndTime() - t_P;
				w[i] = w_i;

				u = v;
			}

			x[u][k] = true;
		}

		return new MIPSolution(x, w);
	}
}
