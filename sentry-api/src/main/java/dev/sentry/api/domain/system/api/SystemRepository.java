package dev.sentry.api.domain.system.api;

import dev.sentry.api.domain.system.ExternalSystem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemRepository extends ListCrudRepository<ExternalSystem, Long>, JpaSpecificationExecutor<ExternalSystem> {

    // quem busca pelo código (login, cliente OAuth, tokens) usa a organização e as URIs; vêm juntas, sem depender de
    // uma sessão do Hibernate aberta, que não existe nos filtros do Spring Security
    @EntityGraph(attributePaths = {"organization", "redirectUris"})
    ExternalSystem findByExternalCode(String externalCode);
}
