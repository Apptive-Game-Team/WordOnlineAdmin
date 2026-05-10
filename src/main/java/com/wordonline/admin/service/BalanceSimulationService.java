package com.wordonline.admin.service;

public interface BalanceSimulationService {

    SimulationExecutionResult executeSimulation(Long parameterProfileId, String matchupSet, int runsPerMatchup);

    record SimulationExecutionResult(
            String simulationBatchId,
            String rawOutput
    ) {
    }
}
