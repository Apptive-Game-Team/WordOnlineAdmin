package com.wordonline.admin.repository.balance;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wordonline.admin.entity.balance.BalanceProposalItem;

public interface BalanceProposalItemRepository extends JpaRepository<BalanceProposalItem, Long> {
}
