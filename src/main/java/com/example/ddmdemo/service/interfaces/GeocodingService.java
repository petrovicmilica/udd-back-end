package com.example.ddmdemo.service.interfaces;

import org.springframework.stereotype.Service;

@Service
public interface GeocodingService {
    double[] getCoordinates(String location);
}
