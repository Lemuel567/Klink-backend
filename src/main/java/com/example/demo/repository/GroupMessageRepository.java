package com.example.demo.repository;

import com.example.demo.model.GroupMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, UUID> {

    List<GroupMessage> findByChurchIdAndGroupIdOrderByCreatedAtAsc(UUID churchId, UUID groupId);

    Page<GroupMessage> findByChurchIdAndGroupIdOrderByCreatedAtAsc(UUID churchId, UUID groupId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM GroupMessage gm WHERE gm.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
