package com.prompt.demo.service;

import com.prompt.demo.ai.GeminiService;
import com.prompt.demo.dto.request.PromptDTOs;
import com.prompt.demo.dto.response.PromptResponse;
import com.prompt.demo.entity.Prompt;
import com.prompt.demo.entity.QueryExecution;
import com.prompt.demo.entity.User;
import com.prompt.demo.exception.AppExceptions;
import com.prompt.demo.repository.PromptRepository;
import com.prompt.demo.repository.QueryExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptService {

    private final PromptRepository promptRepository;
    private final QueryExecutionRepository queryExecutionRepository;
    private final ProjectService projectService;
    private final SchemaService schemaService;
    private final GeminiService geminiService;

    @Transactional
    public PromptResponse generateSql(PromptDTOs.GeneratePromptRequest request, User currentUser) {
        // Validate project access
        projectService.getProjectEntity(request.getProjectId(), currentUser.getId());

        // Get schema for this project
        String schemaJson = schemaService.getSchemaJson(request.getProjectId());

        // Create prompt record
        Prompt prompt = Prompt.builder()
                .user(currentUser)
                .project(projectService.getProjectEntity(request.getProjectId(), currentUser.getId()))
                .promptText(request.getPromptText())
                .status(Prompt.PromptStatus.PENDING)
                .build();

        prompt = promptRepository.save(prompt);

        try {
            // Call Gemini API
            log.info("Generating SQL for prompt: {} by user: {}", prompt.getId(), currentUser.getEmail());
            GeminiService.GeminiResponse aiResponse = geminiService.generateSql(
                    schemaJson,
                    request.getPromptText()
            );

            // Update prompt with generated SQL
            prompt.setGeneratedSql(aiResponse.sqlQuery());
            prompt.setExplanation(aiResponse.explanation());
            prompt.setStatus(Prompt.PromptStatus.SUCCESS);
            prompt = promptRepository.save(prompt);

            log.info("SQL generated successfully for prompt: {}", prompt.getId());

        } catch (AppExceptions.AiServiceException e) {
            prompt.setStatus(Prompt.PromptStatus.FAILED);
            promptRepository.save(prompt);
            throw e;
        }

        return toResponse(prompt, null);
    }

    @Transactional(readOnly = true)
    public Page<PromptResponse> getHistory(User currentUser, UUID projectId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));

        Page<Prompt> prompts = projectId != null
                ? promptRepository.findByUserIdAndProjectIdOrderByCreatedAtDesc(
                currentUser.getId(), projectId, pageable)
                : promptRepository.findByUserIdOrderByCreatedAtDesc(
                currentUser.getId(), pageable);

        return prompts.map(p -> {
            QueryExecution lastExec = queryExecutionRepository
                    .findTopByPromptIdOrderByCreatedAtDesc(p.getId())
                    .orElse(null);
            return toResponse(p, lastExec);
        });
    }

    @Transactional(readOnly = true)
    public PromptResponse getPrompt(UUID promptId, User currentUser) {
        Prompt prompt = promptRepository.findByIdAndUserId(promptId, currentUser.getId())
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Prompt", promptId));

        QueryExecution lastExec = queryExecutionRepository
                .findTopByPromptIdOrderByCreatedAtDesc(promptId)
                .orElse(null);

        return toResponse(prompt, lastExec);
    }

    // Internal method for query execution service
    public Prompt getPromptEntity(UUID promptId, UUID userId) {
        return promptRepository.findByIdAndUserId(promptId, userId)
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Prompt", promptId));
    }

    private PromptResponse toResponse(Prompt prompt, QueryExecution lastExecution) {
        PromptResponse.QueryExecutionSummary execSummary = null;
        if (lastExecution != null) {
            execSummary = PromptResponse.QueryExecutionSummary.builder()
                    .id(lastExecution.getId())
                    .executionTime(lastExecution.getExecutionTime())
                    .rowsReturned(lastExecution.getRowsReturned())
                    .status(lastExecution.getStatus())
                    .createdAt(lastExecution.getCreatedAt())
                    .build();
        }

        return PromptResponse.builder()
                .id(prompt.getId())
                .userId(prompt.getUser().getId())
                .projectId(prompt.getProject().getId())
                .promptText(prompt.getPromptText())
                .generatedSql(prompt.getGeneratedSql())
                .explanation(prompt.getExplanation())
                .status(prompt.getStatus())
                .createdAt(prompt.getCreatedAt())
                .lastExecution(execSummary)
                .build();
    }
}