package com.familyshield.repository;

import com.familyshield.model.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChildRepository extends JpaRepository<Child, Long> {
    List<Child> findByParentId(Long parentId);
    Optional<Child> findByIdAndParentId(Long id, Long parentId);
}
