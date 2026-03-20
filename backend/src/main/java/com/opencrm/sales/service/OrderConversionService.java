package com.opencrm.sales.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencrm.common.exception.EntityNotFoundException;
import com.opencrm.common.exception.ValidationException;
import com.opencrm.data.dto.RecordDTO;
import com.opencrm.data.model.Record;
import com.opencrm.data.repository.RecordRepository;
import com.opencrm.metadata.model.EntityDef;
import com.opencrm.metadata.service.EntityDefService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class OrderConversionService {

    private final RecordRepository recordRepository;
    private final EntityDefService entityDefService;
    private final ObjectMapper objectMapper;

    public OrderConversionService(RecordRepository recordRepository, EntityDefService entityDefService,
                                   ObjectMapper objectMapper) {
        this.recordRepository = recordRepository;
        this.entityDefService = entityDefService;
        this.objectMapper = objectMapper;
    }

    public RecordDTO convertQuoteToOrder(UUID quoteId, UUID userId) {
        Record quoteRecord = recordRepository.findByIdAndIsDeletedFalse(quoteId)
                .orElseThrow(() -> new EntityNotFoundException("Quote not found: " + quoteId));

        Map<String, Object> quoteData = parseData(quoteRecord.getData());

        String status = quoteData.get("Status") != null ? quoteData.get("Status").toString() : "";
        if (!"Approved".equals(status)) {
            throw new ValidationException("Only approved quotes can be converted to orders. Current status: " + status);
        }

        // Create Order
        EntityDef orderEntity = entityDefService.findByApiName("Order");
        Map<String, Object> orderData = new LinkedHashMap<>();
        orderData.put("AccountId", quoteData.get("AccountId"));
        orderData.put("QuoteId", quoteId.toString());
        orderData.put("OpportunityId", quoteData.get("OpportunityId"));
        orderData.put("Status", "Draft");
        orderData.put("EffectiveDate", LocalDate.now().toString());
        orderData.put("BillingAddress", quoteData.get("BillingAddress"));
        orderData.put("ShippingAddress", quoteData.get("ShippingAddress"));
        orderData.put("Description", "Converted from Quote: " + quoteRecord.getName());
        orderData.put("TotalAmount", quoteData.get("GrandTotal"));

        Record order = new Record();
        order.setEntityDefId(orderEntity.getId());
        order.setName("ORD-" + quoteRecord.getName());
        order.setOwnerId(userId);
        order.setCreatedBy(userId);
        order.setData(toJson(orderData));
        order = recordRepository.save(order);

        // Copy Quote Line Items to Order Items
        EntityDef quoteLineItemEntity = entityDefService.findByApiName("QuoteLineItem");
        EntityDef orderItemEntity = entityDefService.findByApiName("OrderItem");

        List<Record> quoteLineItems = recordRepository.findByEntityAndFieldValue(
                quoteLineItemEntity.getId(), "QuoteId", quoteId.toString());

        for (Record qli : quoteLineItems) {
            Map<String, Object> qliData = parseData(qli.getData());

            Map<String, Object> oiData = new LinkedHashMap<>();
            oiData.put("OrderId", order.getId().toString());
            oiData.put("ProductId", qliData.get("ProductId"));
            oiData.put("Quantity", qliData.get("Quantity"));
            oiData.put("UnitPrice", qliData.get("UnitPrice"));
            oiData.put("TotalPrice", qliData.get("TotalPrice"));
            oiData.put("Description", qliData.get("Description"));

            Record orderItem = new Record();
            orderItem.setEntityDefId(orderItemEntity.getId());
            orderItem.setName(qli.getName());
            orderItem.setOwnerId(userId);
            orderItem.setCreatedBy(userId);
            orderItem.setData(toJson(oiData));
            recordRepository.save(orderItem);
        }

        // Update quote status to "Converted"
        quoteData.put("Status", "Converted");
        quoteRecord.setData(toJson(quoteData));
        recordRepository.save(quoteRecord);

        return toDTO(order);
    }

    private RecordDTO toDTO(Record record) {
        Map<String, Object> data = parseData(record.getData());
        return new RecordDTO(record.getId(), record.getEntityDefId(), record.getName(),
                data, record.getOwnerId(), record.getCreatedBy(),
                record.getCreatedAt(), record.getUpdatedAt());
    }

    private Map<String, Object> parseData(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Invalid data format");
        }
    }
}
