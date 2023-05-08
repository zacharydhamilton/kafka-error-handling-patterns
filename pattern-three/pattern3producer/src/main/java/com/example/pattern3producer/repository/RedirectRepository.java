package com.example.pattern3producer.repository;

import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface RedirectRepository extends JpaRepository<RedirectEntity, String> {
    // Nothing to do here
}
