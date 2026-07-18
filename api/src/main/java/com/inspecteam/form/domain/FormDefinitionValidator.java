package com.inspecteam.form.domain;

import tools.jackson.databind.JsonNode;
import com.inspecteam.shared.exception.ApiException;
import java.util.HashSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class FormDefinitionValidator {

    private static final Set<String> TYPES = Set.of(
            "text", "textarea", "number", "date", "time", "select", "multiselect",
            "checkbox", "photo", "signature", "heading", "instructions");

    public void validate(JsonNode definition) {
        if (definition == null || !definition.isObject() || !definition.path("sections").isArray()) {
            invalid("A definição deve ser um objeto contendo o array sections");
        }
        Set<String> sectionIds = new HashSet<>();
        Set<String> fieldIds = new HashSet<>();
        for (JsonNode section : definition.path("sections")) {
            String sectionId = requiredText(section, "id");
            if (!sectionIds.add(sectionId)) {
                invalid("ID de seção duplicado: " + sectionId);
            }
            if (!section.path("fields").isArray()) {
                invalid("Cada seção deve possuir um array fields");
            }
            for (JsonNode field : section.path("fields")) {
                String fieldId = requiredText(field, "id");
                String type = requiredText(field, "type");
                if (!fieldIds.add(fieldId)) {
                    invalid("ID de campo duplicado: " + fieldId);
                }
                if (!TYPES.contains(type)) {
                    invalid("Tipo de campo não suportado: " + type);
                }
            }
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText("").trim();
        if (value.isEmpty()) {
            invalid("Campo obrigatório ausente na definição: " + field);
        }
        return value;
    }

    private void invalid(String message) {
        throw new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
