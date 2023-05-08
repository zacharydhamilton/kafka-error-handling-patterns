package com.example.pattern3producer.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class RedirectEntity {
    @Id
    private String id; 

    public RedirectEntity() {}
    public RedirectEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }
}
