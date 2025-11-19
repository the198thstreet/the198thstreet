package com.the198thstreet.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleListController {

    @GetMapping("/getList")
    public Map<String, Object> getList() {
        return Map.of(
            "status", "success",
            "count", 3,
            "items", List.of(
                Map.of(
                    "id", 1,
                    "name", "Crispy Tofu Bowl",
                    "price", 12.50,
                    "tags", List.of("vegan", "gluten-free")
                ),
                Map.of(
                    "id", 2,
                    "name", "Seoul-Style Fried Chicken",
                    "price", 18.00,
                    "tags", List.of("spicy", "shareable")
                ),
                Map.of(
                    "id", 3,
                    "name", "Lavender Lemonade",
                    "price", 6.25,
                    "tags", List.of("drink", "seasonal")
                )
            )
        );
    }
}
