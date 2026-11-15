package com.mfaraujo.financialapi.service.enums;

public enum TransactionTypesEnum {

    CREDIT("CREDIT"),
    DEBIT("DEBIT");

    private final String type;

    TransactionTypesEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
