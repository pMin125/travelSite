package com.toyProject.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Getter
public class OrderRequest {
    private List<Long> selectedProductIds;

}
