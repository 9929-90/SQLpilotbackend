package com.prompt.demo.service;


import com.prompt.demo.dto.request.ProjectDTOs;
import com.prompt.demo.dto.response.ProjectResponse;
import com.prompt.demo.entity.Project;
import com.prompt.demo.entity.User;
import com.prompt.demo.exception.AppExceptions;
import com.prompt.demo.repository.ProjectRepository;
import com.prompt.demo.repository.SchemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final SchemaRepository schemaRepository;

    @Transactional
    public ProjectResponse createProject(ProjectDTOs.CreateProjectRequest request, User currentUser) {
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .user(currentUser)
                .build();

        project = projectRepository.save(project);
        log.info("Project created: {} by user: {}", project.getId(), currentUser.getEmail());

        return toResponse(project, false);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getUserProjects(User currentUser) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(p -> toResponse(p, schemaRepository.existsByProjectId(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId, User currentUser) {
        Project project = projectRepository.findByIdAndUserId(projectId, currentUser.getId())
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Project", projectId));

        boolean hasSchema = schemaRepository.existsByProjectId(projectId);
        return toResponse(project, hasSchema);
    }

    // Internal method - used by other services
    public Project getProjectEntity(UUID projectId, UUID userId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException("Project", projectId));
    }

    private ProjectResponse toResponse(Project project, boolean hasSchema) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .userId(project.getUser().getId())
                .hasSchema(hasSchema)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}