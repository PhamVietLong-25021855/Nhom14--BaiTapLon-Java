package userauth.gui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public final class NotificationUtil {
    private NotificationUtil() {
    }

    public static void success(Component parent, String title, String message) {
        show(parent, title, message, UITheme.SUCCESS);
    }

    public static void info(Component parent, String title, String message) {
        show(parent, title, message, UITheme.PRIMARY);
    }

    public static void warning(Component parent, String title, String message) {
        show(parent, title, message, UITheme.WARNING);
    }

    public static void error(Component parent, String title, String message) {
        show(parent, title, message, UITheme.DANGER);
    }

    public static boolean confirm(Component parent, String title, String message) {
        JPanel panel = createDepthPanel(title, message, UITheme.WARNING);
        int choice = JOptionPane.showConfirmDialog(
                parent,
                panel,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        return choice == JOptionPane.YES_OPTION;
    }

    public static String input(Component parent, String title, String message, String defaultValue) {
        JPanel panel = createDepthPanel(title, message, UITheme.PRIMARY);
        JTextField input = new JTextField(defaultValue == null ? "" : defaultValue, 20);
        UITheme.styleTextField(input);

        JPanel wrap = new JPanel(new BorderLayout(0, 12));
        wrap.setOpaque(false);
        wrap.add(panel, BorderLayout.CENTER);
        wrap.add(input, BorderLayout.SOUTH);

        int choice = JOptionPane.showConfirmDialog(
                parent,
                wrap,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (choice == JOptionPane.OK_OPTION) {
            return input.getText().trim();
        }
        return null;
    }

    private static void show(Component parent, String title, String message, Color accent) {
        JPanel panel = createDepthPanel(title, message, accent);
        JOptionPane.showMessageDialog(parent, panel, title, JOptionPane.PLAIN_MESSAGE);
    }

    private static JPanel createDepthPanel(String title, String message, Color accent) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(createDepthBorder(accent));

        JPanel bar = new GradientBar(accent);
        bar.setPreferredSize(new Dimension(320, 8));
        root.add(bar, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(Color.BLACK);

        JTextArea txt = new JTextArea(message);
        txt.setEditable(false);
        txt.setLineWrap(true);
        txt.setWrapStyleWord(true);
        txt.setOpaque(false);
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txt.setForeground(Color.BLACK);

        body.add(lblTitle, BorderLayout.NORTH);
        body.add(txt, BorderLayout.CENTER);
        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private static Border createDepthBorder(Color accent) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 4, 0, fade(accent, 0.35f)),
                        BorderFactory.createLineBorder(fade(accent, 0.6f), 1, true)
                ),
                BorderFactory.createEmptyBorder(0, 0, 2, 0)
        );
    }

    private static Color fade(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(alpha * 255));
    }

    private static class GradientBar extends JPanel {
        private final Color accent;

        private GradientBar(Color accent) {
            this.accent = accent;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(
                    0, 0, brighten(accent),
                    getWidth(), 0, accent
            );
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g2.dispose();
        }

        private Color brighten(Color color) {
            return new Color(
                    Math.min(255, color.getRed() + 35),
                    Math.min(255, color.getGreen() + 35),
                    Math.min(255, color.getBlue() + 35)
            );
        }
    }
}
