package br.com.evolui.portalevolui.web.beans.listener;

public class ProdutoBeanListener {
//    private ProdutoRepository repository;
//
//    @PrePersist
//    @PreUpdate
//    protected void beforeAnyUpdate(ProdutoBean body) {
//        if (body.getId() == null || body.getId() <= 0 || !body.isMain()) {
//            return;
//        }
//        this.repository = BeanUtilService.getBean(ProdutoRepository.class);
//        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
//        ProdutoBean bean = this.repository.findById(body.getId()).orElse(null);
//        if (bean.getModules() == null || bean.getModules().isEmpty()) {
//            return;
//        }
//        for(ProdutoBean mod: bean.getModules()) {
//            Long idModule = mod.getId();
//            if (!body.getModules().stream().anyMatch(x -> x.getId() != null && x.getId().equals(idModule))) {
//                this.repository.deleteById(idModule);
//            }
//        }
//
//
//    }
}
