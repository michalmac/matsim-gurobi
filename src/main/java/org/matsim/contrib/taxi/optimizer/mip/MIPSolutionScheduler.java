package org.matsim.contrib.taxi.optimizer.mip;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.router.DijkstraWithThinPath;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.VehicleData;
import org.matsim.contrib.taxi.optimizer.mip.MIPProblem.MIPSolution;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

class MIPSolutionScheduler {
	private final TaxiScheduler scheduler;
	private final TravelTime travelTime;
	private final MIPRequestData rData;
	private final VehicleData vData;

	private final int m;
	private final int n;

	private LeastCostPathCalculator router;

	private MIPSolution solution;
	private Vehicle currentVeh;

	MIPSolutionScheduler(TaxiScheduler scheduler, Network network, TravelTime travelTime,
			TravelDisutility travelDisutility, MIPRequestData rData, VehicleData vData) {
		this.scheduler = scheduler;
		this.travelTime = travelTime;
		this.rData = rData;
		this.vData = vData;
		this.m = vData.getSize();
		this.n = rData.dimension;

		router = new DijkstraWithThinPath(network, travelDisutility, travelTime);
	}

	void updateSchedules(MIPSolution solution) {
		this.solution = solution;

		for (int k = 0; k < m; k++) {
			currentVeh = vData.getEntry(k).vehicle;
			appendSubsequentRequestsToCurrentVehicle(k);
		}
	}

	private void appendSubsequentRequestsToCurrentVehicle(int u) {
		boolean[] x_u = solution.x[u];
		for (int i = 0; i < n; i++) {
			if (x_u[m + i]) {
				appendRequestToCurrentVehicle(i);
				appendSubsequentRequestsToCurrentVehicle(m + i);
				return;
			}
		}
	}

	private void appendRequestToCurrentVehicle(int i) {
		LinkTimePair earliestDeparture = scheduler.getEarliestIdleness(currentVeh);
		TaxiRequest req = rData.requests[i];

		// use earliestDeparture.time instead of w[i]-tt (latest departure time) due to:
		// - possible inaccuracy of the optimization results
		// (if x[.][m+i] = 0.9999 then w[i] < earliestDeparture.time may occur
		// - we want to dispatch vehicles as soon as possible
		// (because tt in MIP are based on the free flow speed estimates, while the actual
		// times are usually longer hence the vehicle is likely to arrive after w[i])
		VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(earliestDeparture.link, req.getFromLink(),
				earliestDeparture.time, router, travelTime);

		scheduler.scheduleRequest(currentVeh, req, path);
	}
}
