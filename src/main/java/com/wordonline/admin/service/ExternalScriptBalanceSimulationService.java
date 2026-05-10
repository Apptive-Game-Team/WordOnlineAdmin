package com.wordonline.admin.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordonline.admin.config.BalanceProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExternalScriptBalanceSimulationService implements BalanceSimulationService {

    private final BalanceProperties balanceProperties;
    private final ObjectMapper objectMapper;

    @Override
    public SimulationExecutionResult executeSimulation(Long parameterProfileId, String matchupSet, int runsPerMatchup) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("parameterProfileId", parameterProfileId);
            payload.put("matchupSet", matchupSet);
            payload.put("runsPerMatchup", runsPerMatchup);

            Path payloadFile = Files.createTempFile("balance-simulation-", ".json");
            Files.writeString(payloadFile, objectMapper.writeValueAsString(payload));

            Process process = new ProcessBuilder(balanceProperties.getSimulationScriptPath(), payloadFile.toString())
                    .redirectErrorStream(true)
                    .start();

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            Files.deleteIfExists(payloadFile);

            if (exitCode != 0) {
                throw new IllegalStateException("Simulation script failed: " + output);
            }

            Map<String, Object> response = objectMapper.readValue(output, new TypeReference<>() {
            });
            Object simulationBatchId = response.get("simulationBatchId");
            if (simulationBatchId == null) {
                simulationBatchId = UUID.randomUUID().toString();
                response.put("simulationBatchId", simulationBatchId);
                output = objectMapper.writeValueAsString(response);
            }
            return new SimulationExecutionResult(String.valueOf(simulationBatchId), output);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to run simulation script", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Simulation script interrupted", e);
        }
    }
}
