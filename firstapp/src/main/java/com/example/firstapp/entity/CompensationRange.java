package com.example.firstapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationRange {

    @Column(name = "compensation_min")
    private Double min;

    @Column(name = "compensation_max")
    private Double max;

    @Builder.Default
    @Column(name = "compensation_currency", nullable = false, length = 10)
    private String currency = "USD";

    @Builder.Default
    @Column(name = "compensation_disclosed", nullable = false)
    private boolean disclosed = true;
}