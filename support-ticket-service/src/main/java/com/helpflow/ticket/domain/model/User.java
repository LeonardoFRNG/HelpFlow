package com.helpflow.ticket.domain.model;

import com.helpflow.ticket.domain.exception.BusinessRuleException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

public class User {
    private static final Pattern CORPORATE_EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@helpflow\\.com$");

    private Long id;
    private String document;
    private String name;
    private String corporateEmail;
    private String password;
    private String location;
    private String area;
    private Role role;
    private UserStatus status;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(Long id, String document, String name, String corporateEmail, String password,
                String location, String area, Role role, UserStatus status, LocalDateTime createdAt) {
        validateDocument(document);
        validateName(name);
        validateCorporateEmail(corporateEmail);
        this.id = id;
        this.document = document.trim();
        this.name = name.trim();
        this.corporateEmail = corporateEmail.trim().toLowerCase();
        this.password = password;
        this.location = location != null ? location.trim() : null;
        this.area = area != null ? area.trim() : null;
        this.role = role != null ? role : Role.ROLE_USUARIO;
        this.status = status != null ? status : UserStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static User createNew(String document, String name, String corporateEmail, String encodedPassword,
                                 String location, String area, Role role) {
        return new User(null, document, name, corporateEmail, encodedPassword,
                location, area, role, UserStatus.ACTIVE, LocalDateTime.now());
    }

    private void validateDocument(String doc) {
        if (doc == null || doc.trim().isEmpty()) {
            throw new BusinessRuleException("Document is mandatory and cannot be empty");
        }
    }

    private void validateName(String n) {
        if (n == null || n.trim().isEmpty()) {
            throw new BusinessRuleException("Name is mandatory and cannot be empty");
        }
    }

    private void validateCorporateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessRuleException("Corporate email is mandatory");
        }
        if (!CORPORATE_EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new BusinessRuleException("Corporate email must have a valid format (e.g. user@helpflow.com)");
        }
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    public void updateProfile(String newName, String newLocation, String newArea) {
        if (newName != null && !newName.trim().isEmpty()) {
            this.name = newName.trim();
        }
        if (newLocation != null) {
            this.location = newLocation.trim();
        }
        if (newArea != null) {
            this.area = newArea.trim();
        }
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCorporateEmail() {
        return corporateEmail;
    }

    public void setCorporateEmail(String corporateEmail) {
        this.corporateEmail = corporateEmail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) || (document != null && Objects.equals(document, user.document));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, document);
    }
}
