package com.example.backend.domain.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntonResult {
    private String word;
    private double start;
    private double end;

    @JsonProperty("curve_time")
    private List<Double> curveTime;

    @JsonProperty("curve_pitch")
    private List<Double> curvePitch;
}
