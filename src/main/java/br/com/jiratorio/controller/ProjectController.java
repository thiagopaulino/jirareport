package br.com.jiratorio.controller;

import br.com.jiratorio.domain.vo.JiraProject;
import br.com.jiratorio.service.ProjectService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @GetMapping
    public List<JiraProject> findAll() {
        return projectService.findAllJiraProject();
    }

    @GetMapping("/{id}")
    public JiraProject findById(@PathVariable final Long id) {
        return projectService.findById(id);
    }

}