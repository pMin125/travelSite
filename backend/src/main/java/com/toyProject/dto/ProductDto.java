package com.toyProject.dto;

import com.toyProject.entity.Product;
import com.toyProject.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private Long id;
    private String productName;
    private String description;
    private String imageUrl;
    private int price;
    private int capacity;
    private long joined;
    private LocalDateTime createdDate;
    private LocalDateTime endDate;
    private List<String> tagNames;

    public static ProductDto from(Product product) {
        List<String> tagNames = product.getTags().stream()
                .map(Tag::getName)
                .toList();

        return new ProductDto(
                product.getId(),
                product.getProductName(),
                product.getDescription(),
                product.getImageUrl(),
                product.getPrice(),
                product.getCapacity(),
                0L,
                product.getCreatedDate(),
                product.getEndDate(),
                tagNames
        );
    }
}
