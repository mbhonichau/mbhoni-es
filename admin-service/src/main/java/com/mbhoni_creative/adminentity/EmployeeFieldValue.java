package com.mbhoni_creative.adminentity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_field_values", uniqueConstraints = {
    @UniqueConstraint(name = "uk_employee_field_def", columnNames = {"employee_id", "field_definition_id"})
})
public class EmployeeFieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "field_definition_id", nullable = false)
    private FieldDefinition fieldDefinition;

    @Column(columnDefinition = "TEXT")
    private String value; // cast by data_type on read

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }

    public FieldDefinition getFieldDefinition() { return fieldDefinition; }
    public void setFieldDefinition(FieldDefinition fieldDefinition) { this.fieldDefinition = fieldDefinition; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
