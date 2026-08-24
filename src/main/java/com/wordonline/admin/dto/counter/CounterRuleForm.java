package com.wordonline.admin.dto.counter;

import lombok.Data;

@Data
public class CounterRuleForm {
    private String attackerTagName;
    private String targetTagName;
    private double weight = 1.0;
}
