package za.co.ernest.payment_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaystackInitResponse(
        boolean status,
        String message,
        PaystackData data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaystackData(
            @JsonProperty("authorization_url") String authorizationUrl,
            @JsonProperty("access_code") String accessCode,
            String reference
    ) {}
}
