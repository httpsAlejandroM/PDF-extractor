import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Main {

    private JFrame frame;
    private JPanel panel;
    private JTextField pathTextField;
    private JButton currentCopyButton; // Variable para el botón actualmente activo

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                Main window = new Main();
                window.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public Main() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("Extractor de Datos de PDF");
        frame.setBounds(100, 100, 600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        frame.setAlwaysOnTop(true);

        JPanel topPanel = new JPanel(new BorderLayout());
        JButton btnOpen = new JButton("Seleccionar PDF");
        topPanel.add(btnOpen, BorderLayout.WEST);

        pathTextField = new JTextField();
        topPanel.add(pathTextField, BorderLayout.CENTER);

        JButton btnLoad = new JButton("Buscar");
        topPanel.add(btnLoad, BorderLayout.EAST);

        frame.getContentPane().add(topPanel, BorderLayout.NORTH);

        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        frame.getContentPane().add(new JScrollPane(panel), BorderLayout.CENTER);

        // Panel para el botón limpiar
        JPanel bottomPanel = new JPanel();
        JButton btnClear = new JButton("Limpiar");
        bottomPanel.add(btnClear);
        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        // Drag and Drop
        frame.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
                    if (droppedFiles.size() > 0) {
                        File selectedFile = droppedFiles.get(0);
                        processPDF(selectedFile.getAbsolutePath());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Buttons
        btnOpen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser(System.getProperty("user.home") + File.separator + "Downloads");
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos PDF", "pdf"));
                int result = fileChooser.showOpenDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    pathTextField.setText(selectedFile.getAbsolutePath());
                    processPDF(selectedFile.getAbsolutePath());
                }
            }
        });

        btnLoad.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String filePath = pathTextField.getText();
                if (!filePath.isEmpty()) {
                    if (filePath.startsWith("file:///")) {
                        filePath = filePath.replace("file:///", "");
                    }
                    filePath = URLDecoder.decode(filePath, StandardCharsets.UTF_8);
                    processPDF(filePath);
                }
            }
        });

        btnClear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clearFields();
            }
        });
    }

    private void processPDF(String filePath) {
        String text = PDFExtractor.extractTextFromPDF(filePath);

        String personalDataSection = PDFExtractor.extractSection(text, "Datos personales del Cliente", "Datos de contactabilidad del Cliente");
        String contactDataSection = PDFExtractor.extractSection(text, "Datos de contactabilidad del Cliente", "Productos que la componen");

        String nombreApellido = PDFExtractor.extractField(personalDataSection, "Nombre y apellido");
        String dni = PDFExtractor.extractField(personalDataSection, "D.N.I.");
        String telefono = cleanPhoneNumber(PDFExtractor.extractField(contactDataSection, "Teléfono particular"));
        String email = PDFExtractor.extractField(contactDataSection, "E-mail");

        displayResults(nombreApellido, dni, telefono, email);
    }

    private void displayResults(String nombreApellido, String dni, String telefono, String email) {
        panel.removeAll();

        addFieldWithCopyButton("Nombre y apellido", nombreApellido);
        addFieldWithCopyButton("D.N.I.", dni);
        addFieldWithCopyButton("Teléfono particular", telefono);
        addFieldWithCopyButton("E-mail", email);

        panel.revalidate();
        panel.repaint();
    }

    private void addFieldWithCopyButton(String label, String value) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel fieldLabel = new JLabel(label + ": ");
        JTextField fieldValue = new JTextField(value, 20);
        fieldValue.setEditable(false);

        JButton copyButton = new JButton("Copiar");
        Dimension buttonSize = new Dimension(80, 25); // Tamaño fijo del botón
        copyButton.setPreferredSize(buttonSize);
        copyButton.setMaximumSize(buttonSize);
        copyButton.setMinimumSize(buttonSize);

        copyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                StringSelection stringSelection = new StringSelection(value);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);

                // Cambiar el estado de los botones
                if (currentCopyButton != null) {
                    currentCopyButton.setText("Copiar");
                }
                copyButton.setText("\u2713");
                currentCopyButton = copyButton;
            }
        });

        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        panel.add(fieldLabel, gbc);

        gbc.gridx = 1;
        panel.add(fieldValue, gbc);

        gbc.gridx = 2;
        panel.add(copyButton, gbc);
    }

    private void clearFields() {
        pathTextField.setText("");
        panel.removeAll();
        panel.revalidate();
        panel.repaint();
        currentCopyButton = null; // Restablecer el botón actual al limpiar
    }

    private String cleanPhoneNumber(String phoneNumber) {
        phoneNumber = phoneNumber.replaceFirst("^0+(?!$)", "");
        return phoneNumber;
    }
}
