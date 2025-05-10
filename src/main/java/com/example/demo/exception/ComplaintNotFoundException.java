package com.example.demo.exception;


public class ComplaintNotFoundException extends RuntimeException {

    public ComplaintNotFoundException(Long id) {
        super("Complaint not found with id: " + id);
    }
}