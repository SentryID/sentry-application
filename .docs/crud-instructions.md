# Read me first
You are a software developer, and you are working on a project that uses Spring Boot.
Your task is implements CRUD operations for a specific entity in the project.
You will be provided with a <EntityName>.java class that represents the entity.
You will create the Mappers, Services, Repositories, Specifications and DTO's.
Do not create the Controller class, as it will be created by another AI.

# Code instructions
You will be provided with an Spring Entity class, and you need to implement the following classes
The naming pattern of the class should be:
* `<EntityName>Service.java` for the service class
* `<EntityName>Repository.java` for the repository interface
* `<EntityName>Specification.java` for the specifications class'
* `<EntityName>Mapper.java` for the mapper class
* `<EntityName>Rest.java` for the interface that holds every REST DTO of the entity as nested records:
  * `QueryRequest` — the query request, it should contain all the fields of the entity
  * `Response` — the query response, it represents the entity in a way that is suitable for the client. This record should be return in the GET and GET/{id} endpoints.
  * `SaveRequest` — the request body for the POST endpoints, it should contain all the fields of the entity except the ID field.
  * `UpdateRequest` — the request body for the PUT endpoints, it should contain all the fields of the entity except the ID field.

Never create standalone `<EntityName>QueryRequest.java` / `Response` / `SaveRequest` / `UpdateRequest`
files: the four records live inside `<EntityName>Rest` and are referenced as
`<EntityName>Rest.QueryRequest`, `<EntityName>Rest.Response`, and so on.

All the classes should be created in the same package as the entity class.
Mappers, Repositories, Services and Controllers should ne created in the /api package inside the package of the entity class.

You will ignore the fields that are extended from `BaseEntity` class, as they are not relevant for the CRUD operations.

## Service
You need to implement a Spring Service class that contains the business logic for the entity.
The service should have methods for creating, reading, updating, and deleting the entity.

The code example below shows how to implement the service class, with and example of Organization class Entity:
```java
@Service
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    public OrganizationService(OrganizationRepository organizationRepository, OrganizationMapper organizationMapper) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
    }
    public List<OrganizationRest.Response> findByQueryParams(OrganizationRest.QueryRequest queryParams) {
        return organizationRepository.findAll(OrganizationSpecification.findBy(queryParams), PageRequest.of(queryParams.page(), queryParams.pageSize()))
                .stream()
                .map(organizationMapper::toDto)
                .collect(Collectors.toList());
    }
    public OrganizationRest.Response findById(Long id) {
        return organizationRepository.findById(id)
                .map(organizationMapper::toDto)
                .orElseThrow(() -> new NoSuchElementException("ME ALTERE")); // TODO
    }
    public void save(OrganizationRest.SaveRequest organizationRequest) {
        var uniqueOrganization = organizationRepository.findByCode(organizationRequest.code());
        if (uniqueOrganization != null) {
            // TODO
            throw new NotImplementedException();
        }
        Organization organization = organizationMapper.toEntity(organizationRequest);
        organizationRepository.save(organization);
    }
    public void delete(Long id) {
        if (!organizationRepository.existsById(id)) {
            throw new NoSuchElementException("me altere"); // TODO
        }
        organizationRepository.deleteById(id);
    }
}
```

## Repository
You need to implement a Spring Repository interface that extends the `JpaRepository` interface.
The repository should have methods for finding the entity by its ID and for finding all entities.

The code example below shows how to implement the repository interface, with and example of Organization class Entity:
```java
@Repository
public interface OrganizationRepository extends ListCrudRepository<Organization, Long>, JpaSpecificationExecutor<Organization> {
    Organization findByCode(String code);
}

```

## Specification
You need to implement a Spring Specification class that contains the specifications for the entity.
The specifications should be used to filter the entities based on the fields of the entity.

The code example below shows how to implement the specification class, with and example of Organization class Entity:
```java
public class OrganizationSpecification {
    public static Specification<Organization> findBy(OrganizationRest.QueryRequest queryParams) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if( queryParams.code() != null ) {
                predicate = cb.and(cb.equal(root.get("code"), queryParams.code()));
            }
            if( queryParams.description() != null ) {
                predicate = cb.and(cb.like(cb.upper(root.get("description")), queryParams.description().toUpperCase()));
            }
            if( queryParams.freeTier() != null ) {
                predicate = cb.and(cb.equal(root.get("freeTier"), queryParams.freeTier()));
            }
            if (queryParams.sortBy() != null && !queryParams.sortBy().isEmpty()) {
                SortEntityHelper.sortBy(queryParams.sortBy(), cb, root, query);
            }
            return predicate;
        };
    }
}
```

## Mapper
You need to implement a Spring Mapper class that maps the entity to the DTOs and vice versa.
You need to use MapStruct for the mapping.

The code example below shows how to implement the mapper class, with and example of Organization class Entity:
```java
@Mapper(componentModel = "spring")
public interface OrganizationMapper {
    OrganizationRest.Response toDto(Organization organization);
    Organization toEntity(OrganizationRest.SaveRequest dto);
}
```

## DTOs
You need to implement the DTOs that represent the entity in a way that is suitable for the client. You need to use validation for page and pageSize fields and Swagger documentation in portuguese, with some examples
All four records live in a single `<EntityName>Rest` interface (nested records are implicitly
`public static`), which keeps the whole REST contract of the entity in one file:

```java
public interface OrganizationRest {

    @Schema(description = "Requisição para consulta de organizações")
    record QueryRequest(
        @Schema(description = "Código da organização", example = "ORG001")
        String code,
        @Schema(description = "Descrição da organização", example = "Organização exemplo")
        String description,
        @Schema(description = "Indica se a organização é do tipo free tier", example = "true")
        Boolean freeTier,
        @Schema(description = "Número da página para paginação", example = "0", requiredMode = RequiredMode.REQUIRED)
        @NotNull Integer page,
        @Schema(description = "Tamanho da página para paginação", example = "10", requiredMode = RequiredMode.REQUIRED)
        @NotNull Integer pageSize,
        @Schema(description = "Campo para ordenação dos resultados.", example = "id,asc;code,desc")
        String sortBy) {
    }

    @Schema(description = "Dados de uma organização")
    record Response(/* ... */) {
    }

    @Schema(description = "Requisição para criação de uma organização")
    record SaveRequest(/* ... */) {
    }

    @Schema(description = "Requisição para atualização de uma organização")
    record UpdateRequest(/* ... */) {
    }
}
```