package userauth.utils;

public class ConsoleUI {
    public static final String RESET = "\033[0m";
    public static final String RED = "\033[0;31m";
    public static final String GREEN = "\033[0;32m";
    public static final String YELLOW = "\033[0;33m";
    public static final String BLUE = "\033[0;34m";
    public static final String CYAN = "\033[0;36m";
    public static final String BOLD_CYAN = "\033[1;36m";
    public static final String BOLD_YELLOW = "\033[1;33m";

    public static void clearScreen() {
        // Try clearing terminal
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static String spaces(int n) {
        if (n <= 0) return "";
        return String.format("%" + n + "s", "");
    }

    public static void printHeader(String title) {
        System.out.println(BOLD_CYAN);
        System.out.println("╔═══════════════════════════════════════════════════╗");
        int padding = (51 - title.length()) / 2;
        int remaining = 51 - title.length() - padding;
        System.out.printf("║%s%s%s║\n", spaces(padding), title, spaces(remaining));
        System.out.println("╚═══════════════════════════════════════════════════╝" + RESET);
    }

    public static void printMenu(String[] options) {
        System.out.println(CYAN + "╭───────────────────────────────────────────────────╮" + RESET);
        for (String option : options) {
            System.out.printf(CYAN + "│" + BOLD_YELLOW + " %-49s " + CYAN + "│\n" + RESET, option);
        }
        System.out.println(CYAN + "╰───────────────────────────────────────────────────╯" + RESET);
    }

    public static void printSuccess(String msg) {
        System.out.println(GREEN + "  [✔] " + msg + RESET);
    }

    public static void printError(String msg) {
        System.out.println(RED + "  [✘] " + msg + RESET);
    }

    public static void printWarning(String msg) {
        System.out.println(YELLOW + "  [!] " + msg + RESET);
    }

    public static void printInputPrompt(String msg) {
        System.out.print(BOLD_CYAN + " ➔ " + RESET + YELLOW + msg + RESET);
    }

    public static void printDivider() {
        System.out.println(CYAN + "  -------------------------------------------------" + RESET);
    }
}
