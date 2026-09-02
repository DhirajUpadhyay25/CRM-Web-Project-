package in.project.main.entities.converters;

import in.project.main.entities.enums.VerificationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class VerificationStatusConverter implements AttributeConverter<VerificationStatus, String> {

    @Override
    public String convertToDatabaseColumn(VerificationStatus attribute) {
        if (attribute == null) {
            return VerificationStatus.VERIFIED.name();
        }
        return attribute.name();
    }

    @Override
    public VerificationStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return VerificationStatus.VERIFIED;
        }
        String clean = dbData.trim().toUpperCase();
        try {
            return VerificationStatus.valueOf(clean);
        } catch (IllegalArgumentException e) {
            if (clean.contains("VERIF") && !clean.contains("UNVERIF")) return VerificationStatus.VERIFIED;
            if (clean.contains("PEND")) return VerificationStatus.PENDING;
            if (clean.contains("UNVERIF")) return VerificationStatus.UNVERIFIED;
            return VerificationStatus.VERIFIED;
        }
    }
}
