//package com.example.htql_nhahang_khachsan.entity;
//
//
//
//
//import java.util.HashSet;
//import java.util.Set;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//@Builder
//@AllArgsConstructor
//@NoArgsConstructor
//@Data
//@Entity
//@Table(name = "roles")
//public class Role {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(unique = true)
//    private String roleName;
//    private String description;
//
//    @ManyToMany(mappedBy = "roles")
//    private Set<UserEntity> users = new HashSet<>();
//}