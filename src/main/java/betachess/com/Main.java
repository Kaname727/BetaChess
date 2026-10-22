package betachess.com;

import betachess.com.GUI.ChessFrame;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

/**
 * Entry point of the BetaChess application.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        // FlatLaf must be installed before the first Swing component exists.
        FlatLightLaf.setup();
        System.setProperty("flatlaf.useWindowDecorations", "true");
        System.setProperty("flatlaf.menuBarEmbedded", "true");
        JFrame.setDefaultLookAndFeelDecorated(true);
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));


        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChessFrame().setVisible(true);
            }
        });
    }
}