package com.mfaraujo.financialapi.service.enums;

public enum CurrencyEnum {
    BRL("BRL");

    private final String type;

    CurrencyEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}