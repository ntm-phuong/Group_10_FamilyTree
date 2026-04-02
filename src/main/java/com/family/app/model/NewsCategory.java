package com.family.app.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NewsCategory {
    EVENT("Sự kiện"),
    ANNOUNCEMENT("Thông báo"),
    HISTORY("Dòng họ"),
    GENERAL("Tin chung");

    private final String label;
}
