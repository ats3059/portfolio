package com.commerce.order.domain;

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
        name = "order_product",
        indexes = {
                @Index(name = "idx_order_product_order_vendor_id", columnList = "orderVendorId")
        }
)
public class OrderProduct {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "order_vendor_id", nullable = false)
    private UUID orderVendorId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    public static OrderProduct create(UUID orderVendorId, UUID productId, String productName) {
        OrderProduct orderProduct = new OrderProduct();
        orderProduct.orderVendorId = orderVendorId;
        orderProduct.productId = productId;
        orderProduct.productName = productName;
        return orderProduct;
    }
}
