package com.propertize.enums;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Custom deserializer for UserRoleEnum to allow tolerant parsing of role strings.
 */
public class UserRoleEnumDeserializer extends JsonDeserializer<UserRoleEnum> {

    @Override
    public UserRoleEnum deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null) return null;
        UserRoleEnum role = UserRoleEnum.fromString(text);
        // If still null, try normalized uppercase valueOf as a last resort
        if (role == null) {
            try {
                role = UserRoleEnum.valueOf(text.trim().toUpperCase().replace(' ', '_').replace('-', '_'));
            } catch (IllegalArgumentException ignored) {
                // return null to allow caller to handle missing/invalid role
                return null;
            }
        }
        return role;
    }
}

