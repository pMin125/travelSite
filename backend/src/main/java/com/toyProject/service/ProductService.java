package com.toyProject.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyProject.dto.*;
import com.toyProject.entity.*;
import com.toyProject.exception.ParticipationException;
import com.toyProject.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.stream.Collectors;

import static com.toyProject.exception.ErrorCode.PRODUCT_NOT_FOUND;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ParticipationRepository participationRepository;
    private final TagRepository tagRepository;
    private final TravelQueryRepository travelQueryRepository;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String POPULAR_TRAVEL_KEY = "popular:travel";

    @Autowired
    public ProductService(
            ProductRepository productRepository,
            ParticipationRepository participationRepository,
            TagRepository tagRepository,
            TravelQueryRepository travelQueryRepository,
            ObjectMapper objectMapper,
            StringRedisTemplate stringRedisTemplate
    ) {
        this.productRepository = productRepository;
        this.participationRepository = participationRepository;
        this.tagRepository = tagRepository;
        this.travelQueryRepository = travelQueryRepository;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }
    public ProductDto addProduct(ProductDto dto) {
        Product product = Product.builder()
                .productName(dto.getProductName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .capacity(dto.getCapacity())
                .imageUrl(dto.getImageUrl())
                .createdDate(dto.getCreatedDate())
                .endDate(dto.getEndDate())
                .build();

        Set<Tag> tags = new HashSet<>();
        for (String name : dto.getTagNames()) {
            Tag tag = tagRepository.findByName(name)
                    .orElseGet(() -> tagRepository.save(new Tag(name)));
            tags.add(tag);
        }

        product.setTags(tags);
        Product saved = productRepository.save(product);
        return ProductDto.from(saved);
    }

    public ProductDto  productDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ParticipationException(PRODUCT_NOT_FOUND));

        return ProductDto.from(product);
    }

    public List<PopularTravelDto> getPopularTravels() {
        List<PopularTravelDto> cached = getFromCache();
        if (cached != null) {
            System.out.println("Redis 캐시에서 인기 여행 가져옴");
            return cached;
        }

        List<PopularTravelDto> freshData = getFromDB();
        saveToCache(freshData);
        return freshData;
    }

    private List<PopularTravelDto> getFromCache() {
        try {
            String json = stringRedisTemplate.opsForValue().get(POPULAR_TRAVEL_KEY);
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<>() {});
            }
        } catch (Exception e) {
            System.err.println("캐시 파싱 실패: " + e.getMessage());
        }
        return null;
    }

    private void saveToCache(List<PopularTravelDto> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            stringRedisTemplate.opsForValue().set(POPULAR_TRAVEL_KEY, json);
            System.out.println("DB 조회 후 캐시 저장 완료");
        } catch (Exception e) {
            System.err.println("캐시 저장 실패: " + e.getMessage());
        }
    }

    private List<PopularTravelDto> getFromDB() {
        return travelQueryRepository.findPopularTravels(10);
    }


    public List<ProductDto> productListV2() {
        List<Product> products = productRepository.findAllWithTags();
        Map<Long, Long> joinedCountMap = getJoinedCounts();

        return products.stream()
                .map(product -> convertToDto(product, joinedCountMap))
                .collect(Collectors.toList());
    }

    private Map<Long, Long> getJoinedCounts() {
        List<Object[]> counts = participationRepository.countGroupByProduct(Participation.ParticipationStatus.JOINED);
        return counts.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private ProductDto convertToDto(Product product, Map<Long, Long> joinedCountMap) {
        long joined = joinedCountMap.getOrDefault(product.getId(), 0L);

        List<String> tagNames = product.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toList());

        return new ProductDto(
                product.getId(),
                product.getProductName(),
                product.getDescription(),
                product.getImageUrl(),
                product.getPrice(),
                product.getCapacity(),
                joined,
                product.getCreatedDate(),
                product.getEndDate(),
                tagNames
        );
    }
}
