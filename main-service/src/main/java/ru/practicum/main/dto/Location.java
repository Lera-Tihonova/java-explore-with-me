package ru.practicum.main.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class Location {

    private Float lat;
    private Float lon;

    @JsonCreator
    public Location(@JsonProperty("lat") Float lat,
                    @JsonProperty("lon") Float lon) {
        this.lat = lat;
        this.lon = lon;
    }
}