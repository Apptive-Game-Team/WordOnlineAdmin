package com.wordonline.admin.entity.statistic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "magics")
public class Magic {

    @Id
    private Long id;
    private String name;
}
