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

import java.util.Map;
import java.util.SortedSet;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.taxi.data.TaxiRequest;

import com.google.common.collect.Maps;

class MIPRequestData {
	final TaxiRequest[] requests;
	final Map<Id<Request>, Integer> reqIdToIdx;
	final int dimension;

	MIPRequestData(SortedSet<TaxiRequest> unplannedRequests, int planningHorizon) {
		dimension = Math.min(planningHorizon, unplannedRequests.size());
		requests = unplannedRequests.stream().limit(dimension).toArray(TaxiRequest[]::new);

		reqIdToIdx = Maps.newHashMapWithExpectedSize(dimension);
		for (int i = 0; i < dimension; i++) {
			reqIdToIdx.put(requests[i].getId(), i);
		}
	}
}
