package com.helpflow.ticket.domain.model;

import com.helpflow.ticket.domain.exception.BusinessRuleException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Technician {
    private Long id;
    private String code;
    private String document;
    private String name;
    private String email;
    private String location;
    private Set<Category> specialties = new HashSet<>();
    private TechnicianStatus status;
    private int maxActiveTickets;

    public Technician() {
    }

    public Technician(Long id, String code, String document, String name, String email,
                      String location, Set<Category> specialties, TechnicianStatus status, int maxActiveTickets) {
        validateCode(code);
        validateDocument(document);
        validateName(name);
        this.id = id;
        this.code = code.trim().toUpperCase();
        this.document = document.trim();
        this.name = name.trim();
        this.email = email != null ? email.trim().toLowerCase() : null;
        this.location = location != null ? location.trim() : null;
        if (specialties != null) {
            this.specialties.addAll(specialties);
        }
        this.status = status != null ? status : TechnicianStatus.ACTIVE;
        this.maxActiveTickets = maxActiveTickets > 0 ? maxActiveTickets : 5;
    }

    private void validateCode(String c) {
        if (c == null || c.trim().isEmpty()) {
            throw new BusinessRuleException("Technician code is mandatory (e.g. TEC-001)");
        }
    }

    private void validateDocument(String d) {
        if (d == null || d.trim().isEmpty()) {
            throw new BusinessRuleException("Technician document is mandatory");
        }
    }

    private void validateName(String n) {
        if (n == null || n.trim().isEmpty()) {
            throw new BusinessRuleException("Technician name is mandatory");
        }
    }

    public boolean isActive() {
        return this.status == TechnicianStatus.ACTIVE;
    }

    public void activate() {
        this.status = TechnicianStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = TechnicianStatus.INACTIVE;
    }

    public boolean hasSpecialty(Category category) {
        return category != null && this.specialties.contains(category);
    }

    public boolean isEligibleFor(Category category) {
        return isActive() && hasSpecialty(category);
    }

    public boolean canAcceptMoreTickets(int currentActiveTicketCount) {
        return currentActiveTicketCount < this.maxActiveTickets;
    }

    public void addSpecialty(Category category) {
        if (category != null) {
            this.specialties.add(category);
        }
    }

    public void removeSpecialty(Category category) {
        if (category != null) {
            this.specialties.remove(category);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Set<Category> getSpecialties() {
        return Collections.unmodifiableSet(specialties);
    }

    public void setSpecialties(Set<Category> specialties) {
        this.specialties = specialties != null ? new HashSet<>(specialties) : new HashSet<>();
    }

    public TechnicianStatus getStatus() {
        return status;
    }

    public void setStatus(TechnicianStatus status) {
        this.status = status;
    }

    public int getMaxActiveTickets() {
        return maxActiveTickets;
    }

    public void setMaxActiveTickets(int maxActiveTickets) {
        this.maxActiveTickets = maxActiveTickets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Technician that = (Technician) o;
        return Objects.equals(id, that.id) || (code != null && Objects.equals(code, that.code));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code);
    }
}
