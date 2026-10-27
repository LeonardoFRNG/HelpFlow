-- Initial Seed Data
-- Users: Password for all is Password123!
INSERT INTO users (document, name, corporate_email, password, location, area, role, status, created_at)
VALUES 
('DOC-ADMIN-01', 'System Administrator', 'admin@helpflow.com', '$2a$10$ZXzb/s4c9dX1Gpu.f6s/OOy8eZqXuw6luv139Q16ZKjeOlaQPI1m2', 'Sede Principal', 'Tecnología', 'ROLE_ADMIN', 'ACTIVE', CURRENT_TIMESTAMP),
('DOC-USER-01', 'Juan Pérez', 'usuario@helpflow.com', '$2a$10$ZXzb/s4c9dX1Gpu.f6s/OOy8eZqXuw6luv139Q16ZKjeOlaQPI1m2', 'Sede Norte', 'Finanzas', 'ROLE_USUARIO', 'ACTIVE', CURRENT_TIMESTAMP),
('DOC-TECH-01', 'Carlos Técnico', 'tecnico@helpflow.com', '$2a$10$ZXzb/s4c9dX1Gpu.f6s/OOy8eZqXuw6luv139Q16ZKjeOlaQPI1m2', 'Sede Principal', 'Soporte TI', 'ROLE_TECNICO', 'ACTIVE', CURRENT_TIMESTAMP);

-- Technicians (TEC-001 through TEC-020)
INSERT INTO technicians (code, document, name, email, location, status, max_active_tickets) VALUES
('TEC-001', 'DOC-T001', 'Mateo Morales', 'mateo.morales@helpflow.com', 'Sede Principal', 'ACTIVE', 5),
('TEC-002', 'DOC-T002', 'Sofia Vergara', 'sofia.vergara@helpflow.com', 'Sede Principal', 'ACTIVE', 5),
('TEC-003', 'DOC-T003', 'Diego Castro', 'diego.castro@helpflow.com', 'Sede Norte', 'ACTIVE', 5),
('TEC-004', 'DOC-T004', 'Valentina Ruiz', 'valentina.ruiz@helpflow.com', 'Sede Norte', 'ACTIVE', 5),
('TEC-005', 'DOC-T005', 'Santiago Gomez', 'santiago.gomez@helpflow.com', 'Sede Sur', 'ACTIVE', 5),
('TEC-006', 'DOC-T006', 'Mariana Torres', 'mariana.torres@helpflow.com', 'Sede Sur', 'ACTIVE', 5),
('TEC-007', 'DOC-T007', 'Andres Lopez', 'andres.lopez@helpflow.com', 'Sede Occidente', 'ACTIVE', 5),
('TEC-008', 'DOC-T008', 'Camila Diaz', 'camila.diaz@helpflow.com', 'Sede Occidente', 'ACTIVE', 5),
('TEC-009', 'DOC-T009', 'Sebastian Ortiz', 'sebastian.ortiz@helpflow.com', 'Sede Principal', 'ACTIVE', 5),
('TEC-010', 'DOC-T010', 'Paula Mendoza', 'paula.mendoza@helpflow.com', 'Sede Norte', 'ACTIVE', 5),
('TEC-011', 'DOC-T011', 'Lucas Silva', 'lucas.silva@helpflow.com', 'Sede Sur', 'ACTIVE', 5),
('TEC-012', 'DOC-T012', 'Isabella Rios', 'isabella.rios@helpflow.com', 'Sede Principal', 'ACTIVE', 5),
('TEC-013', 'DOC-T013', 'Daniel Herrera', 'daniel.herrera@helpflow.com', 'Sede Occidente', 'ACTIVE', 5),
('TEC-014', 'DOC-T014', 'Carlos Técnico', 'tecnico@helpflow.com', 'Sede Principal', 'ACTIVE', 5),
('TEC-015', 'DOC-T015', 'Gabriela Vargas', 'gabriela.vargas@helpflow.com', 'Sede Norte', 'ACTIVE', 5),
('TEC-016', 'DOC-T016', 'Nicolas Romero', 'nicolas.romero@helpflow.com', 'Sede Sur', 'ACTIVE', 5),
('TEC-017', 'DOC-T017', 'Elena Cardenas', 'elena.cardenas@helpflow.com', 'Sede Principal', 'ACTIVE', 5),
('TEC-018', 'DOC-T018', 'Felipe Guerrero', 'felipe.guerrero@helpflow.com', 'Sede Occidente', 'ACTIVE', 5),
('TEC-019', 'DOC-T019', 'Luciana Vega', 'luciana.vega@helpflow.com', 'Sede Norte', 'ACTIVE', 5),
('TEC-020', 'DOC-T020', 'Julian Navas', 'julian.navas@helpflow.com', 'Sede Sur', 'ACTIVE', 5);

-- Specialties mapping
INSERT INTO technician_specialties (technician_id, specialty) VALUES
(1, 'HARDWARE'), (1, 'NETWORK'),
(2, 'HARDWARE'), (2, 'ACCESS'),
(3, 'SOFTWARE'), (3, 'ACCESS'),
(4, 'SOFTWARE'), (4, 'OTHER'),
(5, 'NETWORK'), (5, 'HARDWARE'),
(6, 'NETWORK'), (6, 'SOFTWARE'),
(7, 'ACCESS'), (7, 'OTHER'),
(8, 'ACCESS'), (8, 'HARDWARE'),
(9, 'SOFTWARE'), (9, 'NETWORK'),
(10, 'OTHER'), (10, 'HARDWARE'),
(11, 'HARDWARE'), (11, 'NETWORK'),
(12, 'SOFTWARE'), (12, 'ACCESS'),
(13, 'NETWORK'), (13, 'OTHER'),
(14, 'HARDWARE'), (14, 'NETWORK'), (14, 'SOFTWARE'), (14, 'ACCESS'), (14, 'OTHER'),
(15, 'HARDWARE'), (15, 'ACCESS'),
(16, 'SOFTWARE'), (16, 'NETWORK'),
(17, 'ACCESS'), (17, 'SOFTWARE'),
(18, 'NETWORK'), (18, 'HARDWARE'),
(19, 'OTHER'), (19, 'ACCESS'),
(20, 'SOFTWARE'), (20, 'OTHER');
