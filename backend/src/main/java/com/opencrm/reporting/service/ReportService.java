package com.opencrm.reporting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencrm.data.model.Record;
import com.opencrm.data.repository.RecordRepository;
import com.opencrm.metadata.model.EntityDef;
import com.opencrm.metadata.service.EntityDefService;
import com.opencrm.reporting.dto.PipelineSummary;
import com.opencrm.reporting.dto.RevenueByMonth;
import com.opencrm.reporting.dto.TopAccount;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final RecordRepository recordRepository;
    private final EntityDefService entityDefService;
    private final ObjectMapper objectMapper;

    public ReportService(RecordRepository recordRepository, EntityDefService entityDefService,
                         ObjectMapper objectMapper) {
        this.recordRepository = recordRepository;
        this.entityDefService = entityDefService;
        this.objectMapper = objectMapper;
    }

    public List<PipelineSummary> getPipelineSummary() {
        EntityDef oppEntity = entityDefService.findByApiName("Opportunity");
        List<Record> records = recordRepository.findByEntityDefIdAndIsDeletedFalse(
                oppEntity.getId(), PageRequest.of(0, 10000)).getContent();

        Map<String, List<Record>> byStage = records.stream()
                .collect(Collectors.groupingBy(r -> {
                    Map<String, Object> data = parseData(r.getData());
                    Object stage = data.get("Stage");
                    return stage != null ? stage.toString() : "Unknown";
                }));

        List<String> stageOrder = List.of("Prospecting", "Qualification", "Needs Analysis",
                "Value Proposition", "Id. Decision Makers", "Perception Analysis",
                "Proposal/Price Quote", "Negotiation/Review", "Closed Won", "Closed Lost");

        List<PipelineSummary> result = new ArrayList<>();
        for (String stage : stageOrder) {
            List<Record> stageRecords = byStage.getOrDefault(stage, List.of());
            double total = stageRecords.stream()
                    .mapToDouble(r -> getDoubleField(r, "Amount"))
                    .sum();
            if (!stageRecords.isEmpty()) {
                result.add(new PipelineSummary(stage, stageRecords.size(), total));
            }
        }
        // Add any stages not in the predefined order
        for (Map.Entry<String, List<Record>> entry : byStage.entrySet()) {
            if (!stageOrder.contains(entry.getKey())) {
                double total = entry.getValue().stream()
                        .mapToDouble(r -> getDoubleField(r, "Amount"))
                        .sum();
                result.add(new PipelineSummary(entry.getKey(), entry.getValue().size(), total));
            }
        }
        return result;
    }

    public List<RevenueByMonth> getRevenueByMonth() {
        EntityDef oppEntity = entityDefService.findByApiName("Opportunity");
        List<Record> records = recordRepository.findByEntityDefIdAndIsDeletedFalse(
                oppEntity.getId(), PageRequest.of(0, 10000)).getContent();

        Map<String, Double> byMonth = new TreeMap<>();
        for (Record r : records) {
            Map<String, Object> data = parseData(r.getData());
            Object stage = data.get("Stage");
            if ("Closed Won".equals(stage != null ? stage.toString() : "")) {
                Object closeDate = data.get("CloseDate");
                if (closeDate != null) {
                    String month = closeDate.toString().substring(0, 7); // YYYY-MM
                    double amount = getDoubleField(r, "Amount");
                    byMonth.merge(month, amount, Double::sum);
                }
            }
        }

        return byMonth.entrySet().stream()
                .map(e -> new RevenueByMonth(e.getKey(), e.getValue()))
                .toList();
    }

    public List<TopAccount> getTopAccounts() {
        EntityDef oppEntity = entityDefService.findByApiName("Opportunity");
        EntityDef accountEntity = entityDefService.findByApiName("Account");

        List<Record> opportunities = recordRepository.findByEntityDefIdAndIsDeletedFalse(
                oppEntity.getId(), PageRequest.of(0, 10000)).getContent();

        // Group by AccountId
        Map<String, List<Record>> byAccount = opportunities.stream()
                .filter(r -> {
                    Map<String, Object> data = parseData(r.getData());
                    return data.get("AccountId") != null;
                })
                .collect(Collectors.groupingBy(r -> {
                    Map<String, Object> data = parseData(r.getData());
                    return data.get("AccountId").toString();
                }));

        // Build top accounts list
        List<TopAccount> result = new ArrayList<>();
        for (Map.Entry<String, List<Record>> entry : byAccount.entrySet()) {
            String accountId = entry.getKey();
            List<Record> opps = entry.getValue();
            double total = opps.stream().mapToDouble(r -> getDoubleField(r, "Amount")).sum();

            // Resolve account name
            String accountName = "Unknown";
            try {
                Optional<Record> account = recordRepository.findByIdAndIsDeletedFalse(UUID.fromString(accountId));
                if (account.isPresent()) {
                    accountName = account.get().getName() != null ? account.get().getName() : "Unknown";
                }
            } catch (Exception ignored) {}

            result.add(new TopAccount(accountId, accountName, total, opps.size()));
        }

        result.sort((a, b) -> Double.compare(b.totalAmount(), a.totalAmount()));
        return result.stream().limit(10).toList();
    }

    public Map<String, Long> getRecordCounts() {
        List<EntityDef> entities = entityDefService.findAll();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (EntityDef entity : entities) {
            long count = recordRepository.findByEntityDefIdAndIsDeletedFalse(
                    entity.getId(), PageRequest.of(0, 1)).getTotalElements();
            counts.put(entity.getApiName(), count);
        }
        return counts;
    }

    private double getDoubleField(Record record, String fieldName) {
        Map<String, Object> data = parseData(record.getData());
        Object val = data.get(fieldName);
        if (val == null) return 0.0;
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Map<String, Object> parseData(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}
