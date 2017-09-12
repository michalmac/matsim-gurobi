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

import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizer;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;

public class MIPTaxiOptimizer extends DefaultTaxiOptimizer {
	private final MIPRequestInserter requestInserter;

	public MIPTaxiOptimizer(TaxiConfigGroup taxiCfg, Fleet fleet, TaxiScheduler scheduler,
			MIPTaxiOptimizerParams params, MIPRequestInserter requestInserter) {
		super(taxiCfg, fleet, scheduler, params, requestInserter);

		this.requestInserter = requestInserter;

		if (!taxiCfg.isDestinationKnown()) {
			throw new IllegalArgumentException("Destinations must be known ahead");
		}
	}

	@Override
	protected boolean doReoptimizeAfterNextTask(TaxiTask newCurrentTask) {
		if (newCurrentTask.getTaxiTaskType() == TaxiTaskType.PICKUP) {
			requestInserter.notifyRequestPickedUp();
			return true;
		}

		return false;
	}
}
