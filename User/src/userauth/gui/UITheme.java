package userauth.gui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.AbstractBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;

public final class UITheme {
    private UITheme() {
    }

    public static final Color APP_BG = new Color(226, 232, 240);
    public static final Color CARD_BG = new Color(248, 250, 252);
    public static final Color PRIMARY = new Color(79, 70, 229);
    public static final Color PRIMARY_DARK = new Color(124, 58, 237);
    public static final Color SUCCESS = new Color(16, 185, 129);
    public static final Color WARNING = new Color(245, 158, 11);
    public static final Color DANGER = new Color(239, 68, 68);
    public static final Color TEXT_PRIMARY = Color.BLACK;
    public static final Color TEXT_SECONDARY = Color.BLACK;
    public static final Color BORDER = new Color(148, 163, 184);
    public static final Color INPUT_BG = new Color(241, 245, 249);

    public static Font titleFont() {
        return new Font("Segoe UI", Font.BOLD, 26);
    }

    public static Font sectionTitleFont() {
        return new Font("Segoe UI", Font.BOLD, 21);
    }

    public static Font bodyFont() {
        return new Font("Segoe UI", Font.PLAIN, 14);
    }

    public static Font labelFont() {
        return new Font("Segoe UI", Font.BOLD, 13);
    }

    public static JPanel createCardLayoutPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(CARD_BG);
        panel.setBorder(new CompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 3, 0, new Color(148, 163, 184)),
                        BorderFactory.createLineBorder(new Color(148, 163, 184), 1, true)
                ),
                new EmptyBorder(20, 22, 20, 22)
        ));
        return panel;
    }

    public static JPanel createRoundedSection(String title, LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        panel.setBorder(new CompoundBorder(
                new RoundedShadowBorder(16),
                new EmptyBorder(16, 16, 16, 16)
        ));

        if (title != null && !title.isEmpty()) {
            JLabel sectionTitle = new JLabel(title);
            sectionTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
            sectionTitle.setForeground(Color.BLACK);
            sectionTitle.setBorder(new EmptyBorder(0, 0, 10, 0));
            panel.add(sectionTitle, BorderLayout.NORTH);
        }
        return panel;
    }

    public static Border inputBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)
        );
    }

    public static void styleTextField(JTextField field) {
        field.setFont(bodyFont());
        field.setBackground(INPUT_BG);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(inputBorder());
        field.setPreferredSize(new Dimension(220, 36));
    }

    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(bodyFont());
        comboBox.setBackground(INPUT_BG);
        comboBox.setForeground(TEXT_PRIMARY);
        comboBox.setBorder(inputBorder());
        comboBox.setPreferredSize(new Dimension(220, 36));
    }

    public static void stylePrimaryButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(PRIMARY);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
    }

    public static void styleSuccessButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(SUCCESS);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
    }

    public static void styleGhostButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setForeground(Color.BLACK);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    public static void styleSecondaryButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(new Color(148, 163, 184));
        button.setForeground(TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
    }

    public static void styleTable(JTable table) {
        table.setFont(bodyFont());
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(new Color(248, 250, 252));
        table.setRowHeight(30);
        table.setGridColor(new Color(148, 163, 184));
        table.setSelectionBackground(new Color(186, 230, 253));
        table.setSelectionForeground(TEXT_PRIMARY);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(203, 213, 225));
        header.setForeground(TEXT_PRIMARY);
        header.setReorderingAllowed(false);
    }

    private static class RoundedShadowBorder extends AbstractBorder {
        private final int radius;

        private RoundedShadowBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(6, 6, 10, 6);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = 6;
            insets.top = 6;
            insets.right = 6;
            insets.bottom = 10;
            return insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(15, 23, 42, 45));
            g2.fillRoundRect(x + 2, y + 4, width - 4, height - 6, radius, radius);

            g2.setColor(CARD_BG);
            g2.fillRoundRect(x, y, width - 4, height - 8, radius, radius);

            g2.setColor(new Color(148, 163, 184));
            g2.drawRoundRect(x, y, width - 4, height - 8, radius, radius);
            g2.dispose();
        }
    }
}
