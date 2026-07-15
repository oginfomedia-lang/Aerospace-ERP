package com.aerospace.procurement_service.repository;

import com.aerospace.procurement_service.entity.DebitNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DebitNoteRepository extends JpaRepository<DebitNote, Long> {
}
