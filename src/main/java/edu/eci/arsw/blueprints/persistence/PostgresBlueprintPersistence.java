package edu.eci.arsw.blueprints.persistence;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.entity.BlueprintEntity;
import edu.eci.arsw.blueprints.persistence.entity.BlueprintJpaRepository;
import edu.eci.arsw.blueprints.persistence.entity.PointEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

// Hace que este inyecte en vez de InMemoryBlueprintPersistence en cualquier lugar donde pida la interfaz sin tener
//que tocar el Service ni el controller
@Repository
@Primary
public class PostgresBlueprintPersistence implements BlueprintPersistence {

    private final BlueprintJpaRepository repo;

    public PostgresBlueprintPersistence(BlueprintJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        if (repo.existsByAuthorAndName(bp.getAuthor(), bp.getName())) {
            throw new BlueprintPersistenceException(
                    "Blueprint already exists: " + bp.getAuthor() + ":" + bp.getName());
        }
        BlueprintEntity entity = toEntity(bp);
        repo.save(entity);
    }

    @Override
    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        BlueprintEntity entity = repo.findByAuthorAndName(author, name)
                .orElseThrow(() -> new BlueprintNotFoundException(
                        "Blueprint not found: %s/%s".formatted(author, name)));
        return toDomain(entity);
    }

    @Override
    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException {
        var entities = repo.findByAuthor(author);
        if (entities.isEmpty()) {
            throw new BlueprintNotFoundException("No blueprints for author: " + author);
        }
        return entities.stream().map(this::toDomain).collect(Collectors.toSet());
    }

    @Override
    public Set<Blueprint> getAllBlueprints() {
        return repo.findAll().stream().map(this::toDomain).collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public void addPoint(String author, String name, int x, int y) throws BlueprintNotFoundException {
        BlueprintEntity entity = repo.findByAuthorAndName(author, name)
                .orElseThrow(() -> new BlueprintNotFoundException(
                        "Blueprint not found: %s/%s".formatted(author, name)));
        entity.addPoint(new PointEntity(x, y));
        repo.save(entity);
    }

    //Mapeo entre modelo de dominio y JPA

    private BlueprintEntity toEntity(Blueprint bp) {
        BlueprintEntity entity = new BlueprintEntity(bp.getAuthor(), bp.getName());
        for (Point p : bp.getPoints()) {
            entity.addPoint(new PointEntity(p.x(), p.y()));
        }
        return entity;
    }

    private Blueprint toDomain(BlueprintEntity entity) {
        var points = entity.getPoints().stream()
                .map(pe -> new Point(pe.getX(), pe.getY()))
                .toList();
        return new Blueprint(entity.getAuthor(), entity.getName(), points);
    }
}