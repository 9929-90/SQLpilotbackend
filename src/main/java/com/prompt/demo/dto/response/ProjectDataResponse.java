package com.prompt.demo.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class ProjectDataResponse {
    private String tableName;       // logical name
    private String physicalTable;   // actual PG table name
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private int rowCount;
}