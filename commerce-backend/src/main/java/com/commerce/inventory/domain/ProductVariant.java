package com.commerce.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "product_variant",
        indexes = {
                @Index(name = "idx_product_variant_product_id", columnList = "productId")
        }
)
public class ProductVariant {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "option_name", nullable = false)
    private String optionName;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(nullable = false)
    private int stock;

    public static ProductVariant create(UUID productId, String optionName, long unitPrice, int stock) {
        ProductVariant variant = new ProductVariant();
        variant.productId = productId;
        variant.optionName = optionName;
        variant.unitPrice = unitPrice;
        variant.stock = stock;
        return variant;
    }

    public boolean decrementStock(int quantity) {
        if (stock < quantity) {
            return false;
        }
        stock = stock - quantity;
        return true;
    }
}
