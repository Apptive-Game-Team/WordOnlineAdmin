package com.wordonline.admin.entity.statistic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

@Entity
@Getter
@Table(name = "cards")
public class Card {

    @Id
    private Long id;
    private String name;
    private String cardType;
}
