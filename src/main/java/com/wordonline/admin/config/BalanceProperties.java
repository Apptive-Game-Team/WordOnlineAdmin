package com.wordonline.admin.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "balance")
public class BalanceProperties {

    private int analysisWindowDays = 14;
    private int minMatches = 100;
    private double targetWinRateMin = 0.48d;
    private double targetWinRateMax = 0.52d;
    private double maxChangeRate = 0.10d;
    private int maxParameterEditsPerMagic = 3;
    private String tunableTagName = "BALANCE_TUNABLE";
    private String simulationScriptPath = "scripts/run-balance-simulation.sh";
    private int simulationRunsPerMatchup = 20;
    private String simulationMatchupSet = "default";
    private List<ParameterRule> parameterRules = new ArrayList<>();

    @Getter
    @Setter
    public static class ParameterRule {
        private String parameterName;
        private PowerDirection powerDirection = PowerDirection.HIGHER_IS_STRONGER;
        private int priority = 100;
    }

    public enum PowerDirection {
        HIGHER_IS_STRONGER,
        LOWER_IS_STRONGER
    }
}
