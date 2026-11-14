package org.example;

public final class Fitzgerald {

    private Fitzgerald() {
    }

    public static String cor(int tipo) {
        return switch (tipo) {
            case 1 -> "#F5C85C";
            case 2 -> "#F59A4B";
            case 3 -> "#6FCB5B";
            case 4 -> "#5FA8EE";
            case 5 -> "#EE7FA0";
            default -> "#B7AFC4";
        };
    }


    public static String fundo(int tipo) {
        return switch (tipo) {
            case 1 -> "#FFF6D6";
            case 2 -> "#FFEBD6";
            case 3 -> "#E6F8DF";
            case 4 -> "#E1EFFD";
            case 5 -> "#FDE3EB";
            default -> "#F4F1F8";
        };
    }

    public static String nome(int tipo) {
        return switch (tipo) {
            case 1 -> "Pessoas e nomes";
            case 2 -> "Substantivos";
            case 3 -> "Verbos";
            case 4 -> "Descritivos";
            case 5 -> "Social";
            default -> "Outros";
        };
    }
}
