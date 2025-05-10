package com.example.demo.service;

import com.example.demo.exception.ComplaintNotFoundException;
import com.example.demo.model.Complaint;
import com.example.demo.repository.ComplaintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComplaintService {
    private final ComplaintRepository complaintRepository;
    private final GeolocationService geolocationService;

    public ComplaintService(ComplaintRepository complaintRepository, GeolocationService geolocationService) {
        this.complaintRepository = complaintRepository;
        this.geolocationService = geolocationService;
    }

    @Transactional
    public Complaint addOrIncrement(String productId, String content, String reporter, String ip) {
        return complaintRepository.findByProductIdAndReporter(productId, reporter)
                .map(existing -> {
                    existing.setCounter(existing.getCounter() + 1);
                    return complaintRepository.save(existing);
                })
                .orElseGet(() -> {
                    Complaint c = Complaint.builder()
                            .productId(productId)
                            .content(content)
                            .createdAt(LocalDateTime.now())
                            .reporter(reporter)
                            .country(geolocationService.getCountryByIp(ip))
                            .counter(1)
                            .build();
                    return complaintRepository.save(c);
                });
    }

    @Transactional
    public Complaint updateContent(Long id, String newContent) {
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new ComplaintNotFoundException(id));
        c.setContent(newContent);
        return complaintRepository.save(c);
    }

    public List<Complaint> findAll() {
        return complaintRepository.findAll();
    }

    public Complaint findById(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ComplaintNotFoundException(id));
    }
}
