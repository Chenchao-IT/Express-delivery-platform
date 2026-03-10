package com.campus.express.repository;

import com.campus.express.entity.VirtualShelf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VirtualShelfRepository extends JpaRepository<VirtualShelf, Long> {

    Optional<VirtualShelf> findByShelfCode(String shelfCode);

    List<VirtualShelf> findByStatus(VirtualShelf.ShelfStatus status);

    boolean existsByShelfCode(String shelfCode);
}
