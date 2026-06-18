package com.linksnip.analytics.dto;

import java.io.Serializable;
import java.time.LocalDate;

public record DailyClickPoint(LocalDate date, long clicks) implements Serializable {
}
