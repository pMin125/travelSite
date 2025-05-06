package com.toyProject.entity;

import java.io.Serializable;
import java.util.List;

public class ExpiringProductsMessage implements Serializable {
    private String userEmail;
    private List<Product> expiringProducts;

    public ExpiringProductsMessage() {}

    public ExpiringProductsMessage(String userEmail, List<Product> expiringProducts) {
        this.userEmail = userEmail;
        this.expiringProducts = expiringProducts;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public List<Product> getExpiringProducts() {
        return expiringProducts;
    }
}
