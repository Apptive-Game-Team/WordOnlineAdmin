package com.wordonline.admin.entity.balance;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.wordonline.admin.entity.parameter.ParameterProfile;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "balance_proposals")
public class BalanceProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BalanceProposalStatus status;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BalanceSimulationStatus simulationStatus;

    @Setter
    @Column(nullable = false)
    private Integer analysisWindowDays;

    @Setter
    private LocalDateTime analysisFrom;

    @Setter
    private LocalDateTime analysisTo;

    @Setter
    @Column(nullable = false)
    private Integer minMatches;

    @Setter
    @Column(nullable = false)
    private Double targetWinRateMin;

    @Setter
    @Column(nullable = false)
    private Double targetWinRateMax;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_profile_id")
    private ParameterProfile simulationProfile;

    @Setter
    private String simulationBatchId;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String simulationSummaryJson;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Setter
    private LocalDateTime approvedAt;

    @Setter
    private LocalDateTime appliedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BalanceProposalItem> items = new ArrayList<>();

    public BalanceProposal(
            BalanceProposalStatus status,
            BalanceSimulationStatus simulationStatus,
            Integer analysisWindowDays,
            LocalDateTime analysisFrom,
            LocalDateTime analysisTo,
            Integer minMatches,
            Double targetWinRateMin,
            Double targetWinRateMax
    ) {
        this.status = status;
        this.simulationStatus = simulationStatus;
        this.analysisWindowDays = analysisWindowDays;
        this.analysisFrom = analysisFrom;
        this.analysisTo = analysisTo;
        this.minMatches = minMatches;
        this.targetWinRateMin = targetWinRateMin;
        this.targetWinRateMax = targetWinRateMax;
    }

    public void addItem(BalanceProposalItem item) {
        items.add(item);
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
