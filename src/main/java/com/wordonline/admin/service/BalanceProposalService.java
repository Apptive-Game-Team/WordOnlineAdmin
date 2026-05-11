package com.wordonline.admin.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordonline.admin.client.GameServerClient;
import com.wordonline.admin.config.BalanceProperties;
import com.wordonline.admin.config.BalanceProperties.ParameterRule;
import com.wordonline.admin.config.BalanceProperties.PowerDirection;
import com.wordonline.admin.entity.balance.BalanceProposal;
import com.wordonline.admin.entity.balance.BalanceProposalItem;
import com.wordonline.admin.entity.balance.BalanceProposalStatus;
import com.wordonline.admin.entity.balance.BalanceSimulationStatus;
import com.wordonline.admin.entity.magic.Card;
import com.wordonline.admin.entity.magic.Magic;
import com.wordonline.admin.entity.magic.MagicCard;
import com.wordonline.admin.entity.parameter.GameObject;
import com.wordonline.admin.entity.parameter.Parameter;
import com.wordonline.admin.entity.parameter.ParameterProfile;
import com.wordonline.admin.entity.parameter.ParameterValue;
import com.wordonline.admin.entity.statistic.StatisticGame;
import com.wordonline.admin.entity.statistic.StatisticGameCard;
import com.wordonline.admin.entity.statistic.StatisticGameMagic;
import com.wordonline.admin.repository.balance.BalanceProposalRepository;
import com.wordonline.admin.repository.magic.MagicRepository;
import com.wordonline.admin.repository.parameter.ParameterProfileRepository;
import com.wordonline.admin.repository.parameter.ParameterValueRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BalanceProposalService {

    private final BalanceProposalRepository balanceProposalRepository;
    private final MagicRepository magicRepository;
    private final ParameterProfileRepository parameterProfileRepository;
    private final ParameterValueRepository parameterValueRepository;
    private final StatisticService statisticService;
    private final BalanceSimulationService balanceSimulationService;
    private final GameServerClient gameServerClient;
    private final BalanceProperties balanceProperties;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    public List<BalanceProposal> getAllProposals() {
        return balanceProposalRepository.findAllByOrderByCreatedAtDesc();
    }

    public BalanceProposal getProposal(Long proposalId) {
        return balanceProposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Balance proposal not found"));
    }

    public BalanceProposal generateProposal() {
        LocalDateTime analysisTo = LocalDateTime.now();
        LocalDateTime analysisFrom = analysisTo.minusDays(balanceProperties.getAnalysisWindowDays());

        BalanceProposal proposal = balanceProposalRepository.save(new BalanceProposal(
                BalanceProposalStatus.DRAFT,
                BalanceSimulationStatus.PENDING,
                balanceProperties.getAnalysisWindowDays(),
                analysisFrom,
                analysisTo,
                balanceProperties.getMinMatches(),
                balanceProperties.getTargetWinRateMin(),
                balanceProperties.getTargetWinRateMax()
        ));

        List<StatisticGame> statisticGames = statisticService.getLiveStatisticGames(null, analysisFrom);
        Map<Long, List<StatisticGameMagic>> magicEntriesByMagicId = statisticGames.stream()
                .flatMap(statisticGame -> statisticGame.getStatisticGameMagics().stream())
                .collect(Collectors.groupingBy(statisticGameMagic -> statisticGameMagic.getMagic().getId()));
        Map<Long, Integer> cardUsageCounts = statisticGames.stream()
                .flatMap(statisticGame -> statisticGame.getStatisticGameCards().stream())
                .collect(Collectors.toMap(
                        statisticGameCard -> statisticGameCard.getCard().getId(),
                        StatisticGameCard::getCount,
                        Integer::sum
                ));

        Map<String, ParameterRule> rulesByName = balanceProperties.getParameterRules().stream()
                .filter(rule -> rule.getParameterName() != null && !rule.getParameterName().isBlank())
                .collect(Collectors.toMap(ParameterRule::getParameterName, Function.identity(), (left, right) -> left));

        List<Magic> magics = magicRepository.findAllBy();
        for (Magic magic : magics) {
            List<StatisticGameMagic> entries = magicEntriesByMagicId.getOrDefault(magic.getId(), List.of());
            int matches = entries.size();
            if (matches < balanceProperties.getMinMatches()) {
                continue;
            }

            long wins = entries.stream()
                    .filter(entry -> entry.getStatisticGame().getWinUserId() != null
                            && entry.getStatisticGame().getWinUserId().equals(entry.getUserId()))
                    .count();
            double winRate = (double) wins / matches;
            if (winRate >= balanceProperties.getTargetWinRateMin()
                    && winRate <= balanceProperties.getTargetWinRateMax()) {
                continue;
            }

            List<CandidateParameter> candidates = collectCandidates(magic, cardUsageCounts, rulesByName);
            if (candidates.isEmpty()) {
                continue;
            }

            double nearestBandEdge = winRate > balanceProperties.getTargetWinRateMax()
                    ? balanceProperties.getTargetWinRateMax()
                    : balanceProperties.getTargetWinRateMin();
            double changeRate = Math.min(
                    balanceProperties.getMaxChangeRate(),
                    Math.abs(winRate - nearestBandEdge) * 2.0d
            );
            if (changeRate <= 0) {
                continue;
            }

            List<CandidateParameter> selectedCandidates = candidates.stream()
                    .limit(balanceProperties.getMaxParameterEditsPerMagic())
                    .toList();
            for (CandidateParameter candidate : selectedCandidates) {
                double proposedValue = adjustValue(candidate.parameterValue().getValue(), changeRate, candidate.rule(), winRate);
                String reason = buildReason(winRate, matches, candidate.parameterValue().getParameter().getName());
                String metricsJson = toJson(Map.of(
                        "magicId", magic.getId(),
                        "matches", matches,
                        "wins", wins,
                        "winRate", winRate,
                        "targetMin", balanceProperties.getTargetWinRateMin(),
                        "targetMax", balanceProperties.getTargetWinRateMax()
                ));
                BalanceProposalItem item = new BalanceProposalItem(
                        proposal,
                        magic.getId(),
                        magic.getName(),
                        candidate.card().getId(),
                        candidate.card().getName(),
                        candidate.gameObject().getId(),
                        candidate.gameObject().getName(),
                        candidate.parameterValue().getId(),
                        candidate.parameterValue().getParameter().getId(),
                        candidate.parameterValue().getParameter().getName(),
                        candidate.parameterValue().getValue(),
                        proposedValue,
                        changeRate,
                        reason,
                        metricsJson
                );
                proposal.addItem(item);
            }
        }

        proposal = balanceProposalRepository.save(proposal);
        ParameterProfile simulationProfile = ensureSimulationProfile(proposal);
        proposal.setSimulationProfile(simulationProfile);

        try {
            BalanceSimulationService.SimulationExecutionResult simulationResult = balanceSimulationService.executeSimulation(
                    simulationProfile.getId(),
                    balanceProperties.getSimulationMatchupSet(),
                    balanceProperties.getSimulationRunsPerMatchup()
            );
            proposal.setSimulationBatchId(simulationResult.simulationBatchId());
            proposal.setSimulationStatus(BalanceSimulationStatus.SUCCEEDED);
            proposal.setSimulationSummaryJson(buildSimulationSummary(simulationResult));
        } catch (RuntimeException e) {
            proposal.setSimulationStatus(BalanceSimulationStatus.FAILED);
            proposal.setSimulationSummaryJson(toJson(Map.of(
                    "error", e.getMessage(),
                    "failedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            )));
        }

        return balanceProposalRepository.save(proposal);
    }

    public BalanceProposal approve(Long proposalId) {
        BalanceProposal proposal = getProposal(proposalId);
        if (proposal.getStatus() != BalanceProposalStatus.DRAFT) {
            throw new IllegalStateException("Only draft proposals can be approved");
        }
        if (proposal.getSimulationStatus() != BalanceSimulationStatus.SUCCEEDED) {
            throw new IllegalStateException("Simulation must succeed before approval");
        }
        proposal.setStatus(BalanceProposalStatus.APPROVED);
        proposal.setApprovedAt(LocalDateTime.now());
        return balanceProposalRepository.save(proposal);
    }

    public BalanceProposal reject(Long proposalId, String reason) {
        BalanceProposal proposal = getProposal(proposalId);
        if (proposal.getStatus() != BalanceProposalStatus.DRAFT) {
            throw new IllegalStateException("Only draft proposals can be rejected");
        }
        proposal.setStatus(BalanceProposalStatus.REJECTED);
        proposal.setRejectionReason(reason == null ? "" : reason);
        return balanceProposalRepository.save(proposal);
    }

    public BalanceProposal apply(Long proposalId) {
        BalanceProposal proposal = getProposal(proposalId);
        if (proposal.getStatus() != BalanceProposalStatus.APPROVED) {
            throw new IllegalStateException("Only approved proposals can be applied");
        }
        if (proposal.getAppliedAt() != null) {
            throw new IllegalStateException("Proposal already applied");
        }

        for (BalanceProposalItem item : proposal.getItems()) {
            ParameterValue parameterValue = parameterValueRepository.findById(item.getParameterValueId())
                    .orElseThrow(() -> new IllegalStateException("Parameter value not found: " + item.getParameterValueId()));
            double beforeValue = parameterValue.getValue();
            parameterValue.setValue(item.getProposedValue());
            item.setApplySnapshotJson(toJson(Map.of(
                    "before", beforeValue,
                    "after", item.getProposedValue(),
                    "appliedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            )));
        }

        proposal.setStatus(BalanceProposalStatus.APPLIED);
        proposal.setAppliedAt(LocalDateTime.now());
        gameServerClient.invalidateCache();
        return balanceProposalRepository.save(proposal);
    }

    private ParameterProfile ensureSimulationProfile(BalanceProposal proposal) {
        ParameterProfile defaultProfile = parameterProfileRepository.findByIsDefaultTrue()
                .orElseGet(() -> parameterProfileRepository.save(
                        new ParameterProfile("default", null, "Default live parameters", true)
                ));

        ParameterProfile simulationProfile = parameterProfileRepository.save(new ParameterProfile(
                "balance-proposal-" + proposal.getId() + "-sim",
                defaultProfile,
                "Simulation profile for proposal " + proposal.getId(),
                false
        ));

        List<ParameterValue> overrides = proposal.getItems().stream()
                .map(item -> new ParameterValue(
                        item.getProposedValue(),
                        entityManager.getReference(GameObject.class, item.getGameObjectId()),
                        entityManager.getReference(Parameter.class, item.getParameterId()),
                        simulationProfile
                ))
                .toList();
        parameterValueRepository.saveAll(overrides);
        return simulationProfile;
    }

    private String buildSimulationSummary(BalanceSimulationService.SimulationExecutionResult simulationResult) {
        String rawOutput = simulationResult.rawOutput();
        try {
            UUID batchId = simulationResult.simulationBatchId() == null ? null : UUID.fromString(simulationResult.simulationBatchId());
            if (batchId == null) {
                return rawOutput;
            }
            List<StatisticGame> simulationGames = statisticService.getSimulationStatisticGames(batchId);
            Map<Long, List<StatisticGameMagic>> magicEntries = simulationGames.stream()
                    .flatMap(game -> game.getStatisticGameMagics().stream())
                    .collect(Collectors.groupingBy(entry -> entry.getMagic().getId()));

            List<Map<String, Object>> magicResults = new ArrayList<>();
            for (Map.Entry<Long, List<StatisticGameMagic>> entry : magicEntries.entrySet()) {
                List<StatisticGameMagic> values = entry.getValue();
                long wins = values.stream()
                        .filter(value -> value.getStatisticGame().getWinUserId() != null
                                && value.getStatisticGame().getWinUserId().equals(value.getUserId()))
                        .count();
                magicResults.add(Map.of(
                        "magicId", entry.getKey(),
                        "magicName", values.getFirst().getMagic().getName(),
                        "matches", values.size(),
                        "wins", wins,
                        "winRate", values.isEmpty() ? 0d : (double) wins / values.size()
                ));
            }

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("simulationBatchId", simulationResult.simulationBatchId());
            summary.put("gameCount", simulationGames.size());
            summary.put("magicResults", magicResults);
            summary.put("rawOutput", rawOutput);
            return toJson(summary);
        } catch (IllegalArgumentException ignored) {
            return rawOutput;
        }
    }

    private List<CandidateParameter> collectCandidates(
            Magic magic,
            Map<Long, Integer> cardUsageCounts,
            Map<String, ParameterRule> rulesByName
    ) {
        return magic.getMagicCards().stream()
                .map(MagicCard::getCard)
                .filter(card -> card.getGameObject() != null)
                .filter(card -> hasTunableTag(card.getGameObject()))
                .sorted(Comparator.comparingInt((Card card) -> cardUsageCounts.getOrDefault(card.getId(), 0)).reversed())
                .flatMap(card -> card.getGameObject().getParameterValues().stream()
                        .filter(this::isDefaultParameterValue)
                        .filter(parameterValue -> rulesByName.containsKey(parameterValue.getParameter().getName()))
                        .map(parameterValue -> new CandidateParameter(
                                card,
                                card.getGameObject(),
                                parameterValue,
                                rulesByName.get(parameterValue.getParameter().getName())
                        )))
                .sorted(Comparator.comparingInt((CandidateParameter candidate) -> candidate.rule().getPriority())
                        .thenComparing(candidate -> candidate.parameterValue().getParameter().getName()))
                .toList();
    }

    private boolean isDefaultParameterValue(ParameterValue parameterValue) {
        ParameterProfile profile = parameterValue.getParameterProfile();
        return profile == null || profile.isDefault();
    }

    private boolean hasTunableTag(GameObject gameObject) {
        return gameObject.getGameObjectTags().stream()
                .map(gameObjectTag -> gameObjectTag.getTag().getName())
                .anyMatch(balanceProperties.getTunableTagName()::equals);
    }

    private double adjustValue(double currentValue, double changeRate, ParameterRule rule, double winRate) {
        boolean nerf = winRate > balanceProperties.getTargetWinRateMax();
        boolean increase = switch (rule.getPowerDirection()) {
            case HIGHER_IS_STRONGER -> !nerf;
            case LOWER_IS_STRONGER -> nerf;
        };
        double multiplier = increase ? 1 + changeRate : 1 - changeRate;
        return Math.max(0d, currentValue * multiplier);
    }

    private String buildReason(double winRate, int matches, String parameterName) {
        String direction = winRate > balanceProperties.getTargetWinRateMax() ? "nerf" : "buff";
        return String.format(
                "Magic is outside target band at %.2f%% over %d matches. %s parameter %s.",
                winRate * 100,
                matches,
                direction,
                parameterName
        );
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize payload", e);
        }
    }

    private record CandidateParameter(
            Card card,
            GameObject gameObject,
            ParameterValue parameterValue,
            ParameterRule rule
    ) {
    }
}
