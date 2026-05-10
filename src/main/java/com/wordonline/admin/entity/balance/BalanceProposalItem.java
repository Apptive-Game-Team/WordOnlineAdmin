package com.wordonline.admin.entity.balance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "balance_proposal_items")
public class BalanceProposalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false)
    private BalanceProposal proposal;

    private Long magicId;
    private String magicName;
    private Long cardId;
    private String cardName;
    private Long gameObjectId;
    private String gameObjectName;
    private Long parameterValueId;
    private Long parameterId;
    private String parameterName;
    private Double currentValue;
    private Double proposedValue;
    private Double changeRate;

    @Setter
    private String reason;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String liveMetricsJson;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String applySnapshotJson;

    public BalanceProposalItem(
            BalanceProposal proposal,
            Long magicId,
            String magicName,
            Long cardId,
            String cardName,
            Long gameObjectId,
            String gameObjectName,
            Long parameterValueId,
            Long parameterId,
            String parameterName,
            Double currentValue,
            Double proposedValue,
            Double changeRate,
            String reason,
            String liveMetricsJson
    ) {
        this.proposal = proposal;
        this.magicId = magicId;
        this.magicName = magicName;
        this.cardId = cardId;
        this.cardName = cardName;
        this.gameObjectId = gameObjectId;
        this.gameObjectName = gameObjectName;
        this.parameterValueId = parameterValueId;
        this.parameterId = parameterId;
        this.parameterName = parameterName;
        this.currentValue = currentValue;
        this.proposedValue = proposedValue;
        this.changeRate = changeRate;
        this.reason = reason;
        this.liveMetricsJson = liveMetricsJson;
    }
}
