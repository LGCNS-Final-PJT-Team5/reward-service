package com.modive.rewardservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageInfoDTO {
    private int page;
    private int pageSize;
    private long totalElements;
    private int totalPages;
}