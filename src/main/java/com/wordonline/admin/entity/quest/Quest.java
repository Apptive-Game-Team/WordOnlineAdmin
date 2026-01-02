package com.wordonline.admin.entity.quest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quests")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "progress_checker", length = 31)
    private String progressChecker;

    @Column(name = "require_value", nullable = false)
    private Integer requireValue;

    @Column(name = "reward_giver", length = 31)
    private String rewardGiver;
}
