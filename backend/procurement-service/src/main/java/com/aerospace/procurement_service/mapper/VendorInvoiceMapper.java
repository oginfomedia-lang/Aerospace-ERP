package com.aerospace.procurement_service.mapper;

import com.aerospace.procurement_service.dto.VendorInvoiceLineItemDTO;
import com.aerospace.procurement_service.dto.VendorInvoiceResponse;
import com.aerospace.procurement_service.entity.VendorInvoice;
import com.aerospace.procurement_service.entity.VendorInvoiceLineItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class VendorInvoiceMapper {

    public VendorInvoiceLineItemDTO toLineItemDTO(VendorInvoiceLineItem item) {
        if (item == null) return null;

        return VendorInvoiceLineItemDTO.builder()
                .skuCode(item.getSkuCode())
                .invoicedQuantity(item.getInvoicedQuantity())
                .invoicedUnitPrice(item.getInvoicedUnitPrice())
                .invoicedTotal(item.getInvoicedTotal())
                .matchedPoQuantity(item.getMatchedPoQuantity())
                .matchedPoUnitPrice(item.getMatchedPoUnitPrice())
                .quantityVariance(item.getQuantityVariance())
                .priceVariancePercent(item.getPriceVariancePercent())
                .lineStatus(item.getLineStatus() != null ? item.getLineStatus().name() : null)
                .build();
    }

    public VendorInvoiceResponse toDTO(VendorInvoice invoice) {
        if (invoice == null) return null;

        List<VendorInvoiceLineItemDTO> items = invoice.getItems() != null
                ? invoice.getItems().stream().map(this::toLineItemDTO).collect(Collectors.toList())
                : null;

        return VendorInvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .supplierInvoiceRef(invoice.getSupplierInvoiceRef())
                .purchaseOrderId(invoice.getPurchaseOrder() != null ? invoice.getPurchaseOrder().getId() : null)
                .poNumber(invoice.getPurchaseOrder() != null ? invoice.getPurchaseOrder().getPoNumber() : null)
                .supplierId(invoice.getPurchaseOrder() != null && invoice.getPurchaseOrder().getSupplier() != null
                        ? invoice.getPurchaseOrder().getSupplier().getId() : null)
                .supplierName(invoice.getPurchaseOrder() != null && invoice.getPurchaseOrder().getSupplier() != null
                        ? invoice.getPurchaseOrder().getSupplier().getName() : null)
                .invoiceDate(invoice.getInvoiceDate())
                .currency(invoice.getCurrency())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus() != null ? invoice.getStatus().name() : null)
                .remarks(invoice.getRemarks())
                .items(items)
                .approvedBy(invoice.getApprovedBy())
                .approvedAt(invoice.getApprovedAt())
                .paidAt(invoice.getPaidAt())
                .build();
    }
}
