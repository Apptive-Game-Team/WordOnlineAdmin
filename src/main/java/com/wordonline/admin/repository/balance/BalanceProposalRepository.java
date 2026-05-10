package com.wordonline.admin.repository.balance;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.balance.BalanceProposal;

public interface BalanceProposalRepository extends JpaRepository<BalanceProposal, Long> {

    @EntityGraph(attributePaths = {
            "items",
            "simulationProfile"
    })
    List<BalanceProposal> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {
            "items",
            "simulationProfile"
    })
    Optional<BalanceProposal> findById(Long id);
}
