import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Main extends JFrame {

    private Point initialClick; // Para arrastar o botão flutuante

    public Main() {
        // Configurações do "botão flutuante" (JFrame principal)
        setUndecorated(true); // Remove a barra de título
        setBackground(new Color(0, 0, 0, 0)); // Fundo totalmente transparente para que apenas o conteúdo apareça
        setSize(32, 32); // Tamanho pequeno do "botão"
        setLocationRelativeTo(null); // Centraliza na tela inicialmente
        setAlwaysOnTop(true); // Faz com que fique sempre visível por cima de outras janelas

        // Cria um painel para o conteúdo visual do botão
        JPanel buttonPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // Desenha um círculo com um ícone ou texto para representar o botão
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Desenha o círculo de fundo
                g2d.setColor(new Color(255, 0, 0, 100)); // Azul com transparência
                g2d.fillOval(0, 0, getWidth(), getHeight());
            }
        };
        buttonPanel.setOpaque(false); // Torna o painel transparente para ver o JFrame por baixo
        add(buttonPanel);

        // --- Listeners para arrastar o botão flutuante ---
        buttonPanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
                getComponentAt(initialClick);
            }

            public void mouseClicked(MouseEvent e) {
                // Ao clicar no botão, inicia o seletor de tela
                startScreenshotSelection();
            }
        });

        buttonPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // Move a janela do botão flutuante
                int thisX = getLocation().x;
                int thisY = getLocation().y;

                int xMoved = (thisX + e.getX()) - (thisX + initialClick.x);
                int yMoved = (thisY + e.getY()) - (thisY + initialClick.y);

                int X = thisX + xMoved;
                int Y = thisY + yMoved;
                setLocation(X, Y);
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    // --- Método para iniciar a seleção de tela ---
    private void startScreenshotSelection() {
        // Esconde o botão flutuante enquanto a seleção está ativa
        setVisible(false);

        // Cria e mostra a janela de seleção de área
        ScreenshotSelector selector = new ScreenshotSelector(this);
        // O ScreenshotSelector cuidará de reexibir o FloatingScreenshotButton quando terminar.
    }

    // --- CLASSE INTERNA para a lógica de seleção de área (quase a mesma da anterior) ---
    private class ScreenshotSelector extends JFrame {
        private Rectangle selectionRect;
        private Point startPoint;
        private JFrame parentButton; // Referência para o botão flutuante pai

        public ScreenshotSelector(JFrame parentButton) {
            this.parentButton = parentButton; // Salva a referência

            setUndecorated(true);
            setOpacity(0.3f);
            setBackground(new Color(0, 0, 0, 0));
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    startPoint = e.getPoint();
                    selectionRect = null;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (selectionRect != null) {
                        Point screenLocation = getLocationOnScreen();
                        int x = screenLocation.x + selectionRect.x;
                        int y = screenLocation.y + selectionRect.y;
                        int width = selectionRect.width;
                        int height = selectionRect.height;

                        Rectangle finalCaptureArea = new Rectangle(x, y, width, height);
                        copyScreenshotToClipboard(finalCaptureArea);
                    }
                    dispose(); // Fecha a janela de seleção
                    parentButton.setVisible(true); // Torna o botão flutuante visível novamente
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    int x = Math.min(startPoint.x, e.getX());
                    int y = Math.min(startPoint.y, e.getY());
                    int width = Math.abs(startPoint.x - e.getX());
                    int height = Math.abs(startPoint.y - e.getY());
                    selectionRect = new Rectangle(x, y, width, height);
                    repaint();
                }
            });

            // Não use setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE) aqui, pois fecharia todo o app
            setVisible(true);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (selectionRect != null) {
                Graphics2D g2d = (Graphics2D) g;

                // --- Nova Linha: Define a espessura da linha ---
                float espessuraLinha = 5.0f; // Altere este valor para a espessura desejada (ex: 1.0f, 2.0f, 5.0f)
                g2d.setStroke(new BasicStroke(espessuraLinha));
                // --- Fim da Nova Linha ---

                g2d.setColor(new Color(255, 0, 0, 255)); // Cor vermelha semi-transparente para a seleção
                g2d.drawRect(selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height);

            }
        }

        private void copyScreenshotToClipboard(Rectangle area) {
            try {
                Robot robot = new Robot();
                BufferedImage screenshot = robot.createScreenCapture(area);

                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

                Transferable transferableImage = new Transferable() {
                    @Override
                    public DataFlavor[] getTransferDataFlavors() {
                        return new DataFlavor[]{DataFlavor.imageFlavor};
                    }

                    @Override
                    public boolean isDataFlavorSupported(DataFlavor flavor) {
                        return DataFlavor.imageFlavor.equals(flavor);
                    }

                    @Override
                    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                        if (DataFlavor.imageFlavor.equals(flavor)) {
                            return screenshot;
                        }
                        throw new UnsupportedFlavorException(flavor);
                    }
                };

                clipboard.setContents(transferableImage, null);
                System.out.println("Print screen da área selecionada copiado para o clipboard!");

            } catch (AWTException ex) {
                System.err.println("Erro ao tirar o print: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}