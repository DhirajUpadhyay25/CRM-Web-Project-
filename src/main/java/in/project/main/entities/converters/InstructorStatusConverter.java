package in.project.main.entities.converters;

import in.project.main.entities.enums.InstructorStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class InstructorStatusConverter implements AttributeConverter<InstructorStatus, String> {

    @Override
    public String convertToDatabaseColumn(InstructorStatus attribute) {
        if (attribute == null) {
            return InstructorStatus.ACTIVE.name();
        }
        return attribute.name();
    }

    @Override
    public InstructorStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return InstructorStatus.ACTIVE;
        }
        String clean = dbData.trim().toUpperCase();
        try {
            return InstructorStatus.valueOf(clean);
        } catch (IllegalArgumentException e) {
            // Gracefully handle legacy database values (e.g. "Active", "Inactive", "Pending")
            if (clean.contains("ACT") && !clean.contains("INACT")) return InstructorStatus.ACTIVE;
            if (clean.contains("INACT")) return InstructorStatus.INACTIVE;
            if (clean.contains("PEND")) return InstructorStatus.PENDING;
            if (clean.contains("SUSP")) return InstructorStatus.SUSPENDED;
            if (clean.contains("BAN")) return InstructorStatus.BANNED;
            return InstructorStatus.ACTIVE;
        }
    }
}
