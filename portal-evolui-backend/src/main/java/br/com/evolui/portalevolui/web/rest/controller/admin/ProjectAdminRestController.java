package br.com.evolui.portalevolui.web.rest.controller.admin;

import br.com.evolui.portalevolui.web.beans.ProjectBean;
import br.com.evolui.portalevolui.web.beans.ProjectModuleBean;
import br.com.evolui.portalevolui.web.repository.project.ProjectRepository;
import br.com.evolui.portalevolui.web.service.GithubVersionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@PreAuthorize("hasAnyRole('ROLE_SUPER', 'ROLE_HYPER', 'ROLE_ADMIN')")
@RequestMapping("/api/admin/project")
public class ProjectAdminRestController {

    @Autowired
    ProjectRepository repository;

    @Autowired
    private GithubVersionService githubService;

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/all")
    public ResponseEntity<List<ProjectBean>> getAll() {
        return ResponseEntity.ok(this.repository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectBean> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(this.repository.findById(id).orElse(null));
    }

    @Transactional(rollbackFor = Exception.class)
    @PostMapping()
    public ResponseEntity<ProjectBean> save(@RequestBody ProjectBean body) throws Exception {
        // 1. Flatten nested childBonds into a flat module list + collect bond relationships
        List<ProjectModuleBean> flatModules = new ArrayList<>();
        Map<String, ProjectModuleBean> bondMap = new HashMap<>();
        flattenModules(body.getModules(), null, flatModules, bondMap);
        body.setModules(flatModules);

        // 2. Set temporary bond placeholders for validation
        for (ProjectModuleBean module : flatModules) {
            ProjectModuleBean bondTarget = bondMap.get(module.getIdentifier().toLowerCase());
            if (bondTarget != null) {
                ProjectModuleBean bondRef = new ProjectModuleBean();
                bondRef.setId(bondTarget.getId());
                bondRef.setIdentifier(bondTarget.getIdentifier());
                bondRef.setRepository(bondTarget.getRepository());
                module.setBond(bondRef);
            }
        }

        // 3. Validate (before any DB modification)
        validate(body);

        // 4. Clean up removed modules (for updates only)
        if (body.getId() != null && body.getId() > 0) {
            cleanupRemovedModules(body, flatModules);
        }

        // 5. Clear childBonds and set project for first save (avoids transient entity references)
        for (ProjectModuleBean module : flatModules) {
            module.setProject(body);
            module.setBond(null);
            module.setChildBonds(null);
        }

        // 6. First save: project + modules WITHOUT self-referential childBonds
        ProjectBean saved = repository.saveAndFlush(body);

        // 7. Now all entities are managed with IDs. Set bond references.
        if (!bondMap.isEmpty()) {
            Map<String, ProjectModuleBean> byIdentifier = saved.getModules().stream()
                    .collect(Collectors.toMap(
                            m -> m.getIdentifier().toLowerCase(),
                            m -> m
                    ));
            for (ProjectModuleBean module : saved.getModules()) {
                ProjectModuleBean bondTarget = bondMap.get(module.getIdentifier().toLowerCase());
                if (bondTarget != null) {
                    module.setBond(byIdentifier.get(bondTarget.getIdentifier().toLowerCase()));
                }
            }
            // 8. Flush to persist bond FK updates
            entityManager.flush();
        }

        return ResponseEntity.ok(saved);
    }

    public void validate(ProjectBean body) throws Exception {
        ProjectBean workflowStructure = null;
        try {
            workflowStructure = getWorkflowStructure(body.getRepository());
        }
        catch (Exception e) {}
        if (workflowStructure == null) {
            throw new Exception("Repositório não está preparado para pipelines de construção!");
        }

        validateModules(body);
        validateConsistencyWithWorkflow(body, workflowStructure);
    }

    @Transactional(rollbackFor = Exception.class)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) throws Exception {
        ProjectBean project = repository.findById(id).orElse(null);
        if (project != null) {
            if (project.getModules() != null && !project.getModules().isEmpty()) {
                List<Long> moduleIds = project.getModules().stream()
                        .map(ProjectModuleBean::getId)
                        .collect(Collectors.toList());
                repository.clearBondsByModuleIds(moduleIds);
                entityManager.flush();
            }
            repository.delete(project);
        }
        return ResponseEntity.ok(null);
    }

    @GetMapping("/structure/{repository}")
    public ResponseEntity<ProjectBean> getProjectStructure(@PathVariable("repository") String repository) throws Exception {
        if (this.githubService.initialize()) {
            return ResponseEntity.ok(this.getWorkflowStructure(repository));
        }
        return ResponseEntity.ok(null);
    }

    /**
     * Recursively flattens modules from a nested tree (childBonds inside modules)
     * into a flat list. Collects bond relationships in a separate map.
     * Guarantees topological order: parents appear before children.
     */
    private void flattenModules(List<ProjectModuleBean> modules, ProjectModuleBean parent,
                                List<ProjectModuleBean> result, Map<String, ProjectModuleBean> bondMap) {
        if (modules == null) return;
        for (ProjectModuleBean module : modules) {
            List<ProjectModuleBean> childBonds = module.getChildBonds();
            module.setChildBonds(null);
            module.setBond(null);

            if (parent != null) {
                bondMap.put(module.getIdentifier().toLowerCase(), parent);
            }

            result.add(module);

            if (childBonds != null && !childBonds.isEmpty()) {
                flattenModules(childBonds, module, result, bondMap);
            }
        }
    }

    /**
     * Handles deletion of modules removed by the user, including their descendants.
     * Clears self-referential bond FKs before deleting to avoid FK violations.
     * Clears persistence context after DML to avoid stale state on merge.
     */
    private void cleanupRemovedModules(ProjectBean body, List<ProjectModuleBean> flatModules) {
        ProjectBean existing = repository.findById(body.getId()).orElse(null);
        if (existing == null || existing.getModules() == null || existing.getModules().isEmpty()) return;

        Set<Long> incomingIds = flatModules.stream()
                .map(ProjectModuleBean::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Find modules that exist in DB but are not in the incoming list
        Set<Long> toDeleteIds = new LinkedHashSet<>();
        for (ProjectModuleBean mod : existing.getModules()) {
            if (!incomingIds.contains(mod.getId())) {
                toDeleteIds.add(mod.getId());
            }
        }

        if (toDeleteIds.isEmpty()) return;

        // Also find all descendants of deleted modules (cascade business rule)
        Map<Long, List<Long>> childrenByParentId = new HashMap<>();
        for (ProjectModuleBean mod : existing.getModules()) {
            if (mod.getBond() != null) {
                childrenByParentId
                        .computeIfAbsent(mod.getBond().getId(), k -> new ArrayList<>())
                        .add(mod.getId());
            }
        }
        Queue<Long> queue = new LinkedList<>(toDeleteIds);
        while (!queue.isEmpty()) {
            Long parentId = queue.poll();
            List<Long> children = childrenByParentId.getOrDefault(parentId, Collections.emptyList());
            for (Long childId : children) {
                if (toDeleteIds.add(childId)) {
                    queue.add(childId);
                }
            }
        }

        // Remove auto-deleted descendants from the incoming list too
        flatModules.removeIf(m -> m.getId() != null && toDeleteIds.contains(m.getId()));
        body.setModules(flatModules);

        // Execute deletion: clear bond FKs first, then delete (avoids FK violation)
        List<Long> deleteList = new ArrayList<>(toDeleteIds);
        repository.clearBondsByModuleIds(deleteList);
        repository.deleteModulesByIds(deleteList);
        entityManager.flush();
        entityManager.clear();
    }

    private ProjectBean getWorkflowStructure(String repository) throws Exception {
        ProjectBean workflowStructure = githubService.getProjectWorkflowRepositoryStructure(repository);
        for (ProjectModuleBean module : workflowStructure.getModules()) {
            if (module.getBond() != null) {
                ProjectModuleBean projectModule = workflowStructure.getModules().stream().filter(m -> m.getIdentifier().equalsIgnoreCase(module.getBond().getIdentifier())).findFirst().get();
                projectModule.addBond(module);
            }
        }
        return workflowStructure;
    }

    private void validateModules(ProjectBean body) throws Exception {
        List<ProjectModuleBean> modules = body.getModules();

        if (modules == null || modules.isEmpty()) {
            throw new Exception("Projetos devem ter, ao menos, um módulo para poderem ser salvos");
        }

        long mainCount = modules.stream().filter(ProjectModuleBean::isMain).count();
        long frameworkCount = modules.stream().filter(ProjectModuleBean::isFramework).count();

        if (mainCount > 1) {
            throw new Exception("Apenas um módulo principal é permitido");
        }
        if (mainCount != 1) {
            throw new Exception("É obrigatório informar qual o módulo principal");
        }
        if (frameworkCount > 1) {
            throw new Exception("Apenas um módulo framework é permitido");
        }

        for (ProjectModuleBean module : modules) {
            if (module.getBond() != null) {
                if (!module.getBond().getRepository().equals(module.getRepository())) {
                    throw new Exception("Módulos vinculados devem estar no mesmo repositório que o módulo pai. " +
                            "O módulo " + module.getIdentifier() + " está em repositório diferente de " + module.getBond().getIdentifier());
                }
            }
        }

        if (body.isLuthierProject()) {
//            if (mainCount != 1) {
//                throw new Exception("Projetos do tipo luthier precisam de um módulo principal");
//            }
            if (frameworkCount != 1) {
                throw new Exception("Projetos do tipo luthier precisam de um módulo de framework");
            }
        }
    }

    private void validateConsistencyWithWorkflow(ProjectBean body, ProjectBean workflow) throws Exception {
        if (body.isLuthierProject() != workflow.isLuthierProject()) {
            throw new Exception("Corrupção da estrutura pré-definida no pipeline do projeto: LuthierProject");
        }
        if (body.isFramework() != workflow.isFramework()) {
            throw new Exception("Corrupção da estrutura pré-definida no pipeline do projeto: Framework");
        }

        Map<String, ProjectModuleBean> workflowModules = workflow.getModules().stream()
                .collect(Collectors.toMap(
                        m -> m.getIdentifier().toLowerCase(),
                        m -> m
                ));

        for (ProjectModuleBean module : body.getModules()) {
            ProjectModuleBean defModule = workflowModules.get(module.getIdentifier().toLowerCase());
            if (defModule == null) {
                throw new Exception("Corrupção da estrutura pré-definida no pipeline do projeto: moduleIdentifier " + module.getIdentifier());
            }
            if (defModule.isMain() != module.isMain()) {
                throw new Exception("Corrupção da estrutura pré-definida no pipeline do projeto: moduleMain " + module.getIdentifier());
            }
            if (defModule.isFramework() != module.isFramework()) {
                throw new Exception("Corrupção da estrutura pré-definida no pipeline do projeto: moduleFramework " + module.getIdentifier());
            }
            if (defModule.getBond() != null) {
                if (module.getBond() == null) {
                    throw new Exception("Corrupção da estrutura pré-definida no pipeline do projeto: moduleBond " + module.getIdentifier());
                }
                if (!Objects.equals(defModule.getBond().getIdentifier(), module.getBond().getIdentifier() )) {
                    throw new Exception("Corrupção da estrutura pré-definida no pipeline do projeto: moduleBond " + module.getIdentifier());
                }
            }
            else {
                if (module.getBond() != null) {
                    throw new Exception("Corrupção da estrutura pré-definida no pipeline do projeto: moduleBond " + module.getIdentifier());
                }
            }
        }
    }
}
