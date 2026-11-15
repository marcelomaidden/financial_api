package com.mfaraujo.financialapi.service.enums;

public enum AccountTypesEnum {

    BANK("BANK"),
    CREDIT("CREDIT");

    private final String type;

    AccountTypesEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
