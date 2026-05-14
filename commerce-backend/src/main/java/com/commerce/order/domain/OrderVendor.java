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
        name = "order_vendor",
        indexes = {
                @Index(name = "idx_order_vendor_order_id", columnList = "orderId")
        }
)
public class OrderVendor {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "vendor_name", nullable = false)
    private String vendorName;

    @Column(name = "delivery_fee", nullable = false)
    private long deliveryFee;

    public static OrderVendor create(UUID orderId, UUID vendorId, String vendorName, long deliveryFee) {
        OrderVendor orderVendor = new OrderVendor();
        orderVendor.orderId = orderId;
        orderVendor.vendorId = vendorId;
        orderVendor.vendorName = vendorName;
        orderVendor.deliveryFee = deliveryFee;
        return orderVendor;
    }
}
