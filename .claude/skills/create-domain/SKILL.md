---
name: create-domain
description: Cria um novo domínio CRUD no projeto Sentry seguindo o padrão do domínio `organization` - entidade + api (Rest/Mapper/Repository/Service/Specification) no módulo sentry-api, e Controller + ControllerSwagger no módulo consumidor (sentry-backend ou sentry-auth). Use quando pedirem para criar um domínio, entidade, CRUD ou endpoints novos neste projeto.
---

# create-domain

Gera um domínio CRUD completo copiando a estrutura de `organization`. Sempre leia
os arquivos de `organization` antes de escrever — eles são a fonte da verdade,
este documento só descreve o padrão.

## Estrutura do projeto

Multi-módulo Maven, parent `br.com.sentry:sentry` (Java 25, Spring Boot 4.x):

- **sentry-api** (`dev.sentry.api`) — biblioteca compartilhada. Entidades, DTOs,
  mappers, repositories, services, specifications. Sem `main`. Publicada via
  `ApiAutoConfiguration` (`@ComponentScan`/`@EntityScan`/`@EnableJpaRepositories`/`@EnableJpaAuditing`),
  registrada em `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- **sentry-backend** (`dev.sentry.backend`) e **sentry-auth** (`dev.sentry.auth`) —
  aplicações Spring Boot que dependem de `sentry-api`. Contêm **apenas** os controllers.

Consequência: o domínio nunca fica todo em um módulo só. Regra é sempre a mesma —
lógica em `sentry-api`, exposição HTTP no módulo da aplicação.

## Arquivos a criar

Para uma entidade `Foo` (PascalCase singular), pacote base `foo` (lowercase singular):

```
sentry-api/src/main/java/dev/sentry/api/domain/foo/
├── Foo.java                        entidade JPA
└── api/
    ├── FooRest.java                interface com os 4 records (QueryRequest, Response, SaveRequest, UpdateRequest)
    ├── FooMapper.java              MapStruct
    ├── FooRepository.java          ListCrudRepository + JpaSpecificationExecutor
    ├── FooService.java             regra de negócio
    └── FooSpecification.java       filtros da query

sentry-backend/src/main/java/dev/sentry/backend/foo/     (ou sentry-auth/.../dev/sentry/auth/foo/)
├── FooController.java              @RestController, implementa a interface, só delega
└── FooControllerSwagger.java       interface com @RequestMapping + toda a documentação OpenAPI
```

Nunca criar `FooQueryRequest.java`, `FooResponse.java` etc. como arquivos soltos —
os quatro records são aninhados em `FooRest`.

## Regras por arquivo

**Entidade** (`Foo.java`, pacote `domain.foo`)
`@Getter @Setter @Entity @Table(name = "foos") @EntityListeners(AuditingEntityListener.class)`.
Campos padrão sempre presentes, além dos específicos do domínio:
`id_foo` (`@Id @GeneratedValue(IDENTITY)`), `uuid` (`UUID.randomUUID()`, unique, updatable=false),
`isActive` (default `true`), `isDeleted` (default `false`), e a auditoria
`createdBy`/`createdAt`/`updatedBy`/`updatedAt` com `@CreatedBy`/`@CreatedDate`/`@LastModifiedBy`/`@LastModifiedDate`.
Colunas snake_case explícitas em `@Column(name = ...)` quando o campo é camelCase.
Os campos primary key, mesmo que se chamem id_foo, id_bar, eles devem conter o nome do atributo apenas de id

**FooRest** — interface, um `@Schema` em cada record e em cada componente, descrições e
exemplos **em português**.
- `QueryRequest`: filtros (todos opcionais) + `@NotNull Integer page`, `@NotNull Integer pageSize`,
  `String sortBy` (formato `"id,asc;name,desc"`).
- `Response`: campos do domínio + `uuid`, `isActive`, `isDeleted` e os quatro de auditoria.
- `SaveRequest` / `UpdateRequest`: campos editáveis, sem `id`/`uuid`/auditoria.
  Obrigatórios levam `@NotBlank`/`@NotNull` e `requiredMode = RequiredMode.REQUIRED`.

**FooMapper** — `@Mapper(componentModel = "spring")` com `toDto`, `toEntity` e
`updateEntity(@MappingTarget)` anotado com
`@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)`.

**FooRepository** — `@Repository`, `extends ListCrudRepository<Foo, Long>, JpaSpecificationExecutor<Foo>`.
Derived queries só quando o service precisar (ex.: checagem de unicidade).

**FooSpecification** — classe final de fato: construtor privado, único método estático
`findBy(FooRest.QueryRequest)`. Começa em `cb.conjunction()`, adiciona um `if (queryParams.x() != null)`
por filtro (`like` + `upper` para texto, `equal` para o resto) e **sempre** fecha com o filtro de
soft delete: sem `isDeleted` explícito, só retorna os não excluídos.

**FooService** — `@Service`, injeção por construtor (sem `@Autowired`), sem `@Transactional`
salvo necessidade real. Métodos `findByQueryParams`, `findById`, `save`, `update`, `delete`.
Paginação com `PageRequest.of(page, pageSize, SortUtils.toSort(sortBy))` — reutilize
`dev.sentry.api.utils.SortUtils`, não reimplemente parsing de ordenação.
Erros são `ResponseStatusException` com mensagem em português: `NOT_FOUND` para inexistente
(via um `getOrThrow(id)` privado), `CONFLICT` para violação de unicidade.
`delete` é lógico: `setIsDeleted(true)` + `save`.

**FooControllerSwagger** — interface no módulo da aplicação com `@Tag`, `@RequestMapping("/foos")`
e toda a anotação HTTP/OpenAPI: `@Operation`, `@ApiResponses`, `@GetMapping`, `@PostMapping`
(`@ResponseStatus(CREATED)`), `@PutMapping`/`@DeleteMapping` (`@ResponseStatus(NO_CONTENT)`),
`@ParameterObject @Valid` na query, `@RequestBody @Valid` nos corpos. Textos em português.

**FooController** — `@RestController` implementando a interface. Sem anotação de mapping,
sem lógica: cada método é `@Override` que delega ao service.

## Passos

1. Ler `Organization.java` e os arquivos de `domain/organization/api/` + os dois controllers
   de `sentry-backend/.../organization/` como referência viva.
2. Confirmar com quem pediu: campos do domínio e módulo alvo (`sentry-backend` ou `sentry-auth`),
   se não estiverem claros no pedido.
3. Criar os 5 arquivos em `sentry-api` e os 2 no módulo alvo.
4. `mvn -q -pl sentry-api,<módulo-alvo> -am compile` — o MapStruct precisa gerar
   `FooMapperImpl` sem erro.

Nenhum pom.xml precisa ser alterado: as dependências já estão nos módulos.
