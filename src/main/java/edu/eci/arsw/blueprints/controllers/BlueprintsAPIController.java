package edu.eci.arsw.blueprints.controllers;

import edu.eci.arsw.blueprints.dto.ApiResponse;
import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/blueprints")
public class BlueprintsAPIController {

    private final BlueprintsServices services;

    public BlueprintsAPIController(BlueprintsServices services) { this.services = services; }

    // GET /blueprints 200 OK
    @GetMapping
    public ResponseEntity<ApiResponse<Set<Blueprint>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(services.getAllBlueprints()));
    }

    // GET /blueprints/{author} 200 o 404 Not Found
    @GetMapping("/{author}")
    public ResponseEntity<ApiResponse<Set<Blueprint>>> byAuthor(@PathVariable String author) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(services.getBlueprintsByAuthor(author)));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    // GET /blueprints/{author}/{bpname} 200 ok o 404 Not Found
    @GetMapping("/{author}/{bpname}")
    public ResponseEntity<ApiResponse<Blueprint>> byAuthorAndName(
            @PathVariable String author,
            @PathVariable String bpname) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(services.getBlueprint(author, bpname)));
        } catch (BlueprintNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        }
    }

    // POST /blueprints 201 si se crea o 400 Bad Request
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
    @PutMapping("/{author}/{bpname}/points")
    public ResponseEntity<ApiResponse<Point>> addPoint(
            @PathVariable String author,
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
