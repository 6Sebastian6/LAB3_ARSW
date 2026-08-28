package edu.eci.arsw.blueprints.controllers;

import edu.eci.arsw.blueprints.dto.ApiResponse;
import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/blueprints")
@Tag(name = "Blueprints", description = "Operaciones CRUD sobre planos (blueprints)")
public class BlueprintsAPIController {

    private final BlueprintsServices services;

    public BlueprintsAPIController(BlueprintsServices services) { this.services = services; }

    // GET /blueprints 200 OK
    @Operation(
            summary = "Obtener todos los blueprints",
            description = "Retorna el conjunto completo de blueprints registrados en el sistema."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Consulta exitosa",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Set<Blueprint>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(services.getAllBlueprints()));
    }

    // GET /blueprints/{author} 200 o 404 Not Found
    @Operation(
            summary = "Obtener blueprints por autor",
            description = "Retorna todos los blueprints asociados a un autor especifico."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Blueprints encontrados",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "No existen blueprints para el autor indicado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/{author}")
    public ResponseEntity<ApiResponse<Set<Blueprint>>> byAuthor(
            @Parameter(description = "Nombre del autor", required = true, example = "john")
            @PathVariable String author) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(services.getBlueprintsByAuthor(author)));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    // GET /blueprints/{author}/{bpname} 200 ok o 404 Not Found
    @Operation(
            summary = "Obtener un blueprint por autor y nombre",
            description = "Retorna un blueprint especifico identificado por su autor y nombre."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Blueprint encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "El blueprint solicitado no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/{author}/{bpname}")
    public ResponseEntity<ApiResponse<Blueprint>> byAuthorAndName(
            @Parameter(description = "Nombre del autor", required = true, example = "john")
            @PathVariable String author,
            @Parameter(description = "Nombre del blueprint", required = true, example = "house-plan")
            @PathVariable String bpname) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(services.getBlueprint(author, bpname)));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    // POST /blueprints 201 si se crea o 400 Bad Request
    @Operation(
            summary = "Crear un nuevo blueprint",
            description = "Registra un nuevo blueprint junto con su lista inicial de puntos."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Blueprint creado exitosamente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Ya existe un blueprint con ese autor y nombre, o los datos son invalidos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Blueprint>> add(
            @Valid @RequestBody NewBlueprintRequest req) {
        try {
            Blueprint bp = new Blueprint(req.author(), req.name(), req.points());
            services.addNewBlueprint(bp);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(bp));
        } catch (BlueprintPersistenceException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        }
    }

    // PUT /blueprints/{author}/{bpname}/points 200 accepted o 404 Not Found
    @Operation(
            summary = "Agregar un punto a un blueprint existente",
            description = "Anade un nuevo punto (x, y) al final del blueprint indicado por autor y nombre."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "Punto agregado exitosamente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "El blueprint indicado no existe",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @PutMapping("/{author}/{bpname}/points")
    public ResponseEntity<ApiResponse<Point>> addPoint(
            @Parameter(description = "Nombre del autor", required = true, example = "john")
            @PathVariable String author,
            @Parameter(description = "Nombre del blueprint", required = true, example = "house-plan")
            @PathVariable String bpname,
            @RequestBody Point p) {
        try {
            services.addPoint(author, bpname, p.x(), p.y());
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.accepted(p));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    public record NewBlueprintRequest(
            @NotBlank String author,
            @NotBlank String name,
            @Valid java.util.List<Point> points
    ) { }
}