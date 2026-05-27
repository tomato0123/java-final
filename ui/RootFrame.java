package ui;

import javax.swing.*;
import java.awt.*;

public class RootFrame extends JFrame {
    private PetPanel petPanel;

    public RootFrame() {
        setAlwaysOnTop(true);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setSize(300, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        petPanel = new PetPanel(this);
        add(petPanel);
    }

    public void updatePetState(String state, String message) {
        petPanel.setState(state, message);
    }
}